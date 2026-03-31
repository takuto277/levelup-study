import SwiftUI

// ============================================================
// MARK: - Data Types
// ============================================================

private enum GachaPhase {
    case bannerSelect, confirm, pulling, result
}

private enum BannerType: String {
    case character, weapon, mixed

    var label: String {
        switch self {
        case .character: return "キャラ"
        case .weapon: return "武器"
        case .mixed: return "ミックス"
        }
    }
    var icon: String {
        switch self {
        case .character: return "person.fill"
        case .weapon: return "shield.fill"
        case .mixed: return "sparkles"
        }
    }
    var colors: [Color] {
        switch self {
        case .character: return [Color(red: 0.3, green: 0.2, blue: 0.8), Color(red: 0.5, green: 0.2, blue: 0.9)]
        case .weapon: return [Color(red: 0.8, green: 0.2, blue: 0.2), Color(red: 0.9, green: 0.4, blue: 0.1)]
        case .mixed: return [Color(red: 0.6, green: 0.2, blue: 0.8), Color(red: 0.9, green: 0.3, blue: 0.5)]
        }
    }
}

private struct BannerDisplay: Identifiable {
    let id: String
    let name: String
    let type: BannerType
    let pityThreshold: Int
    let featuredRarity: Int
    let description: String
}

private enum ItemType: String {
    case character, weapon
    var icon: String {
        switch self {
        case .character: return "person.fill"
        case .weapon: return "shield.fill"
        }
    }
}

private struct ResultItem: Identifiable {
    let id: String
    let name: String
    let rarity: Int
    let type: ItemType
    let isNew: Bool

    var rarityColor: Color {
        switch rarity {
        case 5: return Color(red: 1.0, green: 0.84, blue: 0.0)
        case 4: return Color(red: 0.6, green: 0.35, blue: 0.85)
        case 3: return Color(red: 0.2, green: 0.6, blue: 0.9)
        case 2: return Color(red: 0.2, green: 0.8, blue: 0.45)
        default: return Color(red: 0.58, green: 0.65, blue: 0.65)
        }
    }
    var rarityStars: String { String(repeating: "★", count: rarity) }
}

// ============================================================
// MARK: - Store (ViewModel)
// ============================================================

/// ガチャ画面のローカルステート管理
/// TODO: KMP GachaViewModel + Koin 接続後はこのクラスを置き換える
private class GachaStore: ObservableObject {
    @Published var phase: GachaPhase = .bannerSelect
    @Published var banners: [BannerDisplay] = []
    @Published var selectedBanner: BannerDisplay?
    @Published var currentStones: Int = 1250
    @Published var pityCount: Int = 47
    @Published var pullResults: [ResultItem] = []
    @Published var lastPullCount: Int = 0
    @Published var error: String?

    static let singleCost = 50
    static let multiCost = 450

    var canPullSingle: Bool { currentStones >= Self.singleCost }
    var canPullMulti: Bool { currentStones >= Self.multiCost }
    var highestRarity: Int { pullResults.map(\.rarity).max() ?? 3 }

    init() { loadBanners() }

    func loadBanners() {
        banners = [
            BannerDisplay(id: "b1", name: "光の勇者ピックアップ", type: .character, pityThreshold: 90, featuredRarity: 5,
                          description: "★5 光の勇者アリア 排出率UP!"),
            BannerDisplay(id: "b2", name: "伝説の聖剣ガチャ", type: .weapon, pityThreshold: 80, featuredRarity: 5,
                          description: "★5 聖剣エクスカリバー 排出率UP!"),
            BannerDisplay(id: "b3", name: "新学期スペシャル召喚", type: .mixed, pityThreshold: 0, featuredRarity: 4,
                          description: "★4以上キャラ＆武器の排出率2倍!")
        ]
    }

    func selectBanner(_ banner: BannerDisplay) {
        selectedBanner = banner
        pityCount = Int.random(in: 20...75)
        withAnimation(.spring(response: 0.45, dampingFraction: 0.85)) { phase = .confirm }
    }

    func pullSingle() {
        guard canPullSingle else { return }
        currentStones -= Self.singleCost
        lastPullCount = 1
        withAnimation(.easeInOut(duration: 0.3)) { phase = .pulling }
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.2) {
            self.pullResults = self.generateResults(count: 1)
            withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) { self.phase = .result }
        }
    }

    func pullMulti() {
        guard canPullMulti else { return }
        currentStones -= Self.multiCost
        lastPullCount = 10
        withAnimation(.easeInOut(duration: 0.3)) { phase = .pulling }
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.2) {
            self.pullResults = self.generateResults(count: 10)
            withAnimation(.spring(response: 0.5, dampingFraction: 0.75)) { self.phase = .result }
        }
    }

    func pullAgain() {
        pullResults = []
        withAnimation(.spring(response: 0.4, dampingFraction: 0.85)) { phase = .confirm }
    }

    func backToBannerSelect() {
        selectedBanner = nil
        pullResults = []
        withAnimation(.spring(response: 0.4, dampingFraction: 0.85)) { phase = .bannerSelect }
    }

    // ── Mock Data ─────────────────────────────────
    private let charPool: [(String, Int)] = [
        ("光の勇者アリア", 5), ("闇の魔王ゼファー", 5), ("聖女セラフィーナ", 5),
        ("炎の魔術師レイ", 4), ("氷の弓使いリナ", 4), ("風の剣士カイト", 4),
        ("見習い戦士タロウ", 3), ("森の精霊コダマ", 3), ("街の商人マルコ", 3)
    ]
    private let weapPool: [(String, Int)] = [
        ("聖剣エクスカリバー", 5), ("闇の大鎌デスサイズ", 5),
        ("氷の弓フロストアロー", 4), ("炎の杖ヘルフレイム", 4),
        ("鉄の剣", 3), ("木の杖", 3), ("革の盾", 3)
    ]

    private func generateResults(count: Int) -> [ResultItem] {
        (0..<count).map { i in
            let rarity = rollRarity(guarantee4: count >= 10 && i == count - 1)
            let isChar = Bool.random()
            let pool = isChar ? charPool : weapPool
            let item = pool.filter { $0.1 == rarity }.randomElement() ?? pool.filter { $0.1 <= rarity }.randomElement()!
            return ResultItem(
                id: UUID().uuidString, name: item.0, rarity: item.1,
                type: isChar ? .character : .weapon, isNew: Int.random(in: 0...3) == 0
            )
        }
    }

    private func rollRarity(guarantee4: Bool) -> Int {
        let roll = Int.random(in: 1...1000)
        if roll <= 30 { return 5 }
        if roll <= 180 || guarantee4 { return 4 }
        return 3
    }
}

// ============================================================
// MARK: - Main View
// ============================================================

/// 召喚画面（タブ④）
struct GachaScreenView: View {
    @StateObject private var store = GachaStore()

    var body: some View {
        ZStack {
            // 深いグラデーション背景
            LinearGradient(
                colors: [
                    Color(red: 0.04, green: 0.04, blue: 0.18),
                    Color(red: 0.10, green: 0.04, blue: 0.24),
                    Color(red: 0.04, green: 0.09, blue: 0.16)
                ],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            // 背景パーティクル装飾
            BackgroundParticles()

            switch store.phase {
            case .bannerSelect: BannerSelectView(store: store)
            case .confirm:      ConfirmView(store: store)
            case .pulling:      PullAnimationView(highestRarity: store.highestRarity)
            case .result:       ResultView(store: store)
            }
        }
    }
}

// ============================================================
// MARK: - Background Particles
// ============================================================

private struct BackgroundParticles: View {
    @State private var animate = false

    var body: some View {
        ZStack {
            ForEach(0..<15, id: \.self) { i in
                Circle()
                    .fill(Color.white.opacity(Double.random(in: 0.03...0.08)))
                    .frame(width: CGFloat.random(in: 2...6))
                    .offset(
                        x: CGFloat.random(in: -180...180),
                        y: animate ? CGFloat.random(in: -400...400) : CGFloat.random(in: -400...400)
                    )
                    .animation(
                        .easeInOut(duration: Double.random(in: 4...8))
                        .repeatForever(autoreverses: true)
                        .delay(Double(i) * 0.3),
                        value: animate
                    )
            }
        }
        .onAppear { animate = true }
    }
}

// ============================================================
// MARK: - Banner Selection Phase
// ============================================================

private struct BannerSelectView: View {
    @ObservedObject var store: GachaStore
    @State private var appeared = false

    var body: some View {
        VStack(spacing: 0) {
            // ヘッダー
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("召 喚")
                        .font(.system(size: 28, weight: .black))
                        .foregroundColor(.white)
                    Text("知識の結晶で仲間を召喚しよう")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.6))
                }
                Spacer()
                StoneCountBadge(stones: store.currentStones)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 24)

            // バナーカード一覧
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 16) {
                    ForEach(Array(store.banners.enumerated()), id: \.element.id) { index, banner in
                        BannerCard(banner: banner) {
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            store.selectBanner(banner)
                        }
                        .offset(y: appeared ? 0 : 60)
                        .opacity(appeared ? 1 : 0)
                        .animation(
                            .spring(response: 0.6, dampingFraction: 0.8).delay(Double(index) * 0.12),
                            value: appeared
                        )
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 120)
            }
        }
        .onAppear { appeared = true }
    }
}

// ── バナーカード ──────────────────────────────

private struct BannerCard: View {
    let banner: BannerDisplay
    let onTap: () -> Void
    @State private var shimmerOffset: CGFloat = -200

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .bottomLeading) {
                // 背景グラデーション
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(LinearGradient(
                        colors: banner.type.colors,
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))
                    .frame(height: 180)

                // シマー効果
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [.clear, .white.opacity(0.15), .clear],
                            startPoint: .leading, endPoint: .trailing
                        )
                    )
                    .frame(height: 180)
                    .offset(x: shimmerOffset)
                    .mask(RoundedRectangle(cornerRadius: 20, style: .continuous).frame(height: 180))

                // 大きなアイコン（右上）
                Image(systemName: banner.type.icon)
                    .font(.system(size: 80, weight: .ultraLight))
                    .foregroundColor(.white.opacity(0.1))
                    .offset(x: 120, y: -20)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                    .clipped()

                // テキスト情報
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 4) {
                        Text("PICK UP")
                            .font(.system(size: 10, weight: .black))
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Capsule().fill(.white.opacity(0.25)))

                        Text(banner.type.label)
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white.opacity(0.8))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Capsule().fill(.white.opacity(0.15)))
                    }

                    Text(banner.name)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)
                        .shadow(color: .black.opacity(0.3), radius: 4, y: 2)

                    Text(banner.description)
                        .font(.system(size: 12))
                        .foregroundColor(.white.opacity(0.8))

                    HStack(spacing: 2) {
                        ForEach(0..<banner.featuredRarity, id: \.self) { _ in
                            Image(systemName: "star.fill").font(.system(size: 12)).foregroundColor(.yellow)
                        }
                    }
                }
                .padding(20)
            }
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .shadow(color: banner.type.colors.first!.opacity(0.4), radius: 16, y: 8)
        }
        .buttonStyle(.plain)
        .onAppear {
            withAnimation(.linear(duration: 2.5).repeatForever(autoreverses: false)) {
                shimmerOffset = 400
            }
        }
    }
}

// ============================================================
// MARK: - Confirm Phase
// ============================================================

private struct ConfirmView: View {
    @ObservedObject var store: GachaStore
    @State private var appeared = false

    var body: some View {
        VStack(spacing: 0) {
            // ヘッダー
            HStack {
                Button(action: { store.backToBannerSelect() }) {
                    HStack(spacing: 6) {
                        Image(systemName: "chevron.left")
                        Text("バナー選択")
                    }
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.white.opacity(0.7))
                }
                Spacer()
                StoneCountBadge(stones: store.currentStones)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 12)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    if let banner = store.selectedBanner {
                        VStack(spacing: 4) {
                            Text(banner.name)
                                .font(.system(size: 26, weight: .black))
                                .foregroundColor(.white)
                                .multilineTextAlignment(.center)
                            Text(banner.description)
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.6))
                        }
                        .scaleEffect(appeared ? 1 : 0.8)
                        .opacity(appeared ? 1 : 0)

                        // 召喚オーブ
                        SummoningOrb(bannerType: banner.type)
                            .frame(height: 200)
                            .scaleEffect(appeared ? 1 : 0.5)
                            .opacity(appeared ? 1 : 0)

                        // 天井カウンター
                        if banner.pityThreshold > 0 {
                            PityCounter(current: store.pityCount, threshold: banner.pityThreshold)
                                .opacity(appeared ? 1 : 0)
                        }

                        // 召喚ボタン
                        VStack(spacing: 12) {
                            GlowPullButton(
                                label: "単発召喚", cost: GachaStore.singleCost,
                                enabled: store.canPullSingle, colors: banner.type.colors
                            ) {
                                UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
                                store.pullSingle()
                            }
                            GlowPullButton(
                                label: "10連召喚", cost: GachaStore.multiCost,
                                enabled: store.canPullMulti, colors: banner.type.colors,
                                isPrimary: true
                            ) {
                                UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
                                store.pullMulti()
                            }
                        }
                        .padding(.top, 8)
                        .offset(y: appeared ? 0 : 40)
                        .opacity(appeared ? 1 : 0)

                        // 排出率
                        RateInfoCard().opacity(appeared ? 1 : 0)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 120)
            }
        }
        .onAppear { withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) { appeared = true } }
        .onDisappear { appeared = false }
    }
}

// ── 天井カウンター ─────────────────────────────

private struct PityCounter: View {
    let current: Int
    let threshold: Int
    var progress: Double { min(Double(current) / Double(threshold), 1.0) }

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text("天井カウント")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.white.opacity(0.6))
                Spacer()
                Text("\(current) / \(threshold)")
                    .font(.system(size: 15, weight: .bold, design: .monospaced))
                    .foregroundColor(.white)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(.white.opacity(0.1)).frame(height: 6)
                    Capsule()
                        .fill(LinearGradient(
                            colors: [Color(red: 0.3, green: 0.7, blue: 1.0), Color(red: 0.6, green: 0.4, blue: 1.0)],
                            startPoint: .leading, endPoint: .trailing
                        ))
                        .frame(width: geo.size.width * progress, height: 6)
                }
            }
            .frame(height: 6)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(.ultraThinMaterial.opacity(0.3))
                .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(.white.opacity(0.08), lineWidth: 1))
        )
    }
}

// ── 排出率テーブル ─────────────────────────────

private struct RateInfoCard: View {
    @State private var expanded = false

    var body: some View {
        VStack(spacing: 0) {
            Button(action: { withAnimation(.spring(response: 0.3)) { expanded.toggle() } }) {
                HStack {
                    Image(systemName: "info.circle"); Text("排出率"); Spacer()
                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                }
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.white.opacity(0.5))
                .padding(14)
            }
            if expanded {
                VStack(spacing: 6) {
                    rateRow("★★★★★", "3.0%", Color(red: 1.0, green: 0.84, blue: 0.0))
                    rateRow("★★★★", "15.0%", Color(red: 0.6, green: 0.35, blue: 0.85))
                    rateRow("★★★", "82.0%", Color(red: 0.2, green: 0.6, blue: 0.9))
                }
                .padding(.horizontal, 14).padding(.bottom, 14)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(.ultraThinMaterial.opacity(0.2))
                .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(.white.opacity(0.06), lineWidth: 1))
        )
    }

    private func rateRow(_ stars: String, _ rate: String, _ color: Color) -> some View {
        HStack {
            Text(stars).font(.system(size: 12)).foregroundColor(color)
            Spacer()
            Text(rate).font(.system(size: 12, weight: .bold, design: .monospaced)).foregroundColor(.white.opacity(0.7))
        }
    }
}

// ============================================================
// MARK: - Pull Animation Phase
// ============================================================

private struct PullAnimationView: View {
    let highestRarity: Int
    @State private var circleScale: CGFloat = 0.3
    @State private var circleOpacity: Double = 0
    @State private var rotation: Double = 0
    @State private var glowScale: CGFloat = 0.2
    @State private var glowOpacity: Double = 0
    @State private var burstScale: CGFloat = 0.1
    @State private var burstOpacity: Double = 0
    @State private var particlesVisible = false
    @State private var particlesConverge = false

    private var accentColor: Color {
        switch highestRarity {
        case 5: return Color(red: 1.0, green: 0.84, blue: 0.0)
        case 4: return Color(red: 0.6, green: 0.35, blue: 0.85)
        default: return Color(red: 0.3, green: 0.6, blue: 1.0)
        }
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            // パーティクル
            ForEach(0..<20, id: \.self) { i in
                Circle()
                    .fill(accentColor.opacity(0.7))
                    .frame(width: CGFloat(Int.random(in: 3...7)))
                    .offset(
                        x: particlesConverge ? CGFloat.random(in: -10...10) : cos(Double(i) * 0.314) * CGFloat.random(in: 100...200),
                        y: particlesConverge ? CGFloat.random(in: -10...10) : sin(Double(i) * 0.314) * CGFloat.random(in: 100...200)
                    )
                    .opacity(particlesVisible ? 1 : 0)
            }

            // 外側リング
            Circle()
                .stroke(
                    AngularGradient(
                        colors: [accentColor, accentColor.opacity(0.2), .clear, accentColor.opacity(0.2), accentColor],
                        center: .center
                    ), lineWidth: 3
                )
                .frame(width: 220, height: 220)
                .rotationEffect(.degrees(rotation))
                .scaleEffect(circleScale)
                .opacity(circleOpacity)

            // 内側リング
            Circle()
                .stroke(
                    AngularGradient(
                        colors: [.white.opacity(0.6), .clear, accentColor.opacity(0.4), .clear, .white.opacity(0.6)],
                        center: .center
                    ), lineWidth: 2
                )
                .frame(width: 150, height: 150)
                .rotationEffect(.degrees(-rotation * 0.7))
                .scaleEffect(circleScale)
                .opacity(circleOpacity)

            // 十字の装飾線
            ForEach(0..<4, id: \.self) { i in
                Rectangle()
                    .fill(accentColor.opacity(0.3))
                    .frame(width: 1, height: 100)
                    .rotationEffect(.degrees(Double(i) * 45))
                    .scaleEffect(circleScale)
                    .opacity(circleOpacity)
            }

            // 中央グロー
            RadialGradient(
                gradient: Gradient(colors: [accentColor.opacity(0.8), accentColor.opacity(0.2), .clear]),
                center: .center, startRadius: 0, endRadius: 80
            )
            .frame(width: 160, height: 160)
            .scaleEffect(glowScale)
            .opacity(glowOpacity)

            // バースト（フラッシュ）
            Circle()
                .fill(
                    RadialGradient(
                        gradient: Gradient(colors: [.white, accentColor.opacity(0.5), .clear]),
                        center: .center, startRadius: 0, endRadius: 300
                    )
                )
                .scaleEffect(burstScale)
                .opacity(burstOpacity)
                .ignoresSafeArea()

            Text("召 喚 中 ...")
                .font(.system(size: 14, weight: .medium, design: .monospaced))
                .foregroundColor(.white.opacity(circleOpacity * 0.5))
                .offset(y: 160)
        }
        .onAppear { startAnimation() }
    }

    private func startAnimation() {
        // Phase 1: 円が出現
        withAnimation(.easeOut(duration: 0.6)) {
            circleScale = 1.0; circleOpacity = 1.0; glowScale = 0.6; glowOpacity = 0.4
        }
        withAnimation(.linear(duration: 2.5).repeatForever(autoreverses: false)) { rotation = 360 }
        withAnimation(.easeIn(duration: 0.5).delay(0.3)) { particlesVisible = true }

        // Phase 2: 収束
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            withAnimation(.easeIn(duration: 1.0)) {
                circleScale = 1.4; glowScale = 1.2; glowOpacity = 0.8; particlesConverge = true
            }
        }

        // Phase 3: バースト
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.7) {
            UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
            withAnimation(.easeOut(duration: 0.25)) {
                burstOpacity = 1.0; burstScale = 4.0; circleOpacity = 0; glowOpacity = 0; particlesVisible = false
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
            withAnimation(.easeOut(duration: 0.2)) { burstOpacity = 0 }
        }
    }
}

// ── Idle 状態のオーブ ────────────────────────

private struct SummoningOrb: View {
    let bannerType: BannerType
    @State private var pulse = false
    @State private var rotation: Double = 0

    var body: some View {
        ZStack {
            Circle()
                .stroke(AngularGradient(colors: bannerType.colors + [bannerType.colors.first!], center: .center), lineWidth: 2)
                .frame(width: 160, height: 160)
                .rotationEffect(.degrees(rotation))
            Circle()
                .stroke(bannerType.colors.first!.opacity(0.3), lineWidth: 1)
                .frame(width: 120, height: 120)
                .rotationEffect(.degrees(-rotation * 0.5))
            RadialGradient(
                gradient: Gradient(colors: [bannerType.colors.last!.opacity(0.5), .clear]),
                center: .center, startRadius: 10, endRadius: 60
            )
            .frame(width: 120, height: 120)
            .scaleEffect(pulse ? 1.15 : 0.9)
            Image(systemName: bannerType.icon)
                .font(.system(size: 36, weight: .light))
                .foregroundColor(.white.opacity(0.8))
                .scaleEffect(pulse ? 1.05 : 0.95)
        }
        .onAppear {
            withAnimation(.linear(duration: 6).repeatForever(autoreverses: false)) { rotation = 360 }
            withAnimation(.easeInOut(duration: 2).repeatForever(autoreverses: true)) { pulse = true }
        }
    }
}

// ============================================================
// MARK: - Result Phase
// ============================================================

private struct ResultView: View {
    @ObservedObject var store: GachaStore
    @State private var appeared = false
    @State private var cardsRevealed: Set<Int> = []

    private var sortedResults: [ResultItem] { store.pullResults.sorted { $0.rarity > $1.rarity } }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("召喚結果")
                    .font(.system(size: 22, weight: .bold)).foregroundColor(.white)
                Spacer()
                StoneCountBadge(stones: store.currentStones)
            }
            .padding(.horizontal, 20).padding(.top, 16).padding(.bottom, 16)
            .opacity(appeared ? 1 : 0)

            ScrollView(showsIndicators: false) {
                if store.lastPullCount == 1, let item = sortedResults.first {
                    SingleResultCard(item: item, revealed: cardsRevealed.contains(0))
                        .padding(.horizontal, 20).padding(.top, 40)
                        .onAppear { revealCard(index: 0, delay: 0.3) }
                } else {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(Array(sortedResults.enumerated()), id: \.element.id) { index, item in
                            MultiResultCard(item: item, revealed: cardsRevealed.contains(index))
                                .onAppear { revealCard(index: index, delay: 0.15 + Double(index) * 0.08) }
                        }
                    }
                    .padding(.horizontal, 20)
                }

                // アクションボタン
                VStack(spacing: 12) {
                    GlowPullButton(
                        label: "もう一度召喚する",
                        cost: store.lastPullCount == 1 ? GachaStore.singleCost : GachaStore.multiCost,
                        enabled: store.lastPullCount == 1 ? store.canPullSingle : store.canPullMulti,
                        colors: [Color(red: 0.3, green: 0.6, blue: 1.0), Color(red: 0.5, green: 0.3, blue: 0.9)]
                    ) {
                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                        store.pullAgain()
                    }
                    Button(action: {
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        store.backToBannerSelect()
                    }) {
                        Text("バナー選択に戻る")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(.white.opacity(0.5))
                            .frame(maxWidth: .infinity).padding(.vertical, 14)
                    }
                }
                .padding(.horizontal, 20).padding(.top, 24).padding(.bottom, 120)
                .opacity(appeared ? 1 : 0)
            }
        }
        .onAppear { withAnimation(.easeOut(duration: 0.4)) { appeared = true } }
        .onDisappear { appeared = false; cardsRevealed = [] }
    }

    private func revealCard(index: Int, delay: Double) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) { cardsRevealed.insert(index) }
        }
    }
}

// ── 単発結果カード ─────────────────────────────

private struct SingleResultCard: View {
    let item: ResultItem; let revealed: Bool

    var body: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            gradient: Gradient(colors: [item.rarityColor.opacity(0.5), item.rarityColor.opacity(0.1), .clear]),
                            center: .center,
                            startRadius: 20,
                            endRadius: 120
                        )
                    )
                    .frame(width: 240, height: 240)
                Image(systemName: item.type.icon).font(.system(size: 60, weight: .light)).foregroundColor(.white)
                if item.isNew {
                    Text("NEW").font(.system(size: 11, weight: .black)).foregroundColor(.white)
                        .padding(.horizontal, 10).padding(.vertical, 4)
                        .background(Capsule().fill(Color.red))
                        .offset(x: 50, y: -50)
                }
            }
            VStack(spacing: 8) {
                Text(item.rarityStars).font(.system(size: 24)).foregroundColor(item.rarityColor)
                Text(item.name).font(.system(size: 24, weight: .bold)).foregroundColor(.white)
                Text(item.type == .character ? "キャラクター" : "武器")
                    .font(.system(size: 13)).foregroundColor(.white.opacity(0.5))
            }
        }
        .padding(32).frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(.ultraThinMaterial.opacity(0.25))
                .overlay(RoundedRectangle(cornerRadius: 24, style: .continuous).stroke(item.rarityColor.opacity(0.4), lineWidth: 1))
        )
        .scaleEffect(revealed ? 1.0 : 0.3).opacity(revealed ? 1.0 : 0)
        .rotation3DEffect(.degrees(revealed ? 0 : 180), axis: (x: 0, y: 1, z: 0))
    }
}

// ── 10連結果カード ─────────────────────────────

private struct MultiResultCard: View {
    let item: ResultItem; let revealed: Bool

    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            gradient: Gradient(colors: [item.rarityColor.opacity(0.4), .clear]),
                            center: .center, startRadius: 5, endRadius: 40
                        )
                    )
                    .frame(width: 70, height: 70)
                Image(systemName: item.type.icon).font(.system(size: 28, weight: .light)).foregroundColor(.white)
                if item.isNew {
                    Text("NEW").font(.system(size: 8, weight: .black)).foregroundColor(.white)
                        .padding(.horizontal, 5).padding(.vertical, 2)
                        .background(Capsule().fill(Color.red))
                        .offset(x: 25, y: -25)
                }
            }
            Text(item.rarityStars).font(.system(size: 12)).foregroundColor(item.rarityColor)
            Text(item.name).font(.system(size: 13, weight: .semibold)).foregroundColor(.white)
                .lineLimit(1).minimumScaleFactor(0.7)
        }
        .padding(.vertical, 16).padding(.horizontal, 8).frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(.ultraThinMaterial.opacity(0.2))
                .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(item.rarityColor.opacity(0.3), lineWidth: 1))
        )
        .scaleEffect(revealed ? 1.0 : 0.4).opacity(revealed ? 1.0 : 0)
    }
}

// ============================================================
// MARK: - Common Components
// ============================================================

private struct StoneCountBadge: View {
    let stones: Int
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: "sparkles").font(.system(size: 14)).foregroundColor(.yellow)
            Text("\(stones)").font(.system(size: 15, weight: .bold, design: .monospaced)).foregroundColor(.white)
        }
        .padding(.horizontal, 12).padding(.vertical, 8)
        .background(Capsule().fill(.ultraThinMaterial.opacity(0.3)).overlay(Capsule().stroke(.white.opacity(0.1), lineWidth: 1)))
    }
}

private struct GlowPullButton: View {
    let label: String; let cost: Int; let enabled: Bool; let colors: [Color]
    var isPrimary: Bool = false
    let action: () -> Void
    @State private var shimmerOffset: CGFloat = -300

    var body: some View {
        Button(action: action) {
            ZStack {
                Capsule().fill(
                    enabled
                    ? LinearGradient(colors: colors, startPoint: .leading, endPoint: .trailing)
                    : LinearGradient(colors: [.gray.opacity(0.3), .gray.opacity(0.2)], startPoint: .leading, endPoint: .trailing)
                )
                if enabled {
                    Capsule()
                        .fill(LinearGradient(colors: [.clear, .white.opacity(0.2), .clear], startPoint: .leading, endPoint: .trailing))
                        .offset(x: shimmerOffset).mask(Capsule())
                }
                HStack(spacing: 8) {
                    Text(label).font(.system(size: isPrimary ? 17 : 15, weight: .bold))
                    HStack(spacing: 3) {
                        Image(systemName: "sparkles").font(.system(size: 12))
                        Text("\(cost)").font(.system(size: isPrimary ? 17 : 15, weight: .bold, design: .monospaced))
                    }
                    .padding(.horizontal, 8).padding(.vertical, 4)
                    .background(Capsule().fill(.white.opacity(enabled ? 0.2 : 0.05)))
                }
                .foregroundColor(enabled ? .white : .white.opacity(0.3))
            }
            .frame(height: isPrimary ? 56 : 48)
            .shadow(color: enabled ? colors.first!.opacity(0.4) : .clear, radius: 12, y: 4)
        }
        .disabled(!enabled)
        .buttonStyle(.plain)
        .onAppear {
            if enabled {
                withAnimation(.linear(duration: 2.0).repeatForever(autoreverses: false)) { shimmerOffset = 300 }
            }
        }
    }
}

// ============================================================
// MARK: - Preview
// ============================================================

struct GachaScreenView_Previews: PreviewProvider {
    static var previews: some View {
        GachaScreenView()
    }
}
