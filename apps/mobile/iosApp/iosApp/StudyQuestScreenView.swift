import SwiftUI
import Shared
import UIKit

// MARK: - Sprite Helpers

private func hasSpriteAsset(_ name: String) -> Bool {
    UIImage(named: name) != nil
}

private func playerWalkFrameNames() -> [String] {
    ["sprite_player_walk_1", "sprite_player_walk_2"].filter { UIImage(named: $0) != nil }
}

private func hasPlayerWalkSprites() -> Bool {
    !playerWalkFrameNames().isEmpty
}

private func playerPrepAssetName() -> String? {
    if UIImage(named: "sprite_player_prep_1") != nil { return "sprite_player_prep_1" }
    if UIImage(named: "sprite_player_idle_1") != nil { return "sprite_player_idle_1" }
    return playerWalkFrameNames().first
}

@ViewBuilder
private func breakScenePlayerSprite(size: CGFloat) -> some View {
    if hasSpriteAsset("sprite_player_rest_1") {
        Image("sprite_player_rest_1")
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size)
    } else if hasSpriteAsset("sprite_player_idle_1") {
        Image("sprite_player_idle_1")
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size)
    } else if let prep = playerPrepAssetName() {
        Image(prep)
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size)
    } else {
        Text("🧙‍♂️")
            .font(.system(size: size * 0.42))
            .frame(width: size, height: size)
    }
}

/// 冒険ヘッダー用。未設定・general・総合は「総合」
private func studyQuestGenreDisplayLabel(_ genreId: String?) -> String {
    guard let raw = genreId?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty else { return "総合" }
    let lower = raw.lowercased()
    if lower == "general" || lower == "総合" || raw == "総合" { return "総合" }
    return raw
}

/// KMM の `STUDY_QUEST_ATTACK_CYCLE_SEC` と同値（戦闘ターン周期）
private let studyQuestAttackCycleSec: Int64 = 3

private func combatTurnMod(_ phaseTick: Int64) -> Int64 {
    var m = phaseTick % studyQuestAttackCycleSec
    if m < 0 { m += studyQuestAttackCycleSec }
    return m
}

private func enemySpriteFrames(_ key: String) -> [String] {
    var frames: [String] = []
    for i in 1...8 {
        let name = "sprite_enemy_\(key)_\(i)"
        if UIImage(named: name) != nil {
            frames.append(name)
        }
    }
    return frames
}

private func dungeonBgName(_ dungeonName: String?) -> String {
    guard let dn = dungeonName else { return "bg_dungeon_default" }
    if dn.contains("森") || dn.lowercased().contains("forest") { return "bg_dungeon_forest" }
    if dn.contains("洞窟") || dn.contains("水晶") || dn.lowercased().contains("cave") { return "bg_dungeon_cave" }
    if dn.contains("塔") || dn.contains("炎") || dn.lowercased().contains("tower") { return "bg_dungeon_tower" }
    return "bg_dungeon_default"
}

private func remoteDungeonBgUrl(uiState: StudyQuestUiState) -> URL? {
    guard !uiState.isTrainingGround else { return nil }
    guard let raw = uiState.dungeonImageUrl else { return nil }
    let s = String(describing: raw).trimmingCharacters(in: .whitespacesAndNewlines)
    if s.isEmpty { return nil }
    return URL(string: s)
}

struct AnimatedSpriteView: View {
    let frames: [String]
    let interval: TimeInterval
    let size: CGFloat

    @State private var currentFrame = 0

    init(frames: [String], interval: TimeInterval = 0.2, size: CGFloat = 120) {
        self.frames = frames
        self.interval = interval
        self.size = size
    }

    var body: some View {
        Group {
            if !frames.isEmpty {
                Image(frames[currentFrame])
                    .resizable()
                    .interpolation(.none)
                    .scaledToFit()
                    .frame(width: size, height: size, alignment: .bottom)
            }
        }
        .onAppear {
            guard frames.count > 1 else { return }
            Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { _ in
                currentFrame = (currentFrame + 1) % frames.count
            }
        }
    }
}

struct DungeonBackgroundView: View {
    let dungeonName: String?
    var isTrainingGround: Bool = false

    var body: some View {
        let bgName = isTrainingGround ? "bg_dungeon_training" : dungeonBgName(dungeonName)
        if UIImage(named: bgName) != nil {
            GeometryReader { geo in
                Image(bgName)
                    .resizable()
                    .scaledToFill()
                    .frame(width: geo.size.width, height: geo.size.height, alignment: .bottom)
                    .clipped()
                    .opacity(0.82)
            }
        }
    }
}

/// 戦闘中プレイヤー: tick%3 が 1→idle, 2→prep, 0→attack（ダメージ発生時）または idle
@ViewBuilder
private func iosCombatPlayerSprite(phaseTick: Int64, lastDamage: Int32, size: CGFloat) -> some View {
    let m = combatTurnMod(phaseTick)
    if m == 0 {
        if lastDamage > 0, UIImage(named: "sprite_player_attack_1") != nil {
            Image("sprite_player_attack_1")
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else if UIImage(named: "sprite_player_idle_1") != nil {
            Image("sprite_player_idle_1")
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else if let prep = playerPrepAssetName() {
            Image(prep)
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else {
            Text("🧙‍♂️").font(.system(size: 52)).frame(width: size, height: size, alignment: .bottom)
        }
    } else if m == 1 {
        if UIImage(named: "sprite_player_idle_1") != nil {
            Image("sprite_player_idle_1")
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else if let prep = playerPrepAssetName() {
            Image(prep)
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else {
            Text("🧙‍♂️").font(.system(size: 52)).frame(width: size, height: size, alignment: .bottom)
        }
    } else if let prep = playerPrepAssetName() {
        Image(prep)
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size, alignment: .bottom)
    } else if UIImage(named: "sprite_player_idle_1") != nil {
        Image("sprite_player_idle_1")
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size, alignment: .bottom)
    } else {
        Text("🧙‍♂️").font(.system(size: 52)).frame(width: size, height: size, alignment: .bottom)
    }
}

/// 訓練場: 毎ターン mod==0 で攻撃スプライト（lastDamage に依存しない）
@ViewBuilder
private func iosTrainingPlayerSprite(phaseTick: Int64, size: CGFloat) -> some View {
    let m = combatTurnMod(phaseTick)
    if m == 0 {
        if UIImage(named: "sprite_player_attack_1") != nil {
            Image("sprite_player_attack_1")
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else if UIImage(named: "sprite_player_idle_1") != nil {
            Image("sprite_player_idle_1")
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else if let prep = playerPrepAssetName() {
            Image(prep)
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else {
            Text("🧙‍♂️").font(.system(size: 52)).frame(width: size, height: size, alignment: .bottom)
        }
    } else if m == 1 {
        if UIImage(named: "sprite_player_idle_1") != nil {
            Image("sprite_player_idle_1")
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else if let prep = playerPrepAssetName() {
            Image(prep)
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size, alignment: .bottom)
        } else {
            Text("🧙‍♂️").font(.system(size: 52)).frame(width: size, height: size, alignment: .bottom)
        }
    } else if let prep = playerPrepAssetName() {
        Image(prep)
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size, alignment: .bottom)
    } else if UIImage(named: "sprite_player_idle_1") != nil {
        Image("sprite_player_idle_1")
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size, alignment: .bottom)
    } else {
        Text("🧙‍♂️").font(.system(size: 52)).frame(width: size, height: size, alignment: .bottom)
    }
}

/// 訓練場シーン（ResultBuilder の switch 内でローカル let を避けるため分離）
private struct TrainingGroundSceneView: View {
    let phaseTick: Int64
    let hasPlayerSprites: Bool

    private var strike: Bool {
        combatTurnMod(phaseTick) == 0
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.12)
            VStack {
                Spacer()
                ZStack(alignment: .bottom) {
                    if hasPlayerSprites {
                        iosTrainingPlayerSprite(phaseTick: phaseTick, size: 118)
                            .offset(x: strike ? 6 : 0)
                            .animation(.easeOut(duration: 0.08), value: strike)
                    } else {
                        Text("🧙‍♂️")
                            .font(.system(size: 56))
                            .offset(x: strike ? 6 : 0)
                            .animation(.easeOut(duration: 0.08), value: strike)
                    }
                    Group {
                        if UIImage(named: "prop_training_barrel") != nil {
                            Image("prop_training_barrel")
                                .resizable()
                                .interpolation(.none)
                                .scaledToFit()
                                .frame(width: 80, height: 80)
                        } else {
                            Text("🛢")
                                .font(.system(size: 58))
                        }
                    }
                    .rotationEffect(.degrees(strike ? -18 : 0))
                    .animation(.spring(response: 0.12, dampingFraction: 0.55), value: strike)
                    .offset(x: 72, y: 6)
                }
                .frame(maxWidth: .infinity)
                .padding(.bottom, adventureFloorInset)
            }
        }
    }
}

/// 遭遇〜戦闘：ステージ内のラベルなし。床ラインのみ。
private struct BattleConfrontationIOSView: View {
    let isAttackPhase: Bool
    let approach: CGFloat
    let phaseTick: Int64
    let hasPlayerSprites: Bool
    let playerWalkFrames: [String]
    let enemyFirstFrameName: String?
    let enemyEmoji: String
    let lastDamage: Int32
    let currentFloor: Int
    let totalFloors: Int32

    @State private var enemyNudge: CGFloat = 0

    var body: some View {
        let t = min(1.0, max(0.0, approach))
        let isStriking = isAttackPhase && combatTurnMod(phaseTick) == 0 && lastDamage > 0
        let isBossEncounter = currentFloor >= Int(totalFloors)
        let baseEnemy: CGFloat = 118
        let eW: CGFloat = isBossEncounter ? 236 : baseEnemy

        ZStack {
            Color.black.opacity(0.12)

            VStack(spacing: 0) {
                Spacer(minLength: 0)

                GeometryReader { geo in
                    let w = geo.size.width
                    let h = geo.size.height
                    let centerX = w / 2
                    let gap: CGFloat = 30
                    let pW: CGFloat = 118
                    let playerEndLeft = centerX - gap / 2 - pW
                    let enemyEndLeft = centerX + gap / 2 - (eW - baseEnemy) / 2
                    let playerStart: CGFloat = -32
                    let enemyStart = w + 88 + (eW - baseEnemy)
                    let playerLeft = playerStart + (playerEndLeft - playerStart) * t
                    let enemyLeft = enemyStart + (enemyEndLeft - enemyStart) * t
                    let strikeNudge: CGFloat = isStriking ? 6 : 0

                    ZStack(alignment: .bottomLeading) {
                        if hasPlayerSprites {
                            if isAttackPhase {
                                iosCombatPlayerSprite(phaseTick: phaseTick, lastDamage: lastDamage, size: pW)
                                    .padding(.leading, 16)
                                    .padding(.bottom, adventureFloorInset)
                                    .offset(x: playerLeft + strikeNudge)
                            } else if !playerWalkFrames.isEmpty {
                                AnimatedSpriteView(frames: playerWalkFrames, interval: 0.32, size: 118)
                                    .padding(.leading, 16)
                                    .padding(.bottom, adventureFloorInset)
                                    .offset(x: playerLeft)
                            } else {
                                iosCombatPlayerSprite(phaseTick: phaseTick, lastDamage: 0, size: pW)
                                    .padding(.leading, 16)
                                    .padding(.bottom, adventureFloorInset)
                                    .offset(x: playerLeft)
                            }
                        } else {
                            Text("🧙‍♂️")
                                .font(.system(size: 52))
                                .frame(width: pW, height: pW, alignment: .bottom)
                                .padding(.leading, 16)
                                .padding(.bottom, adventureFloorInset)
                                .offset(x: playerLeft)
                        }

                        Group {
                            if !isAttackPhase {
                                TimelineView(.animation(minimumInterval: 0.32, paused: false)) { ctx in
                                    let bob = Int(ctx.date.timeIntervalSinceReferenceDate / 0.32) % 2 == 0 ? CGFloat(-3) : 2
                                    enemySpriteOnly(eW: eW, cellH: eW, isBoss: isBossEncounter)
                                        .offset(x: enemyLeft, y: bob)
                                }
                            } else {
                                enemySpriteOnly(eW: eW, cellH: eW, isBoss: isBossEncounter)
                                    .offset(x: enemyLeft + enemyNudge)
                            }
                        }
                        .padding(.bottom, adventureFloorInset)
                        .onChange(of: phaseTick) { _, newTick in
                            guard isAttackPhase, combatTurnMod(newTick) == 1 else { return }
                            withAnimation(.easeOut(duration: 0.07)) {
                                enemyNudge = -12
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.09) {
                                withAnimation(.easeIn(duration: 0.09)) {
                                    enemyNudge = 0
                                }
                            }
                        }
                    }
                    .frame(width: w, height: h, alignment: .bottomLeading)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }

    /// 正方形セル内に収め、下端で床に接地（ボスはセル約 2 倍）
    @ViewBuilder
    private func enemySpriteOnly(eW: CGFloat, cellH: CGFloat, isBoss: Bool) -> some View {
        if let name = enemyFirstFrameName, UIImage(named: name) != nil {
            Image(name)
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: eW, height: cellH, alignment: .bottom)
        } else {
            Text(enemyEmoji)
                .font(.system(size: isBoss ? 112 : 56))
                .frame(width: eW, height: cellH, alignment: .bottom)
        }
    }
}

// MARK: - Color Helper

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

// MARK: - 定数

private let darkBg = Color(hex: 0x0F172A)
private let darkCard = Color(hex: 0x1E293B)
private let darkSurface = Color(hex: 0x334155)
private let accentBlue = Color(hex: 0x3B82F6)
private let accentIndigo = Color(hex: 0x6366F1)
private let fireRed = Color(hex: 0xEF4444)
private let fireOrange = Color(hex: 0xF59E0B)
private let emeraldGreen = Color(hex: 0x10B981)
private let purpleGlow = Color(hex: 0x8B5CF6)
private let textWhite = Color(hex: 0xF8FAFC)
private let textMuted = Color(hex: 0x94A3B8)
private let damageRed = Color(hex: 0xFF4444)
private let breakBg = Color(hex: 0x0C1E0C)
private let breakCard = Color(hex: 0x1A2E1A)
private let breakAccent = Color(hex: 0x34D399)
private let breakGlow = Color(hex: 0x10B981)

/// 背景の床帯に足を合わせる（値が大きいほどキャラが上に浮く）
private let adventureFloorInset: CGFloat = 20

private func questHpRatioColor(ratio: CGFloat) -> Color {
    if ratio > 0.5 { return emeraldGreen }
    if ratio > 0.25 { return fireOrange }
    return fireRed
}

/// ダンジョン名・ジャンル・F 階バッジと同系の枠付きカプセル（暗背景でも輪郭が見えるようにする）
private struct QuestTitleCapsule<Content: View>: View {
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(.horizontal, 11)
            .padding(.vertical, 6)
            .background(
                Capsule()
                    .fill(Color.white.opacity(0.12))
            )
            .overlay(
                Capsule()
                    .stroke(Color.white.opacity(0.28), lineWidth: 1)
            )
    }
}

private let questHpHeaderRowHeight: CGFloat = 18
private let questHpBarRowTotalHeight: CGFloat = 50

/// プレイヤー列・敵列で共通の HP ヘッダー＋バー＋中央ダメージフロート
private struct QuestHpBarStripView<Header: View>: View {
    let currentHp: Int32
    let maxHp: Int32
    let floatingDamage: Int32
    let adventurePhaseTick: Int64
    /// 0: プレイヤー攻撃秒, 1: 敵反撃秒でフロート再生
    let floatTriggerTurnMod: Int64
    /// 敵 HP 非表示時はバーを隠しつつ、与ダメージフロートだけ右列に出す
    var showChrome: Bool = true
    @ViewBuilder let header: () -> Header

    @State private var floatOffsetY: CGFloat = 0
    @State private var floatOpacity: Double = 0

    private var hpFillRatio: CGFloat {
        maxHp > 0 ? CGFloat(currentHp) / CGFloat(maxHp) : 0
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Group {
                if showChrome {
                    header()
                        .frame(maxWidth: .infinity, alignment: .leading)
                } else {
                    Color.clear.frame(height: questHpHeaderRowHeight)
                }
            }
            .frame(height: questHpHeaderRowHeight, alignment: .leading)

            ZStack {
                if showChrome {
                    VStack(spacing: 0) {
                        Spacer(minLength: 0)
                        RoundedRectangle(cornerRadius: 3)
                            .fill(darkSurface)
                            .frame(height: 6)
                            .overlay(alignment: .leading) {
                                GeometryReader { geo in
                                    RoundedRectangle(cornerRadius: 3)
                                        .fill(questHpRatioColor(ratio: hpFillRatio))
                                        .frame(width: max(0, geo.size.width * hpFillRatio), height: 6)
                                }
                            }
                    }
                    .frame(height: 30)
                } else {
                    Color.clear.frame(height: 30)
                }

                if floatOpacity > 0.02, floatingDamage > 0 {
                    Text("-\(floatingDamage)")
                        .font(.system(size: 15, weight: .heavy))
                        .foregroundColor(damageRed)
                        .offset(y: floatOffsetY)
                        .opacity(floatOpacity)
                }
            }
            .frame(height: 30)
        }
        .frame(height: questHpBarRowTotalHeight, alignment: .top)
        .fixedSize(horizontal: false, vertical: true)
        .onChange(of: adventurePhaseTick) { _, newTick in
            guard combatTurnMod(newTick) == floatTriggerTurnMod, floatingDamage > 0 else { return }
            floatOffsetY = 6
            floatOpacity = 1
            Task { @MainActor in
                withAnimation(.easeOut(duration: 0.52)) {
                    floatOffsetY = -20
                }
                try? await Task.sleep(nanoseconds: 180_000_000)
                withAnimation(.easeIn(duration: 0.42)) {
                    floatOpacity = 0
                }
            }
        }
    }
}

/// パーティ先頭名（KMM の文字列をそのまま表示、空は「冒険者」）
private func playerDisplayName(_ raw: String) -> String {
    let s = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    return s.isEmpty ? "冒険者" : s
}

// MARK: - メイン

struct StudyQuestScreenView: View {
    @Environment(\.dismiss) var dismiss
    let initialStudyMinutes: Int
    let genreId: String?
    let dungeonName: String?
    let dungeonImageUrl: String?
    let isTrainingGround: Bool

    private final class ViewModelHolder: ObservableObject {
        let viewModel: StudyQuestViewModel
        init() {
            self.viewModel = KoinHelperKt.getStudyQuestViewModel()
        }
    }

    @StateObject private var holder = ViewModelHolder()
    @State private var uiState: StudyQuestUiState
    @State private var pulsePhase = false
    @State private var walkPhase = false
    /// 遭遇シーンの左右接近（0→1）。Android の接近アニメと同じく約 2.8 秒
    @State private var confrontationApproach: CGFloat = 0
    @State private var didStartQuest = false

    init(initialStudyMinutes: Int, genreId: String? = nil, dungeonName: String? = nil, dungeonImageUrl: String? = nil, isTrainingGround: Bool = false) {
        self.initialStudyMinutes = initialStudyMinutes
        self.genreId = genreId
        self.dungeonName = dungeonName
        self.dungeonImageUrl = dungeonImageUrl
        self.isTrainingGround = isTrainingGround
        _uiState = State(initialValue: StudyQuestUiState(
            type: .study,
            status: .ready,
            targetStudyMinutes: Int32(initialStudyMinutes),
            targetBreakMinutes: 5,
            elapsedSeconds: 0,
            isOvertime: false,
            currentLog: [],
            displayTime: "\(initialStudyMinutes < 10 ? "0" : "")\(initialStudyMinutes):00",
            genreId: genreId,
            isTrainingGround: isTrainingGround,
            adventurePhase: .walking,
            adventurePhaseTick: 0,
            enemyName: "スライム",
            enemyEmoji: "🟢",
            enemySpriteKey: "slime",
            enemyHp: 100,
            enemyMaxHp: 100,
            lastDamage: 0,
            lastPlayerDamage: 0,
            defeatedCount: 0,
            normalDefeatCount: 0,
            bossDefeatCount: 0,
            serverRewards: [],
            serverSynced: nil,
            partyLeadName: "冒険者",
            partyLeadImageUrl: "",
            partyLeadUserCharacterId: "",
            dungeonName: dungeonName,
            dungeonImageUrl: dungeonImageUrl,
            currentFloor: 1,
            totalFloors: 10,
            floorClearCount: 0,
            playerHp: 100,
            playerMaxHp: 100,
            earnedXp: 0,
            earnedStones: 0,
            completedStudyElapsedSeconds: 0
        ))
    }

    var body: some View {
        let isBreak = uiState.type == .break_
        let bgColor = isBreak ? breakBg : darkBg

        ZStack {
            bgColor.ignoresSafeArea()
            mainQuestView
        }
        .onAppear {
            if !didStartQuest {
                didStartQuest = true
                holder.viewModel.onIntent(intent: StudyQuestIntentStartQuest(studyMinutes: Int32(initialStudyMinutes), genreId: genreId, dungeonName: dungeonName, isTrainingGround: isTrainingGround, dungeonImageUrl: dungeonImageUrl))
            }
            withAnimation(.easeInOut(duration: 1.5).repeatForever(autoreverses: true)) {
                pulsePhase = true
            }
            withAnimation(.easeInOut(duration: 0.6).repeatForever(autoreverses: true)) {
                walkPhase = true
            }
        }
        .onReceive(Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()) { _ in
            self.uiState = holder.viewModel.uiState.value as! StudyQuestUiState
        }
    }

    // MARK: - 休憩中：直前の勉強パートの結果（旧クエスト達成カード相当）

    private var breakAfterStudySummaryBlock: some View {
        let mins = max(1, Int(uiState.completedStudyElapsedSeconds / 60))
        return VStack(spacing: 10) {
            Text("📊 直前の冒険の結果")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(breakAccent)
            HStack {
                Spacer()
                rewardItem(emoji: "⏱", label: "集中時間", value: "\(mins)分")
                Spacer()
                rewardItem(emoji: "⭐", label: "経験値", value: "+\(uiState.earnedXp)")
                Spacer()
                rewardItem(emoji: "💀", label: "討伐数", value: "\(uiState.defeatedCount)体")
                Spacer()
                rewardItem(emoji: "💎", label: "ダイヤ", value: "+\(uiState.earnedStones)")
                Spacer()
            }
            if !uiState.serverRewards.isEmpty {
                Text(uiState.serverRewards.map { "\($0)" }.joined(separator: "  "))
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(textMuted)
            }
            if let kSynced = uiState.serverSynced {
                let synced = kSynced.boolValue
                Text(synced ? "サーバーに記録しました" : "サーバー未同期（あとで再試行）")
                    .font(.system(size: 10))
                    .foregroundColor(synced ? breakAccent : textMuted)
            }
        }
        .padding(14)
        .frame(maxWidth: .infinity)
        .background(breakCard.opacity(0.95))
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(breakAccent.opacity(0.35), lineWidth: 1)
        )
        .cornerRadius(18)
    }

    // MARK: - メインクエストビュー

    private var mainQuestView: some View {
        let isBreak = uiState.type == .break_
        let isOvertime = uiState.isOvertime
        let phase = uiState.adventurePhase
        let targetSec = isBreak ? Int(uiState.targetBreakMinutes) * 60 : Int(uiState.targetStudyMinutes) * 60
        let progress: Double = targetSec > 0 && !isOvertime
            ? min(Double(uiState.elapsedSeconds) / Double(targetSec), 1.0)
            : (isOvertime ? 1.0 : 0.0)
        let glowColor: Color = isOvertime ? purpleGlow : (isBreak ? breakGlow : accentBlue)

        let showEnemyHpBar = !isBreak && (phase == .encounter || phase == .attacking || phase == .training)

        let enemyFloatingDamage: Int32 = {
            if phase == .training {
                return combatTurnMod(uiState.adventurePhaseTick) == 0 ? 12 : 0
            }
            return uiState.lastDamage
        }()

        let dungeonDisplayName: String? = {
            guard !isBreak else { return nil }
            let fromState = uiState.dungeonName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if !fromState.isEmpty { return fromState }
            let fromInit = dungeonName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            return fromInit.isEmpty ? nil : fromInit
        }()

        return VStack(spacing: 0) {
            Spacer().frame(height: 52)

            HStack(alignment: .center, spacing: 8) {
                if isBreak {
                    Text("🌿 休憩")
                        .font(.system(size: 12, weight: .heavy, design: .rounded))
                        .foregroundColor(breakAccent)
                        .padding(.horizontal, 11)
                        .padding(.vertical, 6)
                        .background(
                            Capsule()
                                .fill(breakAccent.opacity(0.14))
                        )
                        .overlay(
                            Capsule()
                                .stroke(breakAccent.opacity(0.35), lineWidth: 1)
                        )
                    Spacer(minLength: 4)
                } else {
                    HStack(spacing: 6) {
                        QuestTitleCapsule {
                            Text(studyQuestGenreDisplayLabel(uiState.genreId))
                                .font(.system(size: 12, weight: .heavy, design: .rounded))
                                .foregroundColor(textWhite)
                                .lineLimit(1)
                                .minimumScaleFactor(0.75)
                        }
                        if uiState.isTrainingGround {
                            QuestTitleCapsule {
                                HStack(spacing: 4) {
                                    Text("⚔️")
                                        .font(.system(size: 11))
                                    Text("訓練場")
                                        .font(.system(size: 11, weight: .bold, design: .rounded))
                                        .foregroundColor(textWhite)
                                        .lineLimit(1)
                                }
                            }
                        } else if let dn = dungeonDisplayName {
                            QuestTitleCapsule {
                                HStack(spacing: 4) {
                                    Text("🏰")
                                        .font(.system(size: 11))
                                    Text(dn)
                                        .font(.system(size: 11, weight: .bold, design: .rounded))
                                        .foregroundColor(textWhite)
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.65)
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                HStack(spacing: 8) {
                    if isOvertime {
                        Text("⚡")
                            .font(.system(size: 12, weight: .heavy))
                            .foregroundColor(purpleGlow)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 6)
                            .background(purpleGlow.opacity(0.14))
                            .clipShape(Capsule())
                    }
                    if uiState.status == .paused {
                        Text("停止中")
                            .font(.system(size: 11, weight: .bold, design: .rounded))
                            .foregroundColor(accentBlue)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(accentBlue.opacity(0.14))
                            .clipShape(Capsule())
                    }
                    if !isBreak && !uiState.isTrainingGround {
                        Text("F\(uiState.currentFloor)/\(uiState.totalFloors)")
                            .font(.system(size: 11, weight: .heavy, design: .rounded))
                            .foregroundColor(textWhite)
                            .padding(.horizontal, 11)
                            .padding(.vertical, 6)
                            .background(darkSurface.opacity(0.9))
                            .clipShape(Capsule())
                        Text("💀 \(uiState.defeatedCount)")
                            .font(.system(size: 11, weight: .heavy, design: .rounded))
                            .foregroundColor(emeraldGreen)
                            .padding(.horizontal, 11)
                            .padding(.vertical, 6)
                            .background(emeraldGreen.opacity(0.14))
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal, 20)

            if isBreak {
                breakAfterStudySummaryBlock
                    .padding(.horizontal, 20)
                Spacer().frame(height: 10)
            }

            Spacer().frame(height: 12)

            Spacer(minLength: 8)

            VStack(spacing: 0) {
                if !isBreak {
                    HStack(alignment: .center, spacing: 8) {
                        QuestHpBarStripView(
                            currentHp: uiState.playerHp,
                            maxHp: uiState.playerMaxHp,
                            floatingDamage: uiState.lastPlayerDamage,
                            adventurePhaseTick: uiState.adventurePhaseTick,
                            floatTriggerTurnMod: 1
                        ) {
                            HStack {
                                Text(playerDisplayName("\(uiState.partyLeadName)"))
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(textMuted)
                                    .lineLimit(1)
                                    .minimumScaleFactor(0.6)
                                Spacer(minLength: 0)
                                Text("\(uiState.playerHp)/\(uiState.playerMaxHp)")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(textWhite)
                            }
                        }
                        .frame(maxWidth: .infinity)

                        Group {
                            if showEnemyHpBar {
                                QuestHpBarStripView(
                                    currentHp: uiState.enemyHp,
                                    maxHp: uiState.enemyMaxHp,
                                    floatingDamage: enemyFloatingDamage,
                                    adventurePhaseTick: uiState.adventurePhaseTick,
                                    floatTriggerTurnMod: 0
                                ) {
                                    HStack {
                                        Text("\(uiState.enemyName)")
                                            .font(.system(size: 10, weight: .bold))
                                            .foregroundColor(textMuted)
                                            .lineLimit(1)
                                            .minimumScaleFactor(0.6)
                                        Spacer(minLength: 0)
                                        Text("\(uiState.enemyHp)/\(uiState.enemyMaxHp)")
                                            .font(.system(size: 10, weight: .bold))
                                            .foregroundColor(textWhite)
                                    }
                                }
                            } else {
                                QuestHpBarStripView(
                                    currentHp: 0,
                                    maxHp: 1,
                                    floatingDamage: enemyFloatingDamage,
                                    adventurePhaseTick: uiState.adventurePhaseTick,
                                    floatTriggerTurnMod: 0,
                                    showChrome: false
                                ) {
                                    EmptyView()
                                }
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .frame(height: 52)
                    .padding(.horizontal, 20)
                }

                Spacer().frame(height: 8)

            // 冒険シーン（大型化でダンジョン潜入感を演出）
            ZStack {
                RoundedRectangle(cornerRadius: 24)
                    .fill(
                        isBreak
                            ? LinearGradient(colors: [Color(hex: 0x0A1F0A), Color(hex: 0x132613), Color(hex: 0x0A1F0A)], startPoint: .top, endPoint: .bottom)
                            : LinearGradient(colors: [Color(hex: 0x06060F), Color(hex: 0x0E1428), Color(hex: 0x1A1040)], startPoint: .top, endPoint: .bottom)
                    )

                if !isBreak {
                    if let u = remoteDungeonBgUrl(uiState: uiState) {
                        AsyncImage(url: u) { phase in
                            switch phase {
                            case .success(let img):
                                img.resizable().scaledToFill()
                            case .failure, .empty:
                                Color(hex: 0x0E1428)
                            @unknown default:
                                Color(hex: 0x0E1428)
                            }
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .overlay(Color.black.opacity(0.28))
                    } else {
                        let bgAssetName = uiState.isTrainingGround ? "bg_dungeon_training" : dungeonBgName(uiState.dungeonName)
                        if UIImage(named: bgAssetName) != nil {
                            DungeonBackgroundView(dungeonName: uiState.dungeonName, isTrainingGround: uiState.isTrainingGround)
                                .clipShape(RoundedRectangle(cornerRadius: 24))
                        } else {
                            VStack {
                                HStack {
                                    ForEach(0..<8, id: \.self) { _ in
                                        RoundedRectangle(cornerRadius: 2)
                                            .fill(textMuted.opacity(0.05))
                                            .frame(width: 40, height: 24)
                                    }
                                }
                                .padding(.top, 8)
                                Spacer()
                                HStack {
                                    Text("🔥").font(.system(size: 16)).opacity(pulsePhase ? 0.9 : 0.5)
                                    Spacer()
                                    Text("🔥").font(.system(size: 16)).opacity(pulsePhase ? 0.5 : 0.9)
                                }
                                .padding(.horizontal, 16)
                                Rectangle()
                                    .fill(
                                        LinearGradient(
                                            colors: [Color(hex: 0x2D1B0E), Color(hex: 0x1A1005)],
                                            startPoint: .top, endPoint: .bottom
                                        )
                                    )
                                    .frame(height: 30)
                                    .cornerRadius(0)
                            }
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                        }
                    }
                }

                if isBreak {
                    breakScene
                } else {
                    adventureScene
                }
            }
            .frame(height: 280)
            .padding(.horizontal, 16)
            .onChange(of: uiState.adventurePhase) { _, newPhase in
                if newPhase == .encounter {
                    confrontationApproach = 0
                    DispatchQueue.main.async {
                        withAnimation(.timingCurve(0.25, 0.1, 0.25, 1.0, duration: 2.8)) {
                            confrontationApproach = 1
                        }
                    }
                } else if newPhase == .attacking {
                    var tr = Transaction()
                    tr.disablesAnimations = true
                    withTransaction(tr) {
                        confrontationApproach = 1
                    }
                }
            }

            }

            Spacer(minLength: 8)

            Spacer().frame(height: 16)

            // タイマー
            HStack(spacing: 16) {
                // 進捗リング（小型）
                ZStack {
                    Circle()
                        .stroke(glowColor.opacity(0.15), lineWidth: 4)
                        .frame(width: 48, height: 48)
                    Circle()
                        .trim(from: 0, to: progress)
                        .stroke(glowColor, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                        .frame(width: 48, height: 48)
                        .rotationEffect(.degrees(-90))
                }

                Text(uiState.displayTime)
                    .font(.system(size: 48, weight: .heavy, design: .monospaced))
                    .foregroundColor(isOvertime ? purpleGlow : (isBreak ? breakAccent : textWhite))
            }

            if isOvertime {
                Text("延長戦！")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(purpleGlow.opacity(pulsePhase ? 0.9 : 0.4))
            }

            Spacer().frame(height: 12)

            // 直近ログ（1行のみ）
            if let lastLog = uiState.currentLog.last {
                Text(lastLog)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(isBreak ? breakAccent : accentBlue)
                    .lineLimit(1)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background((isBreak ? breakCard : darkCard).opacity(0.8))
                    .cornerRadius(12)
                    .padding(.horizontal, 24)
            }

            Spacer()

            // ボタン：休憩中は「終了する」「冒険へ」のみ。勉強中は一時停止 + 終了する。
            HStack(spacing: 12) {
                if isBreak {
                    Button(action: {
                        holder.viewModel.onIntent(intent: StudyQuestIntentStopQuest())
                        dismiss()
                    }) {
                        Text("終了する")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(textMuted)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(textMuted.opacity(0.3), lineWidth: 1)
                            )
                    }
                    Button(action: {
                        holder.viewModel.onIntent(intent: StudyQuestIntentNextSession())
                    }) {
                        Text("⚔️ 冒険へ")
                            .font(.system(size: 14, weight: .heavy))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(accentBlue)
                            .cornerRadius(16)
                    }
                } else {
                    Button(action: {
                        holder.viewModel.onIntent(intent: StudyQuestIntentTogglePause())
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: uiState.status == .running ? "pause.fill" : "play.fill")
                                .font(.system(size: 14))
                            Text(uiState.status == .running ? "一時停止" : "再開")
                                .font(.system(size: 14, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(uiState.status == .running ? darkSurface : accentBlue)
                        .cornerRadius(16)
                    }

                    Button(action: {
                        holder.viewModel.onIntent(intent: StudyQuestIntentEndQuest())
                    }) {
                        Text("🏁 終了する")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(emeraldGreen)
                            .cornerRadius(16)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
    }

    // MARK: - 冒険シーン

    private var adventureScene: some View {
        let phase = uiState.adventurePhase
        let hasPlayerSprites = hasPlayerWalkSprites()
        let enemyKey = uiState.enemySpriteKey
        let enemyFrames = enemySpriteFrames(enemyKey)
        let bgName = uiState.isTrainingGround ? "bg_dungeon_training" : dungeonBgName(uiState.dungeonName)
        let hasBg = UIImage(named: bgName) != nil
        return ZStack {
            if let u = remoteDungeonBgUrl(uiState: uiState) {
                AsyncImage(url: u) { phase in
                    switch phase {
                    case .success(let img):
                        img.resizable().scaledToFill()
                    case .failure, .empty:
                        Color(hex: 0x06060F)
                    @unknown default:
                        Color(hex: 0x06060F)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()
                .clipShape(RoundedRectangle(cornerRadius: 24))
                .overlay(Color.black.opacity(0.28))
            } else if hasBg {
                DungeonBackgroundView(dungeonName: uiState.dungeonName, isTrainingGround: uiState.isTrainingGround)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 24))
            }

            switch phase {
            case .walking:
                VStack {
                    Spacer()
                    HStack(alignment: .bottom) {
                        if hasPlayerSprites {
                            AnimatedSpriteView(frames: playerWalkFrameNames(), interval: 0.32, size: 118)
                                .padding(.leading, 16)
                                .padding(.bottom, adventureFloorInset)
                        } else {
                            Text("🧙‍♂️")
                                .font(.system(size: 56))
                                .padding(.leading, 16)
                                .padding(.bottom, adventureFloorInset)
                        }
                        Spacer()
                    }
                }

            case .training:
                TrainingGroundSceneView(phaseTick: uiState.adventurePhaseTick, hasPlayerSprites: hasPlayerSprites)

            case .encounter:
                BattleConfrontationIOSView(
                    isAttackPhase: false,
                    approach: confrontationApproach,
                    phaseTick: uiState.adventurePhaseTick,
                    hasPlayerSprites: hasPlayerSprites,
                    playerWalkFrames: playerWalkFrameNames(),
                    enemyFirstFrameName: enemyFrames.first,
                    enemyEmoji: uiState.enemyEmoji,
                    lastDamage: uiState.lastDamage,
                    currentFloor: Int(uiState.currentFloor),
                    totalFloors: uiState.totalFloors
                )

            case .attacking:
                BattleConfrontationIOSView(
                    isAttackPhase: true,
                    approach: confrontationApproach,
                    phaseTick: uiState.adventurePhaseTick,
                    hasPlayerSprites: hasPlayerSprites,
                    playerWalkFrames: playerWalkFrameNames(),
                    enemyFirstFrameName: enemyFrames.first,
                    enemyEmoji: uiState.enemyEmoji,
                    lastDamage: uiState.lastDamage,
                    currentFloor: Int(uiState.currentFloor),
                    totalFloors: uiState.totalFloors
                )

            case .enemyDefeated, .floorClear:
                EmptyView()

            case .resting:
                breakSceneContent

            case .playerDead:
                VStack(spacing: 8) {
                    Text("💀").font(.system(size: 48))
                    Text("力尽きた…")
                        .font(.system(size: 18, weight: .heavy))
                        .foregroundColor(fireRed)
                    Text("1Fからやり直し！")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(textMuted)
                }

            default:
                EmptyView()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - 休憩シーン

    private var breakScene: some View {
        breakSceneContent
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var breakSceneContent: some View {
        ZStack {
            ForEach(0..<13, id: \.self) { i in
                Text("✦")
                    .font(.system(size: CGFloat(8 + (i % 4) * 2)))
                    .foregroundColor(.white.opacity(pulsePhase ? 0.28 : 0.12))
                    .offset(
                        x: CGFloat(-118 + (i * 23) % 236),
                        y: CGFloat(-56 + (i % 4) * 26)
                    )
            }

            VStack(spacing: 0) {
                HStack(spacing: 8) {
                    Circle()
                        .fill(breakAccent.opacity(pulsePhase ? 0.95 : 0.45))
                        .frame(width: 9, height: 9)
                    Text("休憩中")
                        .font(.system(size: 15, weight: .heavy))
                        .foregroundColor(textWhite)
                    Text("HP 回復")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(breakAccent.opacity(0.92))
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(breakAccent.opacity(0.12))
                .overlay(
                    Capsule()
                        .stroke(breakAccent.opacity(0.4), lineWidth: 1)
                )
                .clipShape(Capsule())

                Spacer().frame(height: 10)

                ZStack(alignment: .bottom) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(breakAccent.opacity(0.2))
                        .frame(height: 3)
                        .padding(.horizontal, 20)
                    Ellipse()
                        .fill(
                            RadialGradient(
                                colors: [
                                    breakGlow.opacity(pulsePhase ? 0.32 : 0.18),
                                    .clear
                                ],
                                center: .center,
                                startRadius: 2,
                                endRadius: 86
                            )
                        )
                        .frame(width: 150, height: 70)
                        .offset(y: -12)
                    breakScenePlayerSprite(size: 148)
                        .offset(y: pulsePhase ? -6 : -1)
                }
                .frame(height: 156)

                Spacer().frame(height: 8)

                Text("焚き火を囲んで ゆっくり休んでいます")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(breakAccent)
                Text("「冒険へ」で続行、「終了する」でホームへ戻れます")
                    .font(.system(size: 11))
                    .foregroundColor(textMuted)
            }
            .padding(.vertical, 18)
            .padding(.horizontal, 14)
            .background(
                LinearGradient(
                    colors: [
                        Color(hex: 0x0F2818),
                        Color(hex: 0x071209),
                        Color(hex: 0x0D2214)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 22)
                    .stroke(breakAccent.opacity(0.45), lineWidth: 1.5)
            )
            .clipShape(RoundedRectangle(cornerRadius: 22))
            .padding(.horizontal, 10)
        }
    }

    // MARK: - ヘルパー

    private func rewardItem(emoji: String, label: String, value: String) -> some View {
        VStack(spacing: 4) {
            Text(emoji).font(.system(size: 24))
            Text(label)
                .font(.system(size: 10))
                .foregroundColor(textMuted)
            Text(value)
                .font(.system(size: 14, weight: .heavy))
                .foregroundColor(textWhite)
        }
    }
}

struct StudyQuestScreenView_Previews: PreviewProvider {
    static var previews: some View {
        StudyQuestScreenView(initialStudyMinutes: 25)
    }
}
