package com.lazygeniouz.acv.example

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lazygeniouz.acv.AdContainerView
import com.lazygeniouz.acv.compose.AdContainer
import com.lazygeniouz.acv.compose.AdaptiveAdContainer

@Composable
internal fun BannerSample(initializationResult: Result<Unit>?) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        val availableSizes = remember(maxWidth) {
            BannerSize.entries.filter { it.minimumWidthDp <= maxWidth.value }
        }
        var selectedSize by rememberSaveable { mutableStateOf(BannerSize.LARGE_ADAPTIVE) }
        var reloadKey by rememberSaveable { mutableIntStateOf(0) }
        var loadState by remember { mutableStateOf(LoadState.LOADING) }
        var loadError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(availableSizes) {
            if (selectedSize !in availableSizes) {
                loadState = LoadState.LOADING
                selectedSize = availableSizes.first()
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.sample_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.sample_description),
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                val controlsEnabled = initializationResult?.isSuccess == true &&
                    loadState != LoadState.LOADING
                BannerSizeSelector(
                    sizes = availableSizes,
                    selectedSize = selectedSize,
                    enabled = controlsEnabled,
                    onSizeSelected = {
                        loadState = LoadState.LOADING
                        loadError = null
                        selectedSize = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
                StatusRow(
                    initializationResult = initializationResult,
                    loadState = loadState,
                    loadError = loadError,
                    selectedSize = selectedSize,
                    reloadEnabled = controlsEnabled,
                    onReload = {
                        loadState = LoadState.LOADING
                        loadError = null
                        reloadKey++
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            if (initializationResult?.isSuccess == true) {
                key(selectedSize, reloadKey) {
                    Banner(
                        size = selectedSize,
                        onLoaded = {
                            loadState = LoadState.LOADED
                            loadError = null
                        },
                        onFailed = { error ->
                            Log.w(TAG, "Banner failed to load: ${error.message}")
                            loadState = LoadState.FAILED
                            loadError = error.message.takeIf(String::isNotBlank)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Banner(
    size: BannerSize,
    onLoaded: (BannerAd) -> Unit,
    onFailed: (LoadAdError) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val fixedAdSize = size.adSize
        if (fixedAdSize == null) {
            AdaptiveAdContainer(
                adUnitId = AdContainerView.ADAPTIVE_SIZE_TEST_AD_ID,
                modifier = Modifier.fillMaxWidth(),
                onAdLoaded = onLoaded,
                onAdFailedToLoad = onFailed
            )
        } else {
            AdContainer(
                adUnitId = AdContainerView.FIXED_SIZE_TEST_AD_ID,
                adSize = fixedAdSize,
                modifier = Modifier.wrapContentSize(),
                onAdLoaded = onLoaded,
                onAdFailedToLoad = onFailed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BannerSizeSelector(
    sizes: List<BannerSize>,
    selectedSize: BannerSize,
    enabled: Boolean,
    onSizeSelected: (BannerSize) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = stringResource(selectedSize.label),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = enabled
                )
                .fillMaxWidth(),
            enabled = enabled,
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.banner_size_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sizes.forEach { size ->
                DropdownMenuItem(
                    text = { Text(stringResource(size.label)) },
                    onClick = {
                        expanded = false
                        onSizeSelected(size)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    initializationResult: Result<Unit>?,
    loadState: LoadState,
    loadError: String?,
    selectedSize: BannerSize,
    reloadEnabled: Boolean,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = when {
        initializationResult == null -> LoadState.LOADING
        initializationResult.isFailure -> LoadState.FAILED
        else -> loadState
    }
    val title = when {
        initializationResult == null -> R.string.ad_status_initializing_title
        initializationResult.isFailure -> R.string.ad_status_initialization_failed_title
        status == LoadState.LOADING -> R.string.ad_status_loading_title
        status == LoadState.LOADED -> R.string.ad_status_loaded_title
        else -> R.string.ad_status_load_failed_title
    }
    val sizeLabel = stringResource(selectedSize.label)
    val detail = when {
        initializationResult == null -> stringResource(R.string.ad_status_initializing_detail)
        initializationResult.isFailure -> initializationResult.exceptionOrNull()
            .errorDetail()
            ?: stringResource(R.string.ad_status_unknown_error)
        status == LoadState.LOADING -> stringResource(
            R.string.ad_status_loading_detail,
            sizeLabel
        )
        status == LoadState.LOADED -> stringResource(
            R.string.ad_status_loaded_detail,
            sizeLabel
        )
        else -> loadError ?: stringResource(R.string.ad_status_unknown_error)
    }

    Row(
        modifier = modifier
            .height(64.dp)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                LoadState.LOADING -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp
                )
                LoadState.LOADED -> Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                LoadState.FAILED -> Icon(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(onClick = onReload, enabled = reloadEnabled) {
            Text(stringResource(R.string.reload_banner))
        }
    }
}

private enum class LoadState {
    LOADING,
    LOADED,
    FAILED
}

private enum class BannerSize(
    @param:StringRes val label: Int,
    val minimumWidthDp: Int,
    val adSize: AdSize?
) {
    LARGE_ADAPTIVE(R.string.banner_size_large_adaptive, 0, null),
    BANNER(R.string.banner_size_banner, 320, AdSize.BANNER),
    LARGE_BANNER(R.string.banner_size_large_banner, 320, AdSize.LARGE_BANNER),
    MEDIUM_RECTANGLE(R.string.banner_size_medium_rectangle, 300, AdSize.MEDIUM_RECTANGLE),
    FULL_BANNER(R.string.banner_size_full_banner, 468, AdSize.FULL_BANNER),
    LEADERBOARD(R.string.banner_size_leaderboard, 728, AdSize.LEADERBOARD)
}

private fun Throwable?.errorDetail(): String? = this
    ?.let { it.cause ?: it }
    ?.message
    ?.takeIf(String::isNotBlank)

private const val TAG = "AdContainerSample"