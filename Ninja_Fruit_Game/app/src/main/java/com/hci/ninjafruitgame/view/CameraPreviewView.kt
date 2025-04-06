package com.hci.ninjafruitgame.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.hci.ninjafruitgame.ml.PoseCameraAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var previewView: PreviewView
    fun getPreviewView(): PreviewView = previewView

    private val analyzerExecutor = Executors.newSingleThreadExecutor()


    init {
        initView()
    }

    private fun initView() {
        previewView = PreviewView(context)
        previewView.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        addView(previewView)
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun startCamera(lifecycleOwner: LifecycleOwner, analyzer: ImageAnalysis.Analyzer? = null) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            if (analyzer != null) {
                analysis.setAnalyzer(analyzerExecutor, analyzer)
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                cameraProvider.unbindAll()
                if (analyzer != null) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, analysis
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview
                    )
                }
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }


    fun stopCamera() {
        ProcessCameraProvider.getInstance(context).get().unbindAll()
    }
}