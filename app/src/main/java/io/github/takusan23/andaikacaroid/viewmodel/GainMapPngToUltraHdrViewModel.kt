package io.github.takusan23.andaikacaroid.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.contentValuesOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GainMapPngToUltraHdrViewModel(application: Application) : AndroidViewModel(application) {
    private val context
        get() = application.applicationContext

    private val _snackbarStateFlow = MutableStateFlow<SnackbarState?>(null)
    val snackbarStateFlow = _snackbarStateFlow.asStateFlow()

    fun process(uri: Uri) {
        viewModelScope.launch {
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }?.replace(".png", ".jpeg") ?: "PNG_TO_JPEG_${System.currentTimeMillis()}.jpeg"
            val bitmap = context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it) }
            // なければエラー
            if (!bitmap.hasGainmap()) {
                _snackbarStateFlow.value = SnackbarState.RequiredGainMapPng
                return@launch
            }

            // JPEG にするだけ
            val fileContentValues = contentValuesOf(
                MediaStore.Images.ImageColumns.DISPLAY_NAME to fileName,
                MediaStore.Images.ImageColumns.RELATIVE_PATH to "${Environment.DIRECTORY_PICTURES}/andAicaroid"
            )
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, fileContentValues)!!
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }

            _snackbarStateFlow.value = SnackbarState.Save(uri)
        }
    }

    fun dismissSnackbar() {
        _snackbarStateFlow.value = null
    }

    sealed interface SnackbarState {
        data object RequiredGainMapPng : SnackbarState
        data class Save(val videoUri: Uri) : SnackbarState
    }
}