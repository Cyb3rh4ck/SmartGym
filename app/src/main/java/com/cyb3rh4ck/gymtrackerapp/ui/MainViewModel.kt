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

    // INICIAR: Crea ejercicios con 1 serie vacía por defecto
    fun startRoutine(routine: Routine) {
        val exercises = routine.exercises.split(",")
            .filter { it.isNotBlank() }
            .mapIndexed { index, name ->
                ActiveExercise(
                    id = index,
                    name = name.trim(),
                    sets = listOf(ActiveSet()) // Empieza con la Serie 1 vacía
                )
            }
        _activeWorkout.value = exercises
    }

    // AGREGAR SERIE (Con lógica de duplicar datos anteriores)
    fun addSetToExercise(exerciseId: Int) {
        _activeWorkout.value = _activeWorkout.value.map { exercise ->
            if (exercise.id == exerciseId) {
                // Tomamos los datos de la última serie para copiarlos
                val lastSet = exercise.sets.lastOrNull()
                val newWeight = lastSet?.weight ?: ""
                val newReps = lastSet?.reps ?: ""

                // Creamos nueva serie con datos copiados pero check en false
                val newSet = ActiveSet(weight = newWeight, reps = newReps, isCompleted = false)

                exercise.copy(sets = exercise.sets + newSet)
            } else {
                exercise
            }
        }
    }

    // BORRAR SERIE (Opcional, pero útil)
    fun removeSetFromExercise(exerciseId: Int, setId: Long) {
        _activeWorkout.value = _activeWorkout.value.map { exercise ->
            if (exercise.id == exerciseId) {
                exercise.copy(sets = exercise.sets.filter { it.id != setId })
            } else {
                exercise
            }
        }
    }

    // ACTUALIZAR UNA SERIE ESPECÍFICA
    fun updateSet(exerciseId: Int, setId: Long, weight: String?, reps: String?, isDone: Boolean?) {
        _activeWorkout.value = _activeWorkout.value.map { exercise ->
            if (exercise.id == exerciseId) {
                val updatedSets = exercise.sets.map { set ->
                    if (set.id == setId) {
                        set.copy(
                            weight = weight ?: set.weight,
                            reps = reps ?: set.reps,
                            isCompleted = isDone ?: set.isCompleted
                        )
                    } else {
                        set
                    }
                }
                exercise.copy(sets = updatedSets)
            } else {
                exercise
            }
        }
    }



    // GUARDAR TODO (Aplanamos la lista para guardar cada serie individualmente)
    fun finishActiveWorkout() {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()

            _activeWorkout.value.forEach { exercise ->
                exercise.sets.forEach { set ->
                    // Solo guardamos si tiene datos válidos y está marcada como completada (opcional)
                    val w = set.weight.toFloatOrNull()
                    val r = set.reps.toIntOrNull()

                    if (w != null && r != null && w > 0 && r > 0) {
                        val log = WorkoutLog(
                            date = timestamp,
                            exerciseName = exercise.name,
                            muscleGroup = "Routine",
                            weightUsed = w,
                            reps = r,
                            rpe = 8 // Podríamos añadir un slider de RPE por serie si quisieras
                        )
                        dao.logWorkout(log)
                    }
                }
            }
            _activeWorkout.value = emptyList()
            loadData()
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

// 1. NUEVAS CLASES DE DATOS (Jerarquía: Ejercicio -> Lista de Series)
data class ActiveSet(
    val id: Long = System.nanoTime(), // ID único para identificar la serie en la UI
    val weight: String = "",
    val reps: String = "",
    val isCompleted: Boolean = false
)

data class ActiveExercise(
    val id: Int, // ID del ejercicio
    val name: String,
    val sets: List<ActiveSet> // AHORA CONTIENE UNA LISTA DE SERIES
)

