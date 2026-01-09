package com.scanai.application

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val cameraPermission = Manifest.permission.CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

        if (ContextCompat.checkSelfPermission(this, cameraPermission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(cameraPermission)
        }

        setContent {
            var ready by remember { mutableStateOf(false) }

            if (ready) {
                ScanAIScreen()
            } else {
                StartupScreen { ready = true }
            }
        }
    }
}

@Composable
fun ScanAIScreen() {
    var cameraStarted by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var sumText by remember { mutableStateOf("") }
    var detailsText by remember { mutableStateOf("") }

    val executor = remember { Executors.newSingleThreadExecutor() }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(containerColor = Color(0xFF121212)) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "ScanAI",
                    fontSize = 28.sp,
                    color = Color.White,
                    modifier = Modifier.padding(12.dp)
                )

                if (cameraStarted) {
                    CameraPreview { imageCapture = it }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val capture = imageCapture ?: return@Button
                            capture.takePicture(
                                executor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val buffer = image.planes[0].buffer
                                        val bytes = ByteArray(buffer.remaining())
                                        buffer.get(bytes)
                                        image.close()

                                        uploadImage(
                                            bytes,
                                            onResult = { sum, numbers, text ->
                                                sumText = "Sum: $sum"
                                                detailsText =
                                                    "Numbers: ${numbers.joinToString(", ")}\n\nDetected Text:\n$text"
                                            },
                                            onError = {
                                                sumText = ""
                                                detailsText = "Error scanning image."
                                            }
                                        )
                                    }
                                }
                            )
                            sumText = "Scanning..."
                            detailsText = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.Black
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Scan",
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF1E1E1E))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (sumText.isNotEmpty()) {
                        Text(
                            text = sumText,
                            fontSize = 32.sp,
                            color = Color(0xFFFF4080)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(
                        text = detailsText,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                if (!cameraStarted) {
                    Button(
                        onClick = { cameraStarted = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .height(50.dp)
                    ) {
                        Text("Start Camera", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(onImageCaptureReady: (ImageCapture) -> Unit) {
    val context = LocalContext.current

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        factory = {
            val previewView = PreviewView(it)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(it)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                val imageCapture = ImageCapture.Builder().build()

                preview.setSurfaceProvider(previewView.surfaceProvider)

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as ComponentActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

                onImageCaptureReady(imageCapture)
            }, ContextCompat.getMainExecutor(it))

            previewView
        }
    )
}

fun uploadImage(
    bytes: ByteArray,
    onResult: (Int, List<Int>, String) -> Unit,
    onError: () -> Unit
) {
    val client = OkHttpClient()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file",
            "scan.jpg",
            bytes.toRequestBody("image/jpeg".toMediaType())
        )
        .build()

    val request = Request.Builder()
        .url("https://www.scanai.live/upload")
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: java.io.IOException) {
            onError()
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let {
                val json = JSONObject(it)
                val sum = json.getInt("sum")
                val text = json.getString("detected_text")
                val numbersJson = json.getJSONArray("numbers")
                val numbers = List(numbersJson.length()) { i ->
                    numbersJson.getInt(i)
                }
                onResult(sum, numbers, text)
            } ?: onError()
        }
    })
}