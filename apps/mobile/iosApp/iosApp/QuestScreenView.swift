import SwiftUI
import Shared

// MARK: - データモデル

enum DungeonDifficulty: Int, CaseIterable {
    case beginner = 1, intermediate = 2, advanced = 3, expert = 4, legendary = 5
    var label: String {
        switch self {
        case .beginner: return "初級"; case .intermediate: return "中級"
        case .advanced: return "上級"; case .expert: return "超級"; case .legendary: return "伝説"
        }
    }
    var color: Color {
        switch self {
        case .beginner: return Color(hex: 0x34D399); case .intermediate: return Color(hex: 0x60A5FA)
        case .advanced: return Color(hex: 0xFBBF24); case .expert: return Color(hex: 0xF87171)
        case .legendary: return Color(hex: 0xA78BFA)
        }
    }
    var gradient: [Color] {
        switch self {
        case .beginner: return [Color(hex: 0x10B981), Color(hex: 0x34D399)]
        case .intermediate: return [Color(hex: 0x3B82F6), Color(hex: 0x60A5FA)]
        case .advanced: return [Color(hex: 0xF59E0B), Color(hex: 0xFBBF24)]
        case .expert: return [Color(hex: 0xEF4444), Color(hex: 0xF87171)]
        case .legendary: return [Color(hex: 0x8B5CF6), Color(hex: 0xA78BFA)]
        }
    }
}

enum DungeonCategory: String {
    case general, math, science, language, programming, creative
    var label: String {
        switch self {
        case .general: return "総合"; case .math: return "数学"; case .science: return "理科"
        case .language: return "語学"; case .programming: return "プログラミング"; case .creative: return "クリエイティブ"
        }
    }
    var emoji: String {
        switch self {
        case .general: return "📚"; case .math: return "🔢"; case .science: return "🔬"
        case .language: return "🌍"; case .programming: return "💻"; case .creative: return "🎨"
        }
    }
}

struct DungeonReward {
    let gold: Int; let exp: Int; let gachaStones: Int; let bonusItemName: String?; let bonusItemDropRate: Float
    init(gold: Int, exp: Int, gachaStones: Int = 0, bonusItemName: String? = nil, bonusItemDropRate: Float = 0) {
        self.gold = gold; self.exp = exp; self.gachaStones = gachaStones; self.bonusItemName = bonusItemName; self.bonusItemDropRate = bonusItemDropRate
    }
}

struct Dungeon: Identifiable {
    let id: String; let name: String; let description: String; let difficulty: DungeonDifficulty
    let category: DungeonCategory; let totalStages: Int; let clearedStages: Int; let recommendedMinutes: Int
    let rewards: DungeonReward; let iconEmoji: String; let isLocked: Bool
    var isCleared: Bool { clearedStages >= totalStages }
}

private let defaultDungeons: [Dungeon] = [
    Dungeon(id: "forest_of_beginnings", name: "はじまりの森", description: "新米冒険者の修行場。穏やかな森で基礎を固めよう。", difficulty: .beginner, category: .general, totalStages: 10, clearedStages: 5, recommendedMinutes: 25, rewards: DungeonReward(gold: 100, exp: 50, gachaStones: 5), iconEmoji: "🌲", isLocked: false),
    Dungeon(id: "crystal_cave", name: "水晶の洞窟", description: "輝く水晶に囲まれた神秘的な洞窟。集中力が試される。", difficulty: .intermediate, category: .math, totalStages: 15, clearedStages: 3, recommendedMinutes: 30, rewards: DungeonReward(gold: 200, exp: 120, gachaStones: 10, bonusItemName: "知恵のかけら", bonusItemDropRate: 0.15), iconEmoji: "💎", isLocked: false),
    Dungeon(id: "flame_tower", name: "炎の塔", description: "灼熱の試練が待つ高層塔。長時間の集中力が鍵となる。", difficulty: .advanced, category: .science, totalStages: 20, clearedStages: 0, recommendedMinutes: 45, rewards: DungeonReward(gold: 350, exp: 200, gachaStones: 15, bonusItemName: "炎の紋章", bonusItemDropRate: 0.10), iconEmoji: "🔥", isLocked: false),
    Dungeon(id: "sky_sanctuary", name: "天空の聖域", description: "雲の上に広がる聖なる修行場。精神統一が求められる。", difficulty: .expert, category: .language, totalStages: 25, clearedStages: 0, recommendedMinutes: 60, rewards: DungeonReward(gold: 500, exp: 350, gachaStones: 25, bonusItemName: "天翼のペンダント", bonusItemDropRate: 0.08), iconEmoji: "⛅", isLocked: true),
    Dungeon(id: "code_labyrinth", name: "コードの迷宮", description: "プログラミングの論理で解き進む知的ダンジョン。", difficulty: .intermediate, category: .programming, totalStages: 15, clearedStages: 7, recommendedMinutes: 30, rewards: DungeonReward(gold: 250, exp: 150, gachaStones: 12, bonusItemName: "バグ退治の書", bonusItemDropRate: 0.20), iconEmoji: "🏰", isLocked: false),
    Dungeon(id: "abyss_of_knowledge", name: "深淵の図書館", description: "古代の知識が眠る禁断の地。伝説級の試練に挑め。", difficulty: .legendary, category: .general, totalStages: 50, clearedStages: 0, recommendedMinutes: 120, rewards: DungeonReward(gold: 1000, exp: 800, gachaStones: 50, bonusItemName: "叡智の王冠", bonusItemDropRate: 0.03), iconEmoji: "📖", isLocked: true)
]

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

// MARK: - メイン画面

struct QuestScreenView: View {
    @State private var selectedDungeon: Dungeon? = nil
    @State private var showDetail = false
    private let homeViewModel = KoinHelperKt.getHomeViewModel()

    private var unlockedDungeons: [Dungeon] { defaultDungeons.filter { !$0.isLocked } }
    private var lockedDungeons: [Dungeon] { defaultDungeons.filter { $0.isLocked } }

    var body: some View {
        ZStack {
            LinearGradient(colors: [bgDark, Color(hex: 0x0F172A)], startPoint: .top, endPoint: .bottom).ignoresSafeArea()

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("冒 険")
                                .font(.system(size: 28, weight: .black))
                                .foregroundColor(textW)
                            Text("ダンジョンを選んで勉強を始めよう")
                                .font(.caption).foregroundColor(textSub)
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 20).padding(.top, 16)

                    if !unlockedDungeons.isEmpty {
                        Text("🗺️ 挑戦可能").font(.system(size: 14, weight: .bold)).foregroundColor(accentCyan).padding(.horizontal, 20)
                        ForEach(unlockedDungeons) { d in
                            DungeonCardView(dungeon: d) { selectedDungeon = d; showDetail = true }
                                .padding(.horizontal, 20)
                        }
                    }

                    if !lockedDungeons.isEmpty {
                        Text("🔒 未解放").font(.system(size: 14, weight: .bold)).foregroundColor(textSub).padding(.horizontal, 20)
                        ForEach(lockedDungeons) { d in
                            DungeonCardView(dungeon: d, isLocked: true) {}
                                .padding(.horizontal, 20)
                        }
                    }

                    Spacer().frame(height: 100)
                }
            }

            if showDetail, let dungeon = selectedDungeon {
                DungeonDetailOverlay(dungeon: dungeon, onDismiss: {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) { showDetail = false }
                }, onSelect: {
                    homeViewModel.onIntent(intent: HomeIntentSelectDungeon(id: dungeon.id, name: dungeon.name))
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) { showDetail = false }
                })
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: showDetail)
    }
}

// MARK: - ダンジョンカード

private struct DungeonCardView: View {
    let dungeon: Dungeon
    var isLocked: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(LinearGradient(colors: dungeon.difficulty.gradient.map { $0.opacity(0.35) }, startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 52, height: 52)
                    Text(isLocked ? "🔒" : dungeon.iconEmoji).font(.system(size: 26))
                }

                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Text(dungeon.name).font(.system(size: 16, weight: .bold)).foregroundColor(textW).lineLimit(1)
                        Text(dungeon.difficulty.label).font(.system(size: 10, weight: .bold))
                            .foregroundColor(dungeon.difficulty.color).padding(.horizontal, 6).padding(.vertical, 2)
                            .background(dungeon.difficulty.color.opacity(0.2)).cornerRadius(6)
                    }
                    Text(dungeon.description).font(.system(size: 11)).foregroundColor(textSub).lineLimit(1)
                    HStack(spacing: 6) {
                        chipText("🕐 \(dungeon.recommendedMinutes)分")
                        chipText("📍 \(dungeon.totalStages)F")
                        chipText("✨ \(dungeon.rewards.exp)EXP")
                    }
                }
                Spacer()
                if !isLocked {
                    Image(systemName: "chevron.right").font(.system(size: 12, weight: .bold)).foregroundColor(textSub)
                }
            }
            .padding(14)
            .background(bgCard)
            .cornerRadius(16)
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(dungeon.difficulty.color.opacity(0.15), lineWidth: 1))
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
    let dungeon: Dungeon; let onDismiss: () -> Void; let onSelect: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.6).ignoresSafeArea().onTapGesture { onDismiss() }

            VStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 2).fill(Color(hex: 0x475569)).frame(width: 40, height: 4).padding(.top, 12)

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 16) {
                        VStack(spacing: 8) {
                            Text(dungeon.iconEmoji).font(.system(size: 56))
                            Text(dungeon.name).font(.system(size: 22, weight: .heavy)).foregroundColor(textW)
                            HStack(spacing: 8) {
                                Text(dungeon.difficulty.label).font(.system(size: 12, weight: .bold)).foregroundColor(dungeon.difficulty.color)
                                    .padding(.horizontal, 10).padding(.vertical, 3).background(dungeon.difficulty.color.opacity(0.2)).cornerRadius(8)
                                Text("\(dungeon.category.emoji) \(dungeon.category.label)").font(.system(size: 12)).foregroundColor(textSub)
                            }
                        }
                        .frame(maxWidth: .infinity).padding(.vertical, 20)
                        .background(LinearGradient(colors: dungeon.difficulty.gradient.map { $0.opacity(0.15) }, startPoint: .topLeading, endPoint: .bottomTrailing))
                        .cornerRadius(20).padding(.horizontal, 16)

                        Text(dungeon.description).font(.system(size: 14)).foregroundColor(textSub).padding(.horizontal, 20).lineSpacing(4)

                        VStack(alignment: .leading, spacing: 10) {
                            Text("ダンジョン情報").font(.system(size: 14, weight: .bold)).foregroundColor(textW)
                            infoRow("推奨時間", "🕐 \(dungeon.recommendedMinutes)分")
                            infoRow("総ステージ", "📍 \(dungeon.totalStages)F")
                            infoRow("難易度", String(repeating: "⭐", count: dungeon.difficulty.rawValue))
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
                                Image(systemName: "play.fill").font(.system(size: 14, weight: .bold))
                                Text("このダンジョンに出発する").font(.system(size: 15, weight: .bold))
                            }
                            .foregroundColor(.white).frame(maxWidth: .infinity).frame(height: 52)
                            .background(LinearGradient(colors: [accentBlue, accentIndigo], startPoint: .leading, endPoint: .trailing))
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
