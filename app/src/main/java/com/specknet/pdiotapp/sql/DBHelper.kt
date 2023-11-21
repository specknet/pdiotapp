package com.specknet.pdiotapp.sql

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(
            "CREATE TABLE $USERS_TABLE (" +
                    "$COLUMN_USERNAME TEXT PRIMARY KEY," +
                    "$COLUMN_PASSWORD TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $USERS_TABLE")
    }

    fun addUser(username: String, password: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_USERNAME, username)
        contentValues.put(COLUMN_PASSWORD, password)
        val result = db.insert(USERS_TABLE, null, contentValues)

        return result != (0).toLong()
    }

    fun checkUsername(username: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $USERS_TABLE WHERE $COLUMN_USERNAME = ?", arrayOf(username))
        return cursor.count > 0
    }

    fun checkLoginInfo(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $USERS_TABLE WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?", arrayOf(username, password))
        return cursor.count > 0
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "PDIoT.db"
        const val USERS_TABLE = "USERS"
        const val COLUMN_USERNAME = "USERNAME"
        const val COLUMN_PASSWORD = "PASSWORD"
    }

}