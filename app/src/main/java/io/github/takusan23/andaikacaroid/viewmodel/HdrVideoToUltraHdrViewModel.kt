package io.github.takusan23.andaikacaroid.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.opengl.Matrix
import android.os.Environment
import android.provider.MediaStore
import android.view.SurfaceHolder
import androidx.core.content.contentValuesOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import io.github.takusan23.akaricore.common.toAkariCoreInputOutputData
import io.github.takusan23.akaricore.graphics.AkariGraphicsProcessor
import io.github.takusan23.akaricore.graphics.AkariGraphicsSurfaceTexture
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorColorSpaceType
import io.github.takusan23.akaricore.graphics.data.AkariGraphicsProcessorRenderingPrepareData
import io.github.takusan23.akaricore.graphics.mediacodec.AkariVideoDecoder
import io.github.takusan23.libaicaroid.LibUltraHdrBridge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HdrVideoToUltraHdrViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context
        get() = application.applicationContext

    private val _surfaceHolderFlow = MutableStateFlow<SurfaceHolder?>(null)
    private val _colorSpaceFlow = MutableStateFlow<AkariGraphicsProcessorColorSpaceType?>(null)

    private val _videoSizeFlow = MutableStateFlow<VideoSize?>(null)
    val videoSizeFlow = _videoSizeFlow.asStateFlow()

    private val _videoUriFlow = MutableStateFlow<Uri?>(null)
    val videoUriFlow = _videoUriFlow.asStateFlow()

    private val _videoDurationFlowFlow = MutableStateFlow(0L)
    val videoDurationFlow = _videoDurationFlowFlow.asStateFlow()

    private val _currentPositionFlow = MutableStateFlow(0L)
    val currentPositionMsFlow = _currentPositionFlow.asStateFlow()

    private val _snackbarStateFlow = MutableStateFlow<SnackbarState?>(null)
    val snackbarStateFlow = _snackbarStateFlow.asStateFlow()

    /** OpenGL ES */
    private val akariGraphicsProcessorFlow = combine(
        flow = _surfaceHolderFlow,
        flow2 = _videoSizeFlow,
        flow3 = _colorSpaceFlow,
        transform = ::Triple
    ).let { it ->
        var prevProcessor: AkariGraphicsProcessor? = null
        it.transformLatest { (holder, sizePair, colorSpaceType) ->
            prevProcessor?.destroy()
            prevProcessor = null
            emit(null)
            if (holder != null && sizePair != null && colorSpaceType != null) {

                val (width, height) = sizePair
                val newAkariGraphicsProcessor = AkariGraphicsProcessor(
                    renderingPrepareData = AkariGraphicsProcessorRenderingPrepareData.SurfaceRendering(
                        surface = holder.surface,
                        width = width,
                        height = height
                    ),
                    colorSpaceType = colorSpaceType
                ).apply { prepare() }
                holder.setFixedSize(width, height)

                prevProcessor = newAkariGraphicsProcessor
                emit(newAkariGraphicsProcessor)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    /** 動画デコーダーとデコードしたフレームを扱う SurfaceTexture */
    private val videoDecoderFrameTexturePairFlow = combine(
        flow = akariGraphicsProcessorFlow,
        flow2 = _videoUriFlow,
        transform = ::Pair
    ).let {
        // 前回のを破棄できるように
        var prevVideoDecoder: AkariVideoDecoder? = null
        var prevAkariGraphicsSurfaceTexture: AkariGraphicsSurfaceTexture? = null
        it.transformLatest { (processor, uri) ->
            if (processor != null && uri != null) {
                prevVideoDecoder = null
                prevAkariGraphicsSurfaceTexture = null
                // 作り直す
                val newTexture = processor.genTextureId { texId ->
                    AkariGraphicsSurfaceTexture(texId)
                }
                val newVideoDecoder = AkariVideoDecoder().apply {
                    prepare(uri.toAkariCoreInputOutputData(context), newTexture.surface)
                }
                // 破棄用
                prevVideoDecoder = newVideoDecoder
                prevAkariGraphicsSurfaceTexture = newTexture
                emit(newVideoDecoder to newTexture)
            } else {
                // TODO Processor が破棄されても、コンテキストを切り替えればワンちゃん
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    fun updateSurface(surfaceHolder: SurfaceHolder?) {
        _surfaceHolderFlow.value = surfaceHolder
    }

    fun selectVideoUri(uri: Uri) {
        // AutoClosable は Android 10 以上
        val mediaMetadataRetriever = MediaMetadataRetriever().apply {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                setDataSource(it.fileDescriptor)
            }
        }

        // 回転を考慮
        val videoDuration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: return
        val videoWidth = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1280
        val videoHeight = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 720
        val rotation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val (width, height) = when (rotation) {
            // 縦だけ入れ替わるので
            90, 270 -> Pair(videoHeight, videoWidth)
            else -> Pair(videoWidth, videoHeight)
        }

        val colorType = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD)?.toInt() ?: MediaFormat.COLOR_STANDARD_BT709
        val transferType = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)?.toInt() ?: MediaFormat.COLOR_TRANSFER_SDR_VIDEO
        val colorSpaceType = when {
            colorType == MediaFormat.COLOR_STANDARD_BT2020 && transferType == MediaFormat.COLOR_TRANSFER_HLG -> AkariGraphicsProcessorColorSpaceType.TEN_BIT_HDR_BT2020_HLG
            colorType == MediaFormat.COLOR_STANDARD_BT2020 && transferType == MediaFormat.COLOR_TRANSFER_ST2084 -> AkariGraphicsProcessorColorSpaceType.TEN_BIT_HDR_BT2020_PQ
            else -> AkariGraphicsProcessorColorSpaceType.SDR_BT709
        }

        // SDR の場合はエラーを出して続行しない
        if (colorSpaceType == AkariGraphicsProcessorColorSpaceType.SDR_BT709) {
            showSnackbar(SnackbarState.RequiredHdrVideo)
            return
        }

        mediaMetadataRetriever.release()
        _videoUriFlow.value = uri
        _videoDurationFlowFlow.value = videoDuration
        _currentPositionFlow.value = 0
        _videoSizeFlow.value = VideoSize(width, height)
        _colorSpaceFlow.value = colorSpaceType

        // 描画
        seekMs(positionMs = 0L)
    }

    fun seekMs(positionMs: Long) {
        viewModelScope.launch {
            _currentPositionFlow.value = positionMs
            draw()
        }
    }

    fun save() {
        viewModelScope.launch {
            draw(isSave = true)
        }
    }

    fun dismissSnackbar() {
        _snackbarStateFlow.value = null
    }

    private fun showSnackbar(state: SnackbarState) {
        _snackbarStateFlow.value = state
    }

    private suspend fun draw(isSave: Boolean = false) {
        val akariGraphicsProcessor = akariGraphicsProcessorFlow.filterNotNull().first()
        val (videoDecoder, akariSurfaceTexture) = videoDecoderFrameTexturePairFlow.filterNotNull().first()
        val videoSize = videoSizeFlow.filterNotNull().first()
        val colorSpaceType = _colorSpaceFlow.filterNotNull().first()

        // シーク
        videoDecoder.seekTo(seekToMs = _currentPositionFlow.value)

        // 描画
        val glReadPixelsResult = akariGraphicsProcessor.drawOneshotAndGlReadPixels {
            drawSurfaceTexture(
                akariSurfaceTexture = akariSurfaceTexture,
                nullOrTextureUpdateTimeoutMs = 500,
                onTransform = { mvpMatrix ->
                    if (isSave) {
                        Matrix.scaleM(mvpMatrix, 0, 1f, -1f, 1f)
                    }
                }
            )
        }

        // 保存する場合
        if (isSave) {

            // 一時的にファイルに保存
            val rgba1010102File = context.getExternalFilesDir(null)!!
                .resolve("rgba1010102")
                .apply { writeBytes(glReadPixelsResult) }

            // 完成品パスも
            val resultFile = context.getExternalFilesDir(null)!!
                .resolve("ultrahdr_${System.currentTimeMillis()}.jpg")

            // C++ で書いた libultrahdr を呼び出す
            // resultPath に UltraHDR 画像ができる
            LibUltraHdrBridge.encodeFromRgba1010102(
                width = videoSize.width,
                height = videoSize.height,
                rgba1010102FilePath = rgba1010102File.path,
                ultraHdrResultFilePath = resultFile.path,
                colorSpaceType = when (colorSpaceType) {
                    AkariGraphicsProcessorColorSpaceType.SDR_BT709 -> return // ここには来ない
                    AkariGraphicsProcessorColorSpaceType.TEN_BIT_HDR_BT2020_HLG -> LibUltraHdrBridge.HdrColorSpaceType.HLG
                    AkariGraphicsProcessorColorSpaceType.TEN_BIT_HDR_BT2020_PQ -> LibUltraHdrBridge.HdrColorSpaceType.PQ
                }
            )

            // UltraHDR として Google フォトで認識されない問題を修正
            // 一回 Bitmap にして保存すると治る
            val libUltraHdrBitmap = BitmapFactory.decodeFile(resultFile.path)
            // Pictures/HDRVideoToUltraHdr フォルダに保存
            val fileContentValues = contentValuesOf(
                MediaStore.Images.ImageColumns.DISPLAY_NAME to resultFile.name,
                MediaStore.Images.ImageColumns.RELATIVE_PATH to "${Environment.DIRECTORY_PICTURES}/andAicaroid"
            )
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fileContentValues)!!
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                libUltraHdrBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            // 消す
            rgba1010102File.delete()
            resultFile.delete()

            // 通知
            showSnackbar(SnackbarState.Save(uri))
        }
    }

    data class VideoSize(val width: Int, val height: Int)

    sealed interface SnackbarState {
        data object RequiredHdrVideo : SnackbarState
        data class Save(val photoUri: Uri) : SnackbarState
    }
}