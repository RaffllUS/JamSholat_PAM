package com.example.jamsholat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "sholat.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sholat (
                tanggal TEXT PRIMARY KEY,
                subuh TEXT,
                dzuhur TEXT,
                ashar TEXT,
                maghrib TEXT,
                isya TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun insertOrUpdate(tanggal: String, subuh: String, dzuhur: String, ashar: String, maghrib: String, isya: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("tanggal", tanggal)
            put("subuh", subuh)
            put("dzuhur", dzuhur)
            put("ashar", ashar)
            put("maghrib", maghrib)
            put("isya", isya)
        }
        db.insertWithOnConflict("sholat", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getToday(tanggal: String): Map<String, String>? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM sholat WHERE tanggal = ?", arrayOf(tanggal))
        return if (cursor.moveToFirst()) {
            val result = mapOf(
                "subuh" to cursor.getString(1),
                "dzuhur" to cursor.getString(2),
                "ashar" to cursor.getString(3),
                "maghrib" to cursor.getString(4),
                "isya" to cursor.getString(5)
            )
            cursor.close()
            result
        } else {
            cursor.close()
            null
        }
    }
}