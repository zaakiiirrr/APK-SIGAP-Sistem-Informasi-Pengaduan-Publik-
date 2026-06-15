package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "complaints")
data class Complaint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // "Anonim" or citizen's name
    val location: String, // GPS auto-filled text (e.g. sub-district / kecamatan in Bantul)
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String, // Road, Trash, Flood, Air Quality, Other
    val description: String,
    val urgency: String, // Darurat (Emergency), Tinggi (High), Menengah (Medium), Rendah (Low)
    val photoUri: String? = null,
    val status: String = "Diterima", // Diterima, Diproses, Selesai
    val timestamp: Long = System.currentTimeMillis(),
    
    // AI Structured / Parsed Data Fields
    val aiResponse: String? = null, // Empathetic local response from Gemini in Indonesian
    val aiSummaryLocation: String? = null, // Parsed location
    val aiSummaryProblem: String? = null, // Parsed problem type
    val aiSummaryTime: String? = null, // Parsed timeline
    val aiSummaryUrgency: String? = null // Parsed severity
) : Serializable
