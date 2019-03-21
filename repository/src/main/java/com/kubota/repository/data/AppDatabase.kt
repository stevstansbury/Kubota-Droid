package com.kubota.repository.data

import android.arch.persistence.room.*
import android.content.Context

@Database(entities = [Account::class, Model::class, Dealer::class],
    version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun modelDao(): ModelDao
    abstract fun dealerDao(): DealerDao

    companion object {
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "kubota-db")
                .build()
        }
    }

}