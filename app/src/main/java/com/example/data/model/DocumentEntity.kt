package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val fileType: String, // "TXT", "CODE", "PDF"
    val language: String, // e.g. "txt", "kotlin", "python", "html", "javascript", "markdown"
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val localFilePath: String? = null,
    val annotationsJson: String? = null
)
