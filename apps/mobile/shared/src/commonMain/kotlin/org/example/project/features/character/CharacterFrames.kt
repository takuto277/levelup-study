package org.example.project.features.character

/**
 * キャラクターのフレーム構成と URL 導出。
 *
 * 命名規約: `.../characters/{code}/{mode}_{frame}.webp`
 *   - マスタ `image_url` は代表1枚（`.../characters/00002/idle_1.webp`）を指す。
 *   - ディレクトリ（`.../characters/00002`）をベースに、各 mode の URL を組み立てる。
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
     * `image_url`（例 `.../characters/00002/idle_1.webp`）からフレームのベース
     * （`.../characters/00002`）を返す。ファイル名を除いたディレクトリ。
     */
    fun baseFromImageUrl(imageUrl: String): String? {
        val trimmed = imageUrl.trim().substringBefore('?')
        if (trimmed.isEmpty()) return null
        val base = trimmed.substringBeforeLast('/', missingDelimiterValue = "")
        return base.ifEmpty { null }
    }

    /** 指定 mode のフレーム URL 一覧。導出できない場合は空。 */
    fun frameUrls(imageUrl: String, mode: String): List<String> {
        val base = baseFromImageUrl(imageUrl) ?: return emptyList()
        val count = modeFrameCount[mode] ?: 1
        return (1..count).map { "${base}/${mode}_$it.webp" }
    }

    /** mode -> フレーム URL 一覧（全 mode）。 */
    fun byMode(imageUrl: String): Map<String, List<String>> =
        modeFrameCount.keys.associateWith { frameUrls(imageUrl, it) }

    /** 全フレーム URL（12枚）。 */
    fun all(imageUrl: String): List<String> = byMode(imageUrl).values.flatten()

    /**
     * Storage のオブジェクトキー（例 `master/characters/00002/idle_1.webp`）に変換する。
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
