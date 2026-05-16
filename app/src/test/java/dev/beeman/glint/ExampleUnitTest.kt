package dev.beeman.glint

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun parsesWallpaperManifest() {
        val json = """
            {
              "wallpapers": [
                {
                  "name": "Solana Gradient",
                  "imageUrl": "https://example.test/wallpapers/solana-gradient.png",
                  "previewUrl": "https://example.test/previews/solana-gradient.png",
                  "slug": "solana-gradient"
                }
              ]
            }
        """.trimIndent()

        val wallpapers = parseWallpaperCatalog(json)

        assertEquals(1, wallpapers.size)
        assertEquals("Solana Gradient", wallpapers.first().name)
        assertEquals(
            "https://example.test/wallpapers/solana-gradient.png",
            wallpapers.first().imageUrl
        )
        assertEquals(
            "https://example.test/previews/solana-gradient.png",
            wallpapers.first().previewUrl
        )
        assertEquals("solana-gradient", wallpapers.first().slug)
    }

    @Test
    fun parsesEscapedWallpaperName() {
        val json = """
            {
              "wallpapers": [
                {
                  "name": "Solana \"Gradient\"",
                  "imageUrl": "https://example.test/wallpapers/solana-gradient.png",
                  "previewUrl": "https://example.test/previews/solana-gradient.png",
                  "slug": "solana-gradient"
                }
              ]
            }
        """.trimIndent()

        val wallpapers = parseWallpaperCatalog(json)

        assertEquals("Solana \"Gradient\"", wallpapers.first().name)
    }
}
