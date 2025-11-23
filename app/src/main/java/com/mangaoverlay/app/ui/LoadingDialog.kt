package com.mangaoverlay.app.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import com.mangaoverlay.app.R
import com.mangaoverlay.app.databinding.DialogLoadingBinding

/**
 * Loading dialog with progress indicator and cancel button
 */
class LoadingDialog(
    context: Context,
    private val onCancel: (() -> Unit)? = null
) : Dialog(context) {

    private lateinit var binding: DialogLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make dialog background transparent
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Make dialog non-cancelable by touching outside
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        // Setup cancel button
        binding.cancelButton.setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }
    }

    /**
     * Update the progress message
     */
    fun setProgressText(text: String) {
        binding.progressText.text = text
    }

    /**
     * Update the progress message with resource id
     */
    fun setProgressText(resId: Int) {
        binding.progressText.setText(resId)
    }
}
