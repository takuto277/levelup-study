package org.example.project.features.character

/**
 * キャラクターのフレーム構成と URL 導出。
 *
 * 命名規約: `character_{code}_{mode}_{frame}.webp`
 *   - マスタ `image_url` は代表1枚（`.../character_00002_idle_1.webp`）を指す。
 *   - ここから「フレームのベース（`.../character_00002`）」を導出し、各 mode の URL を組み立てる。
 */
object CharacterFrames {

    /** mode -> フレーム数 */
    val modeFrameCount: Map<String, Int> = linkedMapOf(
        "idle" to 2,
        "prep" to 2,
        "attack" to 5,
        "rest" to 1,
        "walk" to 2,
    )

    /**
     * `image_url`（例 `.../character_00002_idle_1.webp`）からフレームベース `.../character_00002` を返す。
     * 命名規約に合わない場合は null。
     */
    fun baseFromImageUrl(imageUrl: String): String? {
        val trimmed = imageUrl.trim()
        if (trimmed.isEmpty()) return null
        val noExt = trimmed.substringBeforeLast('.', missingDelimiterValue = trimmed)
        val lastUnderscore = noExt.lastIndexOf('_')
        if (lastUnderscore <= 0) return null
        val prev = noExt.lastIndexOf('_', startIndex = lastUnderscore - 1)
        return if (prev > 0) noExt.substring(0, prev) else null
    }

    /** 指定 mode のフレーム URL 一覧。導出できない場合は空。 */
    fun frameUrls(imageUrl: String, mode: String): List<String> {
        val base = baseFromImageUrl(imageUrl) ?: return emptyList()
        val count = modeFrameCount[mode] ?: 1
        return (1..count).map { "${base}_${mode}_$it.webp" }
    }

    /** mode -> フレーム URL 一覧（全 mode）。 */
    fun byMode(imageUrl: String): Map<String, List<String>> =
        modeFrameCount.keys.associateWith { frameUrls(imageUrl, it) }

    /** 全フレーム URL（12枚）。 */
    fun all(imageUrl: String): List<String> = byMode(imageUrl).values.flatten()

    /**
     * Storage のオブジェクトキー（例 `master/characters/character_00002_idle_1.webp`）に変換する。
     * 公開URL `.../object/public/{bucket}/{key}` から `{key}` を取り出す。
     */
    fun storageKey(frameUrl: String): String {
        val marker = "/object/public/"
        val idx = frameUrl.indexOf(marker)
        if (idx < 0) return frameUrl.substringAfterLast('/')
        val rest = frameUrl.substring(idx + marker.length)
        return rest.substringAfter('/', missingDelimiterValue = rest)
    }
}
