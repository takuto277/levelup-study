import SwiftUI
import Shared

// ============================================================
// MARK: - Data Types (local Swift types for UI)
// ============================================================

private enum UIPhase {
    case bannerSelect, confirm, pulling, result
}

private enum UIBannerType: String {
    case character, weapon, mixed

    var colors: [Color] {
        switch self {
        case .character: return [Color(red: 0.3, green: 0.2, blue: 0.8), Color(red: 0.5, green: 0.2, blue: 0.9)]
        case .weapon: return [Color(red: 0.8, green: 0.2, blue: 0.2), Color(red: 0.9, green: 0.4, blue: 0.1)]
        case .mixed: return [Color(red: 0.6, green: 0.2, blue: 0.8), Color(red: 0.9, green: 0.3, blue: 0.5)]
        }
    }
}

/// メインタブの BottomNavigation と重ならないよう確保（pt）
private let gachaMainTabBottomReserve: CGFloat = 88

private struct BannerDisplay: Identifiable {
    let id: String
    let name: String
    let type: UIBannerType
    /// ピックアップ立ち絵（マスタの image_url）
    let pickupImageUrl: String?
    /// マスタ `start_at` / `end_at`（API の文字列をそのまま）
    let startAt: String
    let endAt: String

    var periodLine: String { BannerDisplay.formatPeriodLine(startAt: startAt, endAt: endAt) }

    private static func formatOne(_ raw: String) -> String {
        let s = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if s.isEmpty { return "—" }
        if s.count >= 10 {
            let head = String(s.prefix(10))
            let p = head.split(separator: "-")
            if p.count == 3, let y = Int(p[0]), let m = Int(p[1]), let d = Int(p[2]) {
                return "\(y)年\(m)月\(d)日"
            }
        }
        let iso = ISO8601DateFormatter()
        iso.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = iso.date(from: s) {
            return formatDateForLabel(date)
        }
        iso.formatOptions = [.withInternetDateTime]
        if let date = iso.date(from: s) {
            return formatDateForLabel(date)
        }
        return s
    }

    private static func formatDateForLabel(_ date: Date) -> String {
        let cal = Calendar.current
        let c = cal.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        guard let y = c.year, let m = c.month, let d = c.day, let h = c.hour, let min = c.minute else { return "—" }
        return String(format: "%d年%d月%d日 %02d:%02d", y, m, d, h, min)
    }

    static func formatPeriodLine(startAt: String, endAt: String) -> String {
        "開催 \(formatOne(startAt)) 〜 \(formatOne(endAt))"
    }
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
// MARK: - Store (bridges to KMP GachaViewModel)
// ============================================================

private class GachaStore: ObservableObject {
    let kmpViewModel: GachaViewModel

    @Published var phase: UIPhase = .bannerSelect
    @Published var banners: [BannerDisplay] = []
    @Published var selectedBanner: BannerDisplay?
    @Published var currentStones: Int = 0
    @Published var pullResults: [ResultItem] = []
    @Published var lastPullCount: Int = 0
    @Published var error: String?

    static let singleCost = 50
    static let multiCost = 450

    var canPullSingle: Bool { currentStones >= Self.singleCost }
    var canPullMulti: Bool { currentStones >= Self.multiCost }
    var highestRarity: Int { pullResults.map(\.rarity).max() ?? 3 }

    private var syncTimer: Timer?

    init() {
        kmpViewModel = KoinHelperKt.getGachaViewModel()
        syncTimer = Timer.scheduledTimer(withTimeInterval: 0.3, repeats: true) { [weak self] _ in
            self?.syncFromKMP()
        }
    }

    deinit { syncTimer?.invalidate() }

    // MARK: Actions (forwarded to KMP ViewModel)

    func selectBanner(_ banner: BannerDisplay) {
        kmpViewModel.onIntent(intent: GachaIntentSelectBanner(bannerId: banner.id))
    }

    func pullSingle() {
        guard canPullSingle, let banner = selectedBanner else { return }
        kmpViewModel.onIntent(intent: GachaIntentPullSingle(bannerId: banner.id))
    }

    func pullMulti() {
        guard canPullMulti, let banner = selectedBanner else { return }
        kmpViewModel.onIntent(intent: GachaIntentPullMulti(bannerId: banner.id))
    }

    func pullAgain() {
        kmpViewModel.onIntent(intent: GachaIntentPullAgain())
    }

    func backToBannerSelect() {
        kmpViewModel.onIntent(intent: GachaIntentDismissResults())
    }

    // MARK: KMP State Sync

    private func syncFromKMP() {
        guard let state = kmpViewModel.uiState.value as? GachaUiState else { return }

        let newPhase: UIPhase
        let kp = state.phase
        if kp == GachaPhase.bannerSelect { newPhase = .bannerSelect }
        else if kp == GachaPhase.confirm { newPhase = .confirm }
        else if kp == GachaPhase.pulling { newPhase = .pulling }
        else { newPhase = .result }

        if newPhase != phase {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.85)) { phase = newPhase }
        }

        currentStones = Int(state.currentStones)
        lastPullCount = Int(state.lastPullCount)
        error = state.error

        let newBanners: [BannerDisplay] = state.banners.compactMap { item in
            guard let b = item as? GachaBanner else { return nil }
            return convertBanner(b)
        }
        if banners.map(\.id) != newBanners.map(\.id) { banners = newBanners }

        if let sb = state.selectedBanner {
            selectedBanner = banners.first { $0.id == sb.id }
        } else {
            selectedBanner = nil
        }

        let newResults: [ResultItem] = state.pullResults.compactMap { item in
            guard let r = item as? GachaResultItem else { return nil }
            return convertResult(r)
        }
        if pullResults.map(\.id) != newResults.map(\.id) { pullResults = newResults }
    }

    private func convertBanner(_ b: GachaBanner) -> BannerDisplay {
        let bt: UIBannerType
        let kbt = b.bannerType
        if kbt == BannerType.character { bt = .character }
        else if kbt == BannerType.weapon { bt = .weapon }
        else if kbt == BannerType.costume { bt = .mixed }
        else { bt = .mixed }
        let hero = b.primaryFeaturedForHero()
        let urlStr = hero?.imageUrl
        let pickupUrl: String? = (urlStr?.isEmpty == false) ? urlStr : nil
        return BannerDisplay(
            id: b.id,
            name: b.name,
            type: bt,
            pickupImageUrl: pickupUrl,
            startAt: b.startAt,
            endAt: b.endAt
        )
    }

    private func convertResult(_ r: GachaResultItem) -> ResultItem {
        let it: ItemType = (r.type == GachaResultType.character) ? .character : .weapon
        return ResultItem(
            id: r.id, name: r.name,
            rarity: Int(r.rarity),
            type: it, isNew: r.isNew
        )
    }
}

// ============================================================
// MARK: - Main View
// ============================================================

struct GachaScreenView: View {
    @StateObject private var store = GachaStore()

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.04, green: 0.04, blue: 0.18),
                    Color(red: 0.10, green: 0.04, blue: 0.24),
                    Color(red: 0.04, green: 0.09, blue: 0.16)
                ],
                startPoint: .topLeading, endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

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
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("召 喚")
                        .font(.system(size: 28, weight: .black))
                        .foregroundColor(.white)
                    Text("バナーを選んで詳細へ")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.6))
                }
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 12)

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
                .padding(.bottom, 12)
            }

            GachaStonesBottomBar(stones: store.currentStones)
        }
        .onAppear { appeared = true }
    }
}

private struct GachaStonesBottomBar: View {
    let stones: Int

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("知識の結晶").font(.system(size: 11)).foregroundColor(.white.opacity(0.55))
                Text("消費して召喚").font(.system(size: 13, weight: .semibold)).foregroundColor(.white.opacity(0.85))
            }
            Spacer()
            StoneCountBadge(stones: stones)
        }
        .padding(.horizontal, 20)
        .padding(.top, 14)
        .padding(.bottom, 14 + gachaMainTabBottomReserve)
        .background(Color.black.opacity(0.5))
    }
}

private struct BannerCard: View {
    let banner: BannerDisplay
    let onTap: () -> Void
    @State private var shimmerOffset: CGFloat = -200

    var body: some View {
        Button(action: onTap) {
            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(LinearGradient(
                        colors: banner.type.colors,
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ))
                    .frame(height: 312)

                Group {
                    if let s = banner.pickupImageUrl, let u = URL(string: s) {
                        AsyncImage(url: u) { phase in
                            switch phase {
                            case .success(let img):
                                img
                                    .resizable()
                                    .scaledToFill()
                                    .frame(height: 312)
                                    .frame(maxWidth: .infinity)
                                    .clipped()
                            default:
                                LinearGradient(colors: banner.type.colors, startPoint: .top, endPoint: .bottom)
                                    .frame(height: 312)
                            }
                        }
                    } else {
                        Image("sprite_player_idle_1")
                            .resizable()
                            .scaledToFill()
                            .frame(height: 312)
                            .frame(maxWidth: .infinity)
                            .clipped()
                    }
                }
                .frame(height: 312)
                .clipped()

                LinearGradient(
                    colors: [.clear, .clear, .black.opacity(0.82)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 312)
                .allowsHitTesting(false)

                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(
                        LinearGradient(
                            colors: [.clear, .white.opacity(0.16), .clear],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(height: 312)
                    .offset(x: shimmerOffset)
                    .mask(RoundedRectangle(cornerRadius: 24, style: .continuous).frame(height: 312))

                Text("✦")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.white.opacity(0.55))
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(.white.opacity(0.08)))
                    .overlay(Circle().stroke(.white.opacity(0.12), lineWidth: 1))
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                    .padding(14)

                VStack(alignment: .leading, spacing: 6) {
                    Text(banner.name)
                        .font(.system(size: 23, weight: .black))
                        .foregroundColor(.white)
                        .lineLimit(2)
                        .shadow(color: .black.opacity(0.35), radius: 4, y: 2)

                    Text(banner.periodLine)
                        .font(.system(size: 12, weight: .semibold, design: .monospaced))
                        .foregroundColor(.white.opacity(0.78))
                        .lineLimit(2)

                    HStack(spacing: 6) {
                        Text("タップして召喚へ")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white.opacity(0.45))
                            .tracking(0.6)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white.opacity(0.4))
                    }
                    .padding(.top, 4)
                }
                .padding(18)
            }
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: banner.type.colors.first!.opacity(0.42), radius: 18, y: 10)
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
            HStack(alignment: .center) {
                Button(action: { store.backToBannerSelect() }) {
                    HStack(spacing: 6) {
                        Image(systemName: "chevron.left")
                        Text("召喚確認")
                    }
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.white.opacity(0.7))
                }
                Spacer(minLength: 8)
                StoneCountBadge(stones: store.currentStones)
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            .padding(.bottom, 8)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 20) {
                    if let banner = store.selectedBanner {
                        VStack(spacing: 4) {
                            Text(banner.name)
                                .font(.system(size: 26, weight: .black))
                                .foregroundColor(.white)
                                .multilineTextAlignment(.center)
                            Text("知識の結晶を消費して召喚します")
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.6))
                        }
                        .opacity(appeared ? 1 : 0)

                        SummoningOrb(bannerType: banner.type, pickupImageUrl: banner.pickupImageUrl)
                            .frame(height: 220)
                            .scaleEffect(appeared ? 1 : 0.92)
                            .opacity(appeared ? 1 : 0)

                        HStack(spacing: 10) {
                            GlowPullButton(
                                label: "単発召喚", cost: GachaStore.singleCost,
                                enabled: store.canPullSingle, colors: banner.type.colors,
                                compactHorizontal: true
                            ) {
                                UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
                                store.pullSingle()
                            }
                            .frame(maxWidth: .infinity)

                            GlowPullButton(
                                label: "10連召喚", cost: GachaStore.multiCost,
                                enabled: store.canPullMulti, colors: banner.type.colors,
                                isPrimary: true,
                                compactHorizontal: true
                            ) {
                                UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
                                store.pullMulti()
                            }
                            .frame(maxWidth: .infinity)
                        }
                        .opacity(appeared ? 1 : 0)

                        RateInfoCard().opacity(appeared ? 1 : 0)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32 + gachaMainTabBottomReserve)
            }
        }
        .onAppear { withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) { appeared = true } }
        .onDisappear { appeared = false }
    }
}

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
                .fill(Color.white.opacity(0.06))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
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

            Circle()
                .stroke(
                    AngularGradient(
                        gradient: Gradient(colors: [accentColor, accentColor.opacity(0.2), Color.clear, accentColor.opacity(0.2), accentColor]),
                        center: .center
                    ), lineWidth: 3
                )
                .frame(width: 220, height: 220)
                .rotationEffect(.degrees(rotation))
                .scaleEffect(circleScale)
                .opacity(circleOpacity)

            Circle()
                .stroke(
                    AngularGradient(
                        gradient: Gradient(colors: [Color.white.opacity(0.6), Color.clear, accentColor.opacity(0.4), Color.clear, Color.white.opacity(0.6)]),
                        center: .center
                    ), lineWidth: 2
                )
                .frame(width: 150, height: 150)
                .rotationEffect(.degrees(-rotation * 0.7))
                .scaleEffect(circleScale)
                .opacity(circleOpacity)

            ForEach(0..<4, id: \.self) { i in
                Rectangle()
                    .fill(accentColor.opacity(0.3))
                    .frame(width: 1, height: 100)
                    .rotationEffect(.degrees(Double(i) * 45))
                    .scaleEffect(circleScale)
                    .opacity(circleOpacity)
            }

            RadialGradient(
                gradient: Gradient(colors: [accentColor.opacity(0.8), accentColor.opacity(0.2), .clear]),
                center: .center, startRadius: 0, endRadius: 80
            )
            .frame(width: 160, height: 160)
            .scaleEffect(glowScale)
            .opacity(glowOpacity)

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
        withAnimation(.easeOut(duration: 0.6)) {
            circleScale = 1.0; circleOpacity = 1.0; glowScale = 0.6; glowOpacity = 0.4
        }
        withAnimation(.linear(duration: 2.5).repeatForever(autoreverses: false)) { rotation = 360 }
        withAnimation(.easeIn(duration: 0.5).delay(0.3)) { particlesVisible = true }

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            withAnimation(.easeIn(duration: 1.0)) {
                circleScale = 1.4; glowScale = 1.2; glowOpacity = 0.8; particlesConverge = true
            }
        }

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

private struct SummoningOrb: View {
    let bannerType: UIBannerType
    let pickupImageUrl: String?
    @State private var pulse = false
    @State private var rotation: Double = 0

    var body: some View {
        ZStack {
            Group {
                if let s = pickupImageUrl, let u = URL(string: s) {
                    AsyncImage(url: u) { phase in
                        switch phase {
                        case .success(let img):
                            img.resizable()
                                .scaledToFill()
                                .frame(width: 108, height: 108)
                                .clipped()
                        default:
                            Color.black.opacity(0.35)
                                .frame(width: 108, height: 108)
                        }
                    }
                } else {
                    Image("sprite_player_idle_1")
                        .resizable()
                        .scaledToFill()
                        .frame(width: 108, height: 108)
                        .clipped()
                }
            }
            .frame(width: 108, height: 108)
            .clipShape(Circle())
            .overlay(Circle().stroke(Color.white.opacity(0.08), lineWidth: 1))

            Circle()
                .stroke(
                    AngularGradient(
                        gradient: Gradient(colors: bannerType.colors + [bannerType.colors.first!]),
                        center: .center
                    ),
                    lineWidth: 2
                )
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
            .allowsHitTesting(false)
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

    private var repeatCost: Int {
        store.lastPullCount == 1 ? GachaStore.singleCost : GachaStore.multiCost
    }
    private var canRepeat: Bool {
        store.lastPullCount == 1 ? store.canPullSingle : store.canPullMulti
    }

    var body: some View {
        VStack(spacing: 0) {
            resultHeader
            resultScrollContent
        }
        .onAppear {
            withAnimation(.easeOut(duration: 0.4)) { appeared = true }
        }
        .onDisappear {
            appeared = false
            cardsRevealed = []
        }
    }

    private var resultHeader: some View {
        HStack {
            Text("召喚結果")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.white)
            Spacer()
            StoneCountBadge(stones: store.currentStones)
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 16)
        .opacity(appeared ? 1 : 0)
    }

    private var resultScrollContent: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                resultCards
                actionButtons
            }
        }
    }

    @ViewBuilder
    private var resultCards: some View {
        if store.lastPullCount == 1, let item = sortedResults.first {
            SingleResultCard(item: item, revealed: cardsRevealed.contains(0))
                .padding(.horizontal, 20)
                .padding(.top, 40)
                .onAppear { scheduleReveal(index: 0, afterSeconds: 0.3) }
        } else {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ForEach(Array(sortedResults.enumerated()), id: \.element.id) { index, item in
                    MultiResultCard(item: item, revealed: cardsRevealed.contains(index))
                        .onAppear { scheduleReveal(index: index, afterSeconds: 0.15 + Double(index) * 0.08) }
                }
            }
            .padding(.horizontal, 20)
        }
    }

    private var actionButtons: some View {
        VStack(spacing: 12) {
            GlowPullButton(
                label: "もう一度召喚する",
                cost: repeatCost,
                enabled: canRepeat,
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
                    .foregroundColor(Color.white.opacity(0.5))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
        }
        .padding(.horizontal, 20)
        .padding(.top, 24)
        .padding(.bottom, 120)
        .opacity(appeared ? 1 : 0)
    }

    private func scheduleReveal(index: Int, afterSeconds: Double) {
        DispatchQueue.main.asyncAfter(deadline: .now() + afterSeconds) {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) {
                _ = cardsRevealed.insert(index)
            }
        }
    }
}

private struct SingleResultCard: View {
    let item: ResultItem; let revealed: Bool

    var body: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(
                            gradient: Gradient(colors: [item.rarityColor.opacity(0.5), item.rarityColor.opacity(0.1), .clear]),
                            center: .center, startRadius: 20, endRadius: 120
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
                .fill(Color.white.opacity(0.08))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(item.rarityColor.opacity(0.4), lineWidth: 1)
        )
        .scaleEffect(revealed ? 1.0 : 0.3).opacity(revealed ? 1.0 : 0)
        .rotation3DEffect(.degrees(revealed ? 0 : 180), axis: (x: 0, y: 1, z: 0))
    }
}

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
                .fill(Color.white.opacity(0.06))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(item.rarityColor.opacity(0.3), lineWidth: 1)
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
            Text("💎").font(.system(size: 14))
            Text("\(stones)").font(.system(size: 15, weight: .bold, design: .monospaced)).foregroundColor(.white)
        }
        .padding(.horizontal, 12).padding(.vertical, 8)
        .background(Capsule().fill(Color.white.opacity(0.1)))
        .overlay(Capsule().stroke(Color.white.opacity(0.1), lineWidth: 1))
    }
}

private struct GlowPullButton: View {
    let label: String; let cost: Int; let enabled: Bool; let colors: [Color]
    var isPrimary: Bool = false
    /// 横並び2ボタン用（ラベル省略・コスト右寄せ）
    var compactHorizontal: Bool = false
    let action: () -> Void
    @State private var shimmerOffset: CGFloat = -300

    var body: some View {
        Button(action: action) {
            ZStack {
                Capsule().fill(
                    LinearGradient(
                        gradient: Gradient(colors: enabled ? colors : [Color.gray.opacity(0.3), Color.gray.opacity(0.2)]),
                        startPoint: .leading, endPoint: .trailing
                    )
                )
                if enabled {
                    Capsule()
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [Color.clear, Color.white.opacity(0.2), Color.clear]),
                                startPoint: .leading, endPoint: .trailing
                            )
                        )
                        .offset(x: shimmerOffset).mask(Capsule())
                }
                Group {
                    if compactHorizontal {
                        HStack(spacing: 6) {
                            Text(label)
                                .font(.system(size: isPrimary ? 17 : 15, weight: .bold))
                                .lineLimit(1)
                                .minimumScaleFactor(0.72)
                                .frame(maxWidth: .infinity)
                            costChip(isPrimary: isPrimary, enabled: enabled)
                                .fixedSize()
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 6)
                    } else {
                        HStack(spacing: 8) {
                            Text(label).font(.system(size: isPrimary ? 17 : 15, weight: .bold))
                            costChip(isPrimary: isPrimary, enabled: enabled)
                        }
                    }
                }
                .foregroundColor(enabled ? Color.white : Color.white.opacity(0.3))
            }
            .frame(maxWidth: .infinity)
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

    @ViewBuilder
    private func costChip(isPrimary: Bool, enabled: Bool) -> some View {
        HStack(spacing: 3) {
            Image(systemName: "sparkles").font(.system(size: 12))
            Text("\(cost)").font(.system(size: isPrimary ? 17 : 15, weight: .bold, design: .monospaced))
        }
        .padding(.horizontal, 8).padding(.vertical, 4)
        .background(Capsule().fill(Color.white.opacity(enabled ? 0.2 : 0.05)))
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
