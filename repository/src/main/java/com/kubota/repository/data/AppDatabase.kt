package com.kubota.repository.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.*
import androidx.room.migration.Migration
import android.content.Context

@Database(entities = [Account::class, Equipment::class, Dealer::class, FaultCode::class],
    version = 7, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun dealerDao(): DealerDao
    abstract fun faultCodeDao(): FaultCodeDao

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

            val migrationToVersion5 = object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("ALTER TABLE equipments ADD engineHours INTEGER NOT NULL default 0")
                    database.execSQL("ALTER TABLE equipments ADD coolantTemperature INTEGER default null")
                    database.execSQL("ALTER TABLE equipments ADD battery REAL default null")
                    database.execSQL("ALTER TABLE equipments ADD fuelLevel INTEGER default null")
                    database.execSQL("ALTER TABLE equipments ADD defLevel INTEGER default null")
                    database.execSQL("ALTER TABLE equipments ADD engineState TEXT default null")
                    database.execSQL("ALTER TABLE equipments ADD latitude REAL default null")
                    database.execSQL("ALTER TABLE equipments ADD longitude REAL default null")

                    database.execSQL("CREATE TABLE faultcode (" +
                            "equipmentId INTEGER NOT NULL, code INTEGER NOT NULL, description TEXT NOT NULL, action TEXT NOT NULL, " +
                            "PRIMARY KEY (equipmentId, code), " +
                            "FOREIGN KEY (equipmentId) " +
                                "REFERENCES equipments(_id) " +
                                "ON DELETE CASCADE" +
                            ")")
                }
            }

            val migrationToVersion6 = object : Migration(5, 6) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("DROP TABLE IF EXISTS account")
                    database.execSQL("CREATE TABLE account (id INTEGER NOT NULL, userName TEXT NOT NULL, " +
                        "accessToken TEXT NOT NULL, expireDate INTEGER NOT NULL, flags INTEGER NOT NULL, refreshToken TEXT, " +
                        "PRIMARY KEY (id) )")
                }
            }

            val migrationToVersion7 = object : Migration(6, 7) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("DROP TABLE IF EXISTS equipments")
                    database.execSQL("CREATE TABLE equipments (_id INTEGER NOT NULL, serverId TEXT NOT NULL, " +
                            "userId INTEGER NOT NULL, model TEXT NOT NULL, serialNumber TEXT, category TEXT NOT NULL, " +
                            "manualName TEXT NOT NULL, manualLocation TEXT NULL, hasGuide INTEGER NOT NULL, " +
                            "nickname TEXT default null, engineHours INTEGER NOT NULL default 0, " +
                            "coolantTemperature INTEGER default null, battery REAL default null, " +
                            "fuelLevel INTEGER default null, defLevel INTEGER default null, " +
                            "engineState INTEGER default null, latitude REAL default null, " +
                            "longitude REAL default null, " +
                            "isVerified INTEGER NOT NULL, " +
                            "PRIMARY KEY (_id), " +
                            "FOREIGN KEY (userId) REFERENCES account (id) " +
                            "ON DELETE CASCADE) ")
                    database.execSQL("CREATE UNIQUE INDEX index_equipments_serverId ON equipments(serverId)")
                }
            }

            return Room.databaseBuilder(context, AppDatabase::class.java, "kubota-db")
                .addMigrations(migrationToVersion2, migrationToVersion3, migrationToVersion4,
                    migrationToVersion5, migrationToVersion6, migrationToVersion7)
                .build()
        }
    }

}