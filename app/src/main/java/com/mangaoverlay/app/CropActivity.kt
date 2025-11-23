package com.mangaoverlay.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mangaoverlay.app.api.TranslationClient
import com.mangaoverlay.app.databinding.ActivityCropBinding
import com.mangaoverlay.app.ui.LoadingDialog
import com.mangaoverlay.app.utils.TranslationError
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/**
 * Activity for cropping captured screenshots
 * Displays the captured image and allows user to select the area to crop
 */
class CropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropBinding
    private var capturedBitmap: Bitmap? = null
    private val translationClient = TranslationClient()
    private var loadingDialog: LoadingDialog? = null
    private var translationJob: Job? = null

    companion object {
        private const val TAG = "CropActivity"
        const val EXTRA_SCREENSHOT_PATH = "screenshot_path"
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

                // Show success message
                Toast.makeText(
                    this@CropActivity,
                    "Translation received! Check logcat for details.\n" +
                            "Japanese: ${result.japanese.take(30)}...\n" +
                            "English: ${result.english.take(30)}...",
                    Toast.LENGTH_LONG
                ).show()

                // Clean up and finish
                croppedBitmap.recycle()
                deleteScreenshotFile()
                finish()

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

    override fun onDestroy() {
        super.onDestroy()
        translationJob?.cancel()
        hideLoading()
        capturedBitmap?.recycle()
        capturedBitmap = null
    }
}
