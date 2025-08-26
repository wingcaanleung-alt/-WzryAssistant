package com.example.wzryassistant

import android.app.Service
import android.content.Intent
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log

/**
 * ScreenCaptureService: 使用 MediaProjection API 捕捉屏幕。
 */
class ScreenCaptureService : Service() {

    private var virtualDisplay: VirtualDisplay? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 从 Intent 中获取 MediaProjection 授权结果
        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data: Intent? = intent?.getParcelableExtra("data")

        if (resultCode == Activity.RESULT_OK && data != null) {
            // 获取 MediaProjection 实例
            val mediaProjection = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
                .getMediaProjection(resultCode, data)

            // 获取屏幕分辨率和密度
            val metrics = Resources.getSystem().displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val screenDensity = metrics.densityDpi

            // 创建 ImageReader 用于接收帧图像
            val imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                PixelFormat.RGBA_8888, 2
            )

            // 创建虚拟显示，将屏幕内容输出到 ImageReader 的 Surface
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface, null, null
            )

            // 延迟一小段时间以确保有图像可获取
            Thread.sleep(100)  // 简单延时，实际可使用回调

            // 获取最新一帧图像
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                // 将 Image 转换为 Bitmap
                val buffer = image.planes[0].buffer
                val pixelStride = image.planes[0].pixelStride
                val rowStride = image.planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth
                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                // 调用 AI 模型接口（stub 实现）
                val position = AIPlaceholder.predictEnemyPosition(bitmap)
                Log.d("ScreenCaptureService", "Predicted enemy position: $position")
            }

            // 清理并停止虚拟显示
            virtualDisplay?.release()
            mediaProjection.stop()
        } else {
            Log.e("ScreenCaptureService", "Failed to start MediaProjection")
        }

        // 服务工作完成后停止自身
        stopSelf()
        return START_NOT_STICKY
    }
}
