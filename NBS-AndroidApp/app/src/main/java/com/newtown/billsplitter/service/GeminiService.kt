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
    
    private var model: GenerativeModel? = null
    
    init {
        try {
            Log.d("GeminiService", "Starting GeminiService initialization...")
            val apiKey = com.newtown.billsplitter.BuildConfig.GEMINI_API_KEY
            Log.d("GeminiService", "Raw API Key: '$apiKey'")
            Log.d("GeminiService", "API Key length: ${apiKey.length}")
            Log.d("GeminiService", "API Key starts with: ${apiKey.take(10)}...")
            Log.d("GeminiService", "API Key ends with: ...${apiKey.takeLast(10)}")
            
            if (apiKey.isEmpty() || apiKey == "\"\"") {
                Log.e("GeminiService", "API Key is empty or not set properly")
                throw Exception("API Key not configured")
            }
            
            if (!apiKey.startsWith("AIza")) {
                Log.e("GeminiService", "API Key format is invalid - should start with 'AIza'")
                throw Exception("Invalid API Key format")
            }
            
            model = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )
            Log.d("GeminiService", "GeminiService initialized successfully")
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to initialize GeminiService", e)
            Log.e("GeminiService", "Exception message: ${e.message}")
            Log.e("GeminiService", "Exception stack trace: ${e.stackTraceToString()}")
        }
    }
    
    suspend fun processBillImage(imageUri: String): List<BillItem> = withContext(Dispatchers.IO) {
        try {
            Log.d("GeminiService", "Starting bill image processing...")
            
            if (model == null) {
                Log.e("GeminiService", "Model not initialized - API key may be missing")
                throw Exception("Gemini model not initialized. Please check your API key configuration.")
            }
            
            val prompt = """
                Analyze this bill/receipt image and extract all items with their prices. Focus on accurate item extraction and pricing.
                
                Please return the items in this exact JSON format:
                {
                    "items": [
                        {"name": "Item Name", "price": 0.00, "type": "item"},
                        {"name": "Weight Item (1.5kg @ £2.00/kg)", "price": 3.00, "type": "item"},
                        {"name": "Zero Cost Item", "price": 0.00, "type": "item"},
                        {"name": "BIRDSEYE 4 FOR £4.98", "price": -3.94, "type": "deal"}
                    ],
                    "summary": {
                        "subtotal": 0.00,
                        "colleague_discount": 0.00,
                        "final_total": 0.00
                    }
                }
                
                CRITICAL RULES:
                1. **ITEM-SPECIFIC DEALS HANDLING**: 
                   - Extract item-specific deals (e.g., "BIRDSEYE 4 FOR £4.98" with negative price -£3.94) as separate items
                   - Use NEGATIVE prices for deals (e.g., -3.94)
                   - Set type as "deal" for item-specific deals
                   - These deals should be included in subtotal calculation
                
                2. **COLLEAGUE DISCOUNT HANDLING**: 
                   - DO NOT include colleague discounts (e.g., "Colleague Disc", "Employee Discount") in the items list
                   - Colleague discounts should only be mentioned in the summary section
                   - The subtotal should be the amount BEFORE colleague discount (the first TOTAL on receipt)
                   - The final_total should be the amount AFTER colleague discount (the final TOTAL on receipt)
                   - The colleague_discount field should be the difference between subtotal and final_total
                
                3. **ITEM EXTRACTION RULES**:
                   - Extract ALL items including zero-cost items (e.g., "BAG EXCHANGE" £0.00)
                   - For weight-based items, include weight info in name (e.g., "HB MIX MEAT (1.042kg @ £10.90/kg)")
                   - Clean up OCR artifacts when possible (e.g., "PRGLES BLZN" → "Pringles Blazin")
                   - Use exact names as they appear, but improve readability when obvious
                   - Pay special attention to deals with negative prices on the receipt
                   - EXCLUDE colleague discounts from items list
                
                4. **IGNORE THESE ELEMENTS**:
                   - Promotional text (e.g., "For a chance to win £1,000!")
                   - Store information (e.g., "ASDA STORES LTD", "WWW.ASDA.COM")
                   - Payment method details (e.g., "AMERICAN EXPRESS", "A/C No.")
                   - Transaction IDs and manager names
                   - QR codes and barcodes
                   - Colleague discounts (these go in summary only)
                
                5. **PRICING RULES**:
                   - Convert all prices to decimal format (e.g., £5.99 = 5.99)
                   - Include service charges, taxes, and fees as separate items
                   - For weight items, calculate the total price (weight × unit price)
                   - Zero-cost items should have price: 0.00
                   - Item-specific deals should have NEGATIVE prices
                
                6. **CALCULATION RULES**:
                   - subtotal = sum of all items (including deals, excluding colleague discounts)
                   - colleague_discount = difference between first TOTAL and final TOTAL on receipt
                   - final_total = subtotal - colleague_discount
                   - Ensure calculations match the receipt's final amount
                
                7. **FALLBACK**:
                   - If you can't read clearly, return {"items": [], "summary": {"subtotal": 0, "colleague_discount": 0, "final_total": 0}}
                
                Return only the JSON object, nothing else.
            """.trimIndent()
            
            val response = model?.generateContent(
                content {
                    text(prompt)
                    image(loadImageFromUri(imageUri))
                }
            )
            
            Log.d("GeminiService", "Gemini API response received")
            val responseText = response?.text ?: "[]"
            Log.d("GeminiService", "Response text: $responseText")
            
            return@withContext parseGeminiResponse(responseText)
            
        } catch (e: Exception) {
            Log.e("GeminiService", "Error processing bill image", e)
            when {
                e.message?.contains("unregistered callers") == true -> {
                    throw Exception("API Key authentication failed. Please check your GEMINI_API_KEY configuration.")
                }
                e.message?.contains("API Key not configured") == true -> {
                    throw Exception("API Key not configured. Please set GEMINI_API_KEY in your environment.")
                }
                else -> {
                    throw Exception("Failed to process bill image: ${e.message}")
                }
            }
        }
    }
    
    private fun loadImageFromUri(imageUri: String): Bitmap {
        val uri = Uri.parse(imageUri)
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)
        
        if (bitmap == null) {
            throw Exception("Failed to decode image")
        }
        
        Log.d("GeminiService", "Image decoded successfully")
        return bitmap
    }
    
    private fun parseGeminiResponse(response: String): List<BillItem> {
        return try {
            // Clean the response to extract just the JSON
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            
            if (jsonStart == -1 || jsonEnd == 0) {
                return emptyList()
            }
            
            val jsonString = response.substring(jsonStart, jsonEnd)
            
            // Parse the JSON response
            val gson = com.google.gson.Gson()
            val billData = gson.fromJson(jsonString, BillResponse::class.java)
            
            billData.items.mapIndexed { index, item ->
                BillItem(
                    id = System.currentTimeMillis() + index + (index * 1000), // Ensure unique IDs
                    name = item.name,
                    price = item.price,
                    itemType = item.type
                )
            }
            
        } catch (e: Exception) {
            Log.e("GeminiService", "JSON parsing failed, trying fallback", e)
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
                            id = System.currentTimeMillis() + items.size + (items.size * 1000), // Ensure unique IDs
                            name = itemName,
                            price = price,
                            itemType = "item"
                        ))
                    }
                }
            }
        }
        
        return items
    }
    
    private data class BillItemResponse(
        val name: String,
        val price: Double,
        val type: String = "item"
    )
    
    private data class BillSummary(
        val subtotal: Double,
        val colleague_discount: Double,
        val final_total: Double
    )
    
    private data class BillResponse(
        val items: List<BillItemResponse>,
        val summary: BillSummary
    )
} 