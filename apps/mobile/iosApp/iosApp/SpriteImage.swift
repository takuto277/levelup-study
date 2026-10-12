import UIKit

/// 同梱スプライト（WebP の loose resource）を読み込む。
/// iOS の Asset Catalog（imageset）は WebP 非対応のため、`Sprites/` 配下の
/// `.webp` をバンドルから直接読む（`UIImage(data:)` は iOS 14+ で WebP をデコード可能）。
enum SpriteImage {
    static func uiImage(_ name: String) -> UIImage? {
        let bundle = Bundle.main
        let candidates: [URL?] = [
            bundle.url(forResource: name, withExtension: "webp"),
            bundle.url(forResource: name, withExtension: "webp", subdirectory: "Sprites"),
            bundle.url(forResource: name, withExtension: "webp", subdirectory: "Resources/Sprites"),
        ]
        for case let url? in candidates {
            if let data = try? Data(contentsOf: url), let img = UIImage(data: data) {
                return img
            }
        }
        // フォールバック（PNG 等が Asset Catalog にある場合）
        return UIImage(named: name)
    }

    static func exists(_ name: String) -> Bool { uiImage(name) != nil }
}
