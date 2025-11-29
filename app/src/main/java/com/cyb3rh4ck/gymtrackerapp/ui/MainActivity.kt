package com.cyb3rh4ck.gymtrackerapp.ui

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyb3rh4ck.gymtrackerapp.data.Routine
import com.cyb3rh4ck.gymtrackerapp.data.WorkoutLog
import com.cyb3rh4ck.gymtrackerapp.domain.TrainingRecommender
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Close
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cyb3rh4ck.gymtrackerapp.ui.theme.GymTrackerAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymTrackerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 1. Obtenemos el contexto de la aplicación
                    val context = LocalContext.current

                    // 2. Creamos el ViewModel usando un Factory
                    // Esto es necesario porque MainViewModel necesita recibir la Base de Datos/Dao
                    val viewModel: MainViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                // AQUÍ ASUMO QUE TU MainViewModel RECIBE EL DAO O EL CONTEXTO
                                // Si tu MainViewModel se inicializa diferente, ajusta esta línea.

                                // Opción A: Si usas el patrón de instanciar la DB dentro del ViewModel (principiante)
                                return MainViewModel(context) as T

                                // Opción B: Si pasas el DAO (más correcto)
                                // val db = com.cyb3rh4ck.gymtrackerapp.data.AppDatabase.getDatabase(context)
                                // return MainViewModel(db.workoutDao()) as T
                            }
                        }
                    )

                    // 3. Ahora sí pasamos la variable definida arriba
                    SmartGymScreen(viewModel = viewModel)
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartGymScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    val recommendation by viewModel.recommendation.collectAsState()
    val routines by viewModel.routines.collectAsState()
    //var showCreateRoutineDialog by remember { mutableStateOf(false) }


    // Estados UI
    var showBottomSheet by remember { mutableStateOf(false) }
    // Estado para saber si estamos editando (si es null, es un nuevo registro)
    var selectedLog by remember { mutableStateOf<WorkoutLog?>(null) }

    val sheetState = rememberModalBottomSheetState()
    // 1. CAPTURAMOS EL ESTADO DEL ENTRENO ACTIVO
    val activeWorkout by viewModel.activeWorkout.collectAsState()
    var showCreateRoutineDialog by remember { mutableStateOf(false) }

    // Función auxiliar para abrir el modal en modo Edición
    fun openEditModal(log: WorkoutLog) {
        selectedLog = log
        showBottomSheet = true
    }

    // Función auxiliar para abrir el modal en modo Nuevo
    fun openNewModal() {
        selectedLog = null
        showBottomSheet = true
    }

    // 2. LÓGICA DE NAVEGACIÓN SIMPLE (Switch de Vistas)
    if (activeWorkout.isNotEmpty()) {
        ActiveWorkoutScreen(
            exercises = activeWorkout,
            onUpdateSet = { exId, setId, w, r, done ->
                viewModel.updateSet(exId, setId, w, r, done)
            },
            onAddSet = { exId ->
                viewModel.addSetToExercise(exId)
            },
            onRemoveSet = { exId, setId -> // (Opcional)
                viewModel.removeSetFromExercise(exId, setId)
            },
            onFinish = {
                viewModel.finishActiveWorkout()
                Toast.makeText(context, "¡Entreno Guardado! 💪", Toast.LENGTH_LONG).show()
            },
            onCancel = { viewModel.cancelActiveWorkout() }
        )
    } else {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("SmartGym", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openNewModal() }, // Abrir limpio
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo Entreno")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            AICard(recommendation)
            Spacer(modifier = Modifier.height(24.dp))

            // --- NUEVA SECCIÓN DE RUTINAS ---
            RoutineSection(
                routines = routines,
                onCreaRoutineClick = { showCreateRoutineDialog = true },
                onRoutineClick = { routine ->
                    viewModel.startRoutine(routine)
                    Toast.makeText(context, "¡A entrenar! ${routine.name}", Toast.LENGTH_SHORT).show()
                },
                onDeleteRoutine = { viewModel.deleteRoutine(it) }
            )
            // --------------------------------

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Historial (Toca para editar)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = history, key = { it.id }) { log ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteWorkout(log)
                                Toast.makeText(context, "Registro eliminado", Toast.LENGTH_SHORT).show()
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color(0xFFEF4444) else Color.Transparent
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, RoundedCornerShape(16.dp))
                                    .padding(end = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Borrar", tint = Color.White)
                            }
                        },
                        enableDismissFromStartToEnd = false
                    ) {
                        // AÑADIMOS EL CLICKABLE AQUÍ PARA EDITAR
                        WorkoutLogCard(
                            log = log,
                            recommender = TrainingRecommender(),
                            onClick = { openEditModal(log) }
                        )
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                // Pasamos el log seleccionado (o null) al formulario
                WorkoutForm(
                    initialLog = selectedLog,
                    onSave = { exercise, muscle, weight, reps, rpe ->
                        if (selectedLog == null) {
                            // MODO CREAR
                            viewModel.saveWorkout(exercise, muscle, weight, reps, rpe)
                            Toast.makeText(context, "¡Entreno guardado!", Toast.LENGTH_SHORT).show()
                        } else {
                            // MODO EDITAR: Creamos una copia del log con los nuevos datos
                            // pero manteniendo el mismo ID y FECHA original
                            val updatedLog = selectedLog!!.copy(
                                exerciseName = exercise,
                                muscleGroup = muscle,
                                weightUsed = weight,
                                reps = reps,
                                rpe = rpe
                            )
                            viewModel.updateWorkout(updatedLog)
                            Toast.makeText(context, "¡Entreno actualizado!", Toast.LENGTH_SHORT).show()
                        }
                        showBottomSheet = false
                    },
                    onCancel = { showBottomSheet = false }
                )
            }
        }

        if (showCreateRoutineDialog) {
            CreateRoutineDialog(
                onDismiss = { showCreateRoutineDialog = false },
                onSave = { name, exercises ->
                    viewModel.createRoutine(name, exercises)
                    showCreateRoutineDialog = false
                    Toast.makeText(context, "Rutina $name creada", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
}
// --- COMPONENTE: FORMULARIO INTELIGENTE (CREAR / EDITAR) ---
@Composable
fun WorkoutForm(
    initialLog: WorkoutLog? = null, // Nuevo parámetro opcional
    onSave: (String, String, Float, Int, Int) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    // Si initialLog tiene datos, los usamos. Si es null, usamos valores por defecto.
    var exerciseName by remember { mutableStateOf(initialLog?.exerciseName ?: "") }
    var muscleGroup by remember { mutableStateOf(initialLog?.muscleGroup ?: "Chest") }
    var weight by remember { mutableStateOf(initialLog?.weightUsed?.toString() ?: "") }
    var reps by remember { mutableStateOf(initialLog?.reps?.toString() ?: "") }
    var rpe by remember { mutableFloatStateOf(initialLog?.rpe?.toFloat() ?: 7f) }

    val scrollState = rememberScrollState()
    val buttonText = if (initialLog == null) "GUARDAR SERIE" else "ACTUALIZAR SERIE"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .imePadding()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = if (initialLog == null) "Registrar Serie" else "Editar Serie",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = exerciseName,
            onValueChange = { exerciseName = it },
            label = { Text("Ejercicio") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Grupo Muscular:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Antes: Color.Gray
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Chest", "Back", "Legs", "Arms").forEach { muscle ->
                FilterChip(
                    selected = muscleGroup == muscle,
                    onClick = { muscleGroup = muscle },
                    label = { Text(muscle) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = weight,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) weight = it },
                label = { Text("Peso (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = reps,
                onValueChange = { if (it.all { char -> char.isDigit() }) reps = it },
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Esfuerzo (RPE): ${rpe.toInt()}/10", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = rpe,
            onValueChange = { rpe = it },
            valueRange = 1f..10f,
            steps = 8
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val w = weight.toFloatOrNull() ?: 0f
                val r = reps.toIntOrNull() ?: 0
                if (exerciseName.isEmpty()) {
                    Toast.makeText(context, "Escribe el nombre", Toast.LENGTH_SHORT).show()
                } else if (w <= 0 || r <= 0) {
                    Toast.makeText(context, "Revisa peso y reps", Toast.LENGTH_SHORT).show()
                } else {
                    onSave(exerciseName, muscleGroup, w, r, rpe.toInt())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(buttonText, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- COMPONENTE: TARJETA IA (Sin cambios) ---
@Composable
fun AICard(recommendationText: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(8.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier
                .size(100.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape))
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("ANÁLISIS IA", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f), letterSpacing = 1.5.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = recommendationText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium, color = Color.White, lineHeight = 32.sp)
            }
        }
    }
}

// --- COMPONENTE: TARJETA DE HISTORIAL (Con Click) ---
@Composable
fun WorkoutLogCard(
    log: WorkoutLog,
    recommender: TrainingRecommender,
    onClick: () -> Unit // Nuevo evento click
) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val intensityColor = when {
        log.rpe >= 9 -> Color(0xFFEF4444)
        log.rpe >= 7 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() } // <--- HACE QUE LA TARJETA SEA CLICKEABLE
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(intensityColor))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(log.exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    // Row del encabezado
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            dateFormat.format(Date(log.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${log.weightUsed}kg", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(" x ${log.reps} reps", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 2.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    Text("RPE ${log.rpe}", style = MaterialTheme.typography.labelSmall, color = intensityColor, fontWeight = FontWeight.Bold, modifier = Modifier
                        .background(intensityColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = recommender.suggestWeights(log),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
// --- COMPONENTE: SECCIÓN DE RUTINAS ---
@Composable
fun RoutineSection(
    routines: List<Routine>,
    onCreaRoutineClick: () -> Unit,
    onRoutineClick: (Routine) -> Unit,
    onDeleteRoutine: (Routine) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mis Rutinas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onCreaRoutineClick) {
                Text("Crear Nueva")
            }
        }

        if (routines.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "No tienes rutinas guardadas.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(routines) { routine ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(160.dp)
                            .clickable { onRoutineClick(routine) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(routine.name, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                                // Pequeño botón X para borrar rutina
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Borrar",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onDeleteRoutine(routine) },
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${routine.exercises.split(",").size} Ejercicios",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTE: DIÁLOGO CREAR RUTINA ---
@Composable
fun CreateRoutineDialog(
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var exercisesInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Rutina") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (ej. Pierna)") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = exercisesInput,
                    onValueChange = { exercisesInput = it },
                    label = { Text("Ejercicios (separados por coma)") },
                    placeholder = { Text("Sentadilla, Peso Muerto, Prensa...") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotEmpty() && exercisesInput.isNotEmpty()) {
                    val exercisesList = exercisesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(name, exercisesList)
                }
            }) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- PANTALLA DE MODO ENTRENO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    exercises: List<ActiveExercise>, // Asegúrate de que apunte a la clase correcta
    onUpdateSet: (Int, Long, String?, String?, Boolean?) -> Unit, // (ExId, SetId, w, r, done)
    onAddSet: (Int) -> Unit,
    onRemoveSet: (Int, Long) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Entrenando...", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancelar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .imePadding()
        ) {
            items(exercises) { exercise ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Encabezado del ejercicio
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cabecera de la tabla
                        // Dentro de ActiveWorkoutScreen -> Card -> Column
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text(
                                "Serie",
                                modifier = Modifier.width(40.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // <--- Corregido
                            )
                            Text(
                                "Kg",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // <--- Corregido
                            )
                            Text(
                                "Reps",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // <--- Corregido
                            )
                            Text(
                                "Hecho",
                                modifier = Modifier.width(50.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // <--- Corregido
                            )
                        }


                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Lista de series
                        exercise.sets.forEachIndexed { index, set ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                // Número de serie
                                Text(
                                    text = "${index + 1}",
                                    modifier = Modifier.width(40.dp),
                                    fontWeight = FontWeight.Bold
                                )

                                // Input Peso
                                OutlinedTextField(
                                    value = set.weight,
                                    onValueChange = { onUpdateSet(exercise.id, set.id, it, null, null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        focusedContainerColor = if (set.isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                )

                                // Input Reps
                                OutlinedTextField(
                                    value = set.reps,
                                    onValueChange = { onUpdateSet(exercise.id, set.id, null, it, null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedContainerColor = if (set.isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                )

                                // Checkbox y Botón Borrar
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = set.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            onUpdateSet(exercise.id, set.id, null, null, isChecked)
                                        }
                                    )

                                    // Botón pequeño para borrar serie
                                    IconButton(
                                        onClick = { onRemoveSet(exercise.id, set.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close, // O Icons.Filled.Delete si lo tienes
                                            contentDescription = "Borrar serie",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Botón AGREGAR SERIE
                        OutlinedButton(
                            onClick = { onAddSet(exercise.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar Serie")
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))

                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp) // Un toque estético
                ) {
                    Text(
                        "TERMINAR ENTRENO 🏁",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

            }
        }
    }
}
