package com.newtown.billsplitter.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.newtown.billsplitter.databinding.FragmentBillUploadBinding
import com.newtown.billsplitter.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import java.io.FileOutputStream
import android.util.Log
import com.newtown.billsplitter.utils.HapticUtils
import com.newtown.billsplitter.utils.AnimationUtils
import android.widget.ImageView

class BillUploadFragment : Fragment() {
    private var _binding: FragmentBillUploadBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentPhotoUri: Uri? = null
    private var currentFlashMode: Int = ImageCapture.FLASH_MODE_ON

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Start camera immediately when permission is granted
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.processBillImage(uri.toString())
                Toast.makeText(context, "Image selected from gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBillUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupClickListeners()
        observeProcessingStatus()

        // Wire prompt mode toggle with persistence
        val prefs = requireContext().getSharedPreferences("BillSplitterPrefs", Context.MODE_PRIVATE)
        binding.promptModeSwitch?.let { sw ->
            val savedIsEnhanced = prefs.getBoolean("prompt_mode_enhanced", true)
            sw.isChecked = savedIsEnhanced
            viewModel.setPromptModeEnhanced(sw.isChecked)
            sw.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("prompt_mode_enhanced", isChecked).apply()
                viewModel.setPromptModeEnhanced(isChecked)
                Toast.makeText(requireContext(), if (isChecked) "Enhanced prompt enabled" else "Legacy prompt enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeProcessingStatus() {
        viewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            if (isProcessing) {
                // Show ocean loading animation
                binding.processingCard.visibility = View.VISIBLE
                binding.resultCard.visibility = View.GONE
                
                // Show ocean wave loading animation
                AnimationUtils.showOceanLoadingAnimation(binding.oceanLoadingAnimation, requireContext())
                
                // Ocean wave haptic pattern for processing start
                HapticUtils.oceanWavePattern(requireContext())
            } else {
                binding.processingCard.visibility = View.GONE
            }
        }

        viewModel.processingResult.observe(viewLifecycleOwner) { result ->
            if (result == null) {
                binding.resultCard.visibility = View.GONE
            } else {
                binding.resultCard.visibility = View.VISIBLE
                binding.resultText.text = result.message
                
                if (result.success) {
                    // Success state with animations and haptics
                    binding.resultIcon.setImageResource(com.newtown.billsplitter.R.drawable.ic_check)
                    binding.resultIcon.setColorFilter(resources.getColor(com.newtown.billsplitter.R.color.secondary_500, null))
                    
                    // Show success animation
                    AnimationUtils.showSuccessAnimation(binding.resultIcon, requireContext())
                    
                    // Success haptic pattern
                    HapticUtils.successPattern(requireContext())
                    
                    // Slide in result card with ocean wave entrance
                    AnimationUtils.oceanWaveEntrance(binding.resultCard)
                } else {
                    // Error state with haptics
                    binding.resultIcon.setImageResource(com.newtown.billsplitter.R.drawable.ic_error)
                    binding.resultIcon.setColorFilter(resources.getColor(com.newtown.billsplitter.R.color.error_500, null))
                    
                    // Error haptic pattern
                    HapticUtils.errorPattern(requireContext())
                    
                    // Fade in result card
                    AnimationUtils.fadeIn(binding.resultCard)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.cameraButton.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.bounceButton(it)
            openCamera()
        }

        binding.galleryButton.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.bounceButton(it)
            openGallery()
        }
        
        binding.captureButton?.setOnClickListener {
            HapticUtils.mediumTap(it)
            AnimationUtils.bounceButton(it)
            takePhoto()
        }
        
        binding.cancelCaptureButton?.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.bounceButton(it)
            cancelCapture()
        }
        
        // Single flash toggle button with haptic feedback
        binding.flashToggleButton?.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.pulseView(it)
            toggleFlashMode()
        }
        
        // Photo preview buttons with haptic feedback
        binding.retakeButton?.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.bounceButton(it)
            showCameraPreview()
        }
        
        binding.submitButton?.setOnClickListener {
            HapticUtils.mediumTap(it)
            AnimationUtils.bounceButton(it)
            submitPhoto()
        }
    }

    private fun openCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        try {
            // Check if fragment is still attached
            if (!isAdded || context == null) {
                Log.w("CameraX", "Fragment not attached, cannot start camera")
                return
            }
            
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            
            cameraProviderFuture.addListener({
                try {
                    // Double check fragment is still attached
                    if (!isAdded || context == null) {
                        Log.w("CameraX", "Fragment detached while starting camera")
                        return@addListener
                    }
                    
                    val cameraProvider = cameraProviderFuture.get()
                    
                    // Configure preview with better quality
                    val preview = Preview.Builder()
                        .build()
                    
                    // Configure image capture with maximum quality
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(currentFlashMode) // Use current flash mode
                        .build()
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            viewLifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        
                        // Connect preview to PreviewView
                        preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                        
                        // Show camera preview with animation
                        binding.cameraPreviewContainer.visibility = View.VISIBLE
                        binding.cameraButton.visibility = View.GONE
                        binding.galleryButton.visibility = View.GONE
                        
                        // Add a small delay to ensure camera is ready
                        binding.cameraPreview.postDelayed({
                            if (isAdded && context != null) {
                                Toast.makeText(context, "Camera ready! Position your receipt", Toast.LENGTH_SHORT).show()
                                updateFlashToggleButton() // Initialize flash button state
                            }
                        }, 500)
                        
                        Log.d("CameraX", "Camera started successfully")
                        
                    } catch (e: Exception) {
                        Log.e("CameraX", "Camera binding failed", e)
                        if (isAdded && context != null) {
                            Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
                            
                            // Reset UI on error
                            binding.cameraPreviewContainer.visibility = View.GONE
                            binding.cameraButton.visibility = View.VISIBLE
                            binding.galleryButton.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CameraX", "Error getting camera provider", e)
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Camera initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }, ContextCompat.getMainExecutor(requireContext()))
            
        } catch (e: Exception) {
            Log.e("CameraX", "Error starting camera", e)
            if (isAdded && context != null) {
                Toast.makeText(context, "Cannot start camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(context, "Camera not ready. Please try again.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Check if fragment is still attached
        if (!isAdded || context == null) {
            Log.w("CameraX", "Fragment not attached during photo capture")
            return
        }
        
        // Show capture feedback
        Toast.makeText(context, "Capturing image...", Toast.LENGTH_SHORT).show()
        
        try {
            val photoFile = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "bill_${System.currentTimeMillis()}.jpg"
            )
            
            // Ensure directory exists
            photoFile.parentFile?.mkdirs()
            
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            if (!isAdded || context == null) {
                                Log.w("CameraX", "Fragment detached after photo capture")
                                return
                            }
                            
                            Log.d("CameraX", "Photo saved successfully: ${photoFile.absolutePath}")
                            Log.d("CameraX", "File size: ${photoFile.length()} bytes")
                            
                            // Check if file exists and has content
                            if (!photoFile.exists() || photoFile.length() == 0L) {
                                Toast.makeText(context, "Photo file is empty or missing", Toast.LENGTH_LONG).show()
                                return
                            }
                            
                            // Use file URI directly instead of FileProvider for internal storage
                            val photoUri = Uri.fromFile(photoFile)
                            
                            // Show photo preview instead of processing immediately
                            showPhotoPreview(photoUri)
                            
                        } catch (e: Exception) {
                            Log.e("CameraX", "Error in onImageSaved", e)
                            if (isAdded && context != null) {
                                Toast.makeText(context, "Error processing captured photo: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CameraX", "Photo capture failed", exc)
                        if (!isAdded || context == null) {
                            return
                        }
                        
                        val errorMessage = when (exc.imageCaptureError) {
                            ImageCapture.ERROR_CAPTURE_FAILED -> "Capture failed. Please try again."
                            ImageCapture.ERROR_CAMERA_CLOSED -> "Camera was closed unexpectedly."
                            ImageCapture.ERROR_INVALID_CAMERA -> "Invalid camera configuration."
                            ImageCapture.ERROR_FILE_IO -> "Failed to save image. Check storage permissions."
                            ImageCapture.ERROR_UNKNOWN -> "Unknown error occurred."
                            else -> "Capture failed: ${exc.message}"
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Error setting up photo capture", e)
            if (isAdded && context != null) {
                Toast.makeText(context, "Failed to setup photo capture: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cancelCapture() {
        binding.cameraPreviewContainer.visibility = View.GONE
        binding.cameraButton.visibility = View.VISIBLE
        binding.galleryButton.visibility = View.VISIBLE
        
        // Unbind camera
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraX", "Failed to unbind camera", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun showPhotoPreview(photoUri: Uri) {
        try {
            if (!isAdded || context == null) {
                Log.w("PhotoPreview", "Fragment not attached during photo preview")
                return
            }
            
            Log.d("PhotoPreview", "Starting photo preview for URI: $photoUri")
            
            // Store the current photo URI for later use
            currentPhotoUri = photoUri
            
            // Load the captured image with proper orientation
            val rotatedBitmap = loadAndRotateImage(photoUri)
            
            if (rotatedBitmap != null) {
                Log.d("PhotoPreview", "Rotated bitmap loaded successfully: ${rotatedBitmap.width}x${rotatedBitmap.height}")
                
                binding.capturedImageView.setImageBitmap(rotatedBitmap)
                
                // Hide camera and show photo preview
                binding.cameraPreviewContainer.visibility = View.GONE
                binding.photoPreviewContainer.visibility = View.VISIBLE
                
                Log.d("PhotoPreview", "Camera hidden, photo preview shown")
                Toast.makeText(context, "Photo captured! Review and submit", Toast.LENGTH_SHORT).show()
                
                // Success haptic for photo capture
                HapticUtils.successPattern(requireContext())
            } else {
                Log.e("PhotoPreview", "Failed to load bitmap for preview")
                Toast.makeText(context, "Failed to load photo preview. Please try again.", Toast.LENGTH_LONG).show()
                
                // Reset UI on error
                binding.cameraPreviewContainer.visibility = View.VISIBLE
                binding.photoPreviewContainer.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("PhotoPreview", "Failed to show photo preview", e)
            if (isAdded && context != null) {
                Toast.makeText(context, "Failed to show photo preview: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Reset UI on error
                binding.cameraPreviewContainer.visibility = View.VISIBLE
                binding.photoPreviewContainer.visibility = View.GONE
            }
        }
    }

    private fun loadAndRotateImage(uri: Uri): Bitmap? {
        try {
            if (!isAdded || context == null) {
                Log.w("PhotoPreview", "Fragment not attached during image loading")
                return null
            }
            
            Log.d("PhotoPreview", "Loading image from URI: $uri")
            
            // Handle file URIs differently from content URIs
            val inputStream = when (uri.scheme) {
                "file" -> {
                    val file = File(uri.path ?: "")
                    if (!file.exists()) {
                        Log.e("PhotoPreview", "File does not exist: ${uri.path}")
                        return null
                    }
                    file.inputStream()
                }
                "content" -> {
                    requireContext().contentResolver.openInputStream(uri)
                }
                else -> {
                    Log.e("PhotoPreview", "Unsupported URI scheme: ${uri.scheme}")
                    return null
                }
            } ?: run {
                Log.e("PhotoPreview", "Failed to open input stream")
                return null
            }
            
            // First pass: get image dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Calculate sample size to avoid OutOfMemoryError
            val maxSize = 1024
            val sampleSize = maxOf(1, minOf(options.outWidth / maxSize, options.outHeight / maxSize))
            
            Log.d("PhotoPreview", "Image dimensions: ${options.outWidth}x${options.outHeight}, sample size: $sampleSize")
            
            // Second pass: decode with sample size
            val inputStream2 = when (uri.scheme) {
                "file" -> File(uri.path ?: "").inputStream()
                "content" -> requireContext().contentResolver.openInputStream(uri)
                else -> null
            } ?: run {
                Log.e("PhotoPreview", "Failed to open input stream for decoding")
                return null
            }
            
            val options2 = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, options2)
            inputStream2.close()
            
            if (bitmap == null) {
                Log.e("PhotoPreview", "Failed to decode bitmap")
                return null
            }
            
            Log.d("PhotoPreview", "Bitmap decoded: ${bitmap.width}x${bitmap.height}")
            
            // Get EXIF orientation (only for content URIs or if file has EXIF)
            try {
                val exifInputStream = when (uri.scheme) {
                    "file" -> File(uri.path ?: "").inputStream()
                    "content" -> requireContext().contentResolver.openInputStream(uri)
                    else -> null
                }
                
                if (exifInputStream != null) {
                    val exif = android.media.ExifInterface(exifInputStream)
                    val orientation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    exifInputStream.close()
                    
                    val matrix = android.graphics.Matrix()
                    when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                        android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    }
                    
                    return if (matrix.isIdentity) {
                        bitmap
                    } else {
                        try {
                            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        } catch (e: Exception) {
                            Log.e("PhotoPreview", "Error creating rotated bitmap", e)
                            bitmap // Return original if rotation fails
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PhotoPreview", "Error reading EXIF data", e)
                // Continue without rotation
            }
            
            return bitmap
            
        } catch (e: Exception) {
            Log.e("PhotoPreview", "Error loading and rotating image", e)
            return null
        }
    }

    private fun showCameraPreview() {
        // Hide photo preview and show camera again
        binding.photoPreviewContainer.visibility = View.GONE
        binding.cameraPreviewContainer.visibility = View.VISIBLE
        
        // Restart camera
        startCamera()
    }

    private fun submitPhoto() {
        try {
            if (!isAdded || context == null) {
                Log.w("PhotoPreview", "Fragment not attached during photo submission")
                return
            }
            
            // Get the current photo URI from the captured image view
            val currentPhotoUri = currentPhotoUri
            if (currentPhotoUri != null) {
                Log.d("PhotoPreview", "Submitting photo: $currentPhotoUri")
                
                // Hide photo preview and show main buttons
                binding.photoPreviewContainer.visibility = View.GONE
                binding.cameraButton.visibility = View.VISIBLE
                binding.galleryButton.visibility = View.VISIBLE
                
                // Process the captured image
                viewModel.processBillImage(currentPhotoUri.toString())
                Toast.makeText(context, "Processing bill with AI...", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("PhotoPreview", "No photo URI available for submission")
                Toast.makeText(context, "No photo to submit", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PhotoPreview", "Error submitting photo", e)
            if (isAdded && context != null) {
                Toast.makeText(context, "Error submitting photo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED



    private fun toggleFlashMode() {
        // Toggle between flash ON and OFF (no auto mode)
        currentFlashMode = when (currentFlashMode) {
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_ON // Default to ON
        }
        
        // Update camera flash mode
        imageCapture?.flashMode = currentFlashMode
        
        // Update button appearance
        updateFlashToggleButton()
        
        // Show feedback
        val flashText = when (currentFlashMode) {
            ImageCapture.FLASH_MODE_ON -> "Flash: On"
            ImageCapture.FLASH_MODE_OFF -> "Flash: Off"
            else -> "Flash: On"
        }
        Toast.makeText(context, flashText, Toast.LENGTH_SHORT).show()
    }

    private fun updateFlashToggleButton() {
        try {
            if (!isAdded || context == null) {
                return
            }
            
            binding.flashToggleButton?.let { button ->
                val (iconRes, backgroundTint) = when (currentFlashMode) {
                    ImageCapture.FLASH_MODE_ON -> {
                        Pair(com.newtown.billsplitter.R.drawable.ic_flash_on, "#80FFD700")
                    }
                    ImageCapture.FLASH_MODE_OFF -> {
                        Pair(com.newtown.billsplitter.R.drawable.ic_flash_off, "#80FF6B6B")
                    }
                    else -> {
                        Pair(com.newtown.billsplitter.R.drawable.ic_flash_on, "#80FFD700") // Default to ON
                    }
                }
                
                try {
                    button.setIconResource(iconRes)
                    button.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(backgroundTint))
                } catch (e: Exception) {
                    Log.e("CameraX", "Failed to update flash button", e)
                }
            }
        } catch (e: Exception) {
            Log.e("CameraX", "Error updating flash toggle button", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Properly shutdown camera
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    Log.e("CameraX", "Failed to unbind camera in onDestroyView", e)
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        } catch (e: Exception) {
            Log.e("CameraX", "Error getting camera provider in onDestroyView", e)
        }
        
        cameraExecutor.shutdown()
        _binding = null
    }
} 