package io.github.takusan23.andaikacaroid.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.HardwareBufferRenderer
import android.graphics.RenderNode
import android.hardware.DataSpace
import android.hardware.HardwareBuffer
import android.media.ImageWriter
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * special thanks:
 * https://github.com/android/platform-samples/blob/main/samples/media/ultrahdr/src/main/java/com/example/platform/media/ultrahdr/video/UltraHDRToHDRVideo.kt
 *
 * 実際の処理は↑をそのままパクったもの
 */
class UltraHdrToHdrVideoViewModel(application: Application) : AndroidViewModel(application) {
    private val context
        get() = application.applicationContext

    private val _isRunningFlow = MutableStateFlow(false)
    val isRunningFlow = _isRunningFlow.asStateFlow()

    private val _snackbarStateFlow = MutableStateFlow<SnackbarState?>(null)
    val snackbarStateFlow = _snackbarStateFlow.asStateFlow()

    fun createHdrVideo(uri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {

            // 動画にする画像
            // GainMap がない場合は UltraHDR じゃないので return
            val bitmap = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
            if (!bitmap.hasGainmap()) {
                showSnackbar(SnackbarState.RequiredUltraHdrPhoto)
                return@launch
            }

            _isRunningFlow.value = true
            // 一時的に保存
            val tempFile = context.getExternalFilesDir(null)!!.resolve("UltraHdrToHdrVideo_${System.currentTimeMillis()}.mp4")
            // エンコーダー
            val akariVideoEncoder = AkariVideoEncoder().apply {
                prepare(
                    output = tempFile.toAkariCoreInputOutputData(),
                    containerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                    outputVideoWidth = bitmap.width,
                    outputVideoHeight = bitmap.height,
                    frameRate = 30,
                    bitRate = 5_000_000,
                    keyframeInterval = 1,
                    codecName = MediaFormat.MIMETYPE_VIDEO_HEVC,
                    tenBitHdrParametersOrNullSdr = AkariVideoEncoder.TenBitHdrParameters()
                )
            }

            val imageWriter = ImageWriter.Builder(akariVideoEncoder.getInputSurface())
                .setHardwareBufferFormat(HardwareBuffer.RGBA_1010102)
                .setDataSpace(DataSpace.DATASPACE_BT2020_HLG)
                .setMaxImages(32)
                .setUsage(HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE)
                .build()

            coroutineScope {
                val drawJob = launch {
                    (0 until 30).forEach { frameIndex ->

                        // Renders [Bitmap] contents to an [Image]'s [HardwareBuffer].
                        val image = imageWriter.dequeueInputImage()
                        image.hardwareBuffer?.let { buffer ->
                            // Initialize HardwareBufferRenderer
                            val renderer = HardwareBufferRenderer(buffer)

                            // Initialize & Configure RenderNode.
                            val node = RenderNode("ultra-hdr-to-video")
                            node.setPosition(0, 0, image.width, image.height)

                            // Draw the bitmap contents onto the render node.
                            val canvas = node.beginRecording()
                            canvas.drawBitmap(bitmap, .0f, .0f, null)
                            node.endRecording()

                            // Set render node to hardware renderer
                            renderer.setContentRoot(node)

                            // Render the nodes contents to the hardware buffer of the provided image hardware
                            // buffer.
                            renderer.obtainRenderRequest().setColorSpace(ColorSpace.get(ColorSpace.Named.BT2020_HLG)).draw(
                                { exe -> exe.run() },
                                { result -> image.fence = result.fence },
                            )
                            // await until HardwareBufferRenderer.RenderResult fence is set on the image.
                            while (!image.fence.isValid) { /* await */
                            }
                            image.fence.awaitForever()
                            imageWriter.queueInputImage(image)
                        }
                    }
                }
                val encoderJob = launch {
                    akariVideoEncoder.start()
                }
                drawJob.join()
                encoderJob.cancelAndJoin()
            }

            val contentValues = contentValuesOf(
                MediaStore.MediaColumns.DISPLAY_NAME to tempFile.name,
                MediaStore.MediaColumns.RELATIVE_PATH to "${Environment.DIRECTORY_MOVIES}/andAicaroid",
                MediaStore.MediaColumns.MIME_TYPE to "video/mp4"
            )
            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)!!
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            tempFile.delete()
            _isRunningFlow.value = false
            showSnackbar(SnackbarState.Save(uri))
        }
    }

    fun dismissSnackbar() {
        _snackbarStateFlow.value = null
    }

    private fun showSnackbar(state: SnackbarState) {
        _snackbarStateFlow.value = state
    }

    sealed interface CreateState {
        data object Wait : CreateState
        data object Process : CreateState
        data class Finish(
            val saveFilePath: Uri
        ) : CreateState
    }

    sealed interface SnackbarState {
        data object RequiredUltraHdrPhoto : SnackbarState
        data class Save(val videoUri: Uri) : SnackbarState
    }
}