package com.cyb3rh4ck.gymtrackerapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,       // Ej: "Día de Pecho"
    val exercises: String   // Ej: "Press Banca,Aperturas,Flexiones" (Separados por coma)
)