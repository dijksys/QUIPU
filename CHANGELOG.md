# Changelog

Todas las fechas están en formato ISO 8601.

## [Unreleased]

### Añadido

- Motor de búsqueda FTS4 con tokenizador `unicode61` y eliminación de
  diacríticos sobre título, contenido Markdown y etiquetas.
- Buscador prominente en Inicio que abre Notas con la consulta conservada.
- Filtros por fecha, ordenación por relevancia, fecha o título y resaltado de
  coincidencias.
- Sistema normalizado de etiquetas con chips en el editor y búsqueda indexada.
- Migración explícita de Room 1 → 2 que conserva todas las notas existentes.

### Mejorado

- Editor de notas con sintaxis Markdown: negritas, cursivas, encabezados,
  listas, casillas, enlaces y bloques de código.
- Barra de herramientas acoplada sobre el teclado, visible solo con el contenido
  enfocado y en modo edición.
- Formato visual en tiempo real que oculta delimitadores sin alterar el Markdown
  persistido.
- Vista previa Markdown con casillas interactivas y enlaces externos seguros.
- Extractos de notas sin sintaxis Markdown en Inicio y en el listado.

### Verificación

- 147 pruebas unitarias y 56 instrumentadas aprobadas en el Motorola Moto G34
  5G (Android 15, API 35).
- `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:assembleDebug` y la suite instrumentada completa.
- Migración 1 → 2 validada con notas existentes y relleno del índice FTS.

## [1.2.0] - 2026-11-20

### Mejorado

- El selector de apariencia de Ajustes muestra únicamente los iconos de Claro,
  Oscuro y Automático, centrados en píldoras de 56 dp y sin etiquetas visibles.
- Se conservan las descripciones accesibles, la semántica `RadioButton`, el
  estado de selección y los destinos de cada modo.
- El icono oficial del launcher reduce su composición al 82 % para dejar un
  margen blanco equilibrado dentro de las máscaras de Android.
- Se regeneraron el foreground, el monocromático y las variantes heredadas en
  las cinco densidades con la nueva escala.

### Verificación

- 81 pruebas unitarias y 39 instrumentadas aprobadas.
- `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:assembleDebug` y `:app:assembleRelease`.
- Validación manual del selector en tema claro y oscuro.
- Validación del icono en el launcher con máscara circular y comparación con
  aplicaciones del sistema.

### Limitaciones conocidas

- Recordatorios sin recurrencia.
- Interfaz únicamente en español.
- Búsqueda mediante `LIKE`, sin índice FTS.
- Sin sincronización ni cuentas.

## [1.1.7] - 2026-11-20

### Añadido

- Selector horizontal de apariencia con tres píldoras independientes para
  Claro, Oscuro y Automático.
- Tarjetas independientes para el estado de notificaciones y los permisos de
  notificaciones y alarmas exactas.
- Tarjeta de identidad de QUIPU con el logo oficial actualizado en Acerca de.
- Tarjetas independientes para el perfil del desarrollador y el repositorio.
- Nuevo recurso `quipu_logo.png` para la bienvenida y la pantalla de Ajustes.

### Mejorado

- Reorganización visual completa de Ajustes con superficies redondeadas,
  bordes sutiles y espaciados coherentes.
- La sección Proyecto queda separada de la tarjeta Acerca de y utiliza su
  propio encabezado.
- Los permisos muestran insignias de estado y acciones contextuales sin alterar
  la lógica de solicitud ni la programación de alarmas.
- Todos los recursos del launcher, adaptativos, monocromáticos y heredados se
  regeneraron desde el nuevo logo oficial.
- Se retiraron las referencias al logo inclinado anterior.

### Verificación

- 81 pruebas unitarias y 39 instrumentadas aprobadas.
- `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:assembleDebug` y `:app:assembleRelease`.
- Validación manual en emulador Pixel 7 con Android API 37 en tema claro,
  oscuro, pantalla compacta, escala de texto 1.3 y actualización desde
  QUIPU 1.0.9.

### Limitaciones conocidas

- Recordatorios sin recurrencia.
- Interfaz únicamente en español.
- Búsqueda mediante `LIKE`, sin índice FTS.
- Sin sincronización ni cuentas.

## [1.0.9] - 2026-11-19

### Añadido

- Pantalla de bienvenida que aparece únicamente en el primer inicio de una
  instalación nueva de QUIPU.
- Presentación de la identidad de QUIPU y de tres funcionalidades reales:
  notas, recordatorios y privacidad local.
- Indicador persistente `onboarding_completed` en el DataStore existente.
- Detección compatible con instalaciones anteriores mediante la base de datos
  Room, el archivo de preferencias y los tiempos de instalación/actualización
  del paquete.
- Casos de uso para resolver, observar y completar el onboarding.

### Mejorado

- La navegación principal permanece oculta hasta que se resuelve la bienvenida,
  evitando mostrar Inicio antes de tiempo.
- La bienvenida conserva una notificación pendiente y abre el recordatorio
  correspondiente después de pulsar Comenzar.
- La pantalla se adapta a temas claro y oscuro, tamaños de pantalla pequeños,
  escalas de texto e insets del sistema.
- Los errores de lectura o escritura mantienen la pantalla visible y permiten
  reintentar sin acceder prematuramente a Inicio.
- Los usuarios que actualizan desde 1.0.0 o 1.0.3 no vuelven a ver la
  bienvenida.

### Verificación

- 81 pruebas unitarias y 30 instrumentadas aprobadas.
- `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:assembleDebug` y `:app:assembleRelease`.
- Validación manual de instalación limpia, primer inicio, cierre antes de
  completar, temas claro/oscuro, pantalla compacta, recreación y actualización
  desde QUIPU 1.0.3 conservando datos.
- Validación manual en emulador Pixel 7 con Android API 37.

### Limitaciones conocidas

- Recordatorios sin recurrencia.
- Interfaz únicamente en español.
- Búsqueda mediante `LIKE`, sin índice FTS.
- Sin sincronización ni cuentas.

## [1.0.3] - 2026-11-19

### Añadido

- Enlaces al perfil del desarrollador y al repositorio de QUIPU dentro de
  Ajustes → Acerca de, con apertura en el navegador externo.
- Nuevo encabezado principal de Inicio: «Organiza tu día.».
- Estado de guardado explícito en el editor de notas: vacío, cambios sin
  guardar, guardando, guardado y error, con acción de reintento.
- Metadatos claros de creación y última actualización en el editor.

### Mejorado

- Jerarquía tipográfica del encabezado de Inicio: fecha como etiqueta
  secundaria, título principal destacado y descripción de apoyo.
- Navegación inferior rediseñada como una cápsula flotante.
- El editor conserva la sesión «Nueva nota» después del primer autoguardado y
  separa el título contextual del estado de persistencia.
- La sección Acerca de mantiene la información de privacidad y funcionamiento
  sin conexión, ahora acompañada de la información del proyecto.

### Corregido

- Salir, pasar a segundo plano o reabrir una nota sin cambios ya no actualiza
  `updatedAt` ni realiza escrituras innecesarias.
- Las ediciones realizadas durante un guardado activo permanecen pendientes y
  se persisten en una escritura posterior.
- Un error de guardado conserva el borrador y permite reintentar sin cerrar el
  editor.

### Verificación

- 60 pruebas unitarias y 21 instrumentadas aprobadas.
- `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`,
  `:app:assembleDebug` y `:app:assembleRelease`.
- Validación manual en emulador Pixel 7 con Android API 37 en tema claro y
  oscuro, actualización desde QUIPU 1.0.0 y apertura de los enlaces externos.
- Actualización instalada sobre QUIPU 1.0.0 conservando los datos existentes.

### Limitaciones conocidas

- Recordatorios sin recurrencia.
- Interfaz únicamente en español.
- Búsqueda mediante `LIKE`, sin índice FTS.
- Sin sincronización ni cuentas.

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
