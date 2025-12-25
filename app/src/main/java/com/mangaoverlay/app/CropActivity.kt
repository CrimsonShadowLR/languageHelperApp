package com.mangaoverlay.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.mangaoverlay.app.api.TranslationClient
import com.mangaoverlay.app.databinding.ActivityCropBinding
import com.mangaoverlay.app.ui.LoadingDialog
import com.mangaoverlay.app.utils.TranslationError
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for cropping captured screenshots
 * Displays the captured image and allows user to select the area to crop
 */
class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropBinding
    private var capturedBitmap: Bitmap? = null
    private var translatedBitmap: Bitmap? = null
    private val translationClient = TranslationClient()
    private var loadingDialog: LoadingDialog? = null
    private var translationJob: Job? = null

    companion object {
        private const val TAG = "CropActivity"
        const val EXTRA_SCREENSHOT_PATH = "screenshot_path"
        private const val REQUEST_WRITE_STORAGE = 112
        private const val IMAGE_COMPRESSION_QUALITY = 95
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load the captured screenshot
        loadScreenshot()

        // Setup button listeners
        setupButtons()
    }

    private fun loadScreenshot() {
        val screenshotPath = intent.getStringExtra(EXTRA_SCREENSHOT_PATH)
        if (screenshotPath == null) {
            Toast.makeText(this, "Error: No screenshot provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val file = File(screenshotPath)
        if (!file.exists()) {
            Toast.makeText(this, "Error: Screenshot file not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            capturedBitmap = BitmapFactory.decodeFile(screenshotPath)
            capturedBitmap?.let { bitmap ->
                binding.cropView.setBitmap(bitmap)
            } ?: run {
                Toast.makeText(this, "Error: Failed to load screenshot", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            // Delete the temporary screenshot file
            deleteScreenshotFile()
            finish()
        }

        binding.confirmButton.setOnClickListener {
            processCroppedImage()
        }
    }

    private fun processCroppedImage() {
        val croppedBitmap = binding.cropView.getCroppedBitmap()
        if (croppedBitmap == null) {
            Toast.makeText(this, "Error: Failed to crop image", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Processing cropped image: ${croppedBitmap.width}x${croppedBitmap.height}")

        // Show loading dialog
        loadingDialog = LoadingDialog(this) {
            // Cancel button clicked
            Log.d(TAG, "Translation cancelled by user")
            translationJob?.cancel()
            hideLoading()
        }.apply {
            show()
        }

        // Launch coroutine for API call
        translationJob = lifecycleScope.launch {
            try {
                loadingDialog?.setProgressText(R.string.loading_translating)

                // Call translation API
                val result = translationClient.translateImage(croppedBitmap)

                // Hide loading
                hideLoading()

                // Log the result as per Phase 2 requirements
                Log.d(TAG, "=== Translation Result ===")
                Log.d(TAG, "Japanese: ${result.japanese}")
                Log.d(TAG, "Furigana: ${result.furigana}")
                Log.d(TAG, "English: ${result.english}")
                Log.d(TAG, "Confidence: ${result.confidence}")
                Log.d(TAG, "========================")

                // Display the edited image with translation
                result.editedImage?.let { editedBitmap ->
                    Log.d(TAG, "Displaying edited image: ${editedBitmap.width}x${editedBitmap.height}")
                    translatedBitmap?.recycle()
                    translatedBitmap = editedBitmap
                    binding.cropView.setBitmap(editedBitmap)
                    binding.cropView.setShowCropUI(false)
                    binding.instructionsText.text = getString(R.string.translation_complete_instructions)

                    // Update confirm button to "Done"
                    binding.confirmButton.text = getString(R.string.done)
                    binding.confirmButton.setOnClickListener {
                        deleteScreenshotFile()
                        finish()
                    }

                    // Update cancel button to "Save"
                    binding.cancelButton.text = getString(R.string.save)
                    binding.cancelButton.setOnClickListener {
                        translatedBitmap?.let { bitmap ->
                            // Check for WRITE_EXTERNAL_STORAGE permission on Android 9 and below
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                if (ContextCompat.checkSelfPermission(
                                        this@CropActivity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    // Request permission
                                    ActivityCompat.requestPermissions(
                                        this@CropActivity,
                                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                        REQUEST_WRITE_STORAGE
                                    )
                                    return@setOnClickListener
                                }
                            }
                            
                            trySaveImage(bitmap)
                        }
                    }
                } ?: run {
                    // No edited image, just show text
                    Toast.makeText(
                        this@CropActivity,
                        "Translation complete!\nJapanese: ${result.japanese.take(30)}...\nEnglish: ${result.english.take(30)}...",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Clean up cropped bitmap
                croppedBitmap.recycle()

            } catch (e: Exception) {
                Log.e(TAG, "Translation error", e)
                hideLoading()

                // Show error message
                val errorMessage = TranslationError.getErrorMessage(e)
                Toast.makeText(
                    this@CropActivity,
                    "Translation failed: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()

                // Don't finish on error - let user try again or cancel
                croppedBitmap.recycle()
            }
        }
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun deleteScreenshotFile() {
        intent.getStringExtra(EXTRA_SCREENSHOT_PATH)?.let { path ->
            File(path).delete()
        }
    }

    /**
     * Attempts to save the image and handles all error cases with appropriate user feedback
     */
    private fun trySaveImage(bitmap: Bitmap) {
        try {
            val saved = saveImageToGallery(bitmap)
            if (saved) {
                Toast.makeText(
                    this,
                    getString(R.string.image_saved_to_downloads),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.failed_to_save_image),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: IOException) {
            val message = when {
                e.message?.contains("ENOSPC", ignoreCase = true) == true -> "Storage full. Please free up space and try again."
                e.message?.contains("EROFS", ignoreCase = true) == true -> "Storage is read-only. Cannot save image."
                else -> "File system error. Unable to save image."
            }
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Failed to save image due to I/O error", e)
        } catch (e: Exception) {
            val errorMsg = e.message ?: getString(R.string.unknown_error)
            Toast.makeText(
                this,
                getString(R.string.failed_to_save_image_error, errorMsg),
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "Failed to save image due to unexpected error", e)
        }
    }

    /**
     * Saves the translated image to the Downloads folder
     * Uses MediaStore API for Android 10+ and legacy file storage for older versions
     */
    private fun saveImageToGallery(bitmap: Bitmap): Boolean {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "manga_translated_$timestamp.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    val success = contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_QUALITY, outputStream)
                    } ?: run {
                        Log.e(TAG, "Failed to open output stream for URI: $it")
                        false
                    }
                    if (success) {
                        Log.d(TAG, "Image saved to Downloads: $fileName")
                    }
                    success
                } ?: run {
                    Log.e(TAG, "Failed to insert image into MediaStore")
                    false
                }
            } else {
                // Legacy storage for Android 9 and below
                // Check if external storage is available and mounted
                val storageState = Environment.getExternalStorageState()
                if (storageState != Environment.MEDIA_MOUNTED) {
                    throw IOException("External storage not available. State: $storageState")
                }
                
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val imageFile = File(downloadsDir, fileName)
                val success = FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_QUALITY, outputStream)
                }

                if (success) {
                    // Notify media scanner about the new file using MediaScannerConnection
                    MediaScannerConnection.scanFile(
                        this,
                        arrayOf(imageFile.absolutePath),
                        arrayOf("image/jpeg"),
                        null
                    )
                    Log.d(TAG, "Image saved to Downloads: ${imageFile.absolutePath}")
                }
                success
            }
        } catch (e: Exception) {
            val apiLevel = Build.VERSION.SDK_INT
            val method = if (apiLevel >= Build.VERSION_CODES.Q) "MediaStore API" else "legacy file storage"
            Log.e(TAG, "Failed to save image using $method (Android API $apiLevel)", e)
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try saving again
                translatedBitmap?.let { bitmap ->
                    trySaveImage(bitmap)
                }
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied_storage),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translationJob?.cancel()
        hideLoading()
        capturedBitmap?.recycle()
        capturedBitmap = null
        translatedBitmap?.recycle()
        translatedBitmap = null
    }
}
