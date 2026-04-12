import SwiftUI
import Shared

// MARK: - Color Extension

private extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255, opacity: alpha)
    }
}

// MARK: - カラーパレット（青テーマ）
private let bgDark = Color(hex: 0x0B1120)
private let bgCard = Color(hex: 0x111B2E)
private let bgSurface = Color(hex: 0x1A2744)
private let accentCyan = Color(hex: 0x22D3EE)
private let accentBlue = Color(hex: 0x3B82F6)
private let accentIndigo = Color(hex: 0x6366F1)
private let textW = Color(hex: 0xF1F5F9)
private let textSub = Color(hex: 0x94A3B8)

// MARK: - 難易度カラー

private func diffColor(_ d: DungeonDifficulty) -> Color {
    switch d {
    case .beginner: return Color(hex: 0x10B981)
    case .intermediate: return Color(hex: 0x3B82F6)
    case .advanced: return Color(hex: 0xF59E0B)
    case .expert: return Color(hex: 0xEF4444)
    case .legendary: return Color(hex: 0x8B5CF6)
    default: return Color(hex: 0x94A3B8)
    }
}

private func diffGrad(_ d: DungeonDifficulty) -> [Color] {
    switch d {
    case .beginner: return [Color(hex: 0x10B981), Color(hex: 0x34D399)]
    case .intermediate: return [Color(hex: 0x3B82F6), Color(hex: 0x60A5FA)]
    case .advanced: return [Color(hex: 0xF59E0B), Color(hex: 0xFBBF24)]
    case .expert: return [Color(hex: 0xEF4444), Color(hex: 0xF87171)]
    case .legendary: return [Color(hex: 0x8B5CF6), Color(hex: 0xA78BFA)]
    default: return [Color(hex: 0x94A3B8), Color(hex: 0xCBD5E1)]
    }
}

private func diffLabel(_ d: DungeonDifficulty) -> String {
    switch d {
    case .beginner: return "初級"
    case .intermediate: return "中級"
    case .advanced: return "上級"
    case .expert: return "超級"
    case .legendary: return "伝説"
    default: return "?"
    }
}

private func catEmoji(_ c: DungeonCategory) -> String {
    switch c {
    case .general: return "📚"
    case .math: return "🔢"
    case .science: return "🔬"
    case .language: return "🌍"
    case .programming: return "💻"
    case .creative: return "🎨"
    default: return "📚"
    }
}

// MARK: - メイン画面

struct QuestScreenView: View {
    private let questViewModel = KoinHelperKt.getQuestViewModel()
    private let homeViewModel = KoinHelperKt.getHomeViewModel()

    @State private var questState: QuestUiState?
    @State private var homeState: HomeUiState?
    @State private var showDetail = false
    @State private var detailDungeon: Dungeon_? = nil
    @State private var didAutoSelect = false

    private var dungeons: [Dungeon_] { (questState?.dungeons as? [Dungeon_]) ?? [] }
    private var available: [Dungeon_] { dungeons.filter { !$0.isLocked } }
    private var locked: [Dungeon_] { dungeons.filter { $0.isLocked } }
    private var selectedId: String? { homeState?.selectedDungeonId }

    var body: some View {
        ZStack {
            LinearGradient(colors: [bgDark, Color(hex: 0x0F172A)], startPoint: .top, endPoint: .bottom).ignoresSafeArea()

            if questState?.isLoading == true {
                ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
            } else {
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("冒 険").font(.system(size: 28, weight: .black)).foregroundColor(textW)
                                Text("ダンジョンを選んで勉強を始めよう").font(.caption).foregroundColor(textSub)
                            }
                            Spacer()
                        }
                        .padding(.horizontal, 20).padding(.top, 16)

                        if !available.isEmpty {
                            Text("🗺️ 挑戦可能").font(.system(size: 14, weight: .bold)).foregroundColor(accentCyan).padding(.horizontal, 20)
                            ForEach(available, id: \.id) { d in
                                DungeonCardView(dungeon: d, isSelected: d.id == selectedId) {
                                    detailDungeon = d; showDetail = true
                                }.padding(.horizontal, 20)
                            }
                        }

                        if !locked.isEmpty {
                            Text("🔒 未解放").font(.system(size: 14, weight: .bold)).foregroundColor(textSub).padding(.horizontal, 20)
                            ForEach(locked, id: \.id) { d in
                                DungeonCardView(dungeon: d, isSelected: false, isLocked: true) {}
                                    .padding(.horizontal, 20)
                            }
                        }

                        Spacer().frame(height: 100)
                    }
                }
            }

            if showDetail, let dungeon = detailDungeon {
                DungeonDetailOverlay(dungeon: dungeon, isSelected: dungeon.id == selectedId, onDismiss: {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) { showDetail = false }
                }, onSelect: {
                    let img = String(dungeon.imageUrl).trimmingCharacters(in: .whitespacesAndNewlines)
                    homeViewModel.onIntent(intent: HomeIntentSelectDungeon(
                        id: dungeon.id,
                        name: dungeon.name,
                        imageUrl: img.isEmpty ? nil : img
                    ))
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) { showDetail = false }
                })
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: showDetail)
        .onAppear {
            questViewModel.onIntent(intent: QuestIntentRefreshDungeons())
        }
        .onReceive(Timer.publish(every: 0.3, on: .main, in: .common).autoconnect()) { _ in
            self.questState = questViewModel.uiState.value as? QuestUiState
            self.homeState = homeViewModel.uiState.value as? HomeUiState
            autoSelectIfNeeded()
        }
    }

    private func autoSelectIfNeeded() {
        guard !didAutoSelect, !available.isEmpty else { return }
        let currentId = homeState?.selectedDungeonId
        let hasValid = currentId != nil && available.contains(where: { $0.id == currentId })
        if !hasValid {
            let first = available[0]
            let img = String(first.imageUrl).trimmingCharacters(in: .whitespacesAndNewlines)
            homeViewModel.onIntent(intent: HomeIntentSelectDungeon(id: first.id, name: first.name, imageUrl: img.isEmpty ? nil : img))
            didAutoSelect = true
        } else {
            didAutoSelect = true
        }
    }
}

// MARK: - ダンジョンカード

private struct DungeonCardView: View {
    let dungeon: Dungeon_
    var isSelected: Bool = false
    var isLocked: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 0) {
                let banner = String(dungeon.imageUrl).trimmingCharacters(in: .whitespacesAndNewlines)
                if !banner.isEmpty, let u = URL(string: banner) {
                    ZStack(alignment: .bottom) {
                        AsyncImage(url: u) { phase in
                            switch phase {
                            case .success(let img): img.resizable().scaledToFill()
                            case .failure: Color(hex: 0x1A2744)
                            case .empty: Color(hex: 0x1A2744)
                            @unknown default: Color(hex: 0x1A2744)
                            }
                        }
                        .frame(height: 92)
                        .clipped()
                        LinearGradient(colors: [.clear, bgCard.opacity(0.98)], startPoint: .top, endPoint: .bottom)
                            .frame(height: 40)
                    }
                    .clipShape(RoundedCorner(radius: 16, corners: [.topLeft, .topRight]))
                }
                HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(LinearGradient(colors: diffGrad(dungeon.difficulty).map { $0.opacity(isSelected ? 0.6 : 0.35) }, startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 52, height: 52)
                    Text(isLocked ? "🔒" : (dungeon.iconEmoji ?? "🏰")).font(.system(size: 26))
                }

                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(dungeon.name).font(.system(size: 16, weight: .bold)).foregroundColor(textW).lineLimit(1)
                        Text(diffLabel(dungeon.difficulty)).font(.system(size: 10, weight: .bold))
                            .foregroundColor(diffColor(dungeon.difficulty)).padding(.horizontal, 6).padding(.vertical, 2)
                            .background(diffColor(dungeon.difficulty).opacity(0.2)).cornerRadius(6)
                        if isSelected {
                            Text("選択中").font(.system(size: 9, weight: .heavy))
                                .foregroundColor(.white).padding(.horizontal, 6).padding(.vertical, 2)
                                .background(accentCyan).cornerRadius(6)
                        }
                    }
                    Text(dungeon.description_).font(.system(size: 11)).foregroundColor(textSub).lineLimit(1)
                    HStack(spacing: 6) {
                        chipText("🕐 \(dungeon.recommendedMinutes)分")
                        chipText("📍 \(dungeon.totalStages)F")
                        chipText("\(catEmoji(dungeon.category)) \(dungeon.category.label)")
                    }
                }
                Spacer()
                if !isLocked {
                    Image(systemName: "chevron.right").font(.system(size: 12, weight: .bold)).foregroundColor(textSub)
                }
                }
            }
            .padding(14)
            .background(isSelected ? accentBlue.opacity(0.12) : bgCard)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? accentCyan.opacity(0.6) : diffColor(dungeon.difficulty).opacity(0.15), lineWidth: isSelected ? 1.5 : 1)
            )
            .opacity(isLocked ? 0.4 : 1)
        }
        .buttonStyle(.plain).disabled(isLocked)
    }

    private func chipText(_ text: String) -> some View {
        Text(text).font(.system(size: 10)).foregroundColor(textSub).padding(.horizontal, 5).padding(.vertical, 2).background(bgSurface).cornerRadius(4)
    }
}

// MARK: - ダンジョン詳細

private struct DungeonDetailOverlay: View {
    let dungeon: Dungeon_
    let isSelected: Bool
    let onDismiss: () -> Void
    let onSelect: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { onDismiss() }

            VStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 2).fill(Color(hex: 0x475569)).frame(width: 40, height: 4).padding(.top, 12)

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 16) {
                        let hero = String(dungeon.imageUrl).trimmingCharacters(in: .whitespacesAndNewlines)
                        ZStack {
                            if !hero.isEmpty, let u = URL(string: hero) {
                                AsyncImage(url: u) { phase in
                                    switch phase {
                                    case .success(let img): img.resizable().scaledToFill()
                                    case .failure: LinearGradient(colors: diffGrad(dungeon.difficulty).map { $0.opacity(0.35) }, startPoint: .topLeading, endPoint: .bottomTrailing)
                                    case .empty: Color(hex: 0x1A2744)
                                    @unknown default: Color(hex: 0x1A2744)
                                    }
                                }
                                .frame(maxWidth: .infinity).frame(height: 176)
                                .clipped()
                                Color.black.opacity(0.45)
                            } else {
                                LinearGradient(colors: diffGrad(dungeon.difficulty).map { $0.opacity(0.35) }, startPoint: .topLeading, endPoint: .bottomTrailing)
                                    .frame(maxWidth: .infinity).frame(height: 176)
                            }
                            VStack(spacing: 8) {
                                Text(dungeon.iconEmoji ?? "🏰").font(.system(size: 48))
                                Text(dungeon.name).font(.system(size: 22, weight: .heavy)).foregroundColor(textW)
                                HStack(spacing: 8) {
                                    Text(diffLabel(dungeon.difficulty)).font(.system(size: 12, weight: .bold)).foregroundColor(diffColor(dungeon.difficulty))
                                        .padding(.horizontal, 10).padding(.vertical, 3).background(diffColor(dungeon.difficulty).opacity(0.25)).cornerRadius(8)
                                    Text("\(catEmoji(dungeon.category)) \(dungeon.category.label)").font(.system(size: 12)).foregroundColor(textSub)
                                }
                                if isSelected {
                                    Text("✅ 現在選択中").font(.system(size: 12, weight: .bold)).foregroundColor(accentCyan)
                                        .padding(.horizontal, 12).padding(.vertical, 4).background(accentCyan.opacity(0.2)).cornerRadius(8)
                                }
                            }
                            .padding(.vertical, 16)
                        }
                        .frame(maxWidth: .infinity)
                        .clipShape(RoundedRectangle(cornerRadius: 20))
                        .padding(.horizontal, 16)

                        Text(dungeon.description_).font(.system(size: 14)).foregroundColor(textSub).padding(.horizontal, 20).lineSpacing(4)

                        VStack(alignment: .leading, spacing: 10) {
                            Text("ダンジョン情報").font(.system(size: 14, weight: .bold)).foregroundColor(textW)
                            infoRow("推奨時間", "🕐 \(dungeon.recommendedMinutes)分")
                            infoRow("総ステージ", "📍 \(dungeon.totalStages)F")
                            infoRow("難易度", String(repeating: "⭐", count: Int(dungeon.difficulty.stars)))
                        }
                        .padding(16).background(bgSurface).cornerRadius(14).padding(.horizontal, 16)

                        VStack(alignment: .leading, spacing: 10) {
                            Text("🎁 報酬").font(.system(size: 14, weight: .bold)).foregroundColor(textW)
                            HStack {
                                Spacer()
                                VStack(spacing: 4) {
                                    Text("✨").font(.system(size: 24))
                                    Text("\(dungeon.rewards.exp) EXP").font(.system(size: 13, weight: .bold)).foregroundColor(Color(hex: 0x34D399))
                                }
                                Spacer()
                            }
                        }
                        .padding(16).background(bgSurface).cornerRadius(14).padding(.horizontal, 16)

                        Button(action: onSelect) {
                            HStack(spacing: 8) {
                                Image(systemName: isSelected ? "checkmark.circle.fill" : "play.fill").font(.system(size: 14, weight: .bold))
                                Text(isSelected ? "選択済み" : "このダンジョンを選択する").font(.system(size: 15, weight: .bold))
                            }
                            .foregroundColor(.white).frame(maxWidth: .infinity).frame(height: 52)
                            .background(LinearGradient(colors: isSelected ? [accentCyan, accentBlue] : [accentBlue, accentIndigo], startPoint: .leading, endPoint: .trailing))
                            .cornerRadius(16)
                        }
                        .padding(.horizontal, 16)

                        Spacer().frame(height: 100)
                    }.padding(.top, 8)
                }
            }
            .frame(maxHeight: UIScreen.main.bounds.height * 0.8)
            .background(bgCard).cornerRadius(28, corners: [.topLeft, .topRight])
        }
    }

    private func infoRow(_ label: String, _ value: String) -> some View {
        HStack { Text(label).font(.system(size: 12)).foregroundColor(textSub); Spacer(); Text(value).font(.system(size: 12, weight: .bold)).foregroundColor(textW) }
    }
}

private struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity; var corners: UIRectCorner = .allCorners
    func path(in rect: CGRect) -> Path { Path(UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius)).cgPath) }
}

private extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View { clipShape(RoundedCorner(radius: radius, corners: corners)) }
}

private typealias Dungeon_ = Shared.Dungeon
