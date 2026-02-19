package com.sukisu.ultra.ui.susfs.content

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.susfs.component.BottomActionButtons
import com.sukisu.ultra.ui.susfs.component.EmptyStateCard
import com.sukisu.ultra.ui.susfs.component.FeatureStatusCard
import com.sukisu.ultra.ui.susfs.util.SuSFSManager
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun EnabledFeaturesContent(
    enabledFeatures: List<SuSFSManager.EnabledFeature>,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            cornerRadius = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.susfs_enabled_features_description),
                    style = MiuixTheme.textStyles.body2,
                    color = colorScheme.onSurfaceVariantSummary,
                    lineHeight = 16.sp
                )
            }
        }

        if (enabledFeatures.isEmpty()) {
            EmptyStateCard(
                message = stringResource(R.string.susfs_no_features_found)
            )
        } else {
            enabledFeatures.forEach { feature ->
                FeatureStatusCard(
                    feature = feature,
                    onRefresh = onRefresh
                )
            }
        }
    }

    BottomActionButtons(
        primaryButtonText = stringResource(R.string.refresh),
        onPrimaryClick = onRefresh
    )
}

