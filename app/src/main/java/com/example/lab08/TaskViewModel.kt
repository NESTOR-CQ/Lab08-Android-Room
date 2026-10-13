package com.example.lab08

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class StatusFilter { TODAS, PENDIENTES, COMPLETADAS }
enum class SortOption { FECHA, NOMBRE, PRIORIDAD, ESTADO }
enum class SyncState { SINCRONIZADO, SINCRONIZANDO, PENDIENTE }

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())

    // Filtros y búsquedas
    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow(StatusFilter.TODAS)
    val selectedCategoryFilter = MutableStateFlow("Todas")
    val selectedSortOption = MutableStateFlow(SortOption.FECHA)

    // Sincronización en la nube (Simulada)
    val syncState = MutableStateFlow(SyncState.SINCRONIZADO)
    val lastSyncTime = MutableStateFlow("Hoy, recién iniciado")

    // Flujo combinado de tareas
    val tasks: StateFlow<List<Task>> = combine(
        _allTasks,
        searchQuery,
        selectedStatusFilter,
        selectedCategoryFilter,
        selectedSortOption
    ) { list, query, statusFilter, categoryFilter, sortOption ->
        list
            .filter { task ->
                if (query.isBlank()) true
                else task.description.contains(query, ignoreCase = true)
            }
            .filter { task ->
                when (statusFilter) {
                    StatusFilter.TODAS -> true
                    StatusFilter.PENDIENTES -> !task.isCompleted
                    StatusFilter.COMPLETADAS -> task.isCompleted
                }
            }
            .filter { task ->
                if (categoryFilter == "Todas") true
                else task.category.equals(categoryFilter, ignoreCase = true)
            }
            .sortedWith { t1, t2 ->
                when (sortOption) {
                    SortOption.FECHA -> t2.id.compareTo(t1.id)
                    SortOption.NOMBRE -> t1.description.compareTo(t2.description, ignoreCase = true)
                    SortOption.PRIORIDAD -> priorityValue(t1.priority).compareTo(priorityValue(t2.priority))
                    SortOption.ESTADO -> t1.isCompleted.compareTo(t2.isCompleted)
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _allTasks.value = dao.getAllTasks()
        }
    }

    fun addTask(
        description: String,
        priority: String = "Media",
        category: String = "Personal",
        recurrence: String = "Ninguna",
        hasReminder: Boolean = false,
        reminderTime: String = ""
    ) {
        if (description.isBlank()) return
        viewModelScope.launch {
            val newTask = Task(
                description = description.trim(),
                priority = priority,
                category = category,
                recurrence = recurrence,
                hasReminder = hasReminder,
                reminderTime = reminderTime
            )
            dao.insertTask(newTask)
            syncState.value = SyncState.PENDIENTE
            loadTasks()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task)
            syncState.value = SyncState.PENDIENTE
            loadTasks()
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
            syncState.value = SyncState.PENDIENTE
            loadTasks()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            dao.updateTask(updatedTask)
            syncState.value = SyncState.PENDIENTE
            loadTasks()
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            dao.deleteAllTasks()
            syncState.value = SyncState.PENDIENTE
            loadTasks()
        }
    }

    // Característica 10: Sincronización en la nube simulada
    fun syncWithCloud(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            syncState.value = SyncState.SINCRONIZANDO
            delay(1500)
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            lastSyncTime.value = "Hoy, $currentTime"
            syncState.value = SyncState.SINCRONIZADO
            onComplete("Respaldo en la nube sincronizado (${_allTasks.value.size} tareas)")
        }
    }

    private fun priorityValue(priority: String): Int {
        return when (priority.lowercase(Locale.getDefault())) {
            "alta" -> 1
            "media" -> 2
            "baja" -> 3
            else -> 4
        }
    }

    class Factory(private val dao: TaskDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(dao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
