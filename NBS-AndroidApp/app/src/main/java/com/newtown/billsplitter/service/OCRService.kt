package com.newtown.billsplitter.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.newtown.billsplitter.model.BillItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream

class OCRService(private val context: Context) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    suspend fun processBillImage(imageUri: String): List<BillItem> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(imageUri)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)
            
            if (bitmap == null) {
                throw Exception("Failed to decode image")
            }
            
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()
            
            val extractedText = result.text
            return@withContext parseBillText(extractedText)
            
        } catch (e: Exception) {
            throw Exception("OCR processing failed: ${e.message}")
        }
    }
    
    private fun parseBillText(text: String): List<BillItem> {
        val items = mutableListOf<BillItem>()
        val lines = text.split("\n")
        
        // Common patterns for bill items
        val pricePattern = Regex("""£?\s*(\d+\.?\d*)""")
        val itemPattern = Regex("""^([A-Za-z\s]+)\s*£?\s*(\d+\.?\d*)""")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            // Try to match item with price pattern
            val itemMatch = itemPattern.find(trimmedLine)
            if (itemMatch != null) {
                val itemName = itemMatch.groupValues[1].trim()
                val price = itemMatch.groupValues[2].toDoubleOrNull()
                
                if (price != null && itemName.isNotEmpty()) {
                    items.add(BillItem(
                        id = System.currentTimeMillis() + items.size,
                        name = itemName,
                        price = price
                    ))
                }
            } else {
                // Try to find just a price in the line
                val priceMatch = pricePattern.find(trimmedLine)
                if (priceMatch != null) {
                    val price = priceMatch.groupValues[1].toDoubleOrNull()
                    if (price != null) {
                        // Use the line as item name, excluding the price
                        val itemName = trimmedLine.replace(priceMatch.value, "").trim()
                        if (itemName.isNotEmpty()) {
                            items.add(BillItem(
                                id = System.currentTimeMillis() + items.size,
                                name = itemName,
                                price = price
                            ))
                        }
                    }
                }
            }
        }
        
        return items
    }
    
    fun close() {
        textRecognizer.close()
    }
} 