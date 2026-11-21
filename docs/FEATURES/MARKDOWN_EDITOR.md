# Editor Markdown de QUIPU

## Formato almacenado

El contenido de las notas se sigue guardando como texto Markdown en la columna
`content` de `notes`. No se convierte a HTML ni a un modelo intermedio y no se
añadieron columnas a la tabla `notes`.

La base de datos pasa a la versión 2 para incorporar el índice FTS y las
etiquetas, pero el formato de almacenamiento del Markdown no cambia. Las notas
anteriores siguen siendo válidas: el texto plano se interpreta como Markdown sin
formato.

## Sintaxis admitida

- Párrafos con saltos de línea.
- Encabezados `#`, `##` y `###`.
- Negrita `**texto**`.
- Cursiva `*texto*`.
- Código inline con acentos graves.
- Listas desordenadas `- elemento`.
- Listas ordenadas `1. elemento`.
- Casillas `- [ ] pendiente` y `- [x] completada`.
- Enlaces `[texto](https://ejemplo.com)`.
- Bloques de código delimitados por tres acentos graves, con lenguaje opcional.

No se admite HTML, imágenes externas, scripts, tablas, citas complejas, listas
anidadas ni extensiones Markdown avanzadas.

La sintaxis incompleta se conserva como texto literal. Los enlaces solo se
reconocen si la URL es absoluta y usa HTTP o HTTPS.

## Formato visual en tiempo real

Mientras se edita, el campo de contenido aplica una `VisualTransformation`
propia. La transformación es exclusivamente visual: el `TextFieldValue` y el
Markdown persistido conservan todos los caracteres originales.

- `**texto**` se muestra como **texto** en negrita.
- `*texto*` se muestra en cursiva.
- `#`, `##` y `###` ocultan el marcador y aumentan la jerarquía tipográfica.
- Las listas muestran `•` o el número correspondiente.
- `[ ]` y `[x]` se representan con casillas `☐` y `☑`.
- Los enlaces muestran solo el texto visible, con color primario y subrayado.
- El código inline usa tipografía monoespaciada y un fondo discreto.
- Los bloques de código mantienen sus delimitadores y usan fondo diferenciado.

`MarkdownVisualPlanner` calcula en una sola pasada los rangos ocultos, los
estilos y el mapeo de posiciones. `MarkdownVisualTransformation` traduce ese
plan a `AnnotatedString` y `OffsetMapping`, de modo que el cursor y la selección
siguen apuntando al texto Markdown real. La sintaxis incompleta nunca se elimina.

## Barra de herramientas

La barra es una superficie inferior acoplada al teclado. Solo aparece cuando se
cumplen las tres condiciones:

1. El teclado virtual está visible.
2. El campo de contenido tiene el foco.
3. La pantalla está en modo edición.

No se muestra con el teclado cerrado, cuando solo el título tiene el foco, en la
vista previa completa ni fuera del editor. Respeta `WindowInsets.ime` y
`WindowInsets.navigationBars`; el contenido conserva su desplazamiento y la barra
nunca tapa el texto.

Las acciones disponibles son negrita, cursiva, encabezado, listas, casilla,
enlace y bloque de código. El menú de encabezados mantiene H1, H2 y H3. En
pantallas estrechas la barra se desplaza horizontalmente sin recortar opciones.
Cada acción conserva el foco y la posición del cursor.

Las transformaciones se aplican sobre la selección actual:

- Con selección, los estilos inline envuelven el texto.
- Sin selección, se insertan los delimitadores y el cursor queda en el interior.
- Los encabezados y listas trabajan al inicio de las líneas seleccionadas.
- Las listas repetidas no duplican prefijos.
- Las casillas se insertan como pendientes.
- Los bloques de código envuelven la selección o crean un bloque vacío.
- Los enlaces validan el destino antes de insertarse y conservan la selección
  original al abrir el diálogo.

## Selector Editar / Vista previa

El selector se muestra cuando el teclado está cerrado. Al entrar en vista previa
se oculta el teclado y se retira el foco; al volver a edición se recupera el foco
y el cursor. Mientras el teclado está visible el selector se oculta para dejar
espacio a la barra de formato.

Cambiar de modo no modifica el contenido ni `updatedAt`. Los cambios pendientes
siguen el mismo flujo de autoguardado.

## Vista previa

La vista previa usa el mismo texto persistido. Interpreta encabezados, estilos
inline, listas, casillas, enlaces y bloques de código con tipografía monospace.
No ejecuta HTML ni carga recursos remotos.

Las casillas de la vista previa se pueden marcar o desmarcar. El cambio
modifica únicamente el marcador `[ ]` / `[x]` de la línea correspondiente y
pasa por `onContentChange`, por lo que conserva el debounce, el estado de
guardado y SavedStateHandle.

Los enlaces se abren con el navegador externo mediante `LocalUriHandler`.
Solamente se permiten URLs HTTP y HTTPS; los errores de apertura no cierran la
aplicación.

## Autoguardado

Todas las modificaciones, incluidas las de la barra, las casillas y las
etiquetas, actualizan el contenido mediante `onContentChange` o el flujo de
persistencia existente. Se mantienen:

- debounce de 600 ms,
- `SavedStateHandle`,
- mutex de escritura,
- estado `NoteSaveStatus`,
- guardado al salir y al pasar a segundo plano,
- reintento ante errores.

La transformación visual nunca escribe en Room ni altera el Markdown guardado.

## Etiquetas

El menú de opciones del editor permite abrir **Etiquetas**. El diálogo muestra
las etiquetas actuales como chips, permite retirarlas y añadir o reutilizar
etiquetas existentes. Las etiquetas se normalizan sin distinguir mayúsculas ni
tildes y se guardan en las tablas `note_tags` y `note_tag_cross_ref`.

Las etiquetas de una nota se muestran bajo el título durante la edición y en la
vista previa. La búsqueda FTS las indexa junto con título y contenido. Cambiar
etiquetas no modifica `updatedAt`.

## Extractos

`Note.preview` se calcula con el mismo parser, eliminando sintaxis Markdown y
mostrando únicamente el texto visible. Los encabezados y elementos de lista se
separan con espacios; los enlaces aportan su texto visible y los bloques de
código su contenido. La búsqueda usa el índice FTS sobre el contenido Markdown
original persistido.

## Limitaciones

- No hay resaltado sintáctico complejo.
- No se admiten listas anidadas ni tablas.
- No hay imágenes ni contenido HTML.
- La vista previa no edita el texto, salvo las casillas de verificación.
- El lenguaje del bloque de código es opcional y no activa resaltado.
- Ocultar delimitadores puede requerir editar cerca del borde del formato; el
  contenido original siempre se conserva.
