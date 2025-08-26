package com.example.wzryassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * MainActivity: 负责初始化界面、请求权限、启动服务，以及语音播报功能。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tts: TextToSpeech
    private lateinit var mediaProjectionManager: MediaProjectionManager

    // ActivityResultLauncher 用于请求屏幕捕捉权限
    private val capturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 获取 MediaProjection
            val data = result.data
            if (data != null) {
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra("resultCode", result.resultCode)
                    putExtra("data", data)
                }
                startService(serviceIntent)
            }
        } else {
            Log.e("MainActivity", "Screen capture permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 请求悬浮窗权限（仅在 Android M 及以上需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        // 初始化 MediaProjectionManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // 初始化 TextToSpeech（使用中文女性语音）
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.CHINESE
                tts.setPitch(1.0f)
                tts.setSpeechRate(1.0f)
                // 尝试选择女性语音（如果可用）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.voices.forEach { voice ->
                        if (voice.locale.language == Locale.CHINESE.language && voice.name.contains("female")) {
                            tts.voice = voice
                            return@forEach
                        }
                    }
                }
            }
        }

        // 按钮：启动悬浮窗服务
        findViewById<Button>(R.id.button_start_floating).setOnClickListener {
            val intent = Intent(this, FloatingWidgetService::class.java)
            startService(intent)
        }

        // 按钮：请求屏幕捕捉权限并启动服务
        findViewById<Button>(R.id.button_start_capture).setOnClickListener {
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            capturePermissionLauncher.launch(captureIntent)
        }

        // 按钮：语音问答示例
        findViewById<Button>(R.id.button_ask).setOnClickListener {
            val query = findViewById<EditText>(R.id.edit_query).text.toString()
            // 调用 AIPlaceholder 获取答案（stub 实现）
            val answer = AIPlaceholder.getAnswer(query)
            // 语音播报答案
            if (answer.isNotEmpty()) {
                tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "AI_ANSWER_ID")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放 TextToSpeech 资源
        tts.stop()
        tts.shutdown()
    }
}
