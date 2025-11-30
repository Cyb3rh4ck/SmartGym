package com.cyb3rh4ck.gymtrackerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cyb3rh4ck.gymtrackerapp.ui.CompletedSet

@Entity(tableName = "completed_exercises")
data class CompletedExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseName: String,
    val date: Long,
    val sets: List<CompletedSet> // Aquí guardamos la lista completa
)