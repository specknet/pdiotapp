package com.specknet.pdiotapp.sql

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DBHelper(val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(
            "CREATE TABLE $USERS_TABLE (" +
                    "$COLUMN_USERNAME TEXT PRIMARY KEY," +
                    "$COLUMN_PASSWORD TEXT)"
        )

        db.execSQL(
            "CREATE TABLE $ACTIVITIES_TABLE (" +
                    "$COLUMN_USERNAME TEXT," +
                    "$COLUMN_ACTIVITY_NAME TEXT," +
                    "$COLUMN_TIMESTAMP DATETIME)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $USERS_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $ACTIVITIES_TABLE")
        onCreate(db)
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

    fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun addActivity(username: String, activityName: String, timestamp: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_USERNAME, username)
        contentValues.put(COLUMN_ACTIVITY_NAME, activityName)
        contentValues.put(COLUMN_TIMESTAMP, timestamp)
        val result = db.insert(ACTIVITIES_TABLE, null, contentValues)

        return result != (0).toLong()
    }

    fun getDayActivities(username: String, day: String): ArrayList<String> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT $COLUMN_ACTIVITY_NAME FROM $ACTIVITIES_TABLE WHERE $COLUMN_USERNAME = ? AND $COLUMN_TIMESTAMP LIKE ?", arrayOf(username, "$day%"))

        val activities = ArrayList<String>()
        if (cursor.moveToFirst()) {
            do {
                val activityName = cursor.getString(cursor.getColumnIndex(COLUMN_ACTIVITY_NAME))
                activities.add(activityName)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return activities
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 3
        const val DATABASE_NAME = "PDIoT.db"

        const val USERS_TABLE = "USERS"
        const val COLUMN_USERNAME = "USERNAME"
        const val COLUMN_PASSWORD = "PASSWORD"

        const val ACTIVITIES_TABLE = "ACTIVITIES"
        const val COLUMN_ACTIVITY_NAME = "ACTIVITY_NAME"
        const val COLUMN_TIMESTAMP = "TIMESTAMP"
    }

}