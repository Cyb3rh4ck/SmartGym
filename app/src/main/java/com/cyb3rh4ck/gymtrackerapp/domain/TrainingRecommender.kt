package com.cyb3rh4ck.gymtrackerapp.domain

import com.cyb3rh4ck.gymtrackerapp.data.WorkoutLog

class TrainingRecommender {

    // Lógica simple de "IA": Analiza recuperación y volumen
    fun recommendNextWorkout(history: List<WorkoutLog>): String {
        if (history.isEmpty()) return "¡Bienvenido! Empecemos con un Full Body básico para calibrar."

        val lastWorkout = history.first() // El más reciente
        val daysSinceLast = (System.currentTimeMillis() - lastWorkout.date) / (1000 * 60 * 60 * 24)

        // 1. Regla de Descanso
        if (daysSinceLast < 1) return "Has entrenado hoy. ¡Descansa y come bien!"

        // 2. Lógica de Grupos Musculares (Push/Pull/Legs simplificado)
        return when (lastWorkout.muscleGroup) {
            "Chest", "Triceps", "Shoulders" -> "Recomendación IA: Hoy toca **Espalda y Bíceps** (Pull) para equilibrar."
            "Back", "Biceps" -> "Recomendación IA: Hoy toca **Pierna** (Legs). No te la saltes."
            "Legs" -> "Recomendación IA: Hoy toca **Pecho y Tríceps** (Push)."
            else -> "Recomendación IA: Haz una sesión de Cardio o Full Body."
        }
    }

    // Sugerencia de sobrecarga progresiva
    fun suggestWeights(lastLog: WorkoutLog?): String {
        if (lastLog == null) return "Empieza suave y registra tu peso."
        
        // Si la última vez fue fácil (RPE bajo), sugiere subir peso
        return if (lastLog.rpe < 7) {
            "IA: La última vez levantaste ${lastLog.weightUsed}kg fácil. ¡Intenta subir a ${lastLog.weightUsed + 2.5}kg!"
        } else {
            "IA: Mantén ${lastLog.weightUsed}kg pero intenta sacar una repetición más."
        }
    }
}
