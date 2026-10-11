package org.example.project.features.character

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.example.project.core.storage.AssetStore

/**
 * キャラクターのフレーム一式を Storage から取得し、アプリ領域へ保存する。
 * 入手時に呼ぶ想定（オフライン表示用）。
 */
class CharacterFrameDownloader(private val client: HttpClient) {

    /** image_url から導出できる全フレームを取得・保存する。未取得のフレームのみDL。失敗は無視。 */
    suspend fun downloadAll(imageUrl: String) {
        val urls = CharacterFrames.all(imageUrl)
        for (url in urls) {
            val key = CharacterFrames.storageKey(url)
            if (AssetStore.exists(key)) continue
            runCatching {
                val bytes: ByteArray = client.get(url).body()
                AssetStore.save(key, bytes)
            }
        }
    }
}
