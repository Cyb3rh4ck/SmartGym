package com.cyb3rh4ck.gymtrackerapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cyb3rh4ck.gymtrackerapp.ui.Models.RoutineExerciseConfig


@Composable
fun AddExerciseDetailDialog(
    onDismiss: () -> Unit,
    onConfirm: (RoutineExerciseConfig) -> Unit
) {
    // Variables locales con prefijo "input" para evitar conflictos
    var inputName by remember { mutableStateOf("") }
    var inputSets by remember { mutableStateOf("4") }
    var inputReps by remember { mutableStateOf("8-12") }
    var inputRest by remember { mutableStateOf("90") }
    var inputNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Nombre (Ej. Press Banca)") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputSets,
                        onValueChange = { if (it.all { char -> char.isDigit() }) inputSets = it },
                        label = { Text("Series") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = inputReps,
                        onValueChange = { inputReps = it },
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = inputRest,
                    onValueChange = { if (it.all { char -> char.isDigit() }) inputRest = it },
                    label = { Text("Descanso (seg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = inputNote,
                    onValueChange = { inputNote = it },
                    label = { Text("Nota (Opcional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputName.isNotBlank() && inputSets.isNotBlank()) {
                        onConfirm(
                            RoutineExerciseConfig(
                                name = inputName,
                                targetSets = inputSets.toIntOrNull() ?: 3,
                                targetReps = inputReps,
                                restTimeSeconds = inputRest.toIntOrNull() ?: 60,
                                note = inputNote
                            )
                        )
                    }
                }
            ) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}