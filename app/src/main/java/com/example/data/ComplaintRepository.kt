package com.example.data

import kotlinx.coroutines.flow.Flow

class ComplaintRepository(private val complaintDao: ComplaintDao) {
    val allComplaints: Flow<List<Complaint>> = complaintDao.getAllComplaints()

    fun getComplaintById(id: Int): Flow<Complaint?> = complaintDao.getComplaintById(id)

    suspend fun insert(complaint: Complaint): Long {
        return complaintDao.insertComplaint(complaint)
    }

    suspend fun update(complaint: Complaint) {
        complaintDao.updateComplaint(complaint)
    }

    suspend fun delete(complaint: Complaint) {
        complaintDao.deleteComplaint(complaint)
    }
}
