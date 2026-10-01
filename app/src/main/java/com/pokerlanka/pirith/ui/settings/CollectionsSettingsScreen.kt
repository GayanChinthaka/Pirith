package com.pokerlanka.pirith.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.pirith.domain.model.SuttaCollection
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val collections by viewModel.customCollections.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }

    var collectionToRename by remember { mutableStateOf<SuttaCollection?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }

    var collectionToDelete by remember { mutableStateOf<SuttaCollection?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "පිරිත් එකතු (Collections)",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "එකතු පෙළගැස්වීම සහ කළමනාකරණය",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    createName = ""
                    createError = null
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Collection")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (collections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "තවම ඔබගේ එකතු නොමැත",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ඔබට අවශ්‍ය පිරිත් වෙන වෙනම කාණ්ඩ කර තබා ගැනීමට පහත බොත්තම ඔබා නව එකතුවක් සාදන්න.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                createName = ""
                                createError = null
                                showCreateDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("නව එකතුවක් සාදන්න")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "එකතු අනුපිළිවෙල වෙනස් කිරීමට ඊතල (⬆ ⬇) භාවිතා කරන්න:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                }

                itemsIndexed(
                    items = collections,
                    key = { _, item -> item.id }
                ) { index, coll ->
                    val shape = when {
                        collections.size == 1 -> RoundedCornerShape(20.dp)
                        index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                        index == collections.size - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                        else -> RoundedCornerShape(6.dp)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = shape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reorder Arrows (Up / Down)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { viewModel.moveCollection(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move up",
                                        tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.moveCollection(index, index + 1) },
                                    enabled = index < collections.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move down",
                                        tint = if (index < collections.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Collection Name and item count
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = coll.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${coll.suttaIds.size} පිරිත් අඩංගුයි",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Rename Button
                            IconButton(
                                onClick = {
                                    collectionToRename = coll
                                    renameValue = coll.name
                                    renameError = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Delete Button
                            IconButton(
                                onClick = {
                                    collectionToDelete = coll
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Create Collection Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "නව එකතුවක් සාදන්න",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "ඔබට අවශ්‍ය එකතුව සඳහා නමක් ලබා දෙන්න:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = createName,
                        onValueChange = {
                            createName = it
                            createError = null
                        },
                        label = { Text("එකතුවේ නම") },
                        isError = createError != null,
                        supportingText = createError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = createName.trim()
                        if (trimmed.isNotBlank()) {
                            if (collections.any { it.name.equals(trimmed, ignoreCase = true) }) {
                                createError = "මෙම නමින් එකතුවක් දැනටමත් පවතී"
                            } else {
                                viewModel.createCollection(trimmed) { success ->
                                    if (success) {
                                        showCreateDialog = false
                                    } else {
                                        createError = "මෙම නමින් එකතුවක් දැනටමත් පවතී"
                                    }
                                }
                            }
                        }
                    },
                    enabled = createName.isNotBlank()
                ) {
                    Text("සාදන්න")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("අවලංගු කරන්න")
                }
            }
        )
    }

    // Rename Dialog
    collectionToRename?.let { coll ->
        AlertDialog(
            onDismissRequest = { collectionToRename = null },
            title = {
                Text(
                    text = "නම වෙනස් කරන්න",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "'${coll.name}' එකතුව සඳහා නව නමක් ඇතුළත් කරන්න:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameValue,
                        onValueChange = {
                            renameValue = it
                            renameError = null
                        },
                        label = { Text("නව නම") },
                        isError = renameError != null,
                        supportingText = renameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameValue.trim()
                        if (trimmed.isNotBlank()) {
                            if (collections.any { it.id != coll.id && it.name.equals(trimmed, ignoreCase = true) }) {
                                renameError = "මෙම නමින් එකතුවක් දැනටමත් පවතී"
                            } else {
                                viewModel.renameCollection(coll.id, trimmed) { success ->
                                    if (success) {
                                        collectionToRename = null
                                    } else {
                                        renameError = "මෙම නමින් එකතුවක් දැනටමත් පවතී"
                                    }
                                }
                            }
                        }
                    },
                    enabled = renameValue.isNotBlank()
                ) {
                    Text("වෙනස් කරන්න")
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToRename = null }) {
                    Text("අවලංගු කරන්න")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    collectionToDelete?.let { coll ->
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = {
                Text(
                    text = "එකතුව මකන්න ද?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("ඔබට '${coll.name}' එකතුව ඉවත් කිරීමට අවශ්‍ය ද? එහි ඇති පිරිත් ආරක්ෂිතව පවතිනු ඇත.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCollection(coll.id)
                        collectionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("මකන්න")
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) {
                    Text("අවලංගු කරන්න")
                }
            }
        )
    }
}
