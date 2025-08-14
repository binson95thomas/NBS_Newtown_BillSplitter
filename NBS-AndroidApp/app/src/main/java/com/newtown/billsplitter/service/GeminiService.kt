package com.newtown.billsplitter.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.ai.client.generativeai.type.content
import com.newtown.billsplitter.model.BillItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GeminiService(private val context: Context) {
    
    private var model: GenerativeModel? = null
    
    // Allow quick switch between legacy and enhanced prompt/response formats
    enum class PromptMode { LEGACY, ENHANCED }
    private var promptMode: PromptMode = PromptMode.ENHANCED
    fun setPromptMode(mode: PromptMode) { this.promptMode = mode }
    
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
                apiKey = apiKey,
                generationConfig = generationConfig {
                    temperature = 0.15f
                    topK = 1
                    topP = 0.1f
                }
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
            
            val prompt = when (promptMode) {
                PromptMode.LEGACY -> buildLegacyPrompt()
                PromptMode.ENHANCED -> buildEnhancedPrompt()
            }
            
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
        
        val downscaled = try {
            downscaleBitmap(bitmap, 2000)
        } catch (e: Exception) {
            Log.w("GeminiService", "Downscale failed, using original", e)
            bitmap
        }
        Log.d("GeminiService", "Image decoded successfully; size=${downscaled.width}x${downscaled.height}")
        return downscaled
    }

    private fun downscaleBitmap(src: Bitmap, maxEdge: Int): Bitmap {
        val width = src.width
        val height = src.height
        val scale = if (width >= height) maxEdge.toFloat() / width else maxEdge.toFloat() / height
        if (scale >= 1f) return src
        val newW = (width * scale).toInt().coerceAtLeast(1)
        val newH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }
    
    private fun parseGeminiResponse(response: String): List<BillItem> {
        return try {
            // Extract only the JSON payload
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            if (jsonStart == -1 || jsonEnd == 0) return emptyList()
            val jsonString = response.substring(jsonStart, jsonEnd)
            
            val gson = com.google.gson.Gson()

            // First try enhanced schema
            try {
                val enhanced = gson.fromJson(jsonString, EnhancedBillResponse::class.java)
                if (enhanced.items != null) {
                    return enhanced.items.mapIndexed { index, item ->
                        val priceDouble = item.price?.replace(',', '.')?.toDoubleOrNull()
                            ?: (item.price_minor?.let { it.toDouble() / 100.0 } ?: 0.0)
                        BillItem(
                            id = System.currentTimeMillis() + index + (index * 1000),
                            name = item.name ?: "Item",
                            price = priceDouble,
                            itemType = item.type ?: "item",
                            confidence = item.confidence
                        )
                    }
                }
            } catch (ignored: Exception) {
                // Fall through to legacy parse
            }

            // Fallback to legacy schema
            val legacy = gson.fromJson(jsonString, BillResponse::class.java)
            legacy.items.mapIndexed { index, item ->
                BillItem(
                    id = System.currentTimeMillis() + index + (index * 1000),
                    name = item.name,
                    price = item.price,
                    itemType = item.type
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "JSON parsing failed, trying fallback", e)
            parseFallbackResponse(response)
        }
    }
    
    private fun parseFallbackResponse(response: String): List<BillItem> {
        val items = mutableListOf<BillItem>()
        val lines = response.split("\n")
        
        // Hardened: capture signed prices with exactly 2 decimals if present; prefer rightmost match
        val pricePattern = Regex("""-?£?\s*(\d+\.\d{1,2})""")
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            val priceMatch = pricePattern.findAll(trimmedLine).lastOrNull()
            if (priceMatch != null) {
                val priceText = priceMatch.groupValues[1]
                val price = priceText.toDoubleOrNull()
                if (price != null) {
                    val itemName = trimmedLine.replace(priceMatch.value, "").trim()
                    if (itemName.isNotEmpty()) {
                        items.add(BillItem(
                            id = System.currentTimeMillis() + items.size + (items.size * 1000), // Ensure unique IDs
                            name = itemName,
                            price = price,
                            itemType = if (trimmedLine.contains("-")) "deal" else "item"
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

    // Enhanced schema (backup-friendly; supports additional fields and stricter formatting)
    private data class EnhancedBillItemResponse(
        val name: String?,
        val price: String?,              // e.g., "-3.94"
        val price_minor: Int?,           // e.g., -394
        val type: String?,               // "item" | "deal"
        val original_text: String?,
        val confidence: Double?
    )

    private data class EnhancedBillSummary(
        val items_total_before_colleague: String?,
        val items_total_before_colleague_minor: Int?,
        val colleague_discount_amount: String?,
        val colleague_discount_amount_minor: Int?,
        val final_total_after_colleague: String?,
        val final_total_after_colleague_minor: Int?
    )

    private data class EnhancedBillResponse(
        val items: List<EnhancedBillItemResponse>?,
        val summary: EnhancedBillSummary?
    )

    // Legacy prompt kept verbatim for quick rollback
    private fun buildLegacyPrompt(): String = """
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

    // Enhanced prompt aligned to user's clarified rules
    private fun buildEnhancedPrompt(): String = """
        You are extracting UK (ASDA) receipt items. Output ONLY minified JSON.

        Goal:
        - Return every purchased line as an item.
        - Include item-specific deals as separate negative items.
        - EXCLUDE colleague/employee discount from the items list. If present, report it in summary only.
        - The subtotal we care about is sum(items) including deals (should match the first printed TOTAL before colleague discount).

        Formatting rules:
        - Currency: GBP. Dot as decimal separator. Exactly two decimals in strings.
        - Read the rightmost price on each item line; do not infer quantities or weight math. Zero-cost items stay 0.00.
        - Ignore headers, promos, payment, "Asda Rewards", manager names, transaction IDs.

        JSON schema:
        {
          "items": [
            {"name":"string","price":"0.00","price_minor":0,"type":"item|deal","original_text":"string","confidence":0.0}
          ],
          "summary": {
            "items_total_before_colleague":"0.00",
            "items_total_before_colleague_minor":0,
            "colleague_discount_amount":"0.00",
            "colleague_discount_amount_minor":0,
            "final_total_after_colleague":"0.00",
            "final_total_after_colleague_minor":0
          }
        }

        Examples:
        - "BIRDSEYE 4 FOR £4.98                   -£3.94" -> {"name":"BIRDSEYE 4 FOR £4.98","price":"-3.94","price_minor":-394,"type":"deal","original_text":"BIRDSEYE 4 FOR £4.98 -£3.94","confidence":0.95}

        Validation:
        - Ensure sum(items.price_minor) ≈ summary.items_total_before_colleague_minor (±2 pence). Do NOT include colleague discount lines in items.

        Return ONLY the JSON object.
    """.trimIndent()
} 