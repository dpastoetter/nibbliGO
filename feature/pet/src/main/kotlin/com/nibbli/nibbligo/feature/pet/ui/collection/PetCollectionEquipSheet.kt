package com.nibbli.nibbligo.feature.pet.ui.collection

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.pixel.LcdPickerEntry
import com.nibbli.nibbligo.feature.pet.ui.pixel.PetLcdItemPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetCollectionEquipSheet(
    visible: Boolean,
    pet: PetState,
    entries: List<LcdCollectionEntry>,
    initialItemId: String?,
    onEquip: (LcdPickerEntry) -> Unit,
    onSeeAll: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val focusIndex = initialItemId?.let { id -> entries.indexOfFirst { it.id == id } } ?: -1
    val unlockedCount = entries.count { it.unlocked }

    LaunchedEffect(focusIndex) {
        if (focusIndex >= 0) {
            listState.animateScrollToItem(focusIndex)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Collectibles",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (onSeeAll != null) {
                    TextButton(onClick = {
                        onDismiss()
                        onSeeAll()
                    }) {
                        Text("See all")
                    }
                }
            }
            Text(
                text = "$unlockedCount of ${entries.size} unlocked · tap Equip to dress up your P1 shell.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    CollectionEquipRow(
                        entry = entry,
                        pet = pet,
                        highlighted = entry.id == initialItemId,
                        onEquip = {
                            entry.toPickerEntry()?.let { pickerEntry ->
                                onEquip(pickerEntry)
                                onDismiss()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionEquipRow(
    entry: LcdCollectionEntry,
    pet: PetState,
    highlighted: Boolean,
    onEquip: () -> Unit,
) {
    val equipped = entry.isEquipped(pet)
    val shape = RoundedCornerShape(16.dp)
    val borderColor = when {
        highlighted -> MaterialTheme.colorScheme.primary
        equipped -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithLockOverlay(
            entry = entry,
            modifier = Modifier.size(48.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.displayName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = entry.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!entry.unlocked) {
                Text(
                    text = "Unlock: ${entry.unlockHint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        when {
            equipped -> {
                Text(
                    text = "Equipped",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            entry.unlocked -> {
                TextButton(onClick = onEquip) {
                    Text("Equip")
                }
            }
            else -> {
                Text(
                    text = "Locked",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BoxWithLockOverlay(
    entry: LcdCollectionEntry,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        PetLcdItemPreview(
            itemRef = entry.itemRef,
            locked = !entry.unlocked,
            modifier = Modifier
                .size(width = 44.dp, height = 36.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        if (!entry.unlocked) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
