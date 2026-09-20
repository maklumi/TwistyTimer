package com.aricneto.twistytimer.ui.components

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import com.aricneto.twistify.R
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.ScrambleGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolveDetailDialog(
    solve: Solve,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onPenaltyChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onToggleHistory: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var showCommentDialog by remember { mutableStateOf(false) }
    var currentComment by remember { mutableStateOf(solve.comment) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Time and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val timeStr = if (solve.penalty == PuzzleUtils.PENALTY_DNF) "DNF" 
                                 else PuzzleUtils.convertTimeToString(solve.time.toLong(), PuzzleUtils.FORMAT_DEFAULT)
                    Text(
                        text = timeStr,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (solve.penalty == PuzzleUtils.PENALTY_DNF) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    @Suppress("DEPRECATION")
                    val currentLocale = LocalConfiguration.current.locale
                    Text(
                        text = SimpleDateFormat("EEEE, dd MMMM yyyy, HH:mm", currentLocale).format(Date(solve.date)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Twisty Timer solve: ${PuzzleUtils.convertTimeToString(solve.time.toLong(), PuzzleUtils.FORMAT_DEFAULT)}s\nScramble: ${solve.scramble}\n${solve.comment}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Solve"))
                }) {
                    Icon(painterResource(R.drawable.ic_outline_share_24px), contentDescription = "Share")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Scramble Image
            if (solve.scramble.isNotEmpty()) {
                val generator = remember(solve.puzzle) { ScrambleGenerator(solve.puzzle) }
                var drawable by remember { mutableStateOf<Drawable?>(null) }
                
                LaunchedEffect(solve.scramble) {
                    val sp = PreferenceManager.getDefaultSharedPreferences(context)
                    drawable = withContext(Dispatchers.IO) {
                        generator.generateImageFromScramble(sp, solve.scramble)
                    }
                }

                if (drawable != null) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                setImageDrawable(drawable)
                            }
                        },
                        update = { it.setImageDrawable(drawable) },
                        modifier = Modifier.size(240.dp, 180.dp)
                    )
                }
                
                Text(
                    text = solve.scramble,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            if (solve.comment.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = solve.comment,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { 
                    val nextPenalty = when(solve.penalty) {
                        PuzzleUtils.NO_PENALTY -> PuzzleUtils.PENALTY_PLUSTWO
                        PuzzleUtils.PENALTY_PLUSTWO -> PuzzleUtils.PENALTY_DNF
                        else -> PuzzleUtils.NO_PENALTY
                    }
                    onPenaltyChange(nextPenalty)
                }) {
                    val penaltyLabel = when(solve.penalty) {
                        PuzzleUtils.NO_PENALTY -> "Set +2"
                        PuzzleUtils.PENALTY_PLUSTWO -> "Set DNF"
                        else -> "Clear Penalty"
                    }
                    Text(penaltyLabel)
                }

                OutlinedButton(onClick = { showCommentDialog = true }) {
                    Text(if (solve.comment.isEmpty()) "Add Comment" else "Edit Comment")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { onToggleHistory(!solve.history) }) {
                    Text(if (solve.history) "Move to Session" else "Move to History")
                }
                
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("Comment") },
            text = {
                OutlinedTextField(
                    value = currentComment,
                    onValueChange = { currentComment = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter comment...") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCommentChange(currentComment)
                    showCommentDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
