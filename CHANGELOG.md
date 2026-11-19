# Changelog

Todas las fechas están en formato ISO 8601.

## [1.0.0] - 2026-11-19

### Añadido

- Primera versión funcional de QUIPU para Android.
- Gestión completa de notas: creación, edición, guardado automático, fijado,
  archivado, papelera, restauración, borrado permanente y búsqueda.
- Editor de notas con debounce, volcado al salir y borrador en
  `SavedStateHandle`.
- Gestión de recordatorios: crear, editar, completar, cancelar, eliminar,
  vincular a una nota y consultar estado.
- Alarmas locales con AlarmManager, alternativa aproximada sin permiso de
  alarmas exactas, notificaciones locales y apertura directa del recordatorio.
- Reprogramación de alarmas tras reinicio, cambios de hora/zona, actualización
  de la app y concesión del permiso de alarmas exactas.
- Pantallas de Inicio, Notas, Editor de notas, Recordatorios, Editor de
  recordatorios y Ajustes.
- Tema claro, oscuro y automático persistido en DataStore.
- Activación/desactivación de notificaciones con cancelación y reprogramación
  coherente de alarmas.
- Sistema de diseño propio sobre Material 3: colores, tipografía, formas,
  espaciados, componentes, estados vacíos, snackbars y diálogos.
- Persistencia Room con índices, claves foráneas y esquema exportado.
- Pruebas unitarias, pruebas instrumentadas de DAOs, interfaz, flujos de
  usuario e integración real de alarmas/notificaciones.
- Documentación inicial: README, arquitectura, funcionalidades y este changelog.

### Verificación

- 50 pruebas unitarias y 19 instrumentadas aprobadas.
- Emulador Pixel 7 con Android API 37.
- `:app:assembleDebug`, `:app:assembleRelease` y `:app:lintDebug` sin errores.
- Verificación manual de persistencia y reprogramación tras reiniciar el
  emulador.

### Decisiones

- Un único módulo Gradle en esta fase.
- Room como fuente única de verdad; DataStore solo para preferencias.
- Recordatorios independientes de las notas; `ON DELETE SET NULL` al borrar una
  nota definitivamente.
- Sin `fallbackToDestructiveMigration()`.
- Sin WorkManager para alarmas exactas.

### Limitaciones conocidas

- Recordatorios sin recurrencia.
- Interfaz únicamente en español.
- Búsqueda mediante `LIKE`, sin índice FTS.
- Sin sincronización ni cuentas.
