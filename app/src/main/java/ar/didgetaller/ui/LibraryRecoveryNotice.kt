package ar.didgetaller.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.didgetaller.R

/** Recovery is explicit; merely opening the library cannot rewrite a damaged index. */
@Composable
internal fun LibraryRecoveryNotice(damaged: Boolean = true, onRecover: () -> Unit) {
    if (!damaged) return
    var confirming by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().testTag("library-index-warning")) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(R.string.library_index_damaged), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { confirming = true }, modifier = Modifier.testTag("recover-session-index")) {
                Text(stringResource(R.string.library_index_recover))
            }
        }
    }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false },
        title = { Text(stringResource(R.string.library_index_recover)) },
        text = { Text(stringResource(R.string.library_index_recovery_confirmation)) },
        confirmButton = { TextButton(onClick = { confirming = false; onRecover() },
            modifier = Modifier.testTag("confirm-index-recovery")) { Text(stringResource(R.string.library_index_recover)) } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.ui_cancel)) } })
}
