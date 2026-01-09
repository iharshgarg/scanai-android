package com.scanai.application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.Executors

@Composable
fun StartupScreen(onReady: () -> Unit) {
    var status by remember { mutableStateOf("Warming up server…") }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        checkServer(
            onSuccess = { onReady() },
            onFailure = {
                status = "Unable to reach server."
                failed = true
            }
        )
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "ScanAI",
                    fontSize = 28.sp,
                    color = Color.White
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = status,
                    color = Color(0xFFE0E0E0),
                    fontSize = 16.sp
                )

                if (!failed) {
                    Spacer(Modifier.height(20.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF00C853)
                    )
                }

                if (failed) {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            failed = false
                            status = "Retrying…"
                            checkServer(
                                onSuccess = { onReady() },
                                onFailure = {
                                    status = "Unable to reach server."
                                    failed = true
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

private fun checkServer(
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        try {
            val request = Request.Builder()
                .url("https://www.scanai.live")
                .get()
                .build()

            val response = OkHttpClient().newCall(request).execute()
            if (response.isSuccessful) {
                onSuccess()
            } else {
                onFailure()
            }
        } catch (e: Exception) {
            onFailure()
        }
    }
}