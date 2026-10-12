import SwiftUI
import UIKit
import Shared

/// プレイヤー表示モード（同梱/Storage 共通）
enum SpriteMode: Equatable {
    case idle
    case prep
    case attack
    case rest
    case walking(phaseTick: Int64)

    /// CharacterFrames の mode キー
    var key: String {
        switch self {
        case .idle: return "idle"
        case .prep: return "prep"
        case .attack: return "attack"
        case .rest: return "rest"
        case .walking: return "walk"
        }
    }

    var interval: TimeInterval {
        switch self {
        case .idle: return 0.6
        case .prep: return 0.06
        case .attack: return 0.06
        case .rest: return 0
        case .walking: return 0.32
        }
    }

    var isOneShot: Bool {
        switch self {
        case .prep, .attack: return true
        default: return false
        }
    }
}

/// 再生する1コマの解決結果（同梱asset / ローカルファイル / リモートURL）
enum CharacterFrame: Hashable {
    case assetName(String)
    case localFile(String)
    case remoteURL(URL)
}

/// キャラのフレーム解決。キャラ番号ではなく「ソースの種類」で責務を分ける。
protocol CharacterFrameSource {
    func frames(for mode: SpriteMode) -> [CharacterFrame]
    var interpolation: Image.Interpolation { get }
}

extension CharacterFrameSource {
    var interpolation: Image.Interpolation { .none }
}

/// 同梱スプライト（sprite_player_*）
struct BundledCharacterFrameSource: CharacterFrameSource {
    func frames(for mode: SpriteMode) -> [CharacterFrame] {
        let prefix: String
        switch mode {
        case .idle: prefix = "sprite_player_idle"
        case .prep: prefix = "sprite_player_prep"
        case .attack: prefix = "sprite_player_attack"
        case .rest: prefix = "sprite_player_rest"
        case .walking: prefix = "sprite_player_walk"
        }
        return (1...8).compactMap { i in
            let name = "\(prefix)_\(i)"
            return UIImage(named: name) != nil ? .assetName(name) : nil
        }
    }
}

/// Storage 由来のキャラ（image_url からフレームURLを導出。保存済みならローカル、無ければリモート）
struct StorageCharacterFrameSource: CharacterFrameSource {
    let imageUrl: String

    var interpolation: Image.Interpolation { .medium }

    func frames(for mode: SpriteMode) -> [CharacterFrame] {
        let urls = CharacterFrames.shared.frameUrls(imageUrl: imageUrl, mode: mode.key)
        return urls.compactMap { url in
            let key = CharacterFrames.shared.storageKey(frameUrl: url)
            if AssetStore.shared.exists(key: key) {
                return .localFile(AssetStore.shared.localPath(key: key))
            }
            if let u = URL(string: url) { return .remoteURL(u) }
            return nil
        }
    }
}

/// フレーム配列を再生する共通View。`.task(id:)` ベースでライフサイクルに紐づく（Timerは使わない）。
struct AnimatedFramesView: View {
    let frames: [CharacterFrame]
    let interval: TimeInterval
    let size: CGFloat
    var oneShot: Bool = false
    var slide: Bool = false
    var interpolation: Image.Interpolation = .none

    @State private var images: [UIImage] = []
    @State private var currentFrame = 0
    @State private var attackOffset: CGFloat = 0

    var body: some View {
        Group {
            if images.isEmpty {
                Color.clear.frame(width: size, height: size)
            } else {
                Image(uiImage: images[min(currentFrame, images.count - 1)])
                    .resizable()
                    .interpolation(interpolation)
                    .scaledToFit()
                    .frame(width: size, height: size)
                    .offset(x: attackOffset)
            }
        }
        .task(id: frames) {
            let loaded = await Self.loadImages(frames)
            guard !Task.isCancelled else { return }
            images = loaded
            currentFrame = 0
            attackOffset = 0
            guard images.count > 1, interval > 0 else { return }
            if oneShot {
                // 同梱版と同じ: 1回だけ再生し、攻撃は斬撃の瞬間に一瞬スライド
                for i in 1..<images.count {
                    try? await Task.sleep(nanoseconds: UInt64(interval * 1_000_000_000))
                    if Task.isCancelled { return }
                    currentFrame = i
                    if slide && i == 2 { withAnimation(.easeOut(duration: 0.05)) { attackOffset = 20 } }
                }
                if slide { withAnimation(.easeOut(duration: 0.12)) { attackOffset = 0 } }
            } else {
                while !Task.isCancelled {
                    try? await Task.sleep(nanoseconds: UInt64(interval * 1_000_000_000))
                    currentFrame = (currentFrame + 1) % images.count
                }
            }
        }
    }

    private static func loadImages(_ frames: [CharacterFrame]) async -> [UIImage] {
        var result: [UIImage] = []
        for frame in frames {
            if let img = await loadImage(frame) { result.append(img) }
        }
        return result
    }

    private static func loadImage(_ frame: CharacterFrame) async -> UIImage? {
        switch frame {
        case .assetName(let name):
            return SpriteImage.uiImage(name)
        case .localFile(let path):
            return UIImage(contentsOfFile: path)
        case .remoteURL(let url):
            guard let (data, _) = try? await URLSession.shared.data(from: url) else { return nil }
            return UIImage(data: data)
        }
    }
}

/// キャラのスプライト。frame source 経由なので 00001 / 00002 / 00352… を区別しない。
struct CharacterSpriteView: View {
    let source: CharacterFrameSource
    let mode: SpriteMode
    let size: CGFloat

    var body: some View {
        let frames = source.frames(for: mode)
        if frames.isEmpty {
            Text("🧙‍♂️")
                .font(.system(size: size * 0.42))
                .frame(width: size, height: size)
        } else {
            AnimatedFramesView(
                frames: frames,
                interval: mode.interval,
                size: size,
                oneShot: mode.isOneShot,
                slide: mode == .attack,
                interpolation: source.interpolation
            )
        }
    }
}
