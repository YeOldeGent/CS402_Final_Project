package com.example.simpletotp.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase

import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns


class TOTPEntryHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    object TOTPEntryContract {
        object TOTPEntry : BaseColumns {
            const val TABLE_NAME = "TOTPEntries"
            const val COLUMN_ID = "_id"
            const val COLUMN_KEY_TITLE = "secretKey"
            const val COLUMN_NAME_TITLE = "name"
            const val COLUMN_CRYPTO_TITLE = "crypto"
            const val COLUMN_FAVORITE_TITLE = "favorite"
        }

        const val SQL_CREATE_ENTRIES =
            "CREATE TABLE ${TOTPEntry.TABLE_NAME} (" +
                    "${TOTPEntry.COLUMN_ID} TEXT PRIMARY KEY," +
                    "${TOTPEntry.COLUMN_KEY_TITLE} TEXT," +
                    "${TOTPEntry.COLUMN_NAME_TITLE} TEXT," +
                    "${TOTPEntry.COLUMN_CRYPTO_TITLE} TEXT," +
                    "${TOTPEntry.COLUMN_FAVORITE_TITLE} BOOLEAN)"
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${TOTPEntry.TABLE_NAME}"
    }

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(TOTPEntryContract.SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "TOTPEntries"
    }
}