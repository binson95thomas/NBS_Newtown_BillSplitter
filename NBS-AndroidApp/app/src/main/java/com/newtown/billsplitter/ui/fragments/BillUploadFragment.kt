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

class BillUploadFragment : Fragment() {
    private var _binding: FragmentBillUploadBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentPhotoUri: Uri? = null
    private var currentFlashMode: Int = ImageCapture.FLASH_MODE_AUTO

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
            binding.processingCard.visibility = if (isProcessing) View.VISIBLE else View.GONE
            if (isProcessing) {
                binding.resultCard.visibility = View.GONE
            }
        }

        viewModel.processingResult.observe(viewLifecycleOwner) { result ->
            if (result == null) {
                binding.resultCard.visibility = View.GONE
            } else {
                binding.resultCard.visibility = View.VISIBLE
                binding.resultText.text = result.message
                if (result.success) {
                    binding.resultIcon.setImageResource(com.newtown.billsplitter.R.drawable.ic_check)
                    binding.resultIcon.setColorFilter(resources.getColor(com.newtown.billsplitter.R.color.secondary_500, null))
                } else {
                    binding.resultIcon.setImageResource(com.newtown.billsplitter.R.drawable.ic_error)
                    binding.resultIcon.setColorFilter(resources.getColor(com.newtown.billsplitter.R.color.error_500, null))
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.cameraButton.setOnClickListener {
            openCamera()
        }

        binding.galleryButton.setOnClickListener {
            openGallery()
        }
        
        binding.captureButton?.setOnClickListener {
            takePhoto()
        }
        
        binding.cancelCaptureButton?.setOnClickListener {
            cancelCapture()
        }
        
        // Flash control buttons
        binding.flashAutoButton?.setOnClickListener {
            setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            updateFlashButtonStates()
        }
        
        binding.flashOnButton?.setOnClickListener {
            setFlashMode(ImageCapture.FLASH_MODE_ON)
            updateFlashButtonStates()
        }
        
        binding.flashOffButton?.setOnClickListener {
            setFlashMode(ImageCapture.FLASH_MODE_OFF)
            updateFlashButtonStates()
        }
        
        // Photo preview buttons
        binding.retakeButton?.setOnClickListener {
            showCameraPreview()
        }
        
        binding.submitButton?.setOnClickListener {
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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
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
                    Toast.makeText(context, "Camera ready! Position your receipt", Toast.LENGTH_SHORT).show()
                    updateFlashButtonStates() // Initialize flash button states
                }, 500)
                
            } catch (e: Exception) {
                Log.e("CameraX", "Camera binding failed", e)
                Toast.makeText(context, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Reset UI on error
                binding.cameraPreviewContainer.visibility = View.GONE
                binding.cameraButton.visibility = View.VISIBLE
                binding.galleryButton.visibility = View.VISIBLE
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        // Show capture feedback
        Toast.makeText(context, "Capturing image...", Toast.LENGTH_SHORT).show()
        
        val photoFile = File(
            requireContext().getExternalFilesDir(null),
            "bill_${System.currentTimeMillis()}.jpg"
        )
        
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("CameraX", "Photo saved successfully: ${photoFile.absolutePath}")
                    Log.d("CameraX", "File size: ${photoFile.length()} bytes")
                    
                    // Show photo preview instead of processing immediately
                    showPhotoPreview(photoUri)
                }
                
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed", exc)
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
            // Store the current photo URI for later use
            currentPhotoUri = photoUri
            
            // Load the captured image into the preview
            val inputStream = requireContext().contentResolver.openInputStream(photoUri)
            val bitmap: Bitmap? = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                binding.capturedImageView.setImageBitmap(bitmap)
                
                // Hide camera and show photo preview
                binding.cameraPreviewContainer.visibility = View.GONE
                binding.photoPreviewContainer.visibility = View.VISIBLE
                
                Toast.makeText(context, "Photo captured! Review and submit", Toast.LENGTH_SHORT).show()
            } else {
                throw Exception("Failed to decode captured image")
            }
        } catch (e: Exception) {
            Log.e("PhotoPreview", "Failed to show photo preview", e)
            Toast.makeText(context, "Failed to show photo preview", Toast.LENGTH_SHORT).show()
            
            // Fallback to direct processing
            viewModel.processBillImage(photoUri.toString())
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
        // Get the current photo URI from the captured image view
        val currentPhotoUri = currentPhotoUri
        if (currentPhotoUri != null) {
            // Hide photo preview and show main buttons
            binding.photoPreviewContainer.visibility = View.GONE
            binding.cameraButton.visibility = View.VISIBLE
            binding.galleryButton.visibility = View.VISIBLE
            
            // Process the captured image
            viewModel.processBillImage(currentPhotoUri.toString())
            Toast.makeText(context, "Processing bill with AI...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No photo to submit", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun setFlashMode(flashMode: Int) {
        currentFlashMode = flashMode
        imageCapture?.flashMode = flashMode
        
        // Show feedback
        val flashText = when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> "Flash: Auto"
            ImageCapture.FLASH_MODE_ON -> "Flash: On"
            ImageCapture.FLASH_MODE_OFF -> "Flash: Off"
            else -> "Flash: Unknown"
        }
        Toast.makeText(context, flashText, Toast.LENGTH_SHORT).show()
    }

    private fun updateFlashButtonStates() {
        // Update button styles based on current flash mode
        binding.flashAutoButton?.let { button ->
            if (currentFlashMode == ImageCapture.FLASH_MODE_AUTO) {
                button.setBackgroundColor(resources.getColor(com.newtown.billsplitter.R.color.accent_500, null))
                button.setTextColor(resources.getColor(com.newtown.billsplitter.R.color.white, null))
            } else {
                button.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                button.setTextColor(resources.getColor(com.newtown.billsplitter.R.color.accent_500, null))
            }
        }
        
        binding.flashOnButton?.let { button ->
            if (currentFlashMode == ImageCapture.FLASH_MODE_ON) {
                button.setBackgroundColor(resources.getColor(com.newtown.billsplitter.R.color.accent_500, null))
                button.setTextColor(resources.getColor(com.newtown.billsplitter.R.color.white, null))
            } else {
                button.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                button.setTextColor(resources.getColor(com.newtown.billsplitter.R.color.accent_500, null))
            }
        }
        
        binding.flashOffButton?.let { button ->
            if (currentFlashMode == ImageCapture.FLASH_MODE_OFF) {
                button.setBackgroundColor(resources.getColor(com.newtown.billsplitter.R.color.accent_500, null))
                button.setTextColor(resources.getColor(com.newtown.billsplitter.R.color.white, null))
            } else {
                button.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                button.setTextColor(resources.getColor(com.newtown.billsplitter.R.color.accent_500, null))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
} 