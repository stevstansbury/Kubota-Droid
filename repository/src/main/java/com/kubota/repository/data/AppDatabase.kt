package com.kubota.repository.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.*
import androidx.room.migration.Migration
import android.content.Context

@Database(entities = [Account::class, Equipment::class, Dealer::class],
    version = 4, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun dealerDao(): DealerDao

    companion object {
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val migrationToVersion2 = object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE account ADD homeAccountIdentifier text")
                }
            }

            val migrationToVersion3 = object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE models RENAME TO equipments")
                }
            }

            val migrationToVersion4 = object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE equipments ADD nickname text")
                }
            }

            return Room.databaseBuilder(context, AppDatabase::class.java, "kubota-db")
                .addMigrations(migrationToVersion2, migrationToVersion3, migrationToVersion4)
                .build()
        }
    }

}