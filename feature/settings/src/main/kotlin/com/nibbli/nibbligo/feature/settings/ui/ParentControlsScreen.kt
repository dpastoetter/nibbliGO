package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.core.designsystem.component.NibbliSecondaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliTextField
import com.nibbli.nibbligo.feature.settings.presentation.ParentControlsViewModel
import com.nibbli.nibbligo.feature.settings.presentation.ParentalGateViewModel
import kotlinx.coroutines.launch

@Composable
fun ParentControlsScreen(
    modifier: Modifier = Modifier,
    viewModel: ParentControlsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pinInput by remember { mutableStateOf("") }
    var currentPinInput by remember { mutableStateOf("") }

    NibbliScreen(modifier = modifier, scrollable = true) {
        NibbliScreenHeader(
            title = "For parents",
            subtitle = "Privacy, what's stored, and a PIN to lock advanced tools.",
            showOnDeviceBadge = true,
        )

        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("Private by design", style = MaterialTheme.typography.titleMedium)
            Text(
                "nibbliGO runs its AI on this phone. Your child's chats are not sent to a cloud AI for " +
                    "answers. The internet is used only to download AI models from Hugging Face (and optional " +
                    "sign-in for gated models).",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("What's stored on this device", style = MaterialTheme.typography.titleMedium)
            Text(
                "• Chat and Pixel Friend talk history\n" +
                    "• Companion memory facts your child chooses to save\n" +
                    "• Pet progress, settings, and downloaded models\n\n" +
                    "Nothing above leaves the phone unless your child explicitly shares or exports it. " +
                    "Cloud backup of chats is disabled.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("Things to know", style = MaterialTheme.typography.titleMedium)
            Text(
                "• On-device AI can still be wrong or say something unexpected. A basic safety filter blocks " +
                    "clearly unsafe topics, but please supervise younger children.\n" +
                    "• Voice input uses your phone's speech recognizer, which may process audio per its own " +
                    "privacy policy.\n" +
                    "• Connecting external tools (MCP servers) in Agent can send data outside the phone.\n" +
                    "• Advanced tools (Agent email/calendar, Benchmark, Prompt Lab, model tokens) can be locked " +
                    "behind a parent PIN below.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("Parent PIN", style = MaterialTheme.typography.titleMedium)
            Text(
                if (uiState.pinSet) {
                    "A PIN is set. Enter the current PIN, then a new PIN to change it, or remove it below."
                } else {
                    "Set a 4+ digit PIN to lock advanced tools."
                },
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.pinSet) {
                NibbliTextField(
                    value = currentPinInput,
                    onValueChange = { new -> currentPinInput = new.filter { it.isDigit() }.take(8) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text("Current PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
            NibbliTextField(
                value = pinInput,
                onValueChange = { new -> pinInput = new.filter { it.isDigit() }.take(8) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(if (uiState.pinSet) "New PIN" else "Parent PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            NibbliPrimaryButton(
                text = if (uiState.pinSet) "Change PIN" else "Set PIN",
                onClick = {
                    viewModel.setPin(
                        rawPin = pinInput,
                        currentPin = currentPinInput.takeIf { uiState.pinSet },
                    )
                    pinInput = ""
                    currentPinInput = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            if (uiState.pinSet) {
                NibbliSecondaryButton(
                    text = "Remove PIN",
                    onClick = {
                        viewModel.removePin(currentPinInput)
                        currentPinInput = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Lock advanced tools with the PIN",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = uiState.restrictAdultFeatures,
                    onCheckedChange = viewModel::setRestrictAdultFeatures,
                )
            }
            uiState.message?.let { msg ->
                Text(
                    msg,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearMessage()
                }
            }
        }
    }
}

/**
 * PIN prompt shown before entering a gated destination. Calls [onUnlocked] on a correct PIN.
 */
@Composable
fun ParentalGateDialog(
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: ParentalGateViewModel = hiltViewModel(),
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val lockedUntil by viewModel.pinLockedUntilEpochMs.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parent PIN") },
        text = {
            Column {
                Text(
                    "Ask a parent to unlock this. Enter the PIN to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                NibbliTextField(
                    value = pin,
                    onValueChange = { new ->
                        if (lockedUntil > System.currentTimeMillis()) return@NibbliTextField
                        pin = new.filter { it.isDigit() }.take(8)
                        error = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                error?.let { msg ->
                    Text(
                        msg,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clearAndSetSemantics {},
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        when (viewModel.verify(pin)) {
                            ParentalGateViewModel.VerifyResult.Success -> onUnlocked()
                            ParentalGateViewModel.VerifyResult.Locked ->
                                error = "Too many attempts. Try again later."
                            ParentalGateViewModel.VerifyResult.Incorrect ->
                                error = "Incorrect PIN."
                        }
                    }
                },
                enabled = lockedUntil <= System.currentTimeMillis(),
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
