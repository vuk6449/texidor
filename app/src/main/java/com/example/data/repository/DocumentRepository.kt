package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.db.DocumentDao
import com.example.data.model.DocumentEntity
import com.example.editor.pdf.PdfGenerator
import com.example.editor.syntax.EditorLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val context: Context
) {

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments()

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> =
        documentDao.searchDocuments(query)

    suspend fun getDocumentById(id: Long): DocumentEntity? =
        documentDao.getDocumentById(id)

    suspend fun saveDocument(doc: DocumentEntity): Long = withContext(Dispatchers.IO) {
        if (doc.id == 0L) {
            documentDao.insert(doc.copy(updatedAt = System.currentTimeMillis()))
        } else {
            documentDao.update(doc.copy(updatedAt = System.currentTimeMillis()))
            doc.id
        }
    }

    suspend fun deleteDocument(id: Long) = withContext(Dispatchers.IO) {
        documentDao.deleteById(id)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        documentDao.updateFavorite(id, isFavorite)
    }

    /**
     * Initializes the sample PDF document in DB if it doesn't exist yet.
     */
    suspend fun initSamplePdfIfMissing() = withContext(Dispatchers.IO) {
        val existing = documentDao.getDocumentsByType("PDF").firstOrNull()
        if (existing.isNullOrEmpty()) {
            val samplePdfFile = PdfGenerator.createSamplePdf(context)
            val doc = DocumentEntity(
                title = "OmniEdit_Sample_Guide.pdf",
                content = "PDF Document",
                fileType = "PDF",
                language = "pdf",
                updatedAt = System.currentTimeMillis(),
                isFavorite = true,
                localFilePath = samplePdfFile.absolutePath
            )
            documentDao.insert(doc)
        }
    }

    /**
     * Imports a document from device storage using SAF Uri.
     */
    suspend fun importFromUri(uri: Uri): DocumentEntity? = withContext(Dispatchers.IO) {
        try {
            var fileName = "Imported_Document"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            val lowerName = fileName.lowercase()
            val isPdf = lowerName.endsWith(".pdf")

            if (isPdf) {
                // Copy PDF to app storage
                val destFile = File(context.filesDir, "imported_${System.currentTimeMillis()}_$fileName")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val entity = DocumentEntity(
                    title = fileName,
                    content = "Imported PDF Document",
                    fileType = "PDF",
                    language = "pdf",
                    updatedAt = System.currentTimeMillis(),
                    localFilePath = destFile.absolutePath
                )
                val id = documentDao.insert(entity)
                return@withContext entity.copy(id = id)
            } else {
                // Read text/code content
                val contentBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            contentBuilder.append(line).append("\n")
                            line = reader.readLine()
                        }
                    }
                }
                val ext = fileName.substringAfterLast('.', "")
                val detectedLang = EditorLanguage.fromExtension(ext)
                val isCode = detectedLang != EditorLanguage.PLAIN_TEXT && ext.isNotEmpty()

                val entity = DocumentEntity(
                    title = fileName,
                    content = contentBuilder.toString(),
                    fileType = if (isCode) "CODE" else "TXT",
                    language = detectedLang.id,
                    updatedAt = System.currentTimeMillis()
                )
                val id = documentDao.insert(entity)
                return@withContext entity.copy(id = id)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Exports document content or PDF to user-selected Uri.
     */
    suspend fun exportToUri(uri: Uri, doc: DocumentEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            if (doc.fileType == "PDF" && doc.localFilePath != null) {
                val srcFile = File(doc.localFilePath)
                if (srcFile.exists()) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        srcFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    return@withContext true
                }
            } else {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(doc.content.toByteArray(Charsets.UTF_8))
                }
                return@withContext true
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
