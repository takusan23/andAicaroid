package io.github.takusan23.andaicaroid.ui.screen.setting

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.github.takusan23.andaicaroid.R
import io.github.takusan23.andaicaroid.ui.screen.NavigationPaths

private const val GitHubUrl = "https://github.com/takusan23/Ultraika"

/**
 * 設定画面
 *
 * @param onBack 戻るを押した時
 * @param onNavigate 画面遷移の際に呼ばれます
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onNavigate: (NavigationPaths) -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.setting_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back_24px),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {

            item {
                SettingItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(id = R.string.setting_screen_license_title),
                    description = stringResource(id = R.string.setting_screen_license_description),
                    onClick = { onNavigate(NavigationPaths.License) }
                )
            }

            item {
                SettingItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(id = R.string.setting_screen_source_code_title),
                    description = stringResource(id = R.string.setting_screen_source_code_description),
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, GitHubUrl.toUri())) }
                )
            }
        }
    }
}

/**
 * 設定項目
 *
 * @param modifier [Modifier]
 * @param title タイトル
 * @param description 説明
 * @param onClick 押した時
 */
@Composable
private fun SettingItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 20.sp)
            Text(text = description)
        }
    }
}