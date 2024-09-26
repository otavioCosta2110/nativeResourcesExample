package com.example.nativeres

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FormDataDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "form_data.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "FormData"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_COMMENT = "comment"
        private const val COLUMN_PHOTO_PATH = "photo_path"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = ("CREATE TABLE $TABLE_NAME ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_NAME TEXT,"
                + "$COLUMN_EMAIL TEXT,"
                + "$COLUMN_COMMENT TEXT,"
                + "$COLUMN_PHOTO_PATH TEXT" + ")")
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertFormData(name: String, email: String, comment: String, photoPath: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_NAME, name)
        values.put(COLUMN_EMAIL, email)
        values.put(COLUMN_COMMENT, comment)
        values.put(COLUMN_PHOTO_PATH, photoPath)
        db.insert(TABLE_NAME, null, values)
    }

    @SuppressLint("Range")
    fun getAllFormData(): List<Map<String, String>> {
        val formDataList = mutableListOf<Map<String, String>>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, null)

        if (cursor.moveToFirst()) {
            do {
                val formData = mapOf(
                    "id" to cursor.getString(cursor.getColumnIndex("id")),
                    "name" to cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
                    "email" to cursor.getString(cursor.getColumnIndex(COLUMN_EMAIL)),
                    "comment" to cursor.getString(cursor.getColumnIndex(COLUMN_COMMENT)),
                    "photoPath" to cursor.getString(cursor.getColumnIndex(COLUMN_PHOTO_PATH))
                )
                formDataList.add(formData)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return formDataList
    }

}
