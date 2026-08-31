package com.aricneto.twistytimer.database

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.aricneto.twistytimer.TwistyTimer

object TwistyDatabaseFactory {
    private var database: TwistyDatabase? = null

    private val modeAdapter = object : ColumnAdapter<Int, Long> {
        override fun decode(databaseValue: Long): Int = databaseValue.toInt()
        override fun encode(value: Int): Long = value.toLong()
    }

    fun getDatabase(): TwistyDatabase {
        if (database == null) {
            val driver: SqlDriver = AndroidSqliteDriver(
                schema = TwistyDatabase.Schema,
                context = TwistyTimer.getAppContext(),
                name = "databaseManager",
                callback = object : AndroidSqliteDriver.Callback(TwistyDatabase.Schema) {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.enableWriteAheadLogging()
                        // Ensure tables exist
                        db.execSQL("CREATE TABLE IF NOT EXISTS times (_id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, subtype TEXT, time INTEGER, date INTEGER NOT NULL DEFAULT 0, scramble TEXT, penalty INTEGER, comment TEXT, history INTEGER DEFAULT 0, mode INTEGER NOT NULL DEFAULT 0)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS algs (_id INTEGER PRIMARY KEY AUTOINCREMENT, subset TEXT, name TEXT, state TEXT, algs TEXT, progress INTEGER DEFAULT 0)")

                        // Migration for 'mode' column added in v4.7.0
                        db.query("PRAGMA table_info(times)").use { cursor ->
                            var hasMode = false
                            val nameColumnIndex = cursor.getColumnIndex("name")
                            if (nameColumnIndex != -1) {
                                while (cursor.moveToNext()) {
                                    if (cursor.getString(nameColumnIndex) == "mode") {
                                        hasMode = true
                                        break
                                    }
                                }
                            }
                            if (!hasMode) {
                                db.execSQL("ALTER TABLE times ADD COLUMN mode INTEGER NOT NULL DEFAULT 0")
                            }
                        }
                    }
                }
            )
            database = TwistyDatabase(
                driver = driver,
                timesAdapter = Times.Adapter(modeAdapter = modeAdapter)
            )
        }
        return database!!
    }
}
