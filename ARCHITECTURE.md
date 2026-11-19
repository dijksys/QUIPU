# Arquitectura de QUIPU

## Visión general

QUIPU sigue una arquitectura por capas con separación estricta de
responsabilidades, orientada a funcionalidades y con un único módulo Gradle.

```
UI (Compose)
   ↓
ViewModel (StateFlow inmutable)
   ↓
Casos de uso
   ↓
Repositorios (interfaces de dominio)
   ↓
Fuentes de datos (Room / DataStore)
```

Las pantallas nunca consultan Room, DataStore ni APIs de Android directamente.
Los ViewModels no conocen implementaciones concretas de Room. Room es la fuente
única de verdad para notas y recordatorios.

## Capas y paquetes

### `core`

Infraestructura transversal:

- `database`: `AppDatabase`, configuración Room y módulo Hilt.
- `datastore`: preferencias tipadas de tema y notificaciones.
- `alarms`: abstracción de alarmas, implementación Android, `ReminderReceiver` y
  `BootReceiver`.
- `notifications`: canal, interfaz `ReminderNotifier` e implementación Android.
- `permissions`: comprobación y solicitud aislada de permisos desde Compose.
- `navigation`: rutas, grafo de navegación y barra inferior.
- `designsystem`: colores, tipografía, formas, espaciados y componentes
  reutilizables.
- `common`: `Clock`, `AppResult`, utilidades de fecha/hora.

La capa `core` no contiene reglas de negocio de notas ni recordatorios.

### `domain`

Modelos puros (`Note`, `Reminder`, `ReminderStatus`, `AppSettings`,
`ThemeMode`), contratos de repositorio y casos de uso. No depende de Room ni de
clases Android.

Casos de uso principales:

- Notas: crear, actualizar, observar, buscar, fijar, archivar/desarchivar,
  mover a papelera, restaurar, borrar definitivamente y vaciar papelera.
- Recordatorios: crear, actualizar, eliminar, completar, cancelar, observar,
  reprogramar y gestionar el disparo de una alarma.
- Ajustes: observar y modificar tema y notificaciones.

### `data`

- Entidades Room (`NoteEntity`, `ReminderEntity`).
- DAOs con `Flow` para observación y funciones `suspend` para escritura.
- Mappers entre entidad y modelo de dominio.
- Implementaciones de repositorio e implementación de ajustes sobre DataStore.

### `feature`

Pantallas Compose, ViewModels y estados de UI. Cada ViewModel expone un
`StateFlow` inmutable. Las acciones de escritura se ejecutan en `viewModelScope`;
los datos se observan con `Flow` y `stateIn(WhileSubscribed(5_000))`.

### `di`

- `RepositoryModule`: enlaces de repositorios y abstracciones de plataforma.
- `PlatformModule`: `Clock` y `CoroutineScope` de aplicación.
- `core/database/DatabaseModule`: base de datos y DAOs.

Los casos de uso usan inyección por constructor; no se creó un
`UseCaseModule` vacío.

## Persistencia

### Room

Base de datos: `quipu.db`, versión 1, `exportSchema = true`.
El esquema exportado está en
`app/schemas/io.github.dijksys.quipu.core.database.AppDatabase/1.json`.

**Notas** (`notes`):

- PK `id` (UUID).
- `createdAt`, `updatedAt`, `isPinned`, `archivedAt`, `deletedAt`.
- Índices para listas activas, archivadas, fijadas y orden por actualización.

**Recordatorios** (`reminders`):

- PK `id` (UUID).
- FK `noteId → notes.id` con `ON DELETE SET NULL`.
- `scheduledAt`, `createdAt`, `updatedAt`, `completedAt`, `notifiedAt`, `status`.
- Índices por `status`/`scheduledAt`, `scheduledAt`, `completedAt` y `noteId`.

Decisiones:

- La eliminación de notas es lógica (`deletedAt`) salvo al vaciar la papelera o
  borrar definitivamente.
- Al borrar una nota definitivamente, el recordatorio **no se elimina**: queda
  desvinculado mediante la clave foránea. La app nunca usa
  `fallbackToDestructiveMigration()`.
- Las migraciones serán explícitas en `AppDatabase.MIGRATIONS`; la versión 1 no
  necesita migración.
- Al consultar recordatorios vencidos se usa una marca de tiempo periódica en el
  ViewModel, porque el paso del tiempo no genera cambios en la base de datos.

### DataStore

`AppSettingsDataStore` persiste `themeMode` y `notificationsEnabled` mediante
Preferences DataStore. Incluye un manejador de corrupción que reemplaza el
archivo por preferencias vacías. Los modelos de dominio nunca exponen claves de
Preferences.

## Ciclo de vida de recordatorios

1. El editor valida título y fecha futura.
2. El caso de uso guarda el recordatorio en Room **antes** de programar.
3. Si las notificaciones están activas, `AndroidReminderAlarmScheduler` programa
   una alarma:
   - `setExactAndAllowWhileIdle` si `canScheduleExactAlarms()` es verdadero.
   - `setAndAllowWhileIdle` como alternativa explícita si no hay permiso.
4. Cada recordatorio usa un `PendingIntent` estable: `requestCode = 0` y un data
   URI `quipu://reminder/<id>`, evitando colisiones y permitiendo cancelar.
5. Al editar se cancela la alarma anterior, se actualiza Room y se programa de
   nuevo. Al completar, cancelar o eliminar se cancela la alarma y se retira la
   notificación.
6. `ReminderReceiver` recibe la alarma y ejecuta `HandleReminderAlarmUseCase`,
   que consulta el recordatorio persistido. Solo notifica si sigue `PENDING`, no
   fue notificado antes para ese horario y las notificaciones están activas.
7. Tras publicar la notificación, se guarda `notifiedAt` para evitar duplicados.
8. `BootReceiver` escucha `BOOT_COMPLETED`, cambios de hora/zona,
   actualización del paquete y la concesión del permiso de alarmas exactas para
   reprogramar pendientes futuros.
9. La notificación incluye un `PendingIntent` con data URI propia. Al pulsarla,
   `MainActivity` abre el editor del recordatorio correspondiente.

Si un recordatorio está vencido, permanece pendiente en la lista de vencidos y
no se dispara durante la reprogramación para evitar notificaciones tardías
inesperadas.

## Decisiones técnicas relevantes

- **Un solo módulo Gradle** en esta fase: no existe todavía una necesidad
  técnica demostrable para separar módulos.
- **AGP 9 con Kotlin integrado** y **KSP 2.3.14**, necesario para compatibilidad
  con AGP 9/Hilt/Room.
- **Hilt** para todas las dependencias. No se usan singletons manuales ni
  `GlobalScope`; los receivers usan un `CoroutineScope` de aplicación inyectado y
  `goAsync()`.
- **AlarmManager** en lugar de WorkManager porque el requisito es una
  notificación a una hora concreta.
- **Guardado automático** con debounce de 600 ms y escritura forzada al salir.
  El borrador se conserva además en `SavedStateHandle`.
- **DataStore** separado de Room: las preferencias no son datos de negocio.
- **Diseño visual** con Material 3 y tokens propios; sin color dinámico para
  garantizar consistencia entre dispositivos.
- **Formatos de fecha en español** mediante locale explícito, coherente con la
  interfaz.
- **Errores**: las operaciones de escritura se envuelven en `runCatching` y los
  flujos degradan a estados vacíos con snackbar, evitando cierres inesperados.
- **R8 en modo completo** para Release. Se descartó el `packageScope`
  experimental porque provocaba un `IllegalAccessError` con clases internas de
  secuencias de Kotlin; el APK Release se probó en dispositivo tras el cambio.
- **Firma Release** con un keystore JKS externo, leído desde
  `keystore.properties` (ignorado por Git). Debug no depende del material de
  distribución y Release falla explícitamente si la firma no es válida.
- **Esquema Room histórico**: se conserva el esquema exportado bajo el paquete
  anterior y se añade el nuevo bajo
  `io.github.dijksys.quipu.core.database.AppDatabase`, sin cambiar la versión de
  base de datos ni las tablas.

## Pruebas

- **Unitarias**: casos de uso de notas y recordatorios, `NotesViewModel`,
  `NoteEditorViewModel`, `RemindersViewModel`, `ReminderEditorViewModel` y
  `SettingsViewModel`. Usan fakes de repositorios, scheduler, notifier y reloj.
- **Instrumentadas**:
  - `NoteDaoTest` y `ReminderDaoTest` (Room en memoria, índices, filtros,
    búsqueda y FK `SET_NULL`).
  - `UiEmptyStatesTest` (estados vacíos de notas, búsqueda y recordatorios).
  - `UserFlowTest` (crear nota, recrear Activity y recuperar la nota; crear
    recordatorio y encontrarlo en la lista).
  - `ReminderAlarmIntegrationTest` (AlarmManager real, receiver, notificación y
    persistencia de `notifiedAt` en emulador).
- **Lint** con `:app:lintDebug` sin errores.

Estado de la suite: 50 pruebas unitarias y 19 instrumentadas, ejecutadas en un
emulador Pixel 7 con Android API 37.
