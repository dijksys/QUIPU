package io.github.dijksys.quipu.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

/** ��ndice FTS4 independiente; el UUID se conserva en una columna no indexada. */
@Entity(tableName = "notes_fts")
@Fts4(
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=1"],
    notIndexed = ["noteId"],
)
data class NoteFtsEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,
    val noteId: String,
    val title: String,
    val content: String,
    val tags: String,
)
