package com.nocatalog.app.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nocatalog.app.domain.model.Entry

@Composable
fun EntryCard(
    entry: Entry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = entry.code, style = MaterialTheme.typography.labelLarge)
                Text(text = entry.status.name)
            }
            Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "演员：${entry.performers.joinToString { it.name }.ifBlank { "未填写" }}")
            Text(text = "标签：${entry.tags.joinToString { it.name }.ifBlank { "未填写" }}")
            Text(text = "评分：${entry.rating}")
        }
    }
}

