package io.github.takusan23.andaicaroid.ui.screen

import android.content.Intent
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.andaicaroid.R
import io.github.takusan23.andaicaroid.ui.components.MessageCard
import io.github.takusan23.andaicaroid.viewmodel.HdrVideoToUltraHdrViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HdrVideoToUltraHdrScreen(viewModel: HdrVideoToUltraHdrViewModel = viewModel()) {
    val context = LocalContext.current
    val selectUriOrNull = viewModel.videoUriFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = Unit) {
        viewModel.snackbarStateFlow.collectLatest { state ->
            state ?: return@collectLatest
            when (state) {
                HdrVideoToUltraHdrViewModel.SnackbarState.RequiredHdrVideo -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.hdr_video_to_ultra_hdr_screen_required_hdr_video_message)
                    )
                }

                is HdrVideoToUltraHdrViewModel.SnackbarState.Save -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.hdr_video_to_ultra_hdr_screen_save_success_message),
                        actionLabel = context.getString(R.string.hdr_video_to_ultra_hdr_screen_open_button_text)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, state.photoUri))
                    }
                }

                HdrVideoToUltraHdrViewModel.SnackbarState.UnSupportedDevice -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.hdr_video_to_ultra_hdr_screen_unsupported_device_message)
                    )
                }
            }
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = stringResource(id = R.string.hdr_video_to_ultra_hdr_screen_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (selectUriOrNull.value == null) {
            SelectView(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 10.dp),
                onSelect = { viewModel.selectVideoUri(it) }
            )
        } else {
            VideoPreviewView(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun VideoPreviewView(
    modifier: Modifier = Modifier,
    viewModel: HdrVideoToUltraHdrViewModel
) {
    val videoSize = viewModel.videoSizeFlow.collectAsStateWithLifecycle()
    val videoDurationMs = viewModel.videoDurationFlow.collectAsStateWithLifecycle()
    val currentPositionMs = viewModel.currentPositionMsFlow.collectAsStateWithLifecycle()

    Column(modifier = modifier) {

        val aspectRatio = videoSize.value?.let { it.width / it.height.toFloat() } ?: 1f
        AndroidView(
            modifier = Modifier
                .then(other = if (1f < aspectRatio) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(.7f))
                .aspectRatio(ratio = aspectRatio)
                .align(Alignment.CenterHorizontally),
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            // do nothing
                        }

                        override fun surfaceCreated(holder: SurfaceHolder) {
                            viewModel.updateSurface(holder)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            viewModel.updateSurface(null)
                        }
                    })
                }
            }
        )

        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = currentPositionMs.value.toFloat(),
                valueRange = 0f..videoDurationMs.value.toFloat(),
                onValueChange = { viewModel.seekMs(it.toLong()) }
            )

            Button(onClick = { viewModel.save() }) {
                Text(stringResource(id = R.string.hdr_video_to_ultra_hdr_screen_save_button_text))
            }

            MessageCard(
                modifier = Modifier.fillMaxWidth(),
                message = stringResource(id = R.string.hdr_video_to_ultra_hdr_screen_save_location_message)
            )
        }
    }
}

@Composable
private fun SelectView(
    modifier: Modifier = Modifier,
    onSelect: (Uri) -> Unit
) {
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { onSelect(it ?: return@rememberLauncherForActivityResult) }
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }) {
            Text(stringResource(id = R.string.hdr_video_to_ultra_hdr_screen_select_video_button_text))
        }
    }
}