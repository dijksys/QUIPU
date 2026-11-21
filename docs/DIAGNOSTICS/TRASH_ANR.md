# Diagnóstico del ANR al vaciar la papelera

## Síntoma

Al confirmar **Vaciar papelera**, Android mostraba `QUIPU no responde`. Se
reproducía con una o varias notas y también en versiones anteriores.

## Evidencia recogida

Se reprodujo en el Motorola Moto G34 5G usando la variante QA
`io.github.dijksys.quipu.qa`, con datos sintéticos:

- 3 000 notas activas.
- 1 000 notas en papelera.
- 4 000 entradas FTS.
- 50 relaciones de etiquetas y 50 recordatorios vinculados.

Artefactos guardados fuera del repositorio:

```text
C:\Users\JOSUE\QUIPU-REPO-BACKUPS\diagnostics-2.1.3\
  moto-lastanr.txt
  moto-anr-empty-trash-before.logcat.txt
  moto-empty-trash-after.logcat.txt
  moto-empty-trash-after.xml
```

`dumpsys activity lastanr` registró:

```text
ANR time: 21 nov 2026 12:03:33
Reason: Input dispatching timed out ... Waited 10006ms for MotionEvent
```

Logcat mostró:

```text
Throwing OutOfMemoryError "Failed to allocate a 48 byte allocation..."
FATAL EXCEPTION: main
java.lang.OutOfMemoryError
  at NotesViewModel.emptyTrash(NotesViewModel.kt:197)
  at NotesViewModel$emptyTrash$1.invokeSuspend(NotesViewModel.kt:199)
```

## Causa raíz

No era una transacción Room lenta ni el índice FTS. Era una **recursión
infinita**:

```kotlin
private val emptyTrash: EmptyTrashUseCase

fun emptyTrash() {
    viewModelScope.launch {
        runCatching {
            emptyTrash() // resolvía al método miembro, no al caso de uso
        }
    }
}
```

El método `emptyTrash()` y la propiedad inyectada tenían el mismo nombre. La
llamada resolvía al método miembro, que lanzaba otra corrutina y volvía a
llamarse. El bucle creó corrutinas hasta agotar el heap de 256 MB, bloqueó el
hilo principal y provocó el ANR.

## Análisis de FTS y Room

Se revisó la operación de borrado:

```sql
DELETE FROM notes_fts
WHERE noteId IN (
    SELECT id FROM notes WHERE deletedAt IS NOT NULL
)
```

`EXPLAIN QUERY PLAN` muestra un `SCAN notes_fts VIRTUAL TABLE` porque `noteId`
está declarado como `notIndexed`. En una base de 4 000 entradas el borrado FTS
tardó aproximadamente **62,6 ms** en escritorio; no explica un ANR de 10 s.

El borrado de notas sí usa el índice:

```text
SEARCH notes USING INDEX index_notes_deletedAt_archivedAt_updatedAt
```

Las cascadas de etiquetas y `ON DELETE SET NULL` de recordatorios se
comprobaron con `PRAGMA integrity_check` y `PRAGMA foreign_key_check`.

La optimización del índice FTS queda como mejora futura, no como causa de este
defecto.

## Corrección

1. Se renombró la dependencia a `emptyTrashUseCase`.
2. La llamada ahora ejecuta el caso de uso real.
3. Se añadió `isEmptyingTrash` con `compareAndSet` para impedir solicitudes
   simultáneas.
4. El botón y la confirmación se deshabilitan durante la operación.
5. Se muestra un indicador de progreso y se emite `TrashEmptied`/`Failure` al
   terminar.
6. La transacción mantiene la integridad de notas, FTS, etiquetas y
   recordatorios.

## Resultado

Antes: ANR de 10 s y `OutOfMemoryError`.

Después, en el Moto G34 5G:

- Vaciado de 1 000 notas completado antes de 3 s.
- Sin nuevo ANR (`lastanr` conserva únicamente el incidente anterior).
- Snackbar **Papelera vaciada**.
- 3 000 notas activas conservadas.
- 3 000 entradas FTS.
- 0 referencias de etiquetas huérfanas.
- 50 recordatorios conservados y desvinculados.

En la prueba instrumentada con 3 000 notas activas y 1 000 en papelera, la
transacción de vaciado tardó **27 ms** en el emulador Pixel 7.

## Pruebas preventivas

- `TrashEmptyingRepositoryTest`:
  - papelera vacía;
  - una, diez y mil notas eliminadas;
  - notas activas y archivadas;
  - etiquetas y recordatorios;
  - operaciones repetidas;
  - integridad SQLite y claves foráneas;
  - presupuesto de tiempo para 1 000 notas.
- `NotesViewModelTest`: estado `isEmptyingTrash` e idempotencia.
- `MarkdownEditorFieldTest`: interacción real de casillas.

## Cómo capturar un ANR futuro

```powershell
adb -s ZY32JGVXVJ logcat -c
adb -s ZY32JGVXVJ logcat -b main -b system -b crash -v threadtime > anr.log
adb -s ZY32JGVXVJ shell dumpsys activity lastanr
adb -s ZY32JGVXVJ shell dumpsys activity exit-info io.github.dijksys.quipu
```

Revisar el hilo principal, `OutOfMemoryError`, corrutinas y transacciones. No
interpretar un ANR como fallo de FTS sin comprobar la traza. Guardar los logs
fuera del repositorio y sin contenido de notas.
