package org.example.project.core.storage

/** Android: アプリ専用領域（files/asset_cache）。OS に消されない永続領域。 */
actual fun assetRootDir(): String = requireAndroidAppContext().filesDir.absolutePath + "/asset_cache"
