package com.example.qrcodescanner

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.Manifest
import android.graphics.Matrix
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView



class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var barcodeRecyclerView: RecyclerView
    private lateinit var overlayView: OverlayView
    private lateinit var barcodeAdapter: BarcodeAdapter

    // List to store scanned QR codes
    private val scannedBarcodes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        barcodeRecyclerView = findViewById(R.id.barcodeRecyclerView)
        overlayView = findViewById(R.id.overlayView)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Setup RecyclerView
        barcodeAdapter = BarcodeAdapter(scannedBarcodes)
        barcodeRecyclerView.adapter = barcodeAdapter
        barcodeRecyclerView.layoutManager = LinearLayoutManager(this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val barcodeScanner = BarcodeScanning.getClient()
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Use case binding failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        @androidx.camera.core.ExperimentalGetImage
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val newScannedBarcodes = mutableListOf<String>()
                    for (barcode in barcodes) {
                        when (barcode.valueType) {
                            Barcode.TYPE_URL -> {
                                barcode.url?.url?.let { newScannedBarcodes.add(it) }
                                barcode.boundingBox?.let { updateOverlay(it, imageProxy) }
                            }
                            Barcode.TYPE_TEXT -> {
                                barcode.displayValue?.let { newScannedBarcodes.add(it) }
                                barcode.boundingBox?.let { updateOverlay(it, imageProxy) }
                            }
                        }
                    }
                    updateScannedBarcodes(newScannedBarcodes)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to scan barcode", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun updateScannedBarcodes(newScannedBarcodes: List<String>) {
        // Add new scanned barcodes to the list only if list content changes
        // This is to avoid unnecessary updates to the RecyclerView
        if (scannedBarcodes != newScannedBarcodes) {
            scannedBarcodes.clear()
            scannedBarcodes.addAll(newScannedBarcodes)
            barcodeAdapter.notifyDataSetChanged()
        }
    }

    private fun updateOverlay(boundingBox: android.graphics.Rect, imageProxy: ImageProxy) {
        val overlayBoundingBox = transformBoundingBox(boundingBox, imageProxy)
        overlayView.qrCodeBounds = overlayBoundingBox
        overlayView.invalidate()
    }

    private fun transformBoundingBox(boundingBox: android.graphics.Rect, imageProxy: ImageProxy): android.graphics.Rect {
        val previewWidth = previewView.width.toFloat()
        val previewHeight = previewView.height.toFloat()
        val imageWidth = imageProxy.width.toFloat()
        val imageHeight = imageProxy.height.toFloat()

        // Adjust for the aspect ratio
        val aspectRatioPreview = previewWidth / previewHeight
        val aspectRatioImage = imageHeight / imageWidth

        val scaleFactor: Float
        val dx: Float
        val dy: Float

        if (aspectRatioPreview > aspectRatioImage) {
            scaleFactor = previewWidth / imageHeight
            dx = 0f
            dy = (previewHeight - imageWidth * scaleFactor) / 2
        } else {
            scaleFactor = previewHeight / imageWidth
            dx = (previewWidth - imageHeight * scaleFactor) / 2
            dy = 0f
        }

        val matrix = Matrix()
        matrix.postScale(scaleFactor, scaleFactor)
        matrix.postTranslate(dx, dy)

        // Map the bounding box to the previewView coordinates
        val rectF = RectF(boundingBox)
        matrix.mapRect(rectF)

        return android.graphics.Rect(
            rectF.left.toInt(),
            rectF.top.toInt(),
            rectF.right.toInt(),
            rectF.bottom.toInt()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

