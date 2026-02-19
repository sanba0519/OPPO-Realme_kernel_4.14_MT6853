package com.sukisu.ultra.ui.susfs.content

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Loop
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
fun SusLoopPathsContent(
    susLoopPaths: Set<String>,
    isLoading: Boolean,
    onAddLoopPath: () -> Unit,
    onRemoveLoopPath: (String) -> Unit,
    onEditLoopPath: ((String) -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 说明卡片
        DescriptionCard(
            title = stringResource(R.string.sus_loop_paths_description_title),
            description = stringResource(R.string.sus_loop_paths_description_text),
            warning = stringResource(R.string.susfs_loop_path_restriction_warning)
        )

        if (susLoopPaths.isEmpty()) {
            EmptyStateCard(
                message = stringResource(R.string.susfs_no_loop_paths_configured)
            )
        } else {
            SectionHeader(
                title = stringResource(R.string.loop_paths_section),
                subtitle = null,
                icon = Icons.Default.Loop,
                count = susLoopPaths.size
            )

            susLoopPaths.toList().forEach { path ->
                PathItemCard(
                    path = path,
                    icon = Icons.Default.Loop,
                    onDelete = { onRemoveLoopPath(path) },
                    onEdit = if (onEditLoopPath != null) {
                        { onEditLoopPath(path) }
                    } else null,
                    isLoading = isLoading
                )
            }
        }
    }

    BottomActionButtons(
        primaryButtonText = stringResource(R.string.add_loop_path),
        onPrimaryClick = onAddLoopPath,
        isLoading = isLoading
    )

    if (onReset != null && susLoopPaths.isNotEmpty()) {
        ResetButton(
            title = stringResource(R.string.susfs_reset_loop_paths_title),
            onClick = onReset
        )
    }
}

