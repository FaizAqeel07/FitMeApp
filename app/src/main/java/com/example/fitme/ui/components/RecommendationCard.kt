package com.example.fitme.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RecommendationCard(title: String, onStart: () -> Unit) {
    Card(modifier = Modifier.width(160.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Start")
            }
        }
    }
}
