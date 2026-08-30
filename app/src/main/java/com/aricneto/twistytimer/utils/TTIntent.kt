package com.aricneto.twistytimer.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aricneto.twistify.BuildConfig
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.utils.TTIntent.getPuzzleSubtype
import com.aricneto.twistytimer.utils.TTIntent.getPuzzleType
import com.aricneto.twistytimer.utils.TTIntent.getScramble
import com.aricneto.twistytimer.utils.TTIntent.getSolve
import java.util.Arrays

/**
 * The actions for the broadcast intents that notify listeners of changes to the data or to the
 * state of the application.
 * 
 * @author damo
 */
object TTIntent {
    /**
     * Flag to enable debug logging for this class.
     */
     const val DEBUG_ME = false

    /**
     * A "tag" to identify this class in log messages.
     */
     val TAG: String = TTIntent::class.java.simpleName

    /**
     * The name prefix for all categories and actions to ensure that their names do not clash with
     * any system names.
     */
     const val BASE_PREFIX = "com.aricneto.twistytimer."

    /**
     * The name prefix for all categories.
     */
    const val CATEGORY_PREFIX = BASE_PREFIX + "category."

    /**
     * The name prefix for all actions.
     */
    const val ACTION_PREFIX = BASE_PREFIX + "action."

    /**
     * The name prefix for all extras.
     */
    const val EXTRA_PREFIX = BASE_PREFIX + "extra."

    /**
     * The category for intents that communicate interactions with, or changes to the state of, the
     * timer and other user-interface elements.
     */
    @JvmField
    val CATEGORY_UI_INTERACTIONS: String = CATEGORY_PREFIX + "UI_INTERACTIONS"

    /**
     * The category for intents that communicate changes to the solve time data, or to the
     * selection of the set of solve time data to be presented.
     */
    val CATEGORY_TIME_DATA_CHANGES: String = CATEGORY_PREFIX + "TIME_DATA_CHANGES"

    /**
     * The category for intents that communicate changes to the algorithm data, or to the selection
     * of the set of data to be presented.
     */
    @JvmField
    val CATEGORY_ALG_DATA_CHANGES: String = CATEGORY_PREFIX + "ALG_DATA_CHANGES"

    /**
     * One new solve time has been added.
     */
    val ACTION_TIME_ADDED: String = ACTION_PREFIX + "TIME_ADDED"

    /**
     * One new solve time has been added manually via the TimerList FAB.
     */
    @JvmField
    val ACTION_TIME_ADDED_MANUALLY: String = ACTION_PREFIX + "TIME_ADDED_MANUALLY"

    /**
     * One or more solve times have been modified in unspecified ways. Modifications include adding
     * times (bulk import), deleting selected times, or changing the penalties, comments, history
     * status or other properties of one or more times. A full refresh of any displayed time data
     * may be required.
     */
    val ACTION_TIMES_MODIFIED: String = ACTION_PREFIX + "TIMES_MODIFIED"

    /**
     * One or more solves have been moved from the current session to the history of all sessions.
     */
    val ACTION_TIMES_MOVED_TO_HISTORY
            : String = ACTION_PREFIX + "TIMES_MOVED_TO_HISTORY"

    /**
     * The user has selected the option to show only times for the current session. Any solve times
     * being presented should be reloaded to match the new state, if necessary.
     */
    val ACTION_SESSION_TIMES_SHOWN: String = ACTION_PREFIX + "SESSION_TIMES_SHOWN"

    /**
     * The user has added a comment to their last solve
     */
    val ACTION_COMMENT_ADDED: String = ACTION_PREFIX + "COMMENT_ADDED"

    /**
     * The user has selected the option to show the fill history of all times for all past and
     * current sessions. Any solve times being presented should be reloaded to match the new state,
     * if necessary.
     */
    val ACTION_HISTORY_TIMES_SHOWN: String = ACTION_PREFIX + "HISTORY_TIMES_SHOWN"

    /**
     * The user has scrolled a page, causing a different tab to be displayed. At the start of a
     * scroll action, the user touches the screen, which may have been interpreted as an action to
     * start the timer. However, if that touch then developed into a swipe that scrolled the page
     * and switched tabs, the action to start the timer should be cancelled.
     */
    val ACTION_SCROLLED_PAGE: String = ACTION_PREFIX + "SCROLLED_PAGE"

    /**
     * The display of the tool bar has been restored. This action corresponds to the end of the
     * animation that restores the tool bar.
     */
    val ACTION_TOOLBAR_RESTORED: String = ACTION_PREFIX + "TOOLBAR_RESTORED"

    /**
     * The tool bar button to generate a new scramble has been pressed and the receiver should
     * perform that action.
     */
    @JvmField
    val ACTION_GENERATE_SCRAMBLE: String = ACTION_PREFIX + "GENERATE_SCRAMBLE"

    /**
     * The current scramble has been modified, either by the user or by the timer itself.
     */
    val ACTION_SCRAMBLE_MODIFIED: String = ACTION_PREFIX + "SCRAMBLE_MODIFIED"

    /**
     * Selection mode has been turned on for the list of times.
     */
    val ACTION_SELECTION_MODE_ON: String = ACTION_PREFIX + "SELECTION_MODE_ON"

    /**
     * Selection mode has been turned off for the list of times.
     */
    val ACTION_SELECTION_MODE_OFF: String = ACTION_PREFIX + "SELECTION_MODE_OFF"

    /**
     * An item in the list of times has been selected.
     */
    val ACTION_TIME_SELECTED: String = ACTION_PREFIX + "TIME_SELECTED"

    /**
     * An item in the list of times has been unselected.
     */
    val ACTION_TIME_UNSELECTED: String = ACTION_PREFIX + "TIME_UNSELECTED"

    /**
     * The user has chosen the action to delete all of the selected times. The receiver should
     * perform that operation and broadcast [.ACTION_TIMES_MODIFIED].
     */
    val ACTION_DELETE_SELECTED_TIMES
            : String = ACTION_PREFIX + "DELETE_SELECTED_TIMES"

    /**
     * The timer has been started.
     */
    val ACTION_TIMER_STARTED: String = ACTION_PREFIX + "TIMER_STARTED"

    /**
     * The user has selected a new puzzle category (subtype)
     */
    val ACTION_CHANGED_CATEGORY: String = ACTION_PREFIX + "CHANGED_CATEGORY"

    /**
     * The user has selected a new theme
     */
    val ACTION_CHANGED_THEME: String = ACTION_PREFIX + "CHANGED_THEME"

    /**
     * The timer has been stopped.
     */
    val ACTION_TIMER_STOPPED: String = ACTION_PREFIX + "TIMER_STOPPED"

    /**
     * One or more algorithms has been added, deleted or otherwise modified.
     */
    @JvmField
    val ACTION_ALGS_MODIFIED: String = ACTION_PREFIX + "ALGS_MODIFIED"

    /**
     * Statistics have finished loading
     */
    val ACTION_STATISTICS_LOADED: String = ACTION_PREFIX + "STATISTICS_LOADED"

    /**
     * The name of an intent extra that can hold the name of the puzzle type.
     */
    val EXTRA_PUZZLE_TYPE: String = EXTRA_PREFIX + "PUZZLE_TYPE"

    /**
     * The name of an intent extra that can hold the name of the puzzle subtype.
     */
    val EXTRA_PUZZLE_SUBTYPE: String = EXTRA_PREFIX + "PUZZLE_SUBTYPE"

    /**
     * The name of an intent extra that can be used to record a [Solve].
     */
    val EXTRA_SOLVE: String = EXTRA_PREFIX + "SOLVE"

    /**
     * The name of an intent extra that can be used to record a scramble
     */
    val EXTRA_SCRAMBLE: String = EXTRA_PREFIX + "SCRAMBLE"

    /**
     * The name of an intent extra that can be used to record a long
     */
    val EXTRA_LONG: String = EXTRA_PREFIX + "LONG"

    /**
     * The actions that are allowed under each category. The category name is the key and the
     * corresponding entry is a collection of action names that are supported for that category.
     * An action may be supported by more than one category.
     */
    // NOTE: To match an "Intent", it is not sufficient for an "IntentFilter" to simply match all
    // categories defined on the intent, it must also match the action on the "Intent" (unless the
    // intent action is null, in which case it is always matched). For the purposes of receiving
    // local broadcast intents in this app, it is no harm to ensure that intents are not broadcast
    // with the wrong category, so requiring each category to have a defined list of supported
    // actions (for use when creating the "IntentFilter") makes things clearer. It also allows some
    // defensive checks in the "broadcast" methods that might highlight bugs in the code.
     val ACTIONS_SUPPORTED_BY_CATEGORY
            : MutableMap<String, Array<String>> = object : HashMap<String, Array<String>>() {
        init {
            put(
                CATEGORY_TIME_DATA_CHANGES, arrayOf(
                    ACTION_TIME_ADDED,
                    ACTION_TIMES_MODIFIED,
                    ACTION_TIMES_MOVED_TO_HISTORY,
                    ACTION_HISTORY_TIMES_SHOWN,
                    ACTION_SESSION_TIMES_SHOWN,
                    ACTION_COMMENT_ADDED,
                    ACTION_STATISTICS_LOADED
                )
            )

            put(
                CATEGORY_ALG_DATA_CHANGES, arrayOf(
                    ACTION_ALGS_MODIFIED,
                )
            )

            put(
                CATEGORY_UI_INTERACTIONS, arrayOf(
                    ACTION_TIME_SELECTED,
                    ACTION_TIME_UNSELECTED,
                    ACTION_DELETE_SELECTED_TIMES,
                    ACTION_SELECTION_MODE_ON,
                    ACTION_SELECTION_MODE_OFF,
                    ACTION_TIMER_STARTED,
                    ACTION_TIMER_STOPPED,
                    ACTION_TOOLBAR_RESTORED,
                    ACTION_GENERATE_SCRAMBLE,
                    ACTION_SCRAMBLE_MODIFIED,
                    ACTION_SCROLLED_PAGE,
                    ACTION_CHANGED_CATEGORY,
                    ACTION_CHANGED_THEME,
                    ACTION_TIME_ADDED_MANUALLY
                )
            )
        }
    }

    /**
     * Broadcasts an intent for the given category and action. To add more details to the intent
     * (via intent extras), use a [BroadcastBuilder].
     * 
     * @param category The category of the action.
     * @param action   The action.
     */
    @JvmStatic
    fun broadcast(category: String, action: String) {
        BroadcastBuilder(category, action).broadcast()
    }

    /**
     * Registers a broadcast receiver. The receiver will only be notified of intents that require
     * the category given and only for the actions that are supported for that category. If the
     * receiver is used by a fragment, create an instance of [TTFragmentBroadcastReceiver]
     * and register it with the [.registerReceiver] method
     * instead, as it will be easier to maintain.
     * 
     * @param receiver
     * The broadcast receiver to be registered.
     * @param category
     * The category for the actions to be received. Must not be `null` and must be a
     * supported category.
     * 
     * @throws IllegalArgumentException
     * If the category is `null`, or is not one of the supported categories.
     */
    fun registerReceiver(receiver: BroadcastReceiver, category: String) {
        val actions = ACTIONS_SUPPORTED_BY_CATEGORY[category]
            ?: throw IllegalArgumentException("Category is not supported: $category")

        val filter = IntentFilter()

        filter.addCategory(category)

        for (action in actions) {
            // IntentFilter will only match Intents with one of these actions.
            filter.addAction(action)
        }

        LocalBroadcastManager.getInstance(TwistyTimer.getAppContext())
            .registerReceiver(receiver, filter)
    }

    /**
     * Registers a fragment broadcast receiver. The receiver will only be notified of intents that
     * require the category defined for the `TTFragmentBroadcastReceiver` and only for the
     * actions supported by that category.
     * 
     * @param receiver The fragment broadcast receiver to be registered.
     * 
     * @throws IllegalArgumentException
     * If the receiver does not define the name of a supported category.
     */
    fun registerReceiver(receiver: TTFragmentBroadcastReceiver) {
        TTIntent.registerReceiver(receiver, receiver.category)
    }

    /**
     * Unregisters a broadcast receiver. Any further broadcast intent will be ignored.
     * 
     * @param receiver The receiver to be unregistered.
     */
    fun unregisterReceiver(receiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(TwistyTimer.getAppContext()).unregisterReceiver(receiver)
    }

    /**
     * Gets the name of the puzzle type from an intent extra.
     * 
     * @param intent The intent from which to get the puzzle type.
     * @return The puzzle type, or `null` if the intent does not specify a puzzle type.
     */
    fun getPuzzleType(intent: Intent): String? {
        return intent.getStringExtra(EXTRA_PUZZLE_TYPE)
    }

    /**
     * Gets the name of the puzzle subtype from an intent extra.
     * 
     * @param intent The intent from which to get the puzzle subtype.
     * @return The puzzle subtype, or `null` if the intent does not specify a puzzle subtype.
     */
    fun getPuzzleSubtype(intent: Intent): String? {
        return intent.getStringExtra(EXTRA_PUZZLE_SUBTYPE)
    }

    /**
     * Gets the solve specified in an intent extra.
     * 
     * @param intent The intent from which to get the solve.
     * @return The solve, or `null` if the intent does not specify a solve.
     */
    fun getSolve(intent: Intent): Solve? {
        val solve = intent.getParcelableExtra<Parcelable?>(EXTRA_SOLVE)

        return if (solve == null) null else solve as Solve
    }

    /**
     * Gets the scramble specified in an intent extra.
     * 
     * @param intent The intent from which to get the scramble.
     * @return The scramble, or `null` if the intent does not specify a scramble.
     */
    fun getScramble(intent: Intent): String? {
        return intent.getStringExtra(EXTRA_SCRAMBLE)
    }


    /**
     * Gets the long value specified in an intent extra.
     * 
     * @param intent The intent from which to get the value.
     * @return The long value, or -1 if the intent does not specify a long value
     */
    fun getLongValue(intent: Intent): Long {
        return intent.getLongExtra(EXTRA_LONG, -1)
    }

    /**
     * A convenient wrapper for fragments that use a broadcast receiver that will only notify the
     * fragment of an intent when the fragment is currently added to its activity.
     */
    // NOTE: The goal of this class is to make a more obvious connection between the categories and
    // the fragments, as the category will be given in the code of the fragment class at the point
    // where it instantiates an instance of this class. It also simplifies
    abstract class TTFragmentBroadcastReceiver
    /**
     * Creates a new broadcast receiver to be used by a fragment. Matching broadcast intents
     * will only be notified to the fragment via [.onReceiveWhileAdded] if the fragment is
     * added to its activity at the time of the broadcast.
     * 
     * @param fragment
     * The fragment that will be receiving the broadcast intents.
     * @param category
     * The category of the intent actions.
     */(
        /**
         * The fragment that is receiving the broadcasts.
         */
        private val mFragment: Fragment,
        /**
         * The intent category.
         */
        val category: String
    ) : BroadcastReceiver() {
        /**
         * Gets the category of the intent actions that will be matched by this broadcast receiver.
         * 
         * @return The category.
         */

        /**
         * Notifies the receiver of a matching broadcast intent that is received while the fragment
         * is added to its activity. The receiver will only be notified of intents that require the
         * category configured, or intents that require no category. (The latter is not a use-case
         * that is expected in this application.)
         * 
         * @param context The context for the intent.
         * @param intent  The matching intent that was received.
         */
        abstract fun onReceiveWhileAdded(context: Context?, intent: Intent?)

        /**
         * Notifies the receiver of a matching broadcast intent. This implementation will call
         * [.onReceiveWhileAdded] only while the fragment is currently added
         * to its activity, otherwise the intent will be ignored.
         * 
         * @param context The context for the intent.
         * @param intent  The matching intent that was received.
         */
        // Make this final to make sure extensions only override "onReceiveWhileAdded".
        override fun onReceive(context: Context?, intent: Intent?) {
            if (mFragment.isAdded()) {
                if (DEBUG_ME) Log.d(
                    TAG, (mFragment.javaClass.getSimpleName()
                            + ": onReceiveWhileAdded: " + intent)
                )
                onReceiveWhileAdded(context, intent)
            }
        }
    }

    /**
     * A builder for local broadcasts.
     * 
     * @author damo
     */
    class BroadcastBuilder(category: String, action: String) {
        /**
         * The intent that will be broadcast when building is complete.
         */
        private val mIntent: Intent = Intent(action)

        /**
         * Creates a new broadcast builder for the given intent category and action.
         * 
         * @param category The category of the action.
         * @param action   The action.
         */
        init {
            mIntent.addCategory(category)
        }

        /**
         * Broadcasts the intent configured by this builder.
         * 
         * @throws IllegalStateException
         * If the category specified on the intent does not support the defined action.
         */
        fun broadcast() {
            // For sanity, check that the category and action on the intent are supported. This
            // will unearth bugs in the code where actions have the wrong category and will not
            // end up where they are expected. Only do this if this is a debug build, as it would
            // be a waste of time in a release build.
            if (BuildConfig.DEBUG) {
                val action = mIntent.action
                checkNotNull(action) { "An intent action is expected." }

                val categories = mIntent.categories
                check(categories != null && categories.size == 1) { "Exactly one intent category is expected." }

                val category = categories.iterator().next()
                val actions = ACTIONS_SUPPORTED_BY_CATEGORY[category]
                    ?: throw IllegalStateException("Category '$category' is not supported.")

                check(actions.contains(action)) {
                    "Action '$action' not allowed for category '$category'."
                }
            }

            LocalBroadcastManager.getInstance(TwistyTimer.getAppContext()).sendBroadcast(mIntent)
        }

        /**
         * Sets extras that identify the puzzle type and subtype related to the action of the
         * intent that will be broadcast. The receiver can retrieve the type and subtype by calling
         * [getPuzzleType] and [getPuzzleSubtype].
         * 
         * @param puzzleType    The name of the type of puzzle.
         * @param puzzleSubtype The name of the subtype of puzzle.
         * 
         * @return `this` broadcast builder, allowing method calls to be chained.
         */
        fun puzzle(puzzleType: String?, puzzleSubtype: String?): BroadcastBuilder {
            if (puzzleType != null) {
                mIntent.putExtra(EXTRA_PUZZLE_TYPE, puzzleType)
            }
            if (puzzleSubtype != null) {
                mIntent.putExtra(EXTRA_PUZZLE_SUBTYPE, puzzleSubtype)
            }

            return this
        }

        /**
         * Sets an optional extra that identifies a solve time related to the action of the intent
         * that will be broadcast. The receiver can call [getSolve] to
         * retrieve the solve from the intent.
         * 
         * @param solve The solve to be added to the broadcast intent.
         * 
         * @return `this` broadcast builder, allowing method calls to be chained.
         */
        fun solve(solve: Solve?): BroadcastBuilder {
            if (solve != null) {
                // "Solve" implements "Parcelable" to allow it to be passed in an intent extra.
                mIntent.putExtra(EXTRA_SOLVE, solve)
            }

            return this
        }

        /**
         * Sets an optional extra that identifies a scramble string related to the action of the intent
         * that will be broadcast. The receiver can call [getScramble] to
         * retrieve the scramble from the intent.
         * 
         * @param scramble The scramble to be added to the broadcast intent.
         * 
         * @return `this` broadcast builder, allowing method calls to be chained.
         */
        fun scramble(scramble: String?): BroadcastBuilder {
            if (scramble != null) {
                mIntent.putExtra(EXTRA_SCRAMBLE, scramble)
            }

            return this
        }

        /**
         * Sets an optional extra that identifies a long value related to the action of the intent
         * that will be broadcast. The receiver can call [getScramble] to
         * retrieve the long from the intent.
         * 
         * @param value The long to be added to the broadcast intent.
         * 
         * @return `this` broadcast builder, allowing method calls to be chained.
         */
        fun longValue(value: Long): BroadcastBuilder {
            mIntent.putExtra(EXTRA_LONG, value)

            return this
        }
    }
}
