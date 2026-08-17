package com.lazygeniouz.acv.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lazygeniouz.acv.example.ui.theme.AdContainerSampleTheme

/** Hosts the Compose banner sample. */
class MainActivity : ComponentActivity() {

    private val initializationResult = mutableStateOf<Result<Unit>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()

        (application as App).adsInitialization.whenComplete { _, error ->
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    initializationResult.value = error
                        ?.let { Result.failure(it.cause ?: it) }
                        ?: Result.success(Unit)
                }
            }
        }

        setContent {
            AdContainerSampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BannerSample(initializationResult.value)
                }
            }
        }
    }

    private fun hideNavigationBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}