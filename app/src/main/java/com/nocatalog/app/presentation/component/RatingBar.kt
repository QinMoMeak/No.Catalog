package com.nocatalog.app.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RatingBar(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = "评分：${"%.1f".format(rating)}")
        Slider(
            value = rating,
            onValueChange = { onRatingChange((it * 2).toInt() / 2f) },
            valueRange = 0f..5f,
            steps = 9,
        )
    }
}

