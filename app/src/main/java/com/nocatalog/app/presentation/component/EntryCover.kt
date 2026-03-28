package com.nocatalog.app.presentation.component

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nocatalog.app.domain.model.Entry
import java.io.File

/**
 * Presentation 层统一封面入口，优先本地路径，兼容后续缩略图字段扩展。
 */
@Composable
fun EntryCover(
    entry: Entry,
    modifier: Modifier = Modifier.width(84.dp),
) {
    val model = entry.coverThumbPath?.takeIf { it.isNotBlank() }?.let(::File)
        ?: entry.coverLocalPath?.takeIf { it.isNotBlank() }?.let(::File)
        ?: entry.coverRemoteUrl?.takeIf { it.isNotBlank() }

    val coverModifier = modifier.aspectRatio(2f / 3f)

    if (model == null) {
        EmptyCoverPlaceholder(modifier = coverModifier)
    } else {
        AsyncImage(
            model = model,
            contentDescription = entry.title,
            contentScale = ContentScale.Crop,
            modifier = coverModifier,
        )
    }
}
