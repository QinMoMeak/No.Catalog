package com.nocatalog.app.presentation.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nocatalog.app.presentation.component.AppTopBar
import com.nocatalog.app.presentation.component.SectionCard
import com.nocatalog.app.presentation.component.ScreenTopContentPadding
import com.nocatalog.app.presentation.component.StatCard

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScaffoldWithStats(uiState = uiState)
}

@Composable
private fun ScaffoldWithStats(uiState: StatsUiState) {
    androidx.compose.material3.Scaffold(
        topBar = { AppTopBar(title = "统计") },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = ScreenTopContentPadding, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        title = "总记录数",
                        value = uiState.totalCount.toString(),
                        icon = Icons.Default.Movie,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = "已看",
                        value = uiState.watchedCount.toString(),
                        icon = Icons.Default.Visibility,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        title = "未看",
                        value = uiState.unwatchedCount.toString(),
                        icon = Icons.Default.VisibilityOff,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = "星标",
                        value = uiState.favoriteCount.toString(),
                        icon = Icons.Default.Favorite,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                SectionCard(
                    title = "近期概览",
                    subtitle = "把评分和新增趋势独立展示，便于快速判断内容状态。",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(
                            title = "平均评分",
                            value = "%.1f".format(uiState.averageRating),
                            icon = Icons.Default.Star,
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            title = "近 7 天新增",
                            value = uiState.addedIn7Days.toString(),
                            subtitle = "近 30 天 ${uiState.addedIn30Days}",
                            icon = Icons.Default.Schedule,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                SectionCard(title = "状态统计", subtitle = "显示数量、占比和进度，避免只有裸数字。") {
                    if (uiState.statusCounts.isEmpty()) {
                        Text("暂无状态数据", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        uiState.statusCounts.forEach { item ->
                            val ratio = if (uiState.totalCount == 0) 0f else item.count.toFloat() / uiState.totalCount.toFloat()
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(item.status.displayName(), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${item.count} · ${(ratio * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Top 标签") {
                    if (uiState.topTags.isEmpty()) {
                        Text("暂无标签统计", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        uiState.topTags.forEach { item ->
                            TopRankRow(
                                label = item.name,
                                value = item.count,
                                icon = Icons.AutoMirrored.Filled.Label,
                            )
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Top 演员") {
                    if (uiState.topPerformers.isEmpty()) {
                        Text("暂无演员统计", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        uiState.topPerformers.forEach { item ->
                            TopRankRow(
                                label = item.name,
                                value = item.count,
                                icon = Icons.Default.Movie,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopRankRow(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun com.nocatalog.app.domain.model.EntryStatus.displayName(): String = when (this) {
    com.nocatalog.app.domain.model.EntryStatus.WISH -> "想看"
    com.nocatalog.app.domain.model.EntryStatus.COLLECTED -> "已收录"
    com.nocatalog.app.domain.model.EntryStatus.WATCHED -> "已看"
    com.nocatalog.app.domain.model.EntryStatus.ARCHIVED -> "归档"
}
