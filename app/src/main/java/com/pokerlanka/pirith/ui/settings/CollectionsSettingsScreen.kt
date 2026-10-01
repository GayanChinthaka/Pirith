package com.pokerlanka.pirith.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pokerlanka.pirith.domain.model.CollectionItem
import com.pokerlanka.pirith.domain.model.Sutta
import com.pokerlanka.pirith.domain.model.SuttaCollection
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val allCollections by viewModel.allCollections.collectAsState()
    val allSuttas by viewModel.allSuttas.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }

    var collectionToRename by remember { mutableStateOf<SuttaCollection?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }

    var collectionToDelete by remember { mutableStateOf<SuttaCollection?>(null) }

    // Bottom sheet state for reordering suttas inside a custom collection
    var managingCollectionSuttas by remember { mutableStateOf<SuttaCollection?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "පිරිත් එකතු (Collections / Categories)",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "සියලුම ප්‍රවර්ග සහ එකතු පෙළගැස්වීම",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "ප්‍රධාන තිරයේ Tabs දිස්වන අනුපිළිවෙල වෙනස් කිරීමට ඊතල (⬆ ⬇) භාවිතා කරන්න:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )
            }

            itemsIndexed(
                items = allCollections,
                key = { _, item -> item.key }
            ) { index, item ->
                val shape = when {
                    allCollections.size == 1 -> RoundedCornerShape(20.dp)
                    index == 0 -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                    index == allCollections.size - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                    else -> RoundedCornerShape(6.dp)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!item.isDefault && item.customCollection != null) {
                                Modifier.clickable {
                                    managingCollectionSuttas = item.customCollection
                                }
                            } else Modifier
                        ),
                    shape = shape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isDefault) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                enabled = index < allCollections.size - 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    tint = if (index < allCollections.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Type Badge & Title
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (item.isDefault) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        text = if (item.isDefault) "පෙරනිමි" else "මගේ එකතුවක්",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (item.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (!item.isDefault) "${item.suttaCount} පිරිත් • පිළිවෙල වෙනස් කිරීමට තට්ටු කරන්න" else "${item.suttaCount} පිරිත් අඩංගුයි",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Custom collection actions: Sutta order button, Rename and Delete
                        if (!item.isDefault && item.customCollection != null) {
                            // Reorder Suttas Button
                            IconButton(
                                onClick = {
                                    managingCollectionSuttas = item.customCollection
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListNumbered,
                                    contentDescription = "Reorder suttas",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Rename Button
                            IconButton(
                                onClick = {
                                    collectionToRename = item.customCollection
                                    renameValue = item.customCollection.name
                                    renameError = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Delete Button
                            IconButton(
                                onClick = {
                                    collectionToDelete = item.customCollection
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            // System default lock indicator
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Default category",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // ModalBottomSheet: Reorder Suttas within a Custom Collection
    managingCollectionSuttas?.let { targetColl ->
        // Keep updated state from allCollections or customCollections
        val currentColl = allCollections.find { it.key == "coll_${targetColl.id}" }?.customCollection ?: targetColl
        val suttasMap = remember(allSuttas) { allSuttas.associateBy { it.id } }
        val collectionSuttas = currentColl.suttaIds.mapNotNull { suttasMap[it] }

        ModalBottomSheet(
            onDismissRequest = { managingCollectionSuttas = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentColl.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "එකතුව තුළ පිරිත් අනුපිළිවෙල (⬆ ⬇)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { managingCollectionSuttas = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (collectionSuttas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "මෙම එකතුවට තවම පිරිත් කිසිවක් එක් කර නැත.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(
                            items = collectionSuttas,
                            key = { _, sutta -> sutta.id }
                        ) { suttaIndex, sutta ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Sutta Index Badge
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${suttaIndex + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Titles
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sutta.titleSinhala,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = sutta.titlePali,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Up/Down arrows to reorder within collection
                                    IconButton(
                                        onClick = {
                                            viewModel.moveSuttaInCollection(
                                                collectionId = currentColl.id,
                                                fromIndex = suttaIndex,
                                                toIndex = suttaIndex - 1
                                            )
                                        },
                                        enabled = suttaIndex > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move up",
                                            tint = if (suttaIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.moveSuttaInCollection(
                                                collectionId = currentColl.id,
                                                fromIndex = suttaIndex,
                                                toIndex = suttaIndex + 1
                                            )
                                        },
                                        enabled = suttaIndex < collectionSuttas.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move down",
                                            tint = if (suttaIndex < collectionSuttas.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
                            if (allCollections.any { it.name.equals(trimmed, ignoreCase = true) }) {
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
                            if (allCollections.any { it.key != "coll_${coll.id}" && it.name.equals(trimmed, ignoreCase = true) }) {
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
