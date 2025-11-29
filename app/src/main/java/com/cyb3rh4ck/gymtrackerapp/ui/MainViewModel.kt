package com.cyb3rh4ck.gymtrackerapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cyb3rh4ck.gymtrackerapp.data.AppDatabase
import com.cyb3rh4ck.gymtrackerapp.data.WorkoutLog
import com.cyb3rh4ck.gymtrackerapp.domain.TrainingRecommender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.cyb3rh4ck.gymtrackerapp.data.Routine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainViewModel(context: Context) : ViewModel() {

    // 1. Dependencias
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.gymDao()
    private val recommender = TrainingRecommender()

    // 2. Estado
    private val _history = MutableStateFlow<List<WorkoutLog>>(emptyList())
    val history: StateFlow<List<WorkoutLog>> = _history.asStateFlow()

    private val _recommendation = MutableStateFlow("Cargando IA...")
    val recommendation: StateFlow<String> = _recommendation.asStateFlow()

    val routines: StateFlow<List<Routine>> = dao.getAllRoutines()
        .stateIn(viewModelScope,
            SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeWorkout = MutableStateFlow<List<ActiveExercise>>(emptyList())
    val activeWorkout: StateFlow<List<ActiveExercise>> = _activeWorkout.asStateFlow()

    // 3. Inicialización
    init {
        loadData()
    }

    // 4. Acciones
    private fun loadData() {
        viewModelScope.launch {
            val logs = dao.getAllLogs()
            _history.value = logs
            _recommendation.value = recommender.recommendNextWorkout(logs)
        }
    }

    fun saveWorkout(exercise: String, muscle: String, weight: Float, reps: Int, rpe: Int) {
        viewModelScope.launch {
            val newLog = WorkoutLog(
                exerciseName = exercise,
                muscleGroup = muscle,
                weightUsed = weight,
                reps = reps,
                rpe = rpe
            )
            dao.logWorkout(newLog)
            loadData()
        }
    }

    fun deleteWorkout(log: WorkoutLog) {
        viewModelScope.launch {
            dao.deleteWorkout(log)
            loadData()
        }
    }

    // Función para actualizar un entreno existente
    fun updateWorkout(log: WorkoutLog) {
        viewModelScope.launch {
            dao.updateWorkout(log)
            loadData() // Refrescamos la lista y la IA
        }
    }

    fun createRoutine(name: String, exercisesList: List<String>) {
        viewModelScope.launch {
            val exercisesString = exercisesList.joinToString(",")
            val newRoutine = Routine(name = name, exercises = exercisesString)
            dao.insertRoutine(newRoutine)
        }
    }

    // Función para borrar una Rutina completa
    fun deleteRoutine(routine: Routine) {
        viewModelScope.launch {
            dao.deleteRoutine(routine)
        }
    }

    fun startRoutine(routine: Routine) {
        val exercises = routine.exercises.split(",")
            .filter { it.isNotBlank() }
            .mapIndexed { index, name ->
                ActiveExercise(id = index, name = name.trim())
            }
        _activeWorkout.value = exercises
    }

    // Actualizar un campo (peso o reps) mientras el usuario escribe
    fun updateActiveExercise(id: Int, weight: String?, reps: String?, isDone: Boolean?) {
        _activeWorkout.value = _activeWorkout.value.map { item ->
            if (item.id == id) {
                item.copy(
                    weight = weight ?: item.weight,
                    reps = reps ?: item.reps,
                    isCompleted = isDone ?: item.isCompleted
                )
            } else {
                item
            }
        }
    }
    // Guardar TODO en la base de datos
    fun finishActiveWorkout() {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            _activeWorkout.value.forEach { item ->
                // Solo guardamos si rellenó datos válidos
                val w = item.weight.toFloatOrNull()
                val r = item.reps.toIntOrNull()

                if (w != null && r != null && w > 0 && r > 0) {
                    val log = WorkoutLog(
                        date = timestamp,
                        exerciseName = item.name,
                        muscleGroup = "Routine", // Podríamos mejorarlo luego, por ahora genérico
                        weightUsed = w,
                        reps = r,
                        rpe = 8 // Valor por defecto o podríamos pedirlo
                    )
                    dao.logWorkout(log)
                }
            }
            // Limpiamos el estado
            _activeWorkout.value = emptyList()
            loadData() // Recargamos historial
        }
    }

    // Cancelar entreno
    fun cancelActiveWorkout() {
        _activeWorkout.value = emptyList()
    }



}


// La Factory se queda fuera, eso está bien
class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Modelo temporal para la pantalla de "Modo Entreno"
data class ActiveExercise(
    val id: Int, // ID único temporal para la lista
    val name: String,
    val weight: String = "",
    val reps: String = "",
    val isCompleted: Boolean = false
)

