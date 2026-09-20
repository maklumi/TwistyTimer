package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aricneto.twistify.R
import com.aricneto.twistytimer.ui.components.NumberPickerDialog
import com.aricneto.twistytimer.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onBackupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel()
    val updateVersion by viewModel.updateVersion.collectAsState()
    
    var showInspectionTimeDialog by remember { mutableStateOf(false) }
    var showTrimSizeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back_black_24dp),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        key(updateVersion) {
            LazyColumn(
                modifier = modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                item {
                    SettingsCategory("General")
                    SettingsItem(
                        title = "Language",
                        subtitle = viewModel.getString(R.string.pk_locale, "System Default"),
                        icon = R.drawable.ic_translate_black_24dp,
                        onClick = onLanguageClick
                    )
                    
                    SettingsItem(
                        title = "Backup and Restore",
                        subtitle = "Import or export your data",
                        icon = R.drawable.ic_outline_archive_24dp,
                        onClick = onBackupClick
                    )
                }

                item {
                    SettingsCategory("Inspection Behavior")
                    
                    SwitchSettingsItem(
                        title = "Enable Inspection",
                        subtitle = "15s inspection time",
                        icon = R.drawable.ic_outline_wb_incandescent_24px,
                        checked = viewModel.getBoolean(R.string.pk_inspection_enabled, false),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_inspection_enabled, it) }
                    )

                    SettingsItem(
                        title = "Inspection Time",
                        subtitle = "${viewModel.getInt(R.string.pk_inspection_time, 15)} seconds",
                        icon = R.drawable.ic_outline_timer_24px,
                        enabled = viewModel.getBoolean(R.string.pk_inspection_enabled, false),
                        onClick = { showInspectionTimeDialog = true }
                    )

                    SwitchSettingsItem(
                        title = "Inspection Alerts",
                        subtitle = "Sound/vibration warnings",
                        icon = R.drawable.ic_outline_help_outline_24px,
                        checked = viewModel.getBoolean(R.string.pk_inspection_alert_enabled, true),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_inspection_alert_enabled, it) },
                        enabled = viewModel.getBoolean(R.string.pk_inspection_enabled, false)
                    )
                }

                item {
                    SettingsCategory("Timer Control")
                    
                    SwitchSettingsItem(
                        title = "Manual Entry",
                        subtitle = "Enter times manually",
                        icon = R.drawable.ic_outline_edit_24px,
                        checked = viewModel.getBoolean(R.string.pk_enable_manual_entry, false),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_enable_manual_entry, it) }
                    )

                    SwitchSettingsItem(
                        title = "Start Cue",
                        subtitle = "Visual cue when ready",
                        icon = R.drawable.ic_outline_wb_incandescent_24px,
                        checked = viewModel.getBoolean(R.string.pk_start_cue_enabled, true),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_start_cue_enabled, it) }
                    )

                    SwitchSettingsItem(
                        title = "Hold to Start",
                        subtitle = "Requirement for timing",
                        icon = R.drawable.ic_outline_radio_button_unchecked_24px,
                        checked = viewModel.getBoolean(R.string.pk_hold_to_start_enabled, true),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_hold_to_start_enabled, it) }
                    )
                }

                item {
                    SettingsCategory("Scramble")
                    
                    SwitchSettingsItem(
                        title = "Enable Scramble",
                        icon = R.drawable.ic_outline_casino_24px,
                        checked = viewModel.getBoolean(R.string.pk_scramble_enabled, true),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_scramble_enabled, it) }
                    )

                    SwitchSettingsItem(
                        title = "Show Hints",
                        subtitle = "Cross solutions",
                        icon = R.drawable.ic_outline_wb_incandescent_24px,
                        checked = viewModel.getBoolean(R.string.pk_show_scramble_hints, true),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_show_scramble_hints, it) },
                        enabled = viewModel.getBoolean(R.string.pk_scramble_enabled, true)
                    )
                }

                item {
                    SettingsCategory("Appearance")
                    
                    SettingsItem(
                        title = "Theme",
                        subtitle = "App colors and fonts",
                        icon = R.drawable.ic_outline_palette_24px,
                        onClick = onThemeClick
                    )

                    SwitchSettingsItem(
                        title = "Show Scramble Image",
                        icon = R.drawable.ic_outline_casino_24px,
                        checked = viewModel.getBoolean(R.string.pk_show_scramble_image, true),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_show_scramble_image, it) }
                    )

                    SwitchSettingsItem(
                        title = "Show Session Stats",
                        icon = R.drawable.ic_outline_timeline_24px,
                        checked = viewModel.getBoolean(R.string.pk_show_session_stats, true),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_show_session_stats, it) }
                    )

                    SwitchSettingsItem(
                        title = "Show Quick Actions",
                        icon = R.drawable.ic_format_shapes_black_24dp,
                        checked = viewModel.getBoolean(R.string.pk_show_quick_actions, true),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_show_quick_actions, it) }
                    )
                }

                item {
                    SettingsCategory("Advanced")
                    
                    SettingsItem(
                        title = "Trim Size",
                        subtitle = "${viewModel.getInt(R.string.pk_stat_trim_size, 5)}%",
                        icon = R.drawable.ic_outline_track_changes_18px,
                        onClick = { showTrimSizeDialog = true }
                    )
                    
                    SwitchSettingsItem(
                        title = "Show Clear Button",
                        subtitle = "In solve list",
                        icon = R.drawable.ic_outline_delete_sweep_18px,
                        checked = viewModel.getBoolean(R.string.pk_show_clear_button, true),
                        onCheckedChange = { viewModel.setBoolean(R.string.pk_show_clear_button, it) }
                    )
                }
                
                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    if (showInspectionTimeDialog) {
        NumberPickerDialog(
            title = "Inspection Time",
            initialValue = viewModel.getInt(R.string.pk_inspection_time, 15),
            minValue = 0,
            maxValue = 60,
            onValueSelected = {
                viewModel.setInt(R.string.pk_inspection_time, it)
                showInspectionTimeDialog = false
            },
            onDismiss = { showInspectionTimeDialog = false }
        )
    }

    if (showTrimSizeDialog) {
        NumberPickerDialog(
            title = "Trim Size",
            initialValue = viewModel.getInt(R.string.pk_stat_trim_size, 5),
            minValue = 0,
            maxValue = 25,
            onValueSelected = {
                viewModel.setInt(R.string.pk_stat_trim_size, it)
                showTrimSizeDialog = false
            },
            onDismiss = { showTrimSizeDialog = false }
        )
    }
}

@Composable
private fun SettingsCategory(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: Int? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(
                text = title,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        supportingContent = subtitle?.let { { 
            Text(
                text = it,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        } },
        leadingContent = icon?.let { {
            Icon(
                imageVector = ImageVector.vectorResource(it),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        } },
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    )
}

@Composable
private fun SwitchSettingsItem(
    title: String,
    subtitle: String? = null,
    icon: Int? = null,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(
                text = title,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        supportingContent = subtitle?.let { { 
            Text(
                text = it,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        } },
        leadingContent = icon?.let { {
            Icon(
                imageVector = ImageVector.vectorResource(it),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        } },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
        modifier = Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
    )
}
