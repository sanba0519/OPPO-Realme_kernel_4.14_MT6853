package com.sukisu.ultra.ui.susfs.content

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.susfs.component.BottomActionButtons
import com.sukisu.ultra.ui.susfs.component.DescriptionCard
import com.sukisu.ultra.ui.susfs.component.EmptyStateCard
import com.sukisu.ultra.ui.susfs.component.PathItemCard
import com.sukisu.ultra.ui.susfs.component.ResetButton
import com.sukisu.ultra.ui.susfs.component.SectionHeader

@Composable
fun SusMapsContent(
    susMaps: Set<String>,
    isLoading: Boolean,
    onAddSusMap: () -> Unit,
    onRemoveSusMap: (String) -> Unit,
    onEditSusMap: ((String) -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 说明卡片
        DescriptionCard(
            title = stringResource(R.string.sus_maps_description_title),
            description = stringResource(R.string.sus_maps_description_text),
            warning = stringResource(R.string.sus_maps_warning),
            additionalInfo = stringResource(R.string.sus_maps_debug_info)
        )

        if (susMaps.isEmpty()) {
            EmptyStateCard(
                message = stringResource(R.string.susfs_no_sus_maps_configured)
            )
        } else {
            SectionHeader(
                title = stringResource(R.string.sus_maps_section),
                subtitle = null,
                icon = Icons.Default.Security,
                count = susMaps.size
            )

            susMaps.toList().forEach { map ->
                PathItemCard(
                    path = map,
                    icon = Icons.Default.Security,
                    onDelete = { onRemoveSusMap(map) },
                    onEdit = if (onEditSusMap != null) {
                        { onEditSusMap(map) }
                    } else null,
                    isLoading = isLoading
                )
            }
        }
    }

    BottomActionButtons(
        primaryButtonText = stringResource(R.string.add),
        onPrimaryClick = onAddSusMap,
        isLoading = isLoading
    )

    if (onReset != null && susMaps.isNotEmpty()) {
        ResetButton(
            title = stringResource(R.string.susfs_reset_sus_maps_title),
            onClick = onReset
        )
    }
}

