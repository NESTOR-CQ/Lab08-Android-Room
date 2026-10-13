package com.example.lab08

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.lab08.ui.theme.Lab08Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                )
                .fallbackToDestructiveMigration()
                .build()

                val taskDao = db.taskDao()
                val viewModel: TaskViewModel = viewModel(
                    factory = TaskViewModel.Factory(taskDao)
                )

                TaskScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current

    val tasks by viewModel.tasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val categories = listOf("Todas", "Personal", "Trabajo", "Estudio", "Hogar", "Otro")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Todoist Lab08",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Última sincr.: $lastSyncTime",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Característica 10: Sincronización en la nube
                    IconButton(
                        onClick = {
                            viewModel.syncWithCloud { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        if (syncState == SyncState.SINCRONIZANDO) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sincronizar nube",
                                tint = if (syncState == SyncState.PENDIENTE) Color(0xFFF57C00) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteAllTasks()
                            Toast.makeText(context, "Todas las tareas eliminadas", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Limpiar todo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Tarea")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // CARACTERÍSTICA 4: Buscar tareas
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar tareas por nombre...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // CARACTERÍSTICA 9: Categorías de tareas (Filtro por Categorías)
            Text(
                text = "Categorías",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = (selectedCategoryFilter == cat),
                        onClick = { viewModel.selectedCategoryFilter.value = cat },
                        label = { Text(cat) },
                        leadingIcon = if (selectedCategoryFilter == cat) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // CARACTERÍSTICAS 3 y 5: Filtrar por Estado y Ordenar Tareas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CARACTERÍSTICA 3: Filtro por estado
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = (selectedStatusFilter == filter),
                            onClick = { viewModel.selectedStatusFilter.value = filter },
                            label = {
                                Text(
                                    when (filter) {
                                        StatusFilter.TODAS -> "Todas"
                                        StatusFilter.PENDIENTES -> "Pendientes"
                                        StatusFilter.COMPLETADAS -> "Completadas"
                                    },
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                // CARACTERÍSTICA 5: Ordenar tareas
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Ordenar por")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ordenar por Fecha") },
                            onClick = {
                                viewModel.selectedSortOption.value = SortOption.FECHA
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ordenar por Nombre") },
                            onClick = {
                                viewModel.selectedSortOption.value = SortOption.NOMBRE
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ordenar por Prioridad") },
                            onClick = {
                                viewModel.selectedSortOption.value = SortOption.PRIORIDAD
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ordenar por Estado") },
                            onClick = {
                                viewModel.selectedSortOption.value = SortOption.ESTADO
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // CARACTERÍSTICA 10: Banner Estado Sincronización en la Nube
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (syncState) {
                        SyncState.SINCRONIZADO -> Color(0xFFE8F5E9)
                        SyncState.SINCRONIZANDO -> Color(0xFFFFFDE7)
                        SyncState.PENDIENTE -> Color(0xFFFFF3E0)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (syncState) {
                            SyncState.SINCRONIZADO -> "☁️ Nube: Sincronizado"
                            SyncState.SINCRONIZANDO -> "⏳ Nube: Sincronizando..."
                            SyncState.PENDIENTE -> "⚠️ Nube: Cambios pendientes de respaldo"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )

                    TextButton(
                        onClick = {
                            viewModel.syncWithCloud { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Sincronizar", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // LISTA DE TAREAS
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay tareas encontradas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = {
                                viewModel.deleteTask(task)
                                Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // CARACTERÍSTICA 1 & NUEVA TAREA: Diálogo de Agregar / Editar Tarea
    if (showAddDialog) {
        TaskDialog(
            title = "Nueva Tarea",
            initialTask = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { desc, priority, category, recurrence, hasReminder, reminderTime ->
                viewModel.addTask(desc, priority, category, recurrence, hasReminder, reminderTime)
                if (hasReminder) {
                    Toast.makeText(context, "🔔 Recordatorio programado para las $reminderTime", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            }
        )
    }

    if (taskToEdit != null) {
        TaskDialog(
            title = "Editar Tarea",
            initialTask = taskToEdit,
            onDismiss = { taskToEdit = null },
            onConfirm = { desc, priority, category, recurrence, hasReminder, reminderTime ->
                val updated = taskToEdit!!.copy(
                    description = desc,
                    priority = priority,
                    category = category,
                    recurrence = recurrence,
                    hasReminder = hasReminder,
                    reminderTime = reminderTime
                )
                viewModel.updateTask(updated)
                if (hasReminder) {
                    Toast.makeText(context, "🔔 Recordatorio actualizado para las $reminderTime", Toast.LENGTH_SHORT).show()
                }
                taskToEdit = null
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    onToggleCompletion: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // CARACTERÍSTICA 7: Prioridad con distintivo de color estilo Todoist
    val priorityColor = when (task.priority.lowercase()) {
        "alta" -> Color(0xFFD32F2F)   // Rojo estilo Todoist
        "media" -> Color(0xFFF57C00)  // Naranja estilo Todoist
        "baja" -> Color(0xFF1976D2)   // Azul estilo Todoist
        else -> Color.Gray
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // --- FILA SUPERIOR: Indicador de Prioridad, Checkbox, Descripción y Recordatorio Destacado ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Franja de prioridad de color
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(priorityColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Checkbox para marcar completada
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleCompletion() }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Descripción de la tarea
                Text(
                    text = task.description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // CARACTERÍSTICA 6: Recordatorio visible en la esquina superior derecha de la tarjeta
                if (task.hasReminder) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE0F2F1),
                        border = BorderStroke(1.dp, Color(0xFF80CBC4))
                    ) {
                        Text(
                            text = "🔔 ${task.reminderTime.ifEmpty { "Progr." }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00695C),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- FILA INFERIOR: Badges Informativos a la izquierda y Botones de Acción Compactos a la derecha ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badges en un LazyRow (Categoría, Prioridad, Recurrencia)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Badge Categoría (CARACTERÍSTICA 9)
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "🏷️ ${task.category}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Badge Prioridad (CARACTERÍSTICA 7)
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = priorityColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Prioridad: ${task.priority}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Badge Recurrencia (CARACTERÍSTICA 8)
                    if (task.recurrence != "Ninguna") {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "🔁 ${task.recurrence}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // --- BOTONES DE ACCIÓN COMPACTOS (Editar y Eliminar) ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Editar compacto
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Botón Eliminar compacto
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDialog(
    title: String,
    initialTask: Task?,
    onDismiss: () -> Unit,
    onConfirm: (
        description: String,
        priority: String,
        category: String,
        recurrence: String,
        hasReminder: Boolean,
        reminderTime: String
    ) -> Unit
) {
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "Media") }
    var category by remember { mutableStateOf(initialTask?.category ?: "Personal") }
    var recurrence by remember { mutableStateOf(initialTask?.recurrence ?: "Ninguna") }
    var hasReminder by remember { mutableStateOf(initialTask?.hasReminder ?: false) }
    var reminderTime by remember { mutableStateOf(initialTask?.reminderTime ?: "09:00 AM") }

    val priorities = listOf("Alta", "Media", "Baja")
    val categories = listOf("Personal", "Trabajo", "Estudio", "Hogar", "Otro")
    val recurrences = listOf("Ninguna", "Diaria", "Semanal", "Mensual")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción de la tarea") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // CARACTERÍSTICA 7: Prioridad (Alta, Media, Baja)
                Text("Prioridad", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(priorities) { p ->
                        val pColor = when (p) {
                            "Alta" -> Color(0xFFD32F2F)
                            "Media" -> Color(0xFFF57C00)
                            else -> Color(0xFF1976D2)
                        }
                        FilterChip(
                            selected = (priority == p),
                            onClick = { priority = p },
                            label = {
                                Text(
                                    p,
                                    color = if (priority == p) Color.White else pColor,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pColor
                            )
                        )
                    }
                }

                // CARACTERÍSTICA 9: Categorías
                Text("Categoría", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = (category == cat),
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // CARACTERÍSTICA 8: Recurrencia
                Text("Recurrencia", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(recurrences) { rec ->
                        FilterChip(
                            selected = (recurrence == rec),
                            onClick = { recurrence = rec },
                            label = { Text(rec) }
                        )
                    }
                }

                // CARACTERÍSTICA 6: Notificación / Recordatorio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Configurar Recordatorio", style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = hasReminder,
                        onCheckedChange = { hasReminder = it }
                    )
                }

                if (hasReminder) {
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        label = { Text("Hora del recordatorio (ej. 09:00 AM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isNotBlank()) {
                        onConfirm(description, priority, category, recurrence, hasReminder, reminderTime)
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
