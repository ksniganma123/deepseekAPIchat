package com.example.multipromptchat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PromptTemplateEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun promptDao(): PromptDao

    abstract fun conversationDao(): ConversationDao

    abstract fun messageDao(): MessageDao

    companion object {
        private const val DB_NAME = "deepseekapichat.db"

        /**
         * v1 → v2：会话表新增 4 个 token 统计列（增量 C-5）。
         * 全部带 DEFAULT 0，已有会话数据不受影响。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE conversations ADD COLUMN prompt_tokens INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE conversations ADD COLUMN completion_tokens INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE conversations ADD COLUMN cache_hit_tokens INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE conversations ADD COLUMN cache_miss_tokens INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * v2 → v3：消息表新增图片路径列（1.2 的「图片发给 AI」）。
         * 可为空，已有消息不受影响。
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN image_path TEXT")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
