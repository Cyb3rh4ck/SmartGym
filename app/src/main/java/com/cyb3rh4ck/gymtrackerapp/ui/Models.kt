package com.cyb3rh4ck.gymtrackerapp.ui

class Models {

    // Representa la configuración de un ejercicio DENTRO de una rutina
    data class RoutineExerciseConfig(
        val name: String,
        val targetSets: Int,       // Ej: 4
        val targetReps: String,    // Ej: "8-12" (String para permitir rangos)
        val restTimeSeconds: Int,  // Ej: 90
        val note: String = ""      // Ej: "Cuidar lumbar"
    )

}