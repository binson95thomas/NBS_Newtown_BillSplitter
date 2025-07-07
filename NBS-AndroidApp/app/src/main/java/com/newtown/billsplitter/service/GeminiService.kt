package com.newtown.billsplitter.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.newtown.billsplitter.model.BillItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GeminiService(private val context: Context) {
    
    private val apiKey = try {
        val key = com.newtown.billsplitter.BuildConfig.GEMINI_API_KEY
        // Ensure the key is properly formatted
        if (key.startsWith("AIza")) {
            key
        } else {
            throw Exception("Invalid API key format")
        }
    } catch (e: Exception) {
        Log.e("GeminiService", "Failed to read API key from BuildConfig: ${e.message}")
        // Fallback to hardcoded key for testing
        "AIzaSyCBRosZdx4b3HdOsZiEyjQOUjeeA6uPBrQ"
    }
    
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )
    
    init {
        Log.d("GeminiService", "API Key length: ${apiKey.length}")
        Log.d("GeminiService", "API Key starts with: ${apiKey.take(10)}...")
        
        // Test the API key with a simple text request
        testApiKey()
    }
    
    private fun testApiKey() {
        // This will run in background to test the API key
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val testResponse = model.generateContent("Hello")
                Log.d("GeminiService", "API key test successful")
            } catch (e: Exception) {
                Log.e("GeminiService", "API key test failed: ${e.message}")
            }
        }
    }
    
    suspend fun processBillImage(imageUri: String): List<BillItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("GeminiService", "Starting bill image processing...")
            val uri = Uri.parse(imageUri)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)
            
            if (bitmap == null) {
                throw Exception("Failed to decode image")
            }
            
            Log.d("GeminiService", "Image decoded successfully, calling Gemini API...")
            
            val prompt = """
                Analyze this bill/receipt image and extract all items with their prices.
                
                Please return the items in this exact JSON format:
                [
                    {"name": "Item Name", "price": 0.00},
                    {"name": "Another Item", "price": 0.00}
                ]
                
                Rules:
                1. Only include items that have prices
                2. Use the exact item names as they appear
                3. Convert all prices to decimal format (e.g., £5.99 = 5.99)
                4. Ignore totals, taxes, and service charges
                5. If you can't read the image clearly, return an empty array []
                
                Return only the JSON array, nothing else.
            """.trimIndent()
            
            val response = model.generateContent(
                content {
                    text(prompt)
                    image(bitmap)
                }
            )
            
            Log.d("GeminiService", "Gemini API response received")
            val responseText = response.text ?: "[]"
            Log.d("GeminiService", "Response text: $responseText")
            
            return@withContext parseGeminiResponse(responseText)
            
        } catch (e: Exception) {
            Log.e("GeminiService", "Gemini processing failed", e)
            throw Exception("Gemini processing failed: ${e.message}")
        }
    }
    
    private fun parseGeminiResponse(response: String): List<BillItem> {
        return try {
            // Clean the response to extract just the JSON
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']') + 1
            
            if (jsonStart == -1 || jsonEnd == 0) {
                return emptyList()
            }
            
            val jsonString = response.substring(jsonStart, jsonEnd)
            
            // Parse the JSON response
            val gson = com.google.gson.Gson()
            val items = gson.fromJson(jsonString, Array<BillItemResponse>::class.java)
            
            items.map { item ->
                BillItem(
                    id = System.currentTimeMillis() + items.indexOf(item),
                    name = item.name,
                    price = item.price
                )
            }
            
        } catch (e: Exception) {
            // Fallback to basic parsing if JSON parsing fails
            parseFallbackResponse(response)
        }
    }
    
    private fun parseFallbackResponse(response: String): List<BillItem> {
        val items = mutableListOf<BillItem>()
        val lines = response.split("\n")
        
        val pricePattern = Regex("""£?\s*(\d+\.?\d*)""")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            val priceMatch = pricePattern.find(trimmedLine)
            if (priceMatch != null) {
                val price = priceMatch.groupValues[1].toDoubleOrNull()
                if (price != null) {
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
        
        return items
    }
    
    private data class BillItemResponse(
        val name: String,
        val price: Double
    )
} 