package com.apka.terminarzkliniki.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// DAO = operacje na bazie (CRUD).
@Dao
interface VisitDao {

    @Query("SELECT * FROM visits ORDER BY dateTimeMillis ASC")
    fun observeAll(): Flow<List<Visit>>

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Visit?>

    @Insert
    suspend fun insert(visit: Visit): Long

    @Update
    suspend fun update(visit: Visit)

    @Delete
    suspend fun delete(visit: Visit)
}
