package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aricneto.twistify.R
import com.aricneto.twistytimer.ui.components.NumberPickerDialog
import com.aricneto.twistytimer.ui.theme.LocalTwistyColors
import com.aricneto.twistytimer.utils.TTIntent
import com.aricneto.twistytimer.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onBackupClick: () -> Unit,
    onColorSchemeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel()
    val updateVersion by viewModel.updateVersion.collectAsState()
    
    var showInspectionTimeDialog by remember { mutableStateOf(false) }
    var showInspectionAlertTypeDialog by remember { mutableStateOf(false) }
    var showTrimSizeDialog by remember { mutableStateOf(false) }
    var showMaxDnfDialog by remember { mutableStateOf(false) }
    var showScrambleSizeDialog by remember { mutableStateOf(false) }
    var showTimerSizeDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars,
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
        },
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    LocalTwistyColors.current.backgroundGradientStart,
                    LocalTwistyColors.current.backgroundGradientEnd
                )
            )
        )
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                key(updateVersion) {
                    Column {
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

                        val alertType = viewModel.getString(R.string.pk_inspection_alert_type, "both")
                        val alertTypeLabel = when (alertType) {
                            "vibration" -> "Vibration only"
                            "sound" -> "Sound only"
                            else -> "Sound and Vibration"
                        }
                        SettingsItem(
                            title = "Inspection Alert Type",
                            subtitle = alertTypeLabel,
                            icon = R.drawable.ic_outline_help_outline_24px,
                            enabled = viewModel.getBoolean(R.string.pk_inspection_enabled, false) && viewModel.getBoolean(R.string.pk_inspection_alert_enabled, true),
                            onClick = { showInspectionAlertTypeDialog = true }
                        )

                        SettingsCategory("Timer Control")
                        
                        SwitchSettingsItem(
                            title = "Manual Entry",
                            subtitle = "Enter times manually",
                            icon = R.drawable.ic_outline_edit_24px,
                            checked = viewModel.getBoolean(R.string.pk_enable_manual_entry, false),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_enable_manual_entry, it) }
                        )

                        SwitchSettingsItem(
                            title = "Hide Time While Solving",
                            subtitle = "Display time only when solve finishes",
                            icon = R.drawable.ic_outline_timer_24px,
                            checked = viewModel.getBoolean(R.string.pk_hide_time_while_running, false),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_hide_time_while_running, it) }
                        )

                        SwitchSettingsItem(
                            title = "Show Decimals",
                            subtitle = "Display milliseconds on timer",
                            icon = R.drawable.ic_outline_timer_24px,
                            checked = viewModel.getBoolean(R.string.pk_show_hi_res_timer, true),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_show_hi_res_timer, it) }
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

                        SwitchSettingsItem(
                            title = "Back Cancels Solve",
                            subtitle = "Pressing back button during solve cancels it",
                            icon = R.drawable.ic_arrow_back_black_24dp,
                            checked = viewModel.getBoolean(R.string.pk_back_button_cancel_solve_enabled, true),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_back_button_cancel_solve_enabled, it) }
                        )

                        SettingsCategory("Alerts & Personal Bests")

                        SwitchSettingsItem(
                            title = "Best Time Alert",
                            subtitle = "Notify when setting a single personal best",
                            icon = R.drawable.ic_outline_star_border_18px,
                            checked = viewModel.getBoolean(R.string.pk_show_best_time, true),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_show_best_time, it) }
                        )

                        SwitchSettingsItem(
                            title = "Best Average Alert",
                            subtitle = "Notify when setting a new best average",
                            icon = R.drawable.ic_outline_star_border_18px,
                            checked = viewModel.getBoolean(R.string.pk_show_average_record_enabled, true),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_show_average_record_enabled, it) }
                        )

                        SwitchSettingsItem(
                            title = "Worst Time Alert",
                            subtitle = "Notify when setting a worst solve",
                            icon = R.drawable.ic_outline_star_border_18px,
                            checked = viewModel.getBoolean(R.string.pk_show_worst_time, false),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_show_worst_time, it) }
                        )

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

                        SwitchSettingsItem(
                            title = "Show Extended X-Cross Hints",
                            subtitle = "Include X-Cross solutions (may load slower)",
                            icon = R.drawable.ic_outline_wb_incandescent_24px,
                            checked = viewModel.getBoolean(R.string.pk_show_scramble_x_cross_hints, true),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_show_scramble_x_cross_hints, it) },
                            enabled = viewModel.getBoolean(R.string.pk_scramble_enabled, true) && viewModel.getBoolean(R.string.pk_show_scramble_hints, true)
                        )

                        SettingsCategory("Appearance")
                        
                        SettingsItem(
                            title = "Theme",
                            subtitle = "App colors and fonts",
                            icon = R.drawable.ic_outline_palette_24px,
                            onClick = onThemeClick
                        )
                        
                        SettingsItem(
                            title = "Cube Color Scheme",
                            subtitle = "Customize face colors",
                            icon = R.drawable.ic_outline_palette_24px,
                            onClick = onColorSchemeClick
                        )

                        SettingsItem(
                            title = "Scramble Text Size",
                            subtitle = "${viewModel.getInt(R.string.pk_scramble_text_size, 100)}%",
                            icon = R.drawable.ic_outline_text_fields_24px,
                            onClick = { showScrambleSizeDialog = true }
                        )

                        SettingsItem(
                            title = "Timer Text Size",
                            subtitle = "${viewModel.getInt(R.string.pk_timer_text_size, 100)}%",
                            icon = R.drawable.ic_outline_text_fields_24px,
                            onClick = { showTimerSizeDialog = true }
                        )

                        SwitchSettingsItem(
                            title = "Swipe Between Tabs",
                            subtitle = "Allow swiping left/right to change screens",
                            icon = R.drawable.ic_outline_timeline_24px,
                            checked = viewModel.getBoolean(R.string.pk_tab_swiping_enabled, true),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_tab_swiping_enabled, it) }
                        )

                        SwitchSettingsItem(
                            title = "Colored Background",
                            subtitle = "Tint timer background with theme accent",
                            icon = R.drawable.ic_outline_palette_24px,
                            checked = viewModel.getBoolean(R.string.pk_timer_bg_enabled, true),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_timer_bg_enabled, it) }
                        )

                        SwitchSettingsItem(
                            title = "Tint Navigation Bar",
                            subtitle = "Tint system navigation bar with theme color",
                            icon = R.drawable.ic_outline_palette_24px,
                            checked = viewModel.getBoolean(R.string.pk_tint_navigation_bar, false),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_tint_navigation_bar, it) }
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

                        SettingsCategory("Advanced & Statistics")
                        
                        SettingsItem(
                            title = "Trim Size",
                            subtitle = "${viewModel.getInt(R.string.pk_stat_trim_size, 5)}%",
                            icon = R.drawable.ic_outline_track_changes_18px,
                            onClick = { showTrimSizeDialog = true }
                        )

                        SettingsItem(
                            title = "Max DNF Attempts",
                            subtitle = "${viewModel.getInt(R.string.pk_stat_acceptable_dnf_size, 1)}",
                            icon = R.drawable.ic_outline_track_changes_18px,
                            onClick = { showMaxDnfDialog = true }
                        )

                        SwitchSettingsItem(
                            title = "Disqualify Average on Max DNF",
                            subtitle = "Automatically DNF averages exceeding max DNF limit",
                            icon = R.drawable.ic_outline_track_changes_18px,
                            checked = viewModel.getBoolean(R.string.pk_stat_disqualify_dnf, true),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_stat_disqualify_dnf, it) }
                        )

                        SwitchSettingsItem(
                            title = "Discrete Graph Dataset",
                            subtitle = "Draw solve history as points instead of a line",
                            icon = R.drawable.ic_outline_timeline_24px,
                            checked = viewModel.getBoolean(R.string.pk_stat_discrete_graph_dataset, false),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_stat_discrete_graph_dataset, it) }
                        )
                        
                        SwitchSettingsItem(
                            title = "Show Clear Button",
                            subtitle = "In solve list",
                            icon = R.drawable.ic_outline_delete_sweep_18px,
                            checked = viewModel.getBoolean(R.string.pk_show_clear_button, true),
                            onCheckedChange = { viewModel.setBoolean(R.string.pk_show_clear_button, it) }
                        )
                        
                        Spacer(Modifier.height(32.dp))
                    }
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

    if (showInspectionAlertTypeDialog) {
        val currentType = viewModel.getString(R.string.pk_inspection_alert_type, "both")
        AlertDialog(
            onDismissRequest = { showInspectionAlertTypeDialog = false },
            title = { Text("Inspection Alert Type") },
            text = {
                Column {
                    listOf("both" to "Sound and Vibration", "vibration" to "Vibration only", "sound" to "Sound only").forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setString(R.string.pk_inspection_alert_type, key)
                                    showInspectionAlertTypeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentType == key,
                                onClick = {
                                    viewModel.setString(R.string.pk_inspection_alert_type, key)
                                    showInspectionAlertTypeDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInspectionAlertTypeDialog = false }) {
                    Text("Cancel")
                }
            }
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

    if (showMaxDnfDialog) {
        NumberPickerDialog(
            title = "Max DNF Attempts",
            initialValue = viewModel.getInt(R.string.pk_stat_acceptable_dnf_size, 1),
            minValue = 1,
            maxValue = 10,
            onValueSelected = {
                viewModel.setInt(R.string.pk_stat_acceptable_dnf_size, it)
                showMaxDnfDialog = false
            },
            onDismiss = { showMaxDnfDialog = false }
        )
    }

    if (showScrambleSizeDialog) {
        NumberPickerDialog(
            title = "Scramble Text Size (%)",
            initialValue = viewModel.getInt(R.string.pk_scramble_text_size, 100),
            minValue = 50,
            maxValue = 200,
            onValueSelected = {
                viewModel.setInt(R.string.pk_scramble_text_size, it)
                showScrambleSizeDialog = false
                TTIntent.broadcast(TTIntent.CATEGORY_UI_INTERACTIONS, TTIntent.ACTION_CHANGED_THEME)
            },
            onDismiss = { showScrambleSizeDialog = false }
        )
    }

    if (showTimerSizeDialog) {
        NumberPickerDialog(
            title = "Timer Text Size (%)",
            initialValue = viewModel.getInt(R.string.pk_timer_text_size, 100),
            minValue = 50,
            maxValue = 200,
            onValueSelected = {
                viewModel.setInt(R.string.pk_timer_text_size, it)
                showTimerSizeDialog = false
                TTIntent.broadcast(TTIntent.CATEGORY_UI_INTERACTIONS, TTIntent.ACTION_CHANGED_THEME)
            },
            onDismiss = { showTimerSizeDialog = false }
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
    var localChecked by remember(checked) { mutableStateOf(checked) }

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
            Switch(checked = localChecked, onCheckedChange = null, enabled = enabled)
        },
        modifier = Modifier.clickable(enabled = enabled) {
            val newValue = !localChecked
            localChecked = newValue
            onCheckedChange(newValue)
        }
    )
}
