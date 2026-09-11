package com.aricneto.twistytimer.stats

import android.database.Observable
import com.aricneto.twistytimer.stats.StatisticsCache.StatisticsObserver

/**
 * 
 * 
 * A non-persistent cache that maintains a reference to the most recently loaded [Statistics].
 * The statistics can be loaded by an activity or fragment using its `ViewModel`
 * and then saved to this cache. Other components, typically fragments,
 * can register themselves with this cache to be notified when the statistics are first loaded or
 * updated. The statistics can also be retrieved from the cache. See [.getStatistics] for
 * more details on accessing the statistics.
 * 
 * 
 * 
 * A fragment that wants to access the statistics, needs to be able to accommodate the unpredictable
 * timing of the first availability of the statistics. The fragment should, during its creation,
 * call `StatisticsCache.getInstance().getStatistics()` to get the statistics, allowing that
 * it may be `null`, and use the statistics (if not `null`) as required. It can then,
 * before creation is complete, call [.registerObserver] to ensure it is notified of any later
 * updates.
 * 
 * 
 * 
 * When an activity starts loading the statistics, the loading might finish before the fragment is
 * created. In that case, the fragment cannot have been notified of the updated statistics at the
 * time the loading finished, because the fragment was not created. Therefore, the call to
 * `getStatistics()` will get that first statistics instance that could not be notified. Any
 * later changes will be notified once the fragment registers itself as an observer.
 * 
 * 
 * 
 * Of course, the loading might finish after the fragment is created. In that case, the call to
 * `getStatistics()` will have returned `null` and no statistics could be used during
 * creation. However, as soon as loading completes, the fragment, having registered itself as an
 * observer, will receive notification of the update and can then apply the statistics as required.
 * In the time between detecting that the `getStatistics()` returned `null` and the
 * time of the first notification of statistics being available, the fragment could, if appropriate,
 * display a progress indicator.
 * 
 * 
 * 
 * In the above, an `Activity` is assumed to be managing the loading process. However, there
 * is no reason why one `Fragment` could not manage the loading on behalf of other fragments.
 * 
 * 
 * 
 * For simplicity, the statistics cache is a singleton that can maintain only one instance of
 * `Statistics`. This makes it easier to access from the application components, but restricts
 * all components to a single, common set of statistics.
 * 
 * 
 * @author damo
 */
// IMPLEMENTATION NOTE: While a LocalBroadcastManager could be used to pass around the update
// notifications, this "Observable" implementation makes the code much neater, particularly since
// many of the BroadcastReceivers in the fragments have been moved to the new Loader classes, so
// some of the fragments would have a "bulky" receiver for only this one simple purpose.
//
// NOTE: If different "Statistics" are required, it would not be too difficult to change this
// class to a non-singleton pattern and then to hold different instances of the cache in, say,
// one or more Activities. An Activity would then implement an accessor that could be called from
// a child Fragment. TwistyTimer does not require this use-case at present, so the simpler approach
// is taken. However, the "StatisticsCache.getInstance()" access pattern means that none of the
// fields (other than the singleton reference) are "static", so a change to support multiple cached
// statistics would require little more in this class than changing the constructor to "public".
class StatisticsCache
/**
 * Private constructor to prevent instantiation of this singleton class.
 */
private constructor() : Observable<StatisticsObserver>() {
    /**
     * An interface for components that need to be notified when the statistics are first added to
     * the cache or are later updated.
     */
    interface StatisticsObserver {
        /**
         * Notifies the observer when the statistics have been updated.
         * 
         * @param stats The updated statistics.
         */
        fun onStatisticsUpdated(stats: Statistics?)
    }

    /**
     * Gets the statistics currently in this cache. In general, it should not cause problems if the
     * caller retains a reference to the returned statistics instance. However, it should be noted
     * that the instance may be reset without notification shortly before updated statistics are
     * delivered. Therefore, the status should be checked before use and perhaps ignored until the
     * new update notification arrives.
     * 
     * @return
     * The cached statistics. May be `null`, if the statistics have not yet been loaded.
     * *Do not modify this `Statistics` instance.*
     */
    /**
     * The cached statistics. May be `null` if the cache has not been populated.
     */
    var statistics: Statistics? = null
        private set

    /**
     * Updates this cache with the given statistics and notifies any observers of the update.
     * 
     * @param stats The statistics to be saved to the cache. May be `null`.
     */
    fun updateAndNotify(stats: Statistics?) {
        // Set this first. If an observer calls "getStatistics" instead of using the passed
        // statistics object, the two will be the same.
        this.statistics = stats

        for (observer in mObservers) {
            observer.onStatisticsUpdated(this.statistics)
        }
    }

    companion object {
        /**
         * Gets the singleton instance of this cache.
         * 
         * @return The statistics cache.
         */
        /**
         * The singleton instance of this cache.
         */
        val instance: StatisticsCache = StatisticsCache()
    }
}
