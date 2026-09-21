package com.aricneto.twistytimer.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aricneto.twistify.R
import com.aricneto.twistytimer.ui.theme.LocalTwistyColors
import com.aricneto.twistytimer.viewmodel.SettingsViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel()
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportDatabase(it) { success ->
                Toast.makeText(context, if (success) "Export successful" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importDatabase(it) { result ->
                Toast.makeText(context, "Import complete: ${result.successes} added, ${result.duplicates} duplicates", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text("Backup and Restore") },
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
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                SettingsCategory("Export")
                ExportImportItem(
                    title = "Database Backup",
                    subtitle = "Export everything to a CSV file (Backup format)",
                    icon = R.drawable.ic_outline_archive_24dp
                ) {
                    exportLauncher.launch("TwistyTimer_Backup_${System.currentTimeMillis()}.csv")
                }
            }

            item {
                SettingsCategory("Import")
                ExportImportItem(
                    title = "Restore Backup",
                    subtitle = "Restore everything from a CSV file",
                    icon = R.drawable.ic_outline_unarchive_24dp
                ) {
                    importLauncher.launch("text/*")
                }
            }
        }
    }
}

@Composable
private fun SettingsCategory(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun ExportImportItem(
    title: String,
    subtitle: String,
    icon: Int,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}
