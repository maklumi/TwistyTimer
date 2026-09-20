package com.aricneto.twistytimer.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.utils.ImportExportManager
import com.aricneto.twistytimer.utils.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    
    private val context = TwistyTimer.getAppContext()
    
    private val _updateVersion = MutableStateFlow(0)
    val updateVersion = _updateVersion.asStateFlow()

    fun getBoolean(resId: Int, default: Boolean): Boolean {
        return Prefs.getBoolean(resId, default)
    }

    fun setBoolean(resId: Int, value: Boolean) {
        Prefs.edit { putBoolean(resId, value) }
        _updateVersion.value++
    }

    fun getString(resId: Int, default: String): String {
        return Prefs.getString(resId, default) ?: default
    }

    fun setString(resId: Int, value: String) {
        Prefs.edit { putString(resId, value) }
        _updateVersion.value++
    }

    fun getInt(resId: Int, default: Int): Int {
        return Prefs.getInt(resId, default)
    }

    fun setInt(resId: Int, value: Int) {
        Prefs.edit { putInt(resId, value) }
        _updateVersion.value++
    }
    
    fun getStringRes(resId: Int): String {
        return context.getString(resId)
    }

    fun exportDatabase(uri: Uri, onComplete: (Boolean) -> Unit) {
        val manager = TwistyTimer.getImportExportManager()
        val params = TimerViewModel.SolveParams() // Default
        viewModelScope.launch {
            val result = manager.exportSolves(
                fileFormat = ImportExportManager.EXIM_FORMAT_BACKUP,
                uri = uri,
                puzzleType = "",
                puzzleCategory = "",
                mode = 0,
                onProgress = { _, _ -> }
            )
            onComplete(result)
        }
    }

    fun importDatabase(uri: Uri, onComplete: (ImportExportManager.ImportResult) -> Unit) {
        val manager = TwistyTimer.getImportExportManager()
        viewModelScope.launch {
            val result = manager.importSolves(
                fileFormat = ImportExportManager.EXIM_FORMAT_BACKUP,
                uri = uri,
                puzzleType = "",
                puzzleCategory = "",
                mode = 0,
                onProgress = { _, _ -> }
            )
            onComplete(result)
        }
    }
}
