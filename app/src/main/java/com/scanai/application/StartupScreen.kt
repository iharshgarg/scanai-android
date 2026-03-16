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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun StartupScreen(onReady: () -> Unit) {

    var status by remember {
        mutableStateOf(
            "Starting AI engine...\nThis may take up to 2 minutes.\nPlease be patient."
        )
    }

    var showRetry by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(retryTrigger) {

        showRetry = false
        status =
            "Starting AI engine...\nThis may take up to 2 minutes.\nPlease be patient."

        val startTime = System.currentTimeMillis()
        val maxWait = 120000L   // 2 minutes

        while (true) {

            val success = checkServerOnce()

            if (success) {
                onReady()
                break
            }

            val elapsed = System.currentTimeMillis() - startTime

            if (elapsed > maxWait) {
                status =
                    "Server is taking longer than expected.\nPlease tap retry."
                showRetry = true
                break
            }

            delay(4000) // retry every 4 seconds
        }
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

                Spacer(Modifier.height(24.dp))

                CircularProgressIndicator(
                    color = Color(0xFF00C853)
                )

                if (showRetry) {

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { retryTrigger++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

suspend fun checkServerOnce(): Boolean {

    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://www.scanai.live")
                .get()
                .build()

            val response = OkHttpClient().newCall(request).execute()
            response.isSuccessful

        } catch (e: Exception) {
            false
        }
    }
}