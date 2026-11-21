package io.github.dijksys.quipu.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Migraci��n incremental 1 -> 2: etiquetas normalizadas e ��ndice FTS4. */
val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `note_tags` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `normalizedName` TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_note_tags_normalizedName` " +
                "ON `note_tags` (`normalizedName`)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `note_tag_cross_ref` (
                `noteId` TEXT NOT NULL,
                `tagId` INTEGER NOT NULL,
                PRIMARY KEY(`noteId`, `tagId`),
                FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`tagId`) REFERENCES `note_tags`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_noteId` " +
                "ON `note_tag_cross_ref` (`noteId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tagId` " +
                "ON `note_tag_cross_ref` (`tagId`)",
        )
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(
                `noteId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `tags` TEXT NOT NULL,
                tokenize=unicode61 `remove_diacritics=1`,
                notindexed=`noteId`
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `notes_fts` (`title`, `content`, `tags`, `noteId`)
            SELECT `title`, `content`, '', `id` FROM `notes`
            """.trimIndent(),
        )
    }
}
