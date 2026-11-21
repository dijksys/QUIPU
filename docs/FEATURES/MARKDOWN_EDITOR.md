# Editor Markdown de QUIPU

## Formato almacenado

El contenido de las notas se guarda como texto Markdown sin HTML ni estructuras
intermedias. No hay columnas nuevas en Room ni cambios en la versión de la base
de datos. Las notas anteriores siguen siendo válidas: el texto plano se
interpreta como Markdown sin formato.

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

## Barra de herramientas

Debajo del título aparecen las acciones de negrita, cursiva, encabezado, listas,
casilla, enlace y bloque de código. En pantallas estrechas la barra se desplaza
horizontalmente. El control **Editar / Vista previa** es independiente.

Las transformaciones se aplican sobre la selección actual:

- Con selección, los estilos inline envuelven el texto.
- Sin selección, se insertan los delimitadores y el cursor queda en el interior.
- Los encabezados y listas trabajan al inicio de las líneas seleccionadas.
- Las listas repetidas no duplican prefijos.
- Las casillas se insertan como pendientes.
- Los bloques de código envuelven la selección o crean un bloque vacío.
- Los enlaces validan el destino antes de insertarse.

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

## Extractos

`Note.preview` se calcula con el mismo parser, eliminando sintaxis Markdown y
mostrando únicamente el texto visible. Los encabezados y elementos de lista se
separan con espacios; los enlaces aportan su texto visible y los bloques de
código su contenido. La búsqueda continúa usando el contenido Markdown original
persistido.

## Limitaciones

- No hay resaltado sintáctico complejo.
- No se admiten listas anidadas ni tablas.
- No hay imágenes ni contenido HTML.
- La vista previa no edita el texto, salvo las casillas de verificación.
- El lenguaje del bloque de código es opcional y no activa resaltado.
