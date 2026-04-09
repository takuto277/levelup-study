import SwiftUI
import Shared

// MARK: - データモデル（KMP QuestUiState のミラー）

enum DungeonDifficulty: Int, CaseIterable {
    case beginner = 1
    case intermediate = 2
    case advanced = 3
    case expert = 4
    case legendary = 5

    var label: String {
        switch self {
        case .beginner: return "初級"
        case .intermediate: return "中級"
        case .advanced: return "上級"
        case .expert: return "超級"
        case .legendary: return "伝説"
        }
    }

    var color: Color {
        switch self {
        case .beginner: return Color(hex: 0x10B981)
        case .intermediate: return Color(hex: 0x3B82F6)
        case .advanced: return Color(hex: 0xF59E0B)
        case .expert: return Color(hex: 0xEF4444)
        case .legendary: return Color(hex: 0x8B5CF6)
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
        case .general: return "総合"
        case .math: return "数学"
        case .science: return "理科"
        case .language: return "語学"
        case .programming: return "プログラミング"
        case .creative: return "クリエイティブ"
        }
    }

    var emoji: String {
        switch self {
        case .general: return "📚"
        case .math: return "🔢"
        case .science: return "🔬"
        case .language: return "🌍"
        case .programming: return "💻"
        case .creative: return "🎨"
        }
    }
}

struct DungeonReward {
    let gold: Int
    let exp: Int
    let gachaStones: Int
    let bonusItemName: String?
    let bonusItemDropRate: Float

    init(gold: Int, exp: Int, gachaStones: Int = 0, bonusItemName: String? = nil, bonusItemDropRate: Float = 0) {
        self.gold = gold
        self.exp = exp
        self.gachaStones = gachaStones
        self.bonusItemName = bonusItemName
        self.bonusItemDropRate = bonusItemDropRate
    }
}

struct Dungeon: Identifiable {
    let id: String
    let name: String
    let description: String
    let difficulty: DungeonDifficulty
    let category: DungeonCategory
    let totalStages: Int
    let clearedStages: Int
    let recommendedMinutes: Int
    let rewards: DungeonReward
    let iconEmoji: String
    let isLocked: Bool

    var progress: Float {
        totalStages > 0 ? Float(clearedStages) / Float(totalStages) : 0
    }

    var isCleared: Bool {
        clearedStages >= totalStages
    }
}

// MARK: - デフォルトダンジョン一覧

private let defaultDungeons: [Dungeon] = [
    Dungeon(id: "forest_of_beginnings", name: "はじまりの森",
            description: "新米冒険者の修行場。穏やかな森で基礎を固めよう。",
            difficulty: .beginner, category: .general,
            totalStages: 10, clearedStages: 5, recommendedMinutes: 25,
            rewards: DungeonReward(gold: 100, exp: 50, gachaStones: 5),
            iconEmoji: "🌲", isLocked: false),
    Dungeon(id: "crystal_cave", name: "水晶の洞窟",
            description: "輝く水晶に囲まれた神秘的な洞窟。集中力が試される。",
            difficulty: .intermediate, category: .math,
            totalStages: 15, clearedStages: 3, recommendedMinutes: 30,
            rewards: DungeonReward(gold: 200, exp: 120, gachaStones: 10, bonusItemName: "知恵のかけら", bonusItemDropRate: 0.15),
            iconEmoji: "💎", isLocked: false),
    Dungeon(id: "flame_tower", name: "炎の塔",
            description: "灼熱の試練が待つ高層塔。長時間の集中力が鍵となる。",
            difficulty: .advanced, category: .science,
            totalStages: 20, clearedStages: 0, recommendedMinutes: 45,
            rewards: DungeonReward(gold: 350, exp: 200, gachaStones: 15, bonusItemName: "炎の紋章", bonusItemDropRate: 0.10),
            iconEmoji: "🔥", isLocked: false),
    Dungeon(id: "sky_sanctuary", name: "天空の聖域",
            description: "雲の上に広がる聖なる修行場。精神統一が求められる。",
            difficulty: .expert, category: .language,
            totalStages: 25, clearedStages: 0, recommendedMinutes: 60,
            rewards: DungeonReward(gold: 500, exp: 350, gachaStones: 25, bonusItemName: "天翼のペンダント", bonusItemDropRate: 0.08),
            iconEmoji: "⛅", isLocked: true),
    Dungeon(id: "code_labyrinth", name: "コードの迷宮",
            description: "プログラミングの論理で解き進む知的ダンジョン。",
            difficulty: .intermediate, category: .programming,
            totalStages: 15, clearedStages: 7, recommendedMinutes: 30,
            rewards: DungeonReward(gold: 250, exp: 150, gachaStones: 12, bonusItemName: "バグ退治の書", bonusItemDropRate: 0.20),
            iconEmoji: "🏰", isLocked: false),
    Dungeon(id: "abyss_of_knowledge", name: "深淵の図書館",
            description: "古代の知識が眠る禁断の地。伝説級の試練に挑め。",
            difficulty: .legendary, category: .general,
            totalStages: 50, clearedStages: 0, recommendedMinutes: 120,
            rewards: DungeonReward(gold: 1000, exp: 800, gachaStones: 50, bonusItemName: "叡智の王冠", bonusItemDropRate: 0.03),
            iconEmoji: "📖", isLocked: true)
]

// MARK: - Color Extension

private extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}

// MARK: - メイン画面

/// 冒険画面（タブ①: 左端）
/// ダンジョンを選択して勉強冒険に出発する
struct QuestScreenView: View {
    @State private var selectedDungeon: Dungeon? = nil
    @State private var showDetail = false

    // KMP HomeViewModel
    private let homeViewModel = KoinHelperKt.getHomeViewModel()

    private var inProgressDungeons: [Dungeon] {
        defaultDungeons.filter { !$0.isLocked && !$0.isCleared && $0.clearedStages > 0 }
    }

    private var availableDungeons: [Dungeon] {
        defaultDungeons.filter { !$0.isLocked && $0.clearedStages == 0 }
    }

    private var lockedDungeons: [Dungeon] {
        defaultDungeons.filter { $0.isLocked }
    }

    var body: some View {
        ZStack {
            Color(UIColor.systemGroupedBackground)
                .ignoresSafeArea()

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 12) {
                    // ヘッダー
                    questHeader

                    // 探索中
                    if !inProgressDungeons.isEmpty {
                        sectionTitle("⚔️ 探索中のダンジョン")
                        ForEach(inProgressDungeons) { dungeon in
                            DungeonCardView(dungeon: dungeon) {
                                selectedDungeon = dungeon
                                showDetail = true
                            }
                        }
                    }

                    // 挑戦可能
                    if !availableDungeons.isEmpty {
                        sectionTitle("🗺️ 挑戦可能なダンジョン")
                        ForEach(availableDungeons) { dungeon in
                            DungeonCardView(dungeon: dungeon) {
                                selectedDungeon = dungeon
                                showDetail = true
                            }
                        }
                    }

                    // ロック
                    if !lockedDungeons.isEmpty {
                        sectionTitle("🔒 未解放ダンジョン")
                        ForEach(lockedDungeons) { dungeon in
                            DungeonCardView(dungeon: dungeon, isLocked: true) {}
                        }
                    }

                    Spacer().frame(height: 100) // タブバー余白
                }
                .padding(.horizontal, 16)
            }

            // 詳細シート
            if showDetail, let dungeon = selectedDungeon {
                DungeonDetailOverlay(dungeon: dungeon) {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                        showDetail = false
                    }
                } onSelect: {
                    homeViewModel.onIntent(intent: HomeIntentSelectDungeon(id: dungeon.id, name: dungeon.name))
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                        showDetail = false
                    }
                }
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: showDetail)
    }

    // MARK: - Header

    private var questHeader: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Text("⚔️")
                    .font(.system(size: 28))
                Text("冒険")
                    .font(.system(size: 28, weight: .heavy))
                    .foregroundColor(Color(hex: 0x1E293B))
            }
            .padding(.top, 8)

            Text("ダンジョンを選んで勉強を始めよう")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 8)
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 16, weight: .bold))
            .foregroundColor(Color(hex: 0x1E293B))
            .padding(.top, 8)
    }
}

// MARK: - ダンジョンカード

private struct DungeonCardView: View {
    let dungeon: Dungeon
    var isLocked: Bool = false
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 0) {
                // 上部: アイコン + 情報
                HStack(spacing: 14) {
                    // アイコン
                    ZStack {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(LinearGradient(
                                colors: dungeon.difficulty.gradient,
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ))
                            .frame(width: 56, height: 56)
                        Text(isLocked ? "🔒" : dungeon.iconEmoji)
                            .font(.system(size: isLocked ? 24 : 28))
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 8) {
                            Text(dungeon.name)
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(Color(hex: 0x1E293B))
                                .lineLimit(1)

                            // 難易度バッジ
                            Text(dungeon.difficulty.label)
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(dungeon.difficulty.color)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(dungeon.difficulty.color.opacity(0.12))
                                .cornerRadius(8)
                        }

                        Text(dungeon.description)
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                            .lineLimit(1)

                        HStack(spacing: 8) {
                            metaChip("🕐 \(dungeon.recommendedMinutes)分")
                            metaChip("📍 \(dungeon.totalStages)F")
                            metaChip("\(dungeon.category.emoji) \(dungeon.category.label)")
                        }
                    }

                    Spacer()

                    if !isLocked {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(.secondary)
                    }
                }
                .padding(16)

                // 報酬プレビュー（EXPのみ表示）
                HStack(spacing: 16) {
                    rewardChip("✨", "\(dungeon.rewards.exp) EXP", Color(hex: 0x10B981))
                    if let bonus = dungeon.rewards.bonusItemName {
                        rewardChip("🎁", bonus, Color(hex: 0xEC4899))
                    }
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color(UIColor.tertiarySystemGroupedBackground).opacity(0.5))
            }
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(20)
            .shadow(color: dungeon.difficulty.color.opacity(0.10), radius: 8, x: 0, y: 4)
            .opacity(isLocked ? 0.5 : 1.0)
        }
        .buttonStyle(.plain)
        .disabled(isLocked)
    }

    private func metaChip(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11))
            .foregroundColor(.secondary)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color(UIColor.systemGroupedBackground))
            .cornerRadius(6)
    }

    private func rewardChip(_ emoji: String, _ text: String, _ color: Color) -> some View {
        HStack(spacing: 4) {
            Text(emoji).font(.system(size: 14))
            Text(text)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(color)
        }
    }
}

// MARK: - ダンジョン詳細オーバーレイ

private struct DungeonDetailOverlay: View {
    let dungeon: Dungeon
    let onDismiss: () -> Void
    let onSelect: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            // 背景タップで閉じる
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }

            // シートコンテンツ
            VStack(spacing: 0) {
                // ドラッグハンドル
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color(hex: 0xD1D5DB))
                    .frame(width: 40, height: 4)
                    .padding(.top, 12)

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 16) {
                        // ヒーロー
                        VStack(spacing: 8) {
                            Text(dungeon.iconEmoji)
                                .font(.system(size: 64))
                            Text(dungeon.name)
                                .font(.system(size: 24, weight: .heavy))
                                .foregroundColor(Color(hex: 0x1E293B))
                            HStack(spacing: 8) {
                                Text(dungeon.difficulty.label)
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(dungeon.difficulty.color)
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 3)
                                    .background(dungeon.difficulty.color.opacity(0.15))
                                    .cornerRadius(8)
                                Text("\(dungeon.category.emoji) \(dungeon.category.label)")
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 20)
                        .background(
                            LinearGradient(
                                colors: dungeon.difficulty.gradient.map { $0.opacity(0.12) },
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .cornerRadius(20)
                        .padding(.horizontal, 16)

                        // 説明
                        Text(dungeon.description)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 20)
                            .lineSpacing(4)

                        // ダンジョン情報
                        VStack(alignment: .leading, spacing: 12) {
                            Text("ダンジョン情報")
                                .font(.system(size: 15, weight: .bold))
                            infoRow("推奨時間", "🕐 \(dungeon.recommendedMinutes)分")
                            infoRow("総ステージ", "📍 \(dungeon.totalStages)ステージ")
                            infoRow("進行状況", "⚔️ \(dungeon.clearedStages) / \(dungeon.totalStages) クリア")
                            infoRow("難易度", String(repeating: "⭐", count: dungeon.difficulty.rawValue))
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(UIColor.systemGroupedBackground))
                        .cornerRadius(16)
                        .padding(.horizontal, 16)

                        // 報酬詳細
                        VStack(alignment: .leading, spacing: 12) {
                            Text("🎁 クリア報酬")
                                .font(.system(size: 15, weight: .bold))

                            HStack {
                                Spacer()
                                rewardDetailItem("✨", "経験値", "\(dungeon.rewards.exp) EXP", Color(hex: 0x10B981))
                                Spacer()
                            }

                            if let bonusName = dungeon.rewards.bonusItemName {
                                Divider()
                                HStack {
                                    Text("🎁").font(.system(size: 20))
                                    VStack(alignment: .leading) {
                                        Text(bonusName)
                                            .font(.system(size: 14, weight: .bold))
                                            .foregroundColor(Color(hex: 0xEC4899))
                                        Text("レアドロップ")
                                            .font(.system(size: 11))
                                            .foregroundColor(.secondary)
                                    }
                                    Spacer()
                                    Text("確率 \(Int(dungeon.rewards.bonusItemDropRate * 100))%")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(Color(hex: 0xEC4899))
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 4)
                                        .background(Color(hex: 0xEC4899).opacity(0.12))
                                        .cornerRadius(8)
                                }
                            }
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(hex: 0xFFFBEB, alpha: 1.0))
                        .cornerRadius(16)
                        .padding(.horizontal, 16)

                        // 出発ボタン
                        Button(action: {
                            onSelect()
                        }) {
                            HStack(spacing: 8) {
                                Image(systemName: "play.fill")
                                    .font(.system(size: 16, weight: .bold))
                                Text("このダンジョンに出発する")
                                    .font(.system(size: 16, weight: .bold))
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(
                                LinearGradient(
                                    colors: dungeon.difficulty.gradient,
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .cornerRadius(20)
                        }
                        .padding(.horizontal, 16)

                        Spacer().frame(height: 120) // タブバーとの重なりを防ぐための余白
                    }
                    .padding(.top, 8)
                }
            }
            .frame(maxHeight: UIScreen.main.bounds.height * 0.85) // 少し高くして内容を見やすくする
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(28, corners: [.topLeft, .topRight])
            .shadow(color: .black.opacity(0.15), radius: 20, x: 0, y: -5)
        }
    }

    private func infoRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 13))
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Color(hex: 0x1E293B))
        }
    }

    private func rewardDetailItem(_ emoji: String, _ label: String, _ value: String, _ color: Color) -> some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.12))
                    .frame(width: 48, height: 48)
                Text(emoji).font(.system(size: 22))
            }
            Text(value)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(color)
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - RoundedCorner Helper

private struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}

private extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

// MARK: - Preview

struct QuestScreenView_Previews: PreviewProvider {
    static var previews: some View {
        QuestScreenView()
    }
}
