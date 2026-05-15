package dev.beeman.glint

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dev.beeman.glint.ui.theme.GlintTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private var error by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(false)
    private var settingWallpaperSlug by mutableStateOf<String?>(null)
    private var thumbnailReloadKey by mutableStateOf(0)
    private var wallpapers by mutableStateOf<List<WallpaperItem>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlintTheme {
                LaunchedEffect(Unit) {
                    loadWallpapers()
                }

                GlintScreen(
                    error = error,
                    isLoading = isLoading,
                    onRefresh = ::loadWallpapers,
                    onSelect = ::setWallpaper,
                    settingWallpaperSlug = settingWallpaperSlug,
                    thumbnailReloadKey = thumbnailReloadKey,
                    wallpapers = wallpapers,
                )
            }
        }
    }

    private fun loadWallpapers() {
        lifecycleScope.launch {
            error = null
            isLoading = true
            thumbnailReloadKey += 1

            try {
                wallpapers = withContext(Dispatchers.IO) {
                    parseWallpaperCatalog(fetchText(WALLPAPER_MANIFEST_URL))
                }
            } catch (throwable: Throwable) {
                error = throwable.message ?: "Could not load wallpapers"
            } finally {
                isLoading = false
            }
        }
    }

    private fun setWallpaper(wallpaper: WallpaperItem) {
        lifecycleScope.launch {
            error = null
            settingWallpaperSlug = wallpaper.slug

            try {
                val file = withContext(Dispatchers.IO) {
                    downloadWallpaper(wallpaper)
                }
                val uri = Uri.Builder()
                    .scheme("content")
                    .authority("$packageName.cache")
                    .appendPath(file.name)
                    .build()
                openWallpaperSetter(uri, file)
            } catch (throwable: Throwable) {
                error = getString(R.string.set_wallpaper_error)
            } finally {
                settingWallpaperSlug = null
            }
        }
    }

    private fun downloadWallpaper(wallpaper: WallpaperItem): File {
        val file = File(cacheDir, "${wallpaper.slug}.png")
        val connection = URL(wallpaper.imageUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        connection.inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private suspend fun openWallpaperSetter(uri: Uri, file: File) {
        val wallpaperManager = WallpaperManager.getInstance(this)
        val cropIntent = runCatching {
            wallpaperManager.getCropAndSetWallpaperIntent(uri).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.getOrNull()

        if (cropIntent != null && tryStartActivity(cropIntent)) {
            return
        }

        val cropActionIntent = Intent(WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setDataAndType(uri, "image/png")

        if (tryStartActivity(cropActionIntent)) {
            return
        }

        val attachIntent = Intent(Intent.ACTION_ATTACH_DATA)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra("mimeType", "image/png")
            .setDataAndType(uri, "image/png")

        if (tryStartActivity(attachIntent)) {
            return
        }

        withContext(Dispatchers.IO) {
            file.inputStream().use { input ->
                wallpaperManager.setStream(input)
            }
        }
        Toast.makeText(this, R.string.wallpaper_set, Toast.LENGTH_SHORT).show()
    }

    private fun tryStartActivity(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (exception: ActivityNotFoundException) {
            false
        }
    }
}

@Composable
fun GlintScreen(
    error: String?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSelect: (WallpaperItem) -> Unit,
    settingWallpaperSlug: String?,
    thumbnailReloadKey: Int,
    wallpapers: List<WallpaperItem>,
) {
    val isSettingWallpaper = settingWallpaperSlug != null
    val resources = LocalContext.current.resources

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    style = MaterialTheme.typography.headlineMedium,
                    text = stringResource(id = R.string.app_name),
                )
                Button(
                    enabled = !isLoading && !isSettingWallpaper,
                    onClick = onRefresh,
                ) {
                    Text(text = stringResource(id = R.string.refresh))
                }
            }

            when {
                isLoading -> Text(text = "Loading wallpapers...")
                error != null -> Text(text = error)
                else -> Text(
                    text = resources.getQuantityString(
                        R.plurals.wallpaper_count,
                        wallpapers.size,
                        wallpapers.size,
                    ),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(wallpapers) { wallpaper ->
                    WallpaperTile(
                        isEnabled = !isSettingWallpaper,
                        isSetting = settingWallpaperSlug == wallpaper.slug,
                        onSelect = onSelect,
                        thumbnailReloadKey = thumbnailReloadKey,
                        wallpaper = wallpaper,
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperTile(
    isEnabled: Boolean,
    isSetting: Boolean,
    onSelect: (WallpaperItem) -> Unit,
    thumbnailReloadKey: Int,
    wallpaper: WallpaperItem,
) {
    var thumbnailState by remember(wallpaper.previewUrl) {
        mutableStateOf<ThumbnailState>(ThumbnailState.Loading)
    }

    LaunchedEffect(wallpaper.previewUrl, thumbnailReloadKey) {
        thumbnailState = ThumbnailState.Loading
        thumbnailState = withContext(Dispatchers.IO) {
            fetchImageBitmap(wallpaper.previewUrl)
                ?.let { ThumbnailState.Loaded(it) }
                ?: ThumbnailState.Failed
        }
    }

    Card(
        modifier = Modifier.clickable(enabled = isEnabled) {
            onSelect(wallpaper)
        },
    ) {
        Column {
            when (val state = thumbnailState) {
                ThumbnailState.Failed -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            text = stringResource(id = R.string.preview_unavailable),
                        )
                    }
                }

                is ThumbnailState.Loaded -> {
                    Image(
                        bitmap = state.image,
                        contentDescription = wallpaper.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f),
                    )
                }

                ThumbnailState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }

            Text(
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleSmall,
                text = if (isSetting) stringResource(id = R.string.setting_wallpaper) else wallpaper.name,
            )
        }
    }
}

private sealed interface ThumbnailState {
    data object Loading : ThumbnailState

    data class Loaded(val image: ImageBitmap) : ThumbnailState

    data object Failed : ThumbnailState
}

private fun fetchImageBitmap(url: String): ImageBitmap? {
    return runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        connection.inputStream.use { input ->
            BitmapFactory.decodeStream(input)?.asImageBitmap()
        }
    }.getOrNull()
}

@Preview(showBackground = true)
@Composable
fun GlintScreenPreview() {
    GlintTheme {
        GlintScreen(
            error = null,
            isLoading = false,
            onRefresh = {},
            onSelect = {},
            settingWallpaperSlug = null,
            thumbnailReloadKey = 0,
            wallpapers = listOf(
                WallpaperItem(
                    imageUrl = "https://example.test/wallpapers/solana-gradient.png",
                    name = "Solana Gradient",
                    previewUrl = "https://example.test/previews/solana-gradient.png",
                    slug = "solana-gradient",
                ),
                WallpaperItem(
                    imageUrl = "https://example.test/wallpapers/solana-lights.png",
                    name = "Solana Lights",
                    previewUrl = "https://example.test/previews/solana-lights.png",
                    slug = "solana-lights",
                ),
            ),
        )
    }
}
