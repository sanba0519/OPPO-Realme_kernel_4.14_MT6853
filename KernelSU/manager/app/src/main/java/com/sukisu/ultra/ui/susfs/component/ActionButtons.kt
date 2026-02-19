package com.sukisu.ultra.ui.susfs.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BottomActionButtons(
    modifier: Modifier = Modifier,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .padding(top = 12.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (secondaryButtonText != null && onSecondaryClick != null) {
                Button(
                    onClick = onSecondaryClick,
                    enabled = !isLoading && enabled,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    cornerRadius = 8.dp
                ) {
                    Text(
                        text = secondaryButtonText,
                        style = MiuixTheme.textStyles.body2
                    )
                }
                Button(
                    onClick = onPrimaryClick,
                    enabled = !isLoading && enabled,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    cornerRadius = 8.dp
                ) {
                    Text(
                        text = primaryButtonText,
                        style = MiuixTheme.textStyles.body2
                    )
                }
            } else {
                Button(
                    onClick = onPrimaryClick,
                    enabled = !isLoading && enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    cornerRadius = 8.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = primaryButtonText,
                        style = MiuixTheme.textStyles.body2
                    )
                }
            }
        }
    }
}

@Composable
fun ResetButton(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth(),
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            cornerRadius = 8.dp
        ) {
            Text(
                text = title
            )
        }
    }
}
