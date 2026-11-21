# QUIPU

Aplicación Android nativa para gestionar notas personales y recordatorios. Está
diseñada para funcionar **completamente sin conexión**, con almacenamiento local
y una interfaz minimalista en español.

QUIPU 2.0.1 está implementado: CRUD de notas con guardado automático, papelera,
archivado, búsqueda FTS con etiquetas, recordatorios con alarmas locales,
notificaciones, reprogramación tras reinicios, ajustes de tema y pruebas
unitarias e instrumentadas. El editor aplica formato Markdown en tiempo real,
detecta URLs automáticamente y acopla su barra al teclado.

En una instalación nueva, QUIPU muestra una pantalla de bienvenida y no vuelve
a mostrarla después de pulsar Comenzar. Las instalaciones actualizadas desde
versiones anteriores conservan sus datos y omiten la bienvenida.

## Identidad de la aplicación

- **Application ID:** `io.github.dijksys.quipu`
- **Versión estable:** `2.0.1`
- **Nombre visible:** QUIPU
- **Android mínimo:** 8.0 (API 26)

## Stack

- Kotlin 2.2.10
- Jetpack Compose + Material 3 personalizado
- MVVM + Repository Pattern + casos de uso
- Coroutines y Flow
- Room 2.8.5 como fuente única de verdad
- Preferences DataStore 1.3.0
- Navigation Compose 2.10.2
- Hilt 2.60.1 + KSP 2.3.14
- AlarmManager y NotificationManager nativos
- JUnit 4, Kotlin Coroutines Test y pruebas instrumentadas

Versiones centralizadas en `gradle/libs.versions.toml`. El proyecto usa AGP
9.5.1 con soporte Kotlin integrado, Gradle 9.6.0 y un único módulo Gradle.

## Requisitos

- Android Studio reciente (soporte para AGP 9).
- JDK 17 o superior (el entorno de desarrollo usado utilizó el JBR incluido en
  Android Studio).
- Android SDK con `compileSdk 37`.
- Dispositivo o emulador con Android 8.0 (API 26) o superior.

`local.properties` contiene la ruta local del SDK y no debe versionarse.

## Ejecutar

```powershell
# Compilar e instalar en un dispositivo/emulador conectado
.\gradlew.bat :app:installDebug

# Solo compilar el APK debug
.\gradlew.bat :app:assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

### Pruebas y análisis

```powershell
# Pruebas unitarias (casos de uso y ViewModels)
.\gradlew.bat :app:testDebugUnitTest

# Pruebas instrumentadas: DAOs, interfaz y flujo completo
.\gradlew.bat :app:connectedDebugAndroidTest

# Análisis estático
.\gradlew.bat :app:lintDebug
```

Las pruebas instrumentadas requieren un emulador o dispositivo conectado
(`adb devices`). La prueba de integración de alarmas tarda aproximadamente un
minuto porque espera el disparo real de AlarmManager y la publicación de la
notificación.

Estado verificado en QUIPU 2.0.1: **174 pruebas unitarias** y **62 pruebas
instrumentadas** en un Motorola moto g34 5G con Android 15 (API 35), todas en
verde. `lintDebug` no reporta errores. También se verificaron manualmente la
persistencia, la búsqueda FTS, el editor y la reprogramación de alarmas.

## Permisos

| Permiso | Uso | Comportamiento si falta |
|---|---|---|
| `POST_NOTIFICATIONS` | Mostrar avisos de recordatorios (Android 13+) | Los recordatorios se guardan, pero no se muestran avisos. La app explica cómo concederlo. |
| `SCHEDULE_EXACT_ALARM` | Alarmas exactas (Android 12+) | Se usa programación aproximada con `setAndAllowWhileIdle`; la interfaz informa de la limitación. |
| `RECEIVE_BOOT_COMPLETED` | Reprogramar recordatorios tras reiniciar | No aplica: sin permiso Android no entrega el evento de arranque. |

Los permisos se solicitan desde Recordatorios, el editor de recordatorios y
Ajustes, con explicación previa. También se ofrecen accesos directos a los
ajustes del sistema.

## Estructura

```
app/src/main/java/io/github/dijksys/quipu/
  core/        base de datos, DataStore, navegación, alarmas, notificaciones,
               permisos, diseño visual y utilidades comunes
  domain/      modelos puros, contratos de repositorio y casos de uso
  data/        entidades Room, DAOs, mappers e implementaciones de repositorio
  feature/     pantallas y ViewModels de inicio, notas, recordatorios y ajustes
  di/          módulos Hilt de repositorios y plataforma
```

Los detalles de dependencias, persistencia y ciclo de vida de las alarmas están
en [ARCHITECTURE.md](ARCHITECTURE.md). Las funcionalidades y tareas pendientes
están en [docs/FEATURES.md](docs/FEATURES.md).

## Funcionalidades principales

- Notas con guardado automático (debounce de 600 ms + volcado al salir).
- Crear, editar, fijar, archivar, mover a papelera, restaurar y borrar
  definitivamente.
- Búsqueda FTS por título, contenido y etiquetas, con filtros de fecha,
  ordenación y resaltado de coincidencias.
- Buscador prominente en Inicio que abre Notas con la consulta.
- Fechas de creación y última modificación visibles.
- Recordatorios con título, descripción, fecha, hora y nota vinculada opcional.
- Notificación local con apertura directa del recordatorio.
- Reprogramación tras reinicio, cambio de hora/zona y actualización de la app.
- Temas claro, oscuro y automático persistidos.
- Activación/desactivación global de notificaciones con cancelación y
  reprogramación de alarmas.
- Interfaz accesible con estados vacíos, snackbars y confirmaciones destructivas.

## Limitaciones conocidas

- La interfaz está en español; las fechas se formatean con locale español.
- No hay recurrencia de recordatorios ni sincronización en la nube.
- La búsqueda FTS no ofrece búsqueda difusa ni subcadenas internas.
- La ordenación por título usa `COLLATE NOCASE` de SQLite (cobertura ASCII).
- El permiso de alarmas exactas depende del fabricante y de la versión de
  Android. Si no se concede, las notificaciones pueden retrasarse.
- Al borrar definitivamente una nota, sus recordatorios se conservan y quedan
  sin nota vinculada (política `ON DELETE SET NULL`).
- Si el usuario desactiva las notificaciones, las alarmas se cancelan; los
  recordatorios siguen guardados y se reprograman al reactivarlas.

## Documentación

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [CHANGELOG.md](CHANGELOG.md)
- [docs/FEATURES.md](docs/FEATURES.md)
- [docs/FEATURES/ADVANCED_SEARCH.md](docs/FEATURES/ADVANCED_SEARCH.md)
- [docs/FEATURES/MARKDOWN_EDITOR.md](docs/FEATURES/MARKDOWN_EDITOR.md)
- [docs/DEVELOPMENT/GITHUB_ACTIONS.md](docs/DEVELOPMENT/GITHUB_ACTIONS.md)
- [docs/BRANDING.md](docs/BRANDING.md)
- [docs/releases/v2.0.1.md](docs/releases/v2.0.1.md)
- [docs/releases/v1.2.0.md](docs/releases/v1.2.0.md)
- [docs/releases/v1.1.7.md](docs/releases/v1.1.7.md)
- [docs/releases/v1.0.9.md](docs/releases/v1.0.9.md)
- [docs/releases/v1.0.3.md](docs/releases/v1.0.3.md)
- [docs/releases/v1.0.0.md](docs/releases/v1.0.0.md)

## Licencia

Este proyecto se distribuye bajo la licencia MIT. Consulta [LICENSE](LICENSE)
para más detalles.
