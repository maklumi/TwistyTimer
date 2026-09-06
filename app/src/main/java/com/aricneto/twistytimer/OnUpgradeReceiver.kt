package com.aricneto.twistytimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aricneto.twistify.R
import com.aricneto.twistytimer.utils.Prefs

class OnUpgradeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Prefs.edit {
                putBoolean(R.string.pk_show_update_dialog, true)
            }
        }
    }
}
