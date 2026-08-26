package com.example.blueprintai.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logManager: LogManager,
) {
    suspend fun extractText(uri: Uri, type: String): String? = withContext(Dispatchers.IO) {
        logManager.log("INFO", "Attachment", "Extracting text from $type: $uri")
        try {
            when {
                type.startsWith("image/") -> extractTextFromImage(uri)
                type == "application/pdf" -> extractTextFromPdf(uri)
                (type.startsWith("text/") || type == "application/json" || type == "text/markdown") -> extractTextFromPlain(uri)
                else -> null
            }
        } catch (e: Exception) {
            logManager.log("ERROR", "Attachment", "Extraction failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private suspend fun extractTextFromImage(uri: Uri): String? {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            logManager.log("ERROR", "Attachment", "ML Kit failed: ${e.message}")
            null
        }
    }

    private fun extractTextFromPdf(uri: Uri): String? {
        return try {
            PDFBoxResourceLoader.init(context)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            }
        } catch (e: Exception) {
            logManager.log("ERROR", "Attachment", "PDFBox failed: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun extractTextFromPlain(uri: Uri): String? {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }
}
