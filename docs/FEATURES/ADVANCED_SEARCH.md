# Búsqueda avanzada de QUIPU

## Motor FTS4

La búsqueda usa la tabla virtual `notes_fts` de SQLite mediante `@Fts4` de Room.
Se mantiene el controlador SQLite de Android y Room 2.8.5, sin FTS5, sin
`BundledSQLiteDriver` y sin migrar a Room 3.

FTS4 aporta búsqueda indexada, prefijos y coincidencias por token sin recorrer
todas las notas. La tabla es independiente de `notes`: no sustituye a
`NoteEntity` ni duplica el modelo de dominio.

### Tokenizador

`tokenize=unicode61 \`remove_diacritics=1\``

Con este tokenizador:

- `organizacion` encuentra `organización` y viceversa.
- Las mayúsculas y minúsculas no afectan.
- Las eñes y vocales acentuadas se normalizan para la coincidencia.
- `record` encuentra tokens que empiezan por `record`.

### Columnas

`notes_fts` indexa:

- `title`
- `content` (Markdown original)
- `tags`

`noteId` se guarda como columna no indexada para relacionar cada fila con el UUID
de `notes`. El `rowid` entero lo genera FTS4.

## Sincronización del índice

El índice se actualiza dentro de transacciones Room junto con la tabla principal:

- Crear o editar una nota vuelve a indexar título y contenido.
- Cambiar etiquetas actualiza la columna `tags`.
- Eliminar permanentemente una nota borra su fila FTS.
- Vaciar la papelera borra únicamente las filas de notas eliminadas.
- Archivar, fijar o mover a papelera no borra el contenido indexado.

Las escrituras se concentran en `NoteRepositoryImpl`; ningún Composable escribe
directamente en Room. La migración inicial rellena `notes_fts` con todas las
notas existentes.

## Migración 1 → 2

`MIGRATION_1_2` crea:

- `note_tags`
- `note_tag_cross_ref`
- `notes_fts`
- los índices y claves foráneas correspondientes

Después copia título, contenido y `noteId` de todas las notas a `notes_fts`.
No se usa `fallbackToDestructiveMigration()`. Las notas, fechas, estado de
fijación, archivo y papelera se conservan sin cambios. El esquema exportado está
en `app/schemas/io.github.dijksys.quipu.core.database.AppDatabase/2.json`.

## Etiquetas

Modelo normalizado:

- `note_tags`: `id`, `name`, `normalizedName` único.
- `note_tag_cross_ref`: relación muchos a muchos con clave primaria compuesta.

Las claves foráneas usan `ON DELETE CASCADE`: borrar una nota elimina sus
relaciones y borrar una etiqueta limpia sus relaciones sin borrar notas. Las
etiquetas se comparan sin distinguir mayúsculas ni tildes.

El editor permite abrir **Etiquetas**, añadir o reutilizar etiquetas existentes,
mostrarlas como chips y retirarlas. Los listados muestran las etiquetas de cada
nota y las resaltan cuando coinciden con la búsqueda.

## Consultas

`NoteSearchQueryBuilder` construye SQL con parámetros enlazados:

- `MATCH` recibe una expresión normalizada con prefijos y semántica AND.
- Los términos se separan y se limitan a letras y números; comillas, paréntesis
  y operadores FTS nunca llegan a la consulta.
- El título, el contenido y las etiquetas se buscan en una sola tabla FTS.
- Las etiquetas se obtienen con una consulta agrupada, sin N+1.
- No se cargan todas las notas para filtrarlas en memoria.

La búsqueda se ejecuta en SQLite y devuelve un `Flow` de Room. Se conservan el
debounce de 250 ms, `distinctUntilChanged`, `flatMapLatest`,
`SharingStarted.WhileSubscribed` y el manejo de errores.

## Filtros

Estado:

- Activas.
- Archivadas.
- Papelera.

El estado sigue siendo independiente del filtro temporal. Fechas disponibles:

- Todas.
- Hoy.
- Últimos 7 días.
- Últimos 30 días.
- Rango personalizado con inicio y fin.

El filtro usa `updatedAt` y no modifica las fechas originales.

## Ordenación

- **Relevancia:** coincidencia exacta de título, prefijo de título, etiqueta y,
  por último, contenido. Los empates se resuelven por `updatedAt` descendente.
- **Más recientes:** `updatedAt` descendente.
- **Título:** comparación sin distinguir mayúsculas y `updatedAt` como criterio
  secundario.

La ordenación es determinista y se resuelve en SQL. No se muestra una puntuación
BM25 porque FTS4 no la proporciona de forma nativa.

## Resaltado

`NoteSearchHighlight` construye un `AnnotatedString` con `SpanStyle` sobre
`primaryContainer` y `onPrimaryContainer`. Los términos se normalizan con la
misma función que la consulta, por lo que las tildes y mayúsculas no impiden el
resaltado.

Se resaltan:

- el título,
- el extracto limpio del contenido,
- las etiquetas visibles.

El contenido almacenado no se modifica. Los extractos siguen usando
`MarkdownTextExtractor`, de modo que no aparecen `**`, `##`, `- [ ]` ni URLs
completas.

## Buscador en Inicio

El bloque de fecha y encabezado tipográfico de Inicio se sustituyó por un campo
de búsqueda prominente con forma de cápsula, icono Search, acción de limpiar y
altura aproximada de 52–56 dp.

Al pulsar Buscar en el teclado o el icono:

- Si hay consulta, se navega a Notas con el argumento `query` codificado.
- Si está vacía, se abre Notas normalmente.
- El teclado se oculta para inspeccionar los resultados.
- `NotesViewModel` recupera la consulta desde `SavedStateHandle`.

No existe un segundo motor en `HomeViewModel`. El campo de Notas reutiliza el
mismo componente y la misma lógica para refinar la búsqueda.

## Experiencia de resultados

Con una consulta activa se muestran:

- la consulta actual,
- la cantidad de resultados,
- el filtro de fecha,
- el selector de ordenación,
- la acción para borrar filtros y consulta,
- las coincidencias resaltadas.

El estado vacío de búsqueda es específico:

> No encontramos notas para esta búsqueda.

> Prueba con otras palabras o modifica los filtros.

## Rendimiento

- El debounce se mantiene en 250 ms.
- Las consultas aprovechan el índice FTS y se ejecutan fuera del hilo principal.
- Los resultados no se duplican aunque una nota tenga varias etiquetas.
- No se aplican límites silenciosos a los resultados.
- Las pruebas instrumentadas generan 2 000 notas en una base independiente y
  miden la búsqueda del último término.

## Limitaciones

- FTS4 no ofrece BM25; la relevancia usa prioridades deterministas.
- La ordenación por título usa `COLLATE NOCASE` de SQLite, que cubre ASCII.
- No hay búsqueda difusa, comodines internos ni subcadenas arbitrarias.
- No hay etiquetas jerárquicas, colores personalizados ni gestor avanzado.
- El rango personalizado usa fechas locales y límites de día completos.
