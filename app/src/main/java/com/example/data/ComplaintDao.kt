package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY timestamp DESC")
    fun getAllComplaints(): Flow<List<Complaint>>

    @Query("SELECT * FROM complaints WHERE id = :id")
    fun getComplaintById(id: Int): Flow<Complaint?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: Complaint): Long

    @Update
    suspend fun updateComplaint(complaint: Complaint)

    @Delete
    suspend fun deleteComplaint(complaint: Complaint)

    @Query("DELETE FROM complaints")
    suspend fun deleteAll()
}
