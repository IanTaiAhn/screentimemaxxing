package com.brainrotrpg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopSheet(
    spendableBrainrotHours: Float,
    spendableMidHours: Float,
    spendableEnrichmentHours: Float,
    onDismiss: () -> Unit,
    onSelectObject: (RoomObjectType) -> Unit   // enters placement mode for this type
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Shop", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "🧟 ${spendableBrainrotHours.format(1)} hrs  " +
                       "😐 ${spendableMidHours.format(1)} hrs  " +
                       "🎧 ${spendableEnrichmentHours.format(1)} hrs",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(RoomObjectType.entries) { type ->
                    ShopRow(
                        type = type,
                        canAfford = when (type.costCategory) {
                            Category.BRAINROT -> spendableBrainrotHours >= type.costHours
                            Category.MID -> spendableMidHours >= type.costHours
                            Category.ENRICHMENT -> spendableEnrichmentHours >= type.costHours
                            Category.UNTRACKED -> false
                        },
                        onPlace = {
                            onSelectObject(type)
                            onDismiss()
                        }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ShopRow(
    type: RoomObjectType,
    canAfford: Boolean,
    onPlace: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${type.emoji} ${type.displayName}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = type.description,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Cost: ${type.costHours}h ${type.costCategory.name.lowercase()} time",
                style = MaterialTheme.typography.bodySmall,
                color = if (canAfford)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = onPlace,
            enabled = canAfford
        ) {
            Text("Place")
        }
    }
}

// Extension to format Float to N decimal places
private fun Float.format(decimals: Int) = "%.${decimals}f".format(this)
