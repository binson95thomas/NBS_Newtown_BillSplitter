package com.newtown.billsplitter.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

data class GeminiModel(
    val name: String,
    val version: String,
    val supportedMethods: List<String>,
    val isFast: Boolean = false,
    val isStable: Boolean = false
)

class ModelManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("ModelManager", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREF_MODELS_LIST = "available_models"
        private const val PREF_SELECTED_MODEL = "selected_model"
        private const val PREF_LAST_FETCH_TIME = "last_fetch_time"
        private const val FETCH_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
        
        // Fast model priorities (higher number = higher priority)
        // Lite models have highest priority for resource efficiency
        private val FAST_MODEL_PRIORITIES = mapOf(
            "gemini-2.0-flash-lite" to 100,      // HIGHEST - most efficient
            "gemini-2.5-flash-lite" to 99,
            "gemini-2.0-flash-lite-001" to 98,
            "gemini-2.5-flash" to 50,            // Standard flash models
            "gemini-2.0-flash" to 49,
            "gemini-2.0-flash-001" to 48,
            "gemini-1.5-flash" to 40,
            "gemini-2.5-pro" to 10,              // Pro models (slower but powerful)
            "gemini-2.0-pro" to 9
            // Experimental/preview models excluded from priorities
        )
        
        // Stable model indicators
        private val STABLE_MODELS = setOf(
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.0-flash",
            "gemini-2.0-flash-001", 
            "gemini-1.5-flash"
        )
    }
    
    /**
     * Get cached models or fetch if cache is expired/missing
     */
    suspend fun getAvailableModels(forceRefresh: Boolean = false): List<GeminiModel> = withContext(Dispatchers.IO) {
        try {
            val cachedModels = getCachedModels()
            val lastFetchTime = sharedPreferences.getLong(PREF_LAST_FETCH_TIME, 0)
            val currentTime = System.currentTimeMillis()
            
            // Return cached models if:
            // 1. We have cached models
            // 2. Not forcing refresh
            // 3. Cache is still valid (less than 24 hours old)
            if (cachedModels.isNotEmpty() && !forceRefresh && 
                (currentTime - lastFetchTime) < FETCH_CACHE_DURATION) {
                Log.d("ModelManager", "Returning cached models (${cachedModels.size} models)")
                return@withContext cachedModels
            }
            
            // Fetch fresh models
            Log.d("ModelManager", "Using API to get model list...")
            val freshModels = fetchModelsFromAPI()
            
            if (freshModels.isNotEmpty()) {
                saveModelsToCache(freshModels)
                Log.d("ModelManager", "Saved ${freshModels.size} models to cache")
                return@withContext freshModels
            } else {
                Log.w("ModelManager", "No models fetched, using cached models or fallback")
                // If no fresh models and no cached models, return fallback models
                if (cachedModels.isEmpty()) {
                    val fallbackModels = getFallbackModels()
                    Log.d("ModelManager", "Using fallback models (${fallbackModels.size} models)")
                    return@withContext fallbackModels
                }
                return@withContext cachedModels
            }
            
        } catch (e: Exception) {
            Log.e("ModelManager", "Error getting models", e)
            // Return cached models as fallback, or fallback models if no cache
            val cachedModels = getCachedModels()
            return@withContext if (cachedModels.isNotEmpty()) {
                cachedModels
            } else {
                getFallbackModels()
            }
        }
    }
    
    /**
     * Get the currently selected model
     */
    fun getSelectedModel(): String {
        try {
            val selectedModel = sharedPreferences.getString(PREF_SELECTED_MODEL, null)
            return if (selectedModel.isNullOrEmpty()) {
                Log.d("ModelManager", "No selected model found, using default")
                getDefaultFastModel()
            } else {
                Log.d("ModelManager", "Using selected model: $selectedModel")
                selectedModel
            }
        } catch (e: Exception) {
            Log.e("ModelManager", "Error getting selected model", e)
            return getDefaultFastModel()
        }
    }
    
    /**
     * Set the selected model
     */
    fun setSelectedModel(modelName: String) {
        sharedPreferences.edit()
            .putString(PREF_SELECTED_MODEL, modelName)
            .apply()
        Log.d("ModelManager", "Selected model set to: $modelName")
    }
    
    /**
     * Get the fastest available model
     */
    fun getFastestModel(): String {
        try {
            val models = getCachedModels()
            if (models.isEmpty()) {
                Log.d("ModelManager", "No cached models, using default fast model")
                return getDefaultFastModel()
            }
            
            val fastestModel = models
                .filter { it.supportedMethods.contains("generateContent") }
                .maxByOrNull { FAST_MODEL_PRIORITIES[it.name] ?: 0 }?.name
            
            return fastestModel ?: getDefaultFastModel()
        } catch (e: Exception) {
            Log.e("ModelManager", "Error getting fastest model", e)
            return getDefaultFastModel()
        }
    }
    
    /**
     * Check if models need to be refreshed
     */
    fun needsRefresh(): Boolean {
        val lastFetchTime = sharedPreferences.getLong(PREF_LAST_FETCH_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFetchTime) >= FETCH_CACHE_DURATION
    }
    
    /**
     * Get cache age in hours
     */
    fun getCacheAgeHours(): Long {
        val lastFetchTime = sharedPreferences.getLong(PREF_LAST_FETCH_TIME, 0)
        if (lastFetchTime == 0L) return -1
        return (System.currentTimeMillis() - lastFetchTime) / (60 * 60 * 1000)
    }
    
    private suspend fun fetchModelsFromAPI(): List<GeminiModel> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isNullOrEmpty()) {
                Log.w("ModelManager", "No API key available, using fallback models")
                return@withContext getFallbackModels()
            }
            
            // Securely call Gemini REST API to list available models
            val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Content-Type", "application/json")
            
            val responseCode = connection.responseCode
            if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                Log.e("ModelManager", "API request failed with code: $responseCode")
                return@withContext getFallbackModels()
            }
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            
            // Parse JSON response
            val jsonObject = com.google.gson.JsonParser.parseString(response).asJsonObject
            val modelsArray = jsonObject.getAsJsonArray("models")
            
            val geminiModels = mutableListOf<GeminiModel>()
            
            for (modelElement in modelsArray) {
                val modelObj = modelElement.asJsonObject
                val name = modelObj.get("name")?.asString ?: continue
                val displayName = modelObj.get("displayName")?.asString ?: name
                
                // Get supported generation methods
                val methodsArray = modelObj.getAsJsonArray("supportedGenerationMethods")
                val methods = methodsArray?.map { it.asString } ?: emptyList()
                
                // Only include models that support generateContent
                if (!methods.contains("generateContent")) continue
                
                // Extract short name for filtering (e.g., "gemini-2.0-flash-lite" from "models/gemini-2.0-flash-lite")
                val shortName = name.substringAfterLast("/")
                
                // FILTER: Only include STABLE models (exclude experimental, preview, deprecated)
                val isExperimental = shortName.contains("exp") || 
                                     shortName.contains("preview") || 
                                     shortName.contains("thinking") ||
                                     shortName.contains("deprecated")
                if (isExperimental) {
                    Log.d("ModelManager", "Skipping non-stable model: $shortName")
                    continue
                }
                
                // Determine if it's a fast (Flash/Lite) model
                val isFast = shortName.contains("flash") || shortName.contains("lite")
                
                // Determine version from name
                val version = when {
                    shortName.contains("2.5") -> "2.5"
                    shortName.contains("2.0") -> "2.0"
                    shortName.contains("1.5") -> "1.5"
                    shortName.contains("3") -> "3.0"
                    else -> "unknown"
                }
                
                geminiModels.add(GeminiModel(
                    name = name,
                    version = version,
                    supportedMethods = methods,
                    isFast = isFast,
                    isStable = true  // All models passing filter are stable
                ))
            }
            
            // Sort: Lite models first (highest priority), then by priority map, then alphabetically
            val sortedModels = geminiModels.sortedWith(
                compareByDescending<GeminiModel> { model ->
                    val shortName = model.name.substringAfterLast("/")
                    FAST_MODEL_PRIORITIES[shortName] ?: 0
                }.thenBy { !it.isFast }
                 .thenBy { it.name }
            )
            
            Log.d("ModelManager", "Fetched ${sortedModels.size} stable models from API")
            
            if (sortedModels.isEmpty()) {
                Log.w("ModelManager", "No stable models found from API, using fallback")
                return@withContext getFallbackModels()
            }
            
            return@withContext sortedModels
            
        } catch (e: Exception) {
            Log.e("ModelManager", "Failed to fetch models from API", e)
            return@withContext getFallbackModels()
        }
    }
    
    private fun getCachedModels(): List<GeminiModel> {
        try {
            val modelsJson = sharedPreferences.getString(PREF_MODELS_LIST, "[]")
            if (modelsJson.isNullOrEmpty()) {
                Log.d("ModelManager", "No cached models found")
                return emptyList()
            }
            
            val type = object : TypeToken<List<GeminiModel>>() {}.type
            val models = gson.fromJson<List<GeminiModel>>(modelsJson, type)
            return models ?: emptyList()
        } catch (e: Exception) {
            Log.e("ModelManager", "Error reading cached models", e)
            return emptyList()
        }
    }
    
    private fun getFallbackModels(): List<GeminiModel> {
        // Essential fallback models - Lite models first for resource efficiency
        return listOf(
            GeminiModel("models/gemini-2.0-flash-lite", "2.0", listOf("generateContent", "countTokens"), isFast = true, isStable = true),
            GeminiModel("models/gemini-2.5-flash", "2.5", listOf("generateContent", "countTokens"), isFast = true, isStable = true),
            GeminiModel("models/gemini-1.5-flash", "1.5", listOf("generateContent", "countTokens"), isFast = true, isStable = true)
        )
    }
    
    private fun saveModelsToCache(models: List<GeminiModel>) {
        try {
            val modelsJson = gson.toJson(models)
            sharedPreferences.edit()
                .putString(PREF_MODELS_LIST, modelsJson)
                .putLong(PREF_LAST_FETCH_TIME, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e("ModelManager", "Error saving models to cache", e)
        }
    }
    
    private fun getApiKey(): String? {
        // Try to get API key from various sources
        return try {
            // Method 1: From BuildConfig (if set)
            try {
                val buildConfigClass = Class.forName("com.newtown.billsplitter.BuildConfig")
                val apiKeyField = buildConfigClass.getDeclaredField("GEMINI_API_KEY")
                val apiKey = apiKeyField.get(null) as? String
                if (!apiKey.isNullOrEmpty() && apiKey != "your_api_key_here") {
                    return apiKey
                }
            } catch (e: Exception) {
                Log.d("ModelManager", "BuildConfig API key not found")
            }
            
            // Method 2: From SharedPreferences (user might have set it)
            val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val savedApiKey = prefs.getString("gemini_api_key", null)
            if (!savedApiKey.isNullOrEmpty()) {
                return savedApiKey
            }
            
            null
        } catch (e: Exception) {
            Log.e("ModelManager", "Error getting API key", e)
            null
        }
    }
    
    private fun getDefaultFastModel(): String {
        return "models/gemini-2.0-flash-lite" // Lite model for best resource efficiency
    }
    
    /**
     * Clear all cached data (for testing or reset)
     */
    fun clearCache() {
        sharedPreferences.edit()
            .remove(PREF_MODELS_LIST)
            .remove(PREF_LAST_FETCH_TIME)
            .apply()
        Log.d("ModelManager", "Model cache cleared")
    }
}
