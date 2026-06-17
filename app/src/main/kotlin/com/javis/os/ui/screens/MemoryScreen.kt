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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.javis.os.domain.model.Memory
import com.javis.os.domain.repository.MemoryRepository
import com.javis.os.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository
) : ViewModel() {
    val memories: StateFlow<List<Memory>> = memoryRepository.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun forgetMemory(key: String) {
        viewModelScope.launch { memoryRepository.forgetKey(key) }
    }
}

@Composable
fun MemoryScreen(
    navController: NavController,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val memories by viewModel.memories.collectAsState()
    val grouped = memories.groupBy { it.category }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("Memory Bank", style = MaterialTheme.typography.titleLarge, color = CyanPrimary)
            Text(
                "${memories.size} memories",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF546E7A),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        HorizontalDivider(color = CyanPrimary.copy(alpha = 0.2f))

        if (memories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = CyanPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No memories yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceLight.copy(alpha = 0.5f)
                    )
                    Text(
                        "Tell JAVIS your name, interests, or anything\nyou want remembered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF546E7A),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                grouped.forEach { (category, items) ->
                    item {
                        MemoryCategoryHeader(category = category)
                    }
                    items(items) { memory ->
                        MemoryItem(memory = memory, onForget = { viewModel.forgetMemory(memory.key) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCategoryHeader(category: String) {
    val (icon, color) = when (category) {
        "user_info" -> Icons.Default.Person to CyanPrimary
        "preference" -> Icons.Default.Favorite to PurpleAccent
        "habit" -> Icons.Default.Loop to SuccessGreen
        "contact" -> Icons.Default.Contacts to WarningAmber
        "routine" -> Icons.Default.Schedule to Color(0xFF26C6DA)
        else -> Icons.Default.Star to OnSurfaceLight
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text = category.replace("_", " ").replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MemoryItem(memory: Memory, onForget: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memory.key.replace("_", " ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = CyanPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceLight
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(memory.importance.coerceIn(1, 5)) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onForget,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Forget",
                        tint = ErrorRed.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
