package com.nibbli.nibbligo.feature.pet.ui.visit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.domain.PetPostcard
import com.nibbli.nibbligo.feature.pet.domain.PetVisitQrExporter
import com.nibbli.nibbligo.feature.pet.ui.share.stageGlyph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetVisitSheet(
    visible: Boolean,
    pet: PetState,
    visitPostcard: PetPostcard?,
    onScanResult: (String) -> Unit,
    onShareQr: () -> Unit,
    onDismissVisit: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    var showScanner by remember { mutableStateOf(false) }
    var showMyQr by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val myQrBitmap = remember(pet.name, pet.stage, pet.dialogueLine) {
        PetVisitQrExporter.qrBitmapForPet(pet)
    }

    PetVisitScannerDialog(
        visible = showScanner,
        onDismiss = { showScanner = false },
        onScanned = { payload ->
            showScanner = false
            onScanResult(payload)
        },
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Visit", style = MaterialTheme.typography.titleLarge)
            visitPostcard?.let { card ->
                Text("Visiting ${card.senderName}", style = MaterialTheme.typography.titleMedium)
                card.visitMessage?.let { message ->
                    Text(
                        text = "Message: \"$message\"",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                card.careTip?.let { tip ->
                    Text(
                        text = "Care tip: $tip",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stageGlyph(card.stage),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${card.stage.name.lowercase()} · care ${card.careScore} · mood ${card.mood}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (card.borrowedPropId != null || card.borrowedSceneId != null) {
                    Text(
                        text = "Souvenir unlocked on your nibbli for 24h.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    text = "\"${card.dialogueLine.take(120)}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NibbliPrimaryButton(
                    text = "End visit",
                    onClick = onDismissVisit,
                    modifier = Modifier.fillMaxWidth(),
                )
            } ?: run {
                Text(
                    text = "Scan a friend's QR code or share yours to visit for 24 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NibbliPrimaryButton(
                    text = "Scan QR code",
                    onClick = { showScanner = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                NibbliPrimaryButton(
                    text = if (showMyQr) "Hide my QR" else "Show my visit QR",
                    onClick = { showMyQr = !showMyQr },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showMyQr) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            bitmap = myQrBitmap.asImageBitmap(),
                            contentDescription = "Visit QR for ${pet.name}",
                            modifier = Modifier.size(220.dp),
                        )
                        Text(
                            text = "Scan to visit ${pet.name}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        )
                        NibbliPrimaryButton(
                            text = "Share QR",
                            onClick = onShareQr,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
