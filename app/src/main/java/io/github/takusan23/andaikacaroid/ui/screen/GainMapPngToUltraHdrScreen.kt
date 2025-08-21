package io.github.takusan23.andaikacaroid.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.andaikacaroid.R
import io.github.takusan23.andaikacaroid.ui.components.MessageCard
import io.github.takusan23.andaikacaroid.viewmodel.GainMapPngToUltraHdrViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GainMapPngToUltraHdrScreen(viewModel: GainMapPngToUltraHdrViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> viewModel.process(uri ?: return@rememberLauncherForActivityResult) }
    )

    LaunchedEffect(key1 = Unit) {
        viewModel.snackbarStateFlow.collectLatest { state ->
            state ?: return@collectLatest
            when (state) {
                GainMapPngToUltraHdrViewModel.SnackbarState.RequiredGainMapPng -> {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.gainmap_png_to_ultra_hdr_screen_required_gainmap_png_message)
                    )
                }

                is GainMapPngToUltraHdrViewModel.SnackbarState.Save -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.gainmap_png_to_ultra_hdr_screen_save_success_message),
                        actionLabel = context.getString(R.string.gainmap_png_to_ultra_hdr_screen_open_button_text)
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, state.videoUri))
                    }
                }
            }

            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = stringResource(id = R.string.gainmap_png_to_ultra_hdr_screen_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {

            MessageCard(
                modifier = Modifier.fillMaxWidth(),
                message = stringResource(id = R.string.gainmap_png_to_ultra_hdr_screen_save_location_message)
            )

            Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(stringResource(id = R.string.gainmap_png_to_ultra_hdr_screen_select_file_button_text))
            }
        }
    }
}
