package com.example.fileencryptionmanager

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class FeedReaderDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "FileEncryptionManager.db"
    }

    // SQLite
    object FeedReaderContract {
        // Table contents are grouped together in an anonymous object.
        object FeedEntry : BaseColumns {
            const val TABLE_NAME = "File"
            const val MD5SUM = "md5sum"
            const val MIMETYPE = "mimetype"
            const val SALT = "salt"
            const val IV = "iv"
        }
    }

    private val SQL_CREATE_ENTRIES =
        "CREATE TABLE ${FeedReaderContract.FeedEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                "${FeedReaderContract.FeedEntry.MD5SUM} TEXT," +
                "${FeedReaderContract.FeedEntry.MIMETYPE} TEXT," +
                "${FeedReaderContract.FeedEntry.SALT} BLOB," +
                "${FeedReaderContract.FeedEntry.IV} BLOB)"

    private val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${FeedReaderContract.FeedEntry.TABLE_NAME}"
}
