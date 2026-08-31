package com.aricneto.twistytimer

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.aricneto.twistytimer.database.AlgRepository
import com.aricneto.twistytimer.database.DatabaseHandler
import com.aricneto.twistytimer.database.DatabaseInitializer
import com.aricneto.twistytimer.database.SolveRepository
import com.aricneto.twistytimer.database.TwistyDatabaseFactory
import com.aricneto.twistytimer.utils.LocaleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.NoSuchAlgorithmException
import java.security.Provider
import java.security.SecureRandom
import java.security.SecureRandomSpi
import java.security.Security

/**
 * Created by Ari on 28/07/2015.
 */
class TwistyTimer : Application() {

    /**
     * A proxy implementation of "SecureRandomSpi" that delegates to the default "SHA1PRNG"
     * implementation. This is needed to satisfy the "TNoodle" library, which explicitly requests
     * the "SUN" provider for "SHA1PRNG" (which is not available on Android).
     */
    class SecureRandomSHA1PRNG : SecureRandomSpi() {
        private val delegate: SecureRandom

        init {
            var temp: SecureRandom
            try {
                temp = SecureRandom.getInstance("SHA1PRNG")
            } catch (_: NoSuchAlgorithmException) {
                temp = SecureRandom()
            }
            delegate = temp
        }

        override fun engineSetSeed(seed: ByteArray) {
            delegate.setSeed(seed)
        }

        override fun engineNextBytes(bytes: ByteArray) {
            delegate.nextBytes(bytes)
        }

        override fun engineGenerateSeed(numBytes: Int): ByteArray {
            return delegate.generateSeed(numBytes)
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (Security.getProvider("SUN") == null) {
            Security.addProvider(object : Provider("SUN", 1.0, "SUN provider proxy for Android") {
                init {
                    put("SecureRandom.SHA1PRNG", SecureRandomSHA1PRNG::class.java.name)
                }
            })
        }

        sAppContext = applicationContext

        // Create a singleton instance of the "DatabaseHandler" using the application context. This
        // avoids memory leaks elsewhere and is more convenient. There is ABSOLUTELY NO NEED to
        // close the database EVER. Closing it in an ad hoc fashion (as was done in fragments and
        // activities) leads to all sorts of concurrency problems, race conditions, etc. SQLite
        // handles all the concurrency itself, so there is no problem there. Android/Linux will
        // ensure that the database is closed if the application process exits.
        //
        // Note that the database is not opened here; it will not be opened until the first call to
        // "getReadableDatabase" or "getWritableDatabase" and then the open database instance will
        // be cached (by "SQLiteOpenHelper", which is the base class of "DatabaseHandler"). Those
        // two methods should really only be called from a background task, though, as opening the
        // database (particularly for the first time) can take some time.
        sDBHandler = DatabaseHandler()

        sAlgRepository = AlgRepository(TwistyDatabaseFactory.getDatabase().algorithmQueries)
        sSolveRepository = SolveRepository(TwistyDatabaseFactory.getDatabase().solveQueries)

        CoroutineScope(Dispatchers.IO).launch {
            DatabaseInitializer.initialize(sAlgRepository!!, sSolveRepository!!)
        }

        LocaleUtils.updateLocale(getAppContext())
    }

    companion object {
        /**
         * The singleton instance of the database access handler.
         */
        private var sDBHandler: DatabaseHandler? = null

        private var sAlgRepository: AlgRepository? = null
        private var sSolveRepository: SolveRepository? = null

        /**
         * The cached reference to the application context.
         */
        private var sAppContext: Context? = null

        /**
         * Gets the singleton instance of the database access handler. Do not close any database after
         * use!
         *
         * @return The database handler.
         */
        @JvmStatic
        fun getDBHandler(): DatabaseHandler {
            return sDBHandler!!
        }

        @JvmStatic
        fun getAlgRepository(): AlgRepository {
            return sAlgRepository!!
        }

        @JvmStatic
        fun getSolveRepository(): SolveRepository {
            return sSolveRepository!!
        }

        /**
         * Gets a read-only database handle. Do <i>not</i> close the database when it is no longer
         * needed.
         *
         * @return A handle on a readable database.
         */
        @JvmStatic
        fun getReadableDB(): SQLiteDatabase {
            return getDBHandler().readableDatabase
        }

        /**
         * Gets the application context. This is a convenience for cases where a full activity context
         * is not required. A full activity context is required to access theme attributes or inflate
         * layouts, but for other uses, such as accessing string resources, databases, broadcast
         * receivers, the application context is sufficient. Using the application context also avoid
         * memory leaks that can occur if an activity context is used inappropriately.
         *
         * @return The application context.
         */
        @JvmStatic
        fun getAppContext(): Context {
            return sAppContext!!
        }
    }
}
