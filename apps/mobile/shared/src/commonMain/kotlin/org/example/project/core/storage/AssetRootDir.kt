package org.example.project.core.storage

/** アプリ専用の永続ストレージ（ダウンロードしたキャラ画像など）のルート。OS ごとに実装する。 */
expect fun assetRootDir(): String
