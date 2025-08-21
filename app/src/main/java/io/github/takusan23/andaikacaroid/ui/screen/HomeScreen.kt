package io.github.takusan23.andaikacaroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.andaikacaroid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit,
    onNavigate: (NavigationPaths) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.home_screen_title)) },
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.settings_24px),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ご案内
            HelloCard()

            NavigateCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(id = R.string.home_screen_video_to_photo_title),
                description = stringResource(id = R.string.home_screen_video_to_photo_description),
                buttonText = stringResource(id = R.string.home_screen_video_to_photo_button),
                onClick = { onNavigate(NavigationPaths.HdrVideoToUltraHdr) }
            )
            NavigateCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(id = R.string.home_screen_photo_to_video_title),
                description = stringResource(id = R.string.home_screen_photo_to_video_description),
                buttonText = stringResource(id = R.string.home_screen_photo_to_video_button),
                onClick = { onNavigate(NavigationPaths.UltraHdrToHdrVideo) }
            )
        }
    }
}

@Composable
private fun NavigateCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                modifier = Modifier.align(alignment = Alignment.Start),
                text = title,
                fontSize = 20.sp
            )
            Text(text = description)

            HorizontalDivider()
            TextButton(
                modifier = Modifier.align(alignment = Alignment.End),
                onClick = onClick
            ) {
                Text(text = buttonText)
            }
        }
    }
}

@Composable
private fun HelloCard(modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                painter = painterResource(id = R.drawable.arrow_back_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider()
            Text(
                text = stringResource(id = R.string.home_screen_hello_card_title),
                fontSize = 20.sp
            )
            Text(text = stringResource(id = R.string.home_screen_hello_card_description))
        }
    }
}