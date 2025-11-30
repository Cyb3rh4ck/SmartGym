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
import com.cyb3rh4ck.gymtrackerapp.ui.Models.RoutineExerciseConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import com.cyb3rh4ck.gymtrackerapp.data.CompletedExercise
import com.cyb3rh4ck.gymtrackerapp.ui.CompletedSet

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

    private val gson = Gson()
    // --- ESTADO PARA LA CREACIÓN DE RUTINA ---
    // Lista temporal de ejercicios que estamos configurando
    private val _draftRoutineExercises = MutableStateFlow<List<RoutineExerciseConfig>>(emptyList())
    val draftRoutineExercises: StateFlow<List<RoutineExerciseConfig>> = _draftRoutineExercises.asStateFlow()

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

    // Ahora debe ser capaz de leer el formato JSON nuevo
    fun startRoutine(routine: Routine) {
        val exerciseList: List<ActiveExercise> = try {
            // 1. Intentamos leerlo como el nuevo formato JSON
            val listType = object : TypeToken<List<RoutineExerciseConfig>>() {}.type
            val configs: List<RoutineExerciseConfig> = gson.fromJson(routine.exercises, listType)

            // Convertimos la Configuración a Ejercicios Activos
            configs.mapIndexed { index, config ->
                // Creamos las series vacías basadas en el "targetSets"
                val initialSets = List(config.targetSets) {
                    ActiveSet(
                        reps = config.targetReps, // Precargamos el objetivo de reps como sugerencia
                        weight = "" // El peso lo dejamos vacío o lo traemos del historial (ver paso anterior)
                    )
                }

                ActiveExercise(
                    id = index,
                    name = config.name,
                    sets = initialSets
                    // Aquí podrías pasar también el tiempo de descanso al ActiveExercise si quieres usarlo en el Timer
                )
            }
        } catch (e: Exception) {
            // 2. FALLBACK: Si falla (es una rutina vieja separada por comas), usamos la lógica antigua
            routine.exercises.split(",")
                .filter { it.isNotBlank() }
                .mapIndexed { index, name ->
                    ActiveExercise(
                        id = index,
                        name = name.trim(),
                        sets = listOf(ActiveSet())
                    )
                }
        }

        _activeWorkout.value = exerciseList
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
            if (exercise.sets.size > 1) {
                exercise.copy(sets = exercise.sets.filter { it.id != setId })
            } else {
                exercise
            }
        }
    }

    // ACTUALIZAR UNA SERIE ESPECÍFICA
    fun updateSet(exerciseId: Int, setId: Long, weight: String?, reps: String?, isDone: Boolean?, rpe: String?) {
        _activeWorkout.value = _activeWorkout.value.map { exercise ->
            if (exercise.id == exerciseId) {
                val updatedSets = exercise.sets.map { set ->
                    if (set.id == setId) {
                        set.copy(
                            weight = weight ?: set.weight,
                            reps = reps ?: set.reps,
                            isCompleted = isDone ?: set.isCompleted,
                            rpe = rpe ?: set.rpe
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
        val activeWorkoutList = _activeWorkout.value
        if (activeWorkoutList.isEmpty()) return

        viewModelScope.launch {
            val currentDate = System.currentTimeMillis()

            activeWorkoutList.forEach { activeExercise ->
                // 1. Filtramos y transformamos las series completadas
                val validSets = activeExercise.sets
                    .filter { it.isCompleted && it.weight.isNotEmpty() && it.reps.isNotEmpty() }
                    .map { set ->
                        CompletedSet(
                            weight = set.weight.toFloatOrNull() ?: 0f,
                            reps = set.reps.toIntOrNull() ?: 0,
                            rpe = set.rpe.toIntOrNull() ?: 0 // <-- AQUÍ GUARDAMOS EL RPE REAL
                        )
                    }

                // 2. Si hay series válidas, guardamos el ejercicio completo
                if (validSets.isNotEmpty()) {
                    val completedExercise = CompletedExercise(
                        exerciseName = activeExercise.name,
                        date = currentDate,
                        sets = validSets
                    )
                    
                    // Guardamos en la nueva tabla
                    dao.insertCompletedExercise(completedExercise)
                }
            }

            // Limpiamos el estado
            _activeWorkout.value = emptyList()
        }
    }

    // Cancelar entreno
    fun cancelActiveWorkout() {
        _activeWorkout.value = emptyList()
    }

    // Agregar un ejercicio configurado al borrador
    fun addExerciseToDraft(config: RoutineExerciseConfig) {
        _draftRoutineExercises.value = _draftRoutineExercises.value + config
    }

    // Eliminar del borrador
    fun removeExerciseFromDraft(config: RoutineExerciseConfig) {
        _draftRoutineExercises.value = _draftRoutineExercises.value - config
    }

    // Limpiar borrador
    fun clearDraft() {
        _draftRoutineExercises.value = emptyList()
    }

    // GUARDAR RUTINA FINAL (Conversión a JSON)
    fun saveNewRoutine(routineName: String) {
        viewModelScope.launch {
            // Convertimos la lista de objetos a un String JSON
            // Ej: '[{"name":"Squat","targetSets":4...}, ...]'
            val exercisesJson = gson.toJson(_draftRoutineExercises.value)

            val newRoutine = Routine(name = routineName, exercises = exercisesJson)
            dao.insertRoutine(newRoutine)

            clearDraft() // Limpiamos para la próxima
        }
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
    val id: Long = java.util.UUID.randomUUID().mostSignificantBits, // ID único para identificar la serie en la UI
    val weight: String = "",
    val reps: String = "",
    var rpe: String = "",
    val isCompleted: Boolean = false
)

data class ActiveExercise(
    val id: Int, // ID del ejercicio
    val name: String,
    val sets: List<ActiveSet> // AHORA CONTIENE UNA LISTA DE SERIES
)

