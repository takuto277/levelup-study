package org.example.project.core.storage

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/** iOS: Application Support 配下（バックアップ対象だが、消えても再DL可能）。 */
actual fun assetRootDir(): String {
    val dirs = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
    val base = dirs.firstOrNull() as? String ?: ""
    return "$base/asset_cache"
}
