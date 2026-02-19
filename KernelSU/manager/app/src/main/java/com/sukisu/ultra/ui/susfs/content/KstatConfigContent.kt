package com.sukisu.ultra.ui.susfs.content

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.susfs.component.AddKstatPathItemCard
import com.sukisu.ultra.ui.susfs.component.BottomActionButtons
import com.sukisu.ultra.ui.susfs.component.DescriptionCard
import com.sukisu.ultra.ui.susfs.component.EmptyStateCard
import com.sukisu.ultra.ui.susfs.component.KstatConfigItemCard
import com.sukisu.ultra.ui.susfs.component.SectionHeader

@Composable
fun KstatConfigContent(
    kstatConfigs: Set<String>,
    addKstatPaths: Set<String>,
    isLoading: Boolean,
    onAddKstatStatically: () -> Unit,
    onAddKstat: () -> Unit,
    onRemoveKstatConfig: (String) -> Unit,
    onEditKstatConfig: ((String) -> Unit)? = null,
    onRemoveAddKstat: (String) -> Unit,
    onEditAddKstat: ((String) -> Unit)? = null,
    onUpdateKstat: (String) -> Unit,
    onUpdateKstatFullClone: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DescriptionCard(
            title = stringResource(R.string.kstat_config_description_title),
            description = stringResource(R.string.kstat_config_description_add_statically) + "\n" +
                    stringResource(R.string.kstat_config_description_add) + "\n" +
                    stringResource(R.string.kstat_config_description_update) + "\n" +
                    stringResource(R.string.kstat_config_description_update_full_clone)
        )

        if (kstatConfigs.isNotEmpty()) {
            SectionHeader(
                title = stringResource(R.string.static_kstat_config),
                subtitle = null,
                icon = Icons.Default.Settings,
                count = kstatConfigs.size
            )
            kstatConfigs.toList().forEach { config ->
                KstatConfigItemCard(
                    config = config,
                    onDelete = { onRemoveKstatConfig(config) },
                    onEdit = if (onEditKstatConfig != null) {
                        { onEditKstatConfig(config) }
                    } else null,
                    isLoading = isLoading
                )
            }
        }

        if (addKstatPaths.isNotEmpty()) {
            SectionHeader(
                title = stringResource(R.string.kstat_path_management),
                subtitle = null,
                icon = Icons.Default.Folder,
                count = addKstatPaths.size
            )
            addKstatPaths.toList().forEach { path ->
                AddKstatPathItemCard(
                    path = path,
                    onDelete = { onRemoveAddKstat(path) },
                    onEdit = if (onEditAddKstat != null) {
                        { onEditAddKstat(path) }
                    } else null,
                    onUpdate = { onUpdateKstat(path) },
                    onUpdateFullClone = { onUpdateKstatFullClone(path) },
                    isLoading = isLoading
                )
            }
        }

        if (kstatConfigs.isEmpty() && addKstatPaths.isEmpty()) {
            EmptyStateCard(
                message = stringResource(R.string.no_kstat_config_message)
            )
        }
    }

    BottomActionButtons(
        primaryButtonText = stringResource(R.string.add),
        onPrimaryClick = onAddKstat,
        secondaryButtonText = stringResource(R.string.add),
        onSecondaryClick = onAddKstatStatically,
        isLoading = isLoading
    )
}

