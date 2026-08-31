package com.aricneto.twistytimer

import android.app.Application
import android.content.Context
import com.aricneto.twistytimer.database.AlgRepository
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

        sAlgRepository = AlgRepository(TwistyDatabaseFactory.getDatabase().algorithmQueries)
        sSolveRepository = SolveRepository(TwistyDatabaseFactory.getDatabase().solveQueries)

        CoroutineScope(Dispatchers.IO).launch {
            DatabaseInitializer.initialize(sAlgRepository!!, sSolveRepository!!)
        }

        LocaleUtils.updateLocale(getAppContext())
    }

    companion object {
        private var sAlgRepository: AlgRepository? = null
        private var sSolveRepository: SolveRepository? = null

        /**
         * The cached reference to the application context.
         */
        private var sAppContext: Context? = null

        @JvmStatic
        fun getAlgRepository(): AlgRepository {
            return sAlgRepository!!
        }

        @JvmStatic
        fun getSolveRepository(): SolveRepository {
            return sSolveRepository!!
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
