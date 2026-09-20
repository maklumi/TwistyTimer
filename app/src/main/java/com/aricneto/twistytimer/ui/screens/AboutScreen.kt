package com.aricneto.twistytimer.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.aricneto.twistify.R
import com.aricneto.twistytimer.utils.StoreUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var showTesters by remember { mutableStateOf(false) }
    var showTranslators by remember { mutableStateOf(false) }
    var showContributors by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Twisty Timer",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val version = remember {
                        try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        } catch (e: Exception) {
                            "Unknown"
                        }
                    }
                    Text(
                        text = "Version $version",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                AboutItem("Feedback", "Send suggestions or bug reports") {
                    val intent = Intent(Intent.ACTION_SENDTO, "mailto:aricnetodev@gmail.com".toUri()).apply {
                        putExtra(Intent.EXTRA_SUBJECT, "Feedback")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                }
                AboutItem("Rate App", "Support us on the Play Store") {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=com.aricneto.twistytimer".toUri()))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=com.aricneto.twistytimer".toUri()))
                    }
                }
                AboutItem("Source Code", "View on GitHub") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/aricneto/TwistyTimer".toUri()))
                }
                AboutItem("Translate", "Help us translate the app") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://crwd.in/twisty-timer".toUri()))
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                AboutItem("Testers", "View the people who helped test the app") {
                    showTesters = true
                }
                AboutItem("Translators", "View the people who translated the app") {
                    showTranslators = true
                }
                AboutItem("Contributors", "View the people who contributed to the app") {
                    showContributors = true
                }
                AboutItem("Licenses", "Open source software notices") {
                    showLicenses = true
                }
            }
            
            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showTesters) {
        AlertDialog(
            onDismissRequest = { showTesters = false },
            title = { Text("Testers") },
            text = { Text(stringResource(R.string.testers_content)) },
            confirmButton = { TextButton(onClick = { showTesters = false }) { Text("OK") } }
        )
    }

    if (showTranslators) {
        val translators = remember { StoreUtils.getStringFromRaw(context.resources, R.raw.translators) }
        AlertDialog(
            onDismissRequest = { showTranslators = false },
            title = { Text("Translators") },
            text = { Text(stringResource(R.string.translators_content, translators)) },
            confirmButton = { TextButton(onClick = { showTranslators = false }) { Text("OK") } }
        )
    }

    if (showContributors) {
        AlertDialog(
            onDismissRequest = { showContributors = false },
            title = { Text("Contributors") },
            text = { Text(stringResource(R.string.contributors_content, stringResource(R.string.contributors_names))) },
            confirmButton = { TextButton(onClick = { showContributors = false }) { Text("OK") } }
        )
    }
}

@Composable
private fun AboutItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        modifier = Modifier.clickable { onClick() }
    )
}
