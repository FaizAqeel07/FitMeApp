package com.example.fitme.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun StatusChip(label: String, isDone: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isDone,
        onClick = onClick,
        label = { Text(if (isDone) "$label ✓" else "$label ✗") },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}
