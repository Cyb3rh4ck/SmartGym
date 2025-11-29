package com.cyb3rh4ck.gymtrackerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Modelo del Usuario (Medidas)
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Solo un usuario por ahora
    val weight: Float,
    val height: Float,
    val goal: String, // "Gain Muscle", "Lose Fat"
    val experienceLevel: String // "Beginner", "Advanced"
)

// Modelo de Ejercicio Realizado (Historial)
@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseName: String,
    val muscleGroup: String, // "Chest", "Back", "Legs"
    val weightUsed: Float,
    val reps: Int,
    val rpe: Int, // Esfuerzo percibido (1-10)
    val date: Long = System.currentTimeMillis()
)
