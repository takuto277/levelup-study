import SwiftUI
import Shared

/// キャラのフレームを一度メモリに読み込んでから再生（毎フレーム再ロードによるちらつきを防ぐ）
struct PlayerFrameImage: View {
    let frames: [String]
    let interval: TimeInterval
    let size: CGFloat
    var oneShot: Bool = false
    var slide: Bool = false

    @State private var images: [UIImage] = []
    @State private var currentFrame = 0
    @State private var attackOffset: CGFloat = 0

    var body: some View {
        Group {
            if !images.isEmpty {
                Image(uiImage: images[min(currentFrame, images.count - 1)])
                    .resizable().scaledToFit().frame(width: size, height: size)
                    .offset(x: attackOffset)
            } else {
                Color.clear.frame(width: size, height: size)
            }
        }
        .onAppear { loadImages() }
        .onChange(of: frames) { _, _ in loadImages() }
    }

    private func loadImages() {
        let remoteFrames = frames
        var result = [UIImage?](repeating: nil, count: remoteFrames.count)
        let group = DispatchGroup()
        for (i, remote) in remoteFrames.enumerated() {
            let key = CharacterFrames.shared.storageKey(frameUrl: remote)
            if AssetStore.shared.exists(key: key) {
                result[i] = UIImage(contentsOfFile: AssetStore.shared.localPath(key: key))
                continue
            }
            guard let url = URL(string: remote) else { continue }
            group.enter()
            URLSession.shared.dataTask(with: url) { data, _, _ in
                if let d = data { result[i] = UIImage(data: d) }
                group.leave()
            }.resume()
        }
        group.notify(queue: .main) {
            self.images = result.compactMap { $0 }
            self.currentFrame = 0
            self.attackOffset = 0
            guard images.count > 1, interval > 0 else { return }
            let total = images.count
            if oneShot {
                // 同梱版と同じ: 1回だけ再生し、攻撃は斬撃の瞬間に一瞬スライド
                Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { t in
                    if currentFrame + 1 >= total {
                        t.invalidate()
                        if slide { withAnimation(.easeOut(duration: 0.12)) { attackOffset = 0 } }
                    } else {
                        currentFrame += 1
                        if slide && currentFrame == 2 { withAnimation(.easeOut(duration: 0.05)) { attackOffset = 20 } }
                    }
                }
            } else {
                Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { _ in
                    currentFrame = (currentFrame + 1) % total
                }
            }
        }
    }
}
