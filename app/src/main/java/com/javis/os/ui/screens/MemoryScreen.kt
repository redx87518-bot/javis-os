package com.javis.os.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.javis.os.domain.model.UserMemory
import com.javis.os.ui.theme.*
import com.javis.os.ui.viewmodel.MemoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MemoryScreen(viewModel: MemoryViewModel = hiltViewModel()) {
    val memories by viewModel.memories.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Memory", style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = JavisRed)
                }
            }

            if (memories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = TextDim, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No memories yet", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        Text("JAVIS will remember things as you talk", style = MaterialTheme.typography.bodySmall, color = TextDim)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val grouped = memories.groupBy { it.category }
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = JavisCyan,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(items, key = { it.id }) { memory ->
                            MemoryCard(memory = memory, onDelete = { viewModel.forgetMemory(memory.key) })
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Memory", color = TextPrimary) },
            text = { Text("JAVIS will forget everything. This cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllMemories(); showClearDialog = false }) {
                    Text("Clear", color = JavisRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            titleContentColor = TextPrimary
        )
    }
}

@Composable
private fun MemoryCard(memory: UserMemory, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(memory.key.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(memory.value, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(memory.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = TextDim
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Delete", tint = TextDim, modifier = Modifier.size(16.dp))
        }
    }
}
