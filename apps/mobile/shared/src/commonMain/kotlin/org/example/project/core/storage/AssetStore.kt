package org.example.project.core.storage

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * ダウンロードした画像をアプリ専用領域へ読み書きする（okio でクロスプラットフォーム実装）。
 * キーは Storage のオブジェクトキー（例: `master/characters/character_00002_idle_1.webp`）。
 */
object AssetStore {
    private val fs: FileSystem = FileSystem.SYSTEM
    private val root: Path get() = assetRootDir().toPath()

    /** ローカルの絶対パス（存在有無は問わない）。 */
    fun localPath(key: String): String = (root / key).toString()

    fun exists(key: String): Boolean = runCatching { fs.exists(root / key) }.getOrDefault(false)

    fun save(key: String, bytes: ByteArray) {
        val path = root / key
        path.parent?.let { fs.createDirectories(it) }
        fs.write(path) { write(bytes) }
    }
}
