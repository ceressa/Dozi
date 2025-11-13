package com.bardino.dozi

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.max

class SplashActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Container
        val container = FrameLayout(this)
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // VideoView
        val videoView = VideoView(this)
        container.addView(videoView)
        setContentView(container)

        // Tam ekran
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, container)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Video hazırla
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.bardino_splash}")
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener { mp: MediaPlayer ->
            val videoWidth = mp.videoWidth.toFloat()
            val videoHeight = mp.videoHeight.toFloat()
            val screenWidth = resources.displayMetrics.widthPixels.toFloat()
            val screenHeight = resources.displayMetrics.heightPixels.toFloat()

            // Center crop mantığı - ekranı tamamen kaplasın
            val scale = max(screenWidth / videoWidth, screenHeight / videoHeight)

            val scaledWidth = (videoWidth * scale).toInt()
            val scaledHeight = (videoHeight * scale).toInt()

            val params = FrameLayout.LayoutParams(scaledWidth, scaledHeight)
            params.gravity = android.view.Gravity.CENTER
            videoView.layoutParams = params

            mp.start()
        }

        videoView.setOnCompletionListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        videoView.setOnErrorListener { _, _, _ ->
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            true
        }
    }
}