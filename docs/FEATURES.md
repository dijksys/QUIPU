# Funcionalidades

Estado de la Fase 1 de QUIPU.

## Implementado

### Notas

- [x] Crear notas con ID UUID.
- [x] Editar título y contenido.
- [x] Guardado automático con debounce de 600 ms.
- [x] Volcado inmediato al salir y borrador en `SavedStateHandle`.
- [x] Fijar y desfijar notas.
- [x] Archivar y desarchivar.
- [x] Mover a papelera con opción de deshacer.
- [x] Restaurar notas eliminadas.
- [x] Eliminar definitivamente y vaciar la papelera con confirmación.
- [x] Buscar por título, contenido y etiquetas con FTS4.
- [x] Búsqueda sin distinción de tildes o mayúsculas y coincidencias por prefijo.
- [x] Filtros por estado y fecha, y ordenación por relevancia, fecha o título.
- [x] Resaltado de coincidencias y extractos Markdown limpios.
- [x] Etiquetas normalizadas reutilizables desde el editor.
- [x] Editor único con formato visual en tiempo real, sin selector de vista previa.
- [x] Barra acoplada al teclado y estable al aplicar encabezados.
- [x] Detección automática de URLs y apertura en el navegador externo.
- [x] Fechas de creación y última modificación visibles.
- [x] Listados de activas, archivadas y papelera.

### Recordatorios

- [x] Título y descripción opcional.
- [x] Fecha y hora con selectores Material 3.
- [x] Asociación opcional a una nota activa.
- [x] Edición y reprogramación.
- [x] Completar, cancelar y eliminar.
- [x] Estados pendiente, vencido, completado y cancelado.
- [x] Notificaciones locales con canal propio.
- [x] Permiso de notificaciones con explicación y acceso a ajustes.
- [x] Permiso de alarmas exactas con alternativa aproximada explicada.
- [x] Reprogramación tras reinicio, cambio de hora/zona y actualización.
- [x] Prevención de notificaciones duplicadas.
- [x] Apertura del recordatorio al pulsar la notificación.

### Inicio

- [x] Buscador prominente que abre Notas con la consulta.
- [x] Resumen de notas y recordatorios pendientes.
- [x] Notas fijadas.
- [x] Notas recientes.
- [x] Próximos recordatorios.
- [x] Accesos rápidos para crear nota o recordatorio.

### Ajustes

- [x] Tema claro, oscuro y automático.
- [x] Interruptor global de notificaciones.
- [x] Estado y solicitud de permisos de notificaciones y alarmas exactas.
- [x] Información de versión, privacidad y funcionamiento sin conexión.

### Calidad

- [x] Arquitectura MVVM + Repository + casos de uso.
- [x] Hilt, KSP, Room, DataStore y Navigation Compose.
- [x] Pruebas unitarias de casos de uso y ViewModels.
- [x] Pruebas instrumentadas de DAOs.
- [x] Pruebas instrumentadas de búsqueda FTS, etiquetas y migración 1 → 2.
- [x] Pruebas instrumentadas de estados vacíos.
- [x] Pruebas de flujo de usuario de nota, búsqueda y recordatorio.
- [x] Prueba de integración de alarma y notificación en emulador.
- [x] `lintDebug` sin errores.
- [x] Documentación de arquitectura y permisos.

## Pendiente para fases posteriores

- [ ] Recordatorios recurrentes (diarios, semanales, personalizados).
- [ ] Exportación e importación de datos.
- [ ] Copia de seguridad en la nube opcional.
- [ ] Widgets de pantalla de inicio.
- [ ] Adjuntos e imágenes.
- [ ] Notas colaborativas o multiusuario.
- [ ] Localización a otros idiomas.
- [ ] Sincronización entre dispositivos.
- [ ] Posponer recordatorios desde la notificación.
- [ ] Pruebas de accesibilidad automatizadas.

## Limitaciones actuales

- Las alarmas exactas dependen del permiso del sistema y del fabricante.
- Si el usuario deniega las notificaciones, los recordatorios se guardan pero no
  generan avisos.
- La eliminación definitiva de una nota desvincula sus recordatorios, no los
  elimina.
- La papelera no tiene purga automática por antigüedad.
- La interfaz está en español y no se han añadido traducciones.
- FTS4 no ofrece BM25; la relevancia usa prioridades deterministas.
- No hay búsqueda difusa, comodines internos ni subcadenas arbitrarias.
- El editor no admite WYSIWYG total, HTML, listas anidadas ni imágenes.
