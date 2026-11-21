# Editor Markdown de QUIPU

## Una sola superficie de edición

El editor no tiene selector **Editar / Vista previa**. El usuario escribe y ve el
formato en el mismo campo, en tiempo real. El contenido sigue siendo un único
`TextFieldValue` y el Markdown original se conserva íntegro.

El campo de contenido usa `BasicTextField` con una transformación visual y un
desplazamiento propio. Esto permite:

- mantener el cursor y la selección sobre el texto fuente;
- desplazar notas largas sin depender del scroll interno del campo;
- conocer el `TextLayoutResult` para resolver pulsaciones sobre enlaces.

## Formato almacenado

El contenido se guarda como texto Markdown en la columna `content` de `notes`.
No se convierte a HTML, no se guarda el resultado de la transformación visual y
no se añadieron columnas nuevas para esta tarea.

## Sintaxis admitida

- Párrafos con saltos de línea.
- Encabezados `#`, `##` y `###`.
- Negrita `**texto**`.
- Cursiva `*texto*`.
- Código inline con acentos graves.
- Listas desordenadas `- elemento`.
- Listas ordenadas `1. elemento`.
- Casillas `- [ ] pendiente` y `- [x] completada`.
- Enlaces Markdown `[texto](https://ejemplo.com)`.
- URLs automáticas con o sin esquema.
- Bloques de código delimitados por tres acentos graves, con lenguaje opcional.

No se admite HTML, imágenes externas, scripts, tablas, citas complejas, listas
anidadas ni extensiones Markdown avanzadas.

## Formato visual en tiempo real

`MarkdownVisualPlanner` calcula en una pasada los rangos que se ocultan, los
estilos y el mapeo de posiciones. `MarkdownVisualTransformation` lo traduce a
`AnnotatedString` y `OffsetMapping`.

- `**texto**` se muestra como **texto** en negrita.
- `*texto*` se muestra en cursiva.
- `#`, `##` y `###` ocultan el marcador y aplican jerarquía tipográfica.
- Las listas muestran `•` o el número correspondiente.
- `[ ]` y `[x]` se representan con casillas `☐` y `☑`.
- Los enlaces Markdown muestran solo el texto visible, con color primario y
  subrayado.
- Las URLs automáticas se muestran con el mismo estilo de enlace, sin añadir
  `https://` visualmente.
- El código inline usa tipografía monoespaciada y un fondo discreto.
- Los bloques de código conservan sus delimitadores y usan fondo diferenciado.

Las casillas son interactivas directamente en el campo. `MarkdownVisualPlan`
expone el rango visual de cada `☐`/`☑`, su estado y el índice de línea del
Markdown original. Un toque corto sobre la casilla ejecuta
`MarkdownChecklist.toggleItem()`, actualiza el `TextFieldValue`, refresca la
representación y pasa por `onContentChange` para el autoguardado. Un toque sobre
el texto, un arrastre o una pulsación larga no cambian la casilla.

La sintaxis incompleta se conserva literalmente. Los delimitadores no se
eliminan del texto persistido; solo se ocultan visualmente cuando la estructura
está completa. Editar cerca de un delimitador no altera el contenido fuente.

## Barra de herramientas

La barra es una superficie inferior acoplada al teclado. Solo aparece cuando:

1. El teclado virtual está visible.
2. El campo de contenido tiene el foco.

No se muestra con el teclado cerrado, cuando el título tiene el foco, al abrir
la pantalla o durante un diálogo. Se apoya en `WindowInsets.isImeVisible`,
`imePadding` y la navegación del sistema.

Acciones disponibles:

- Negrita.
- Cursiva.
- Encabezados H1, H2 y H3.
- Lista desordenada.
- Lista numerada.
- Casilla.
- Bloque de código.

No existe acción manual para insertar enlaces: los enlaces se detectan
automáticamente.

El menú de encabezados usa un popup no enfocable para no robar el foco al campo
de texto. `applyContent()` solo recupera el foco si realmente se perdió. No se
oculta el teclado ni se cambia de modo al aplicar formato. En el Moto G34 5G se
comprobó que Gboard permanece visible y el contenido conserva el foco al elegir
H1, H2 o H3.

## Detección automática de URLs

`AutoLinkDetector` es una implementación Kotlin pura y determinista. Es la única
fuente de validación de URLs para el parser, el planner visual y el editor.

### Formatos admitidos

- `https://platform.deepseek.com/usage`
- `http://example.com`
- `platform.deepseek.com`
- `www.example.com`
- `docs.example.com`
- `example.com/path`
- `example.com/search?q=quipu#results`
- `example.com:8080/path`
- `[QUIPU](https://github.com/dijksys/QUIPU)`
- `[https://platform.deepseek.com/usage](https://platform.deepseek.com/usage)`

### Normalización

- Los esquemas admitidos son únicamente `http` y `https`.
- El esquema se normaliza a minúsculas para abrir el navegador.
- Un dominio sin esquema se abre como `https://dominio`.
- El texto visible y el Markdown persistido no se modifican.

### Validaciones de dominios

- Al menos un punto real.
- Etiquetas no vacías y sin puntos consecutivos.
- Sin punto inicial ni final.
- Etiquetas de 1 a 63 caracteres.
- Sin guiones al inicio o al final.
- Validación IDN con `java.net.IDN` cuando corresponde.
- Sufijo superior controlado por una lista de gTLD y ccTLD.
- Puerto entre 1 y 65535.
- Sin espacios, correos electrónicos ni esquemas inseguros.

### Casos rechazados

- `javascript:alert(1)`
- `file:///storage/file`
- `intent://...`
- `content://...`
- `data:...`
- `usuario@example.com`
- `palabra...otra`
- `hola.mundo`
- `texto..com`
- `.example.com`
- `example.`
- `example.1`
- `example.c`
- `version.1.2.3`
- `example.com@evil.com`
- `https://exa mple.com`

### Puntuación

La puntuación de cierre no forma parte del destino:

```text
Consulta platform.deepseek.com.
```

El enlace termina antes del punto final. Se manejan comas, punto y coma, signos
de interrogación, comillas y paréntesis no balanceados. Los paréntesis
balanceados dentro de una ruta se conservan.

### Código

No se detectan enlaces dentro de bloques de código ni de código inline. Estos
contenidos se conservan literalmente.

## Apertura de enlaces

En el campo editable, una pulsación corta sobre una URL detectada abre el
navegador externo mediante `LocalUriHandler.openUri()`. La interacción se
resuelve con `TextLayoutResult`:

- se identifica el desplazamiento pulsado;
- se comprueba que cae dentro de un rango de enlace;
- se comprueba que la pulsación está en la línea del texto;
- una pulsación larga o un arrastre no abren el enlace;
- los toques sobre texto normal siguen colocando el cursor;
- la selección y el arrastre siguen funcionando.

Solo se abren destinos HTTP y HTTPS. No se realizan solicitudes HTTP, no se
descargan recursos y no se añadió el permiso `INTERNET`.

Los enlaces Markdown existentes siguen funcionando y no se convierten a otro
formato. Si un enlace Markdown es inválido, sus delimitadores se muestran de
forma literal.

## Autoguardado

Todas las modificaciones pasan por `viewModel.onContentChange()`:

- escritura normal;
- negrita y cursiva;
- encabezados;
- listas;
- casillas;
- bloques de código;
- texto pegado, incluidas URLs.

Se conservan:

- debounce de 600 ms;
- `SavedStateHandle`;
- mutex de escritura;
- transiciones `UNSAVED → SAVING → SAVED` y `ERROR`;
- guardado al salir y al pasar a segundo plano;
- reintento;
- protección de cambios concurrentes.

La transformación visual nunca se persiste. Las etiquetas siguen su flujo
independiente y no modifican `updatedAt`.

## Etiquetas

El menú de opciones permite abrir **Etiquetas**, añadir o reutilizar etiquetas,
mostrarlas como chips y retirarlas. La búsqueda FTS las indexa junto con el
título y el contenido.

## Extractos y búsqueda

`Note.preview` usa `MarkdownTextExtractor` para mostrar texto legible. Las URLs
se conservan como texto visible y la búsqueda FTS sigue indexando el Markdown
original, incluidas las URLs y las palabras dentro de enlaces Markdown.

## Limitaciones

- No hay resaltado sintáctico complejo en bloques de código.
- No se admiten listas anidadas ni tablas.
- No hay imágenes ni contenido HTML.
- El formato visual oculta delimitadores; editar justo en sus bordes puede
  requerir precisión.
- La barra se desplaza horizontalmente en pantallas estrechas.
- Los dominios sin esquema se abren siempre como HTTPS.
- No se detectan URLs con espacios internos como un único destino.
