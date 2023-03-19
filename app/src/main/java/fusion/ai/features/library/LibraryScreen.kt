@file:OptIn(ExperimentalMaterial3Api::class)

package fusion.ai.features.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import fusion.ai.billing.Plan
import fusion.ai.features.library.components.MaterialBadge
import fusion.ai.features.library.components.MaterialBadgeOutline
import fusion.ai.ui.theme.InterFontFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryVM = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    navigateToChatScreen: (presetId: Int, toolId: Int, extrasId: Int?) -> Unit,
    navigateToPricingScreen: () -> Unit
) {
    val state = viewModel.state.collectAsState().value
    val context = LocalContext.current
    val (showExtrasMenu, updateShowExtraMenu) = rememberSaveable {
        mutableStateOf(false to -1)
    }
    val (selectedExtraId, updateSelectedExtraId) = rememberSaveable {
        mutableStateOf<Int?>(null)
    }

    LaunchedEffect(key1 = state.errorEvent) {
        if (state.errorEvent != null) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(state.errorEvent.message),
                withDismissAction = true,
                actionLabel = "Retry"
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.refresh()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(Modifier.then(modifier)),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (state.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(25.dp)
                            .align(Alignment.Center),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        itemsIndexed(state.presets) { index, preset ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = preset.title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(
                            vertical = 10.dp,
                            horizontal = 15.dp
                        ),
                    fontSize = 18.sp
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(preset.tools) { index, it ->
                        ElevatedCard(
                            modifier = Modifier
                                .width(250.dp)
                                .height(100.dp)
                                .padding(
                                    start = when (index) {
                                        0 -> 10.dp
                                        else -> 0.dp
                                    },
                                    end = when (index) {
                                        preset.tools.size - 1 -> 10.dp
                                        else -> 0.dp
                                    }
                                ),
                            onClick = {
                                when {
                                    it.comingSoon -> Unit
                                    it.locked -> {
                                        if (state.userPlan == Plan.Trial) {
                                            navigateToPricingScreen()
                                        } else {
                                            if (it.extras.isNotEmpty()) {
                                                // show extras menu
                                                updateShowExtraMenu(true to it.id)
                                            } else {
                                                navigateToChatScreen(preset.id, it.id, null)
                                            }
                                        }
                                    }
                                    it.extras.isNotEmpty() -> {
                                        // show extras menu
                                        updateShowExtraMenu(true to it.id)
                                    }
                                    else -> {
                                        navigateToChatScreen(preset.id, it.id, null)
                                    }
                                }
                            }
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = it.title,
                                        fontFamily = InterFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    when {
                                        it.comingSoon -> {
                                            MaterialBadgeOutline(
                                                modifier = Modifier
                                                    .padding(
                                                        horizontal = 8.dp,
                                                        vertical = 2.dp
                                                    ),
                                                text = "Coming Soon",
                                                textSize = 10
                                            )
                                        }

                                        it.locked -> {
                                            if (state.userPlan == Plan.Trial) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Lock,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }

                                        it.extras.isNotEmpty() -> {
                                            if (it.extrasHeader != null) {
                                                MaterialBadge(
                                                    modifier = Modifier
                                                        .padding(
                                                            horizontal = 8.dp,
                                                            vertical = 2.dp
                                                        ),
                                                    text = it.extrasHeader,
                                                    textSize = 10,
                                                    onClick = {
                                                        updateShowExtraMenu(true to it.id)
                                                    }
                                                )
                                            }
                                        }

                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = Icons.Default.PlayArrow.name,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                                if (it.id == showExtrasMenu.second) {
                                    AlertDialog(
                                        onDismissRequest = {
                                            updateShowExtraMenu(false to -1)
                                            updateSelectedExtraId(null)
                                        },
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        LazyColumn {
                                            it.extrasHeader?.let {
                                                stickyHeader {
                                                    Text(
                                                        text = it,
                                                        fontFamily = InterFontFamily,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .padding(12.dp),
                                                        letterSpacing = .5.sp
                                                    )
                                                }
                                            }
                                            items(it.extras) { extra ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            updateSelectedExtraId(extra.id)
                                                        }
                                                        .padding(start = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = extra.title,
                                                        fontFamily = InterFontFamily,
                                                        fontWeight = FontWeight.Normal,
                                                        fontSize = 14.sp
                                                    )

                                                    RadioButton(
                                                        selected = selectedExtraId == extra.id,
                                                        onClick = {
                                                            updateSelectedExtraId(
                                                                extra.id
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(2.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            updateShowExtraMenu(false to -1)
                                                            navigateToChatScreen(
                                                                preset.id,
                                                                it.id,
                                                                selectedExtraId
                                                            )
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .align(
                                                                Alignment.Center
                                                            )
                                                            .padding(
                                                                horizontal = 10.dp,
                                                                vertical = 5.dp
                                                            ),
                                                        enabled = selectedExtraId != null,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(text = "Proceed")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = it.description,
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(.7f),
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}
