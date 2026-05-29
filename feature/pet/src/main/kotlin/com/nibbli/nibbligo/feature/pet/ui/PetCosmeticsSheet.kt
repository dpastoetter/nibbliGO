package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.pixel.PetCosmeticLcdPreview

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PetCosmeticsSheet(
    visible: Boolean,
    pet: PetState,
    onDismiss: () -> Unit,
    onEquip: (PetCosmetic?) -> Unit,
) {
    if (!visible) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Looks", style = MaterialTheme.typography.titleMedium)
            PetCosmeticLcdPreview(
                equippedCosmetic = pet.equippedCosmetic,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = "Tap a look to wear it on your pixel friend.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (pet.equippedCosmetic != null) {
                    FilterChip(
                        selected = false,
                        onClick = { onEquip(null) },
                        label = { Text("None") },
                    )
                }
                pet.unlockedCosmetics.sortedBy { it.ordinal }.forEach { cosmetic ->
                    FilterChip(
                        selected = pet.equippedCosmetic == cosmetic,
                        onClick = { onEquip(cosmetic) },
                        label = { Text(cosmeticLabel(cosmetic)) },
                    )
                }
            }
        }
    }
}

private fun cosmeticLabel(cosmetic: PetCosmetic): String =
    cosmetic.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
