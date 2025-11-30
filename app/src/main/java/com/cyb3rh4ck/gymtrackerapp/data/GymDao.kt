package com.cyb3rh4ck.gymtrackerapp.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.room.TypeConverters

@Dao
interface GymDao {
    // Operaciones de Usuario
    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    // Operaciones de Historial
    @Insert
    suspend fun logWorkout(log: WorkoutLog)

    @Query("SELECT * FROM workout_logs ORDER BY date DESC")
    suspend fun getAllLogs(): List<WorkoutLog>

    // Consulta para la "IA": Obtener últimos entrenos de un músculo
    @Query("SELECT * FROM workout_logs WHERE muscleGroup = :muscle ORDER BY date DESC LIMIT 1")
    suspend fun getLastWorkoutForMuscle(muscle: String): WorkoutLog?

    @Delete
    suspend fun deleteWorkout(log: WorkoutLog)

    @Update
    suspend fun updateWorkout(log: WorkoutLog)

    // --- NUEVO PARA RUTINAS ---
    @Query("SELECT * FROM Routine")
    fun getAllRoutines(): Flow<List<Routine>>

    @Insert
    suspend fun insertRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)
    
    @Insert
    suspend fun insertCompletedExercise(completedExercise: CompletedExercise)

    @Query("SELECT * FROM completed_exercises ORDER BY date DESC")
    fun getAllCompletedExercises(): Flow<List<CompletedExercise>>
}

// Añade Routine a las entidades de la Database
@Database(entities = [WorkoutLog::class, Routine::class,UserProfile::class, CompletedExercise::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase(){
    abstract fun gymDao(): GymDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "gym_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
