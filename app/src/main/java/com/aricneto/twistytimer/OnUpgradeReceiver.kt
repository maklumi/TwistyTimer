package com.aricneto.twistytimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aricneto.twistify.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class OnUpgradeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.app_name)
            .setIcon(R.drawable.icon_launch)
            .setMessage(R.string.pref_summary_new)
            .show()
    }
}
