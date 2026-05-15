package dev.beeman.glint

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WallpaperItem(
    val imageUrl: String,
    val name: String,
    val previewUrl: String,
    val slug: String,
)

const val WALLPAPER_MANIFEST_URL =
    "https://raw.githubusercontent.com/beeman/solana-mobile-wallpapers/main/wallpapers.json"

fun parseWallpaperCatalog(json: String): List<WallpaperItem> {
    val items = JSONObject(json).getJSONArray("wallpapers")
    return List(items.length()) { index ->
        val item = items.getJSONObject(index)
        WallpaperItem(
            imageUrl = item.getString("imageUrl"),
            name = item.getString("name"),
            previewUrl = item.getString("previewUrl"),
            slug = item.getString("slug"),
        )
    }
}

fun fetchText(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 10_000
    connection.readTimeout = 20_000
    return connection.inputStream.bufferedReader().use { it.readText() }
}
