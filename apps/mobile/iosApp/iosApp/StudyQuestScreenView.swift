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
                    .frame(width: size, height: size)
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

    var body: some View {
        let bgName = dungeonBgName(dungeonName)
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
    let m = phaseTick % 3
    if m == 0 {
        if lastDamage > 0, UIImage(named: "sprite_player_attack_1") != nil {
            Image("sprite_player_attack_1")
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: size, height: size)
        } else if UIImage(named: "sprite_player_idle_1") != nil {
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
            Text("🧙‍♂️").font(.system(size: 52))
        }
    } else if m == 1 {
        if UIImage(named: "sprite_player_idle_1") != nil {
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
            Text("🧙‍♂️").font(.system(size: 52))
        }
    } else if let prep = playerPrepAssetName() {
        Image(prep)
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size)
    } else if UIImage(named: "sprite_player_idle_1") != nil {
        Image("sprite_player_idle_1")
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size)
    } else {
        Text("🧙‍♂️").font(.system(size: 52))
    }
}

/// 遭遇〜戦闘：キャラは探索時と同じ床ライン。HP・接近文は画面上部。接近中のみ敵スプライトが微上下
private struct BattleConfrontationIOSView: View {
    let isAttackPhase: Bool
    let approach: CGFloat
    let phaseTick: Int64
    let hasPlayerSprites: Bool
    let playerWalkFrames: [String]
    let hasEnemySprites: Bool
    let enemyFirstFrameName: String?
    let enemyEmoji: String
    let enemyName: String
    let enemyHp: Int32
    let enemyMaxHp: Int32
    let lastDamage: Int32

    var body: some View {
        let showEnemyHp = isAttackPhase || approach >= 0.9
        let isStriking = isAttackPhase && lastDamage > 0
        let t = min(1.0, max(0.0, approach))

        ZStack {
            Color.black.opacity(0.12)

            Text(isAttackPhase ? "⚔️ 戦闘中" : "⚠️ 遭遇！")
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(textMuted)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                .padding(.top, 8)

            if showEnemyHp {
                VStack(alignment: .trailing, spacing: 2) {
                    Text(enemyName)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(textMuted)
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 3)
                            .fill(darkSurface)
                            .frame(width: 72, height: 5)
                        let hpRatio = enemyMaxHp > 0 ? CGFloat(enemyHp) / CGFloat(enemyMaxHp) : 0
                        RoundedRectangle(cornerRadius: 3)
                            .fill(hpRatio > 0.5 ? emeraldGreen : (hpRatio > 0.25 ? fireOrange : fireRed))
                            .frame(width: 72 * hpRatio, height: 5)
                    }
                    Text("\(enemyHp)/\(enemyMaxHp)")
                        .font(.system(size: 8))
                        .foregroundColor(textMuted)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                .padding(.top, 28)
                .padding(.trailing, 8)
            } else if !isAttackPhase {
                VStack(spacing: 2) {
                    Text(enemyName)
                        .font(.system(size: 12, weight: .heavy))
                        .foregroundColor(fireOrange)
                    Text("接近中…")
                        .font(.system(size: 10))
                        .foregroundColor(textMuted)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                .padding(.top, 28)
            }

            GeometryReader { geo in
                let w = geo.size.width
                let centerX = w / 2
                let gap: CGFloat = 30
                let pW: CGFloat = 118
                let eW: CGFloat = 118
                let playerEndLeft = centerX - gap / 2 - pW
                let enemyEndLeft = centerX + gap / 2
                let playerStart: CGFloat = -32
                let enemyStart = w + 88
                let playerLeft = playerStart + (playerEndLeft - playerStart) * t
                let enemyLeft = enemyStart + (enemyEndLeft - enemyStart) * t
                let strikeNudge: CGFloat = (isAttackPhase && isStriking) ? 6 : 0

                ZStack(alignment: .bottomLeading) {
                    if hasPlayerSprites {
                        if isAttackPhase {
                            iosCombatPlayerSprite(phaseTick: phaseTick, lastDamage: lastDamage, size: pW)
                                .offset(x: playerLeft + strikeNudge)
                                .padding(.leading, 16)
                                .padding(.bottom, adventureFloorInset)
                        } else if !playerWalkFrames.isEmpty {
                            AnimatedSpriteView(frames: playerWalkFrames, interval: 0.32, size: pW)
                                .offset(x: playerLeft)
                                .padding(.leading, 16)
                                .padding(.bottom, adventureFloorInset)
                        } else {
                            iosCombatPlayerSprite(phaseTick: phaseTick, lastDamage: 0, size: pW)
                                .offset(x: playerLeft)
                                .padding(.leading, 16)
                                .padding(.bottom, adventureFloorInset)
                        }
                    } else {
                        Text("🧙‍♂️")
                            .font(.system(size: 52))
                            .offset(x: playerLeft)
                            .padding(.leading, 16)
                            .padding(.bottom, adventureFloorInset)
                    }

                    if !isAttackPhase {
                        TimelineView(.animation(minimumInterval: 0.32, paused: false)) { ctx in
                            let bob = Int(ctx.date.timeIntervalSinceReferenceDate / 0.32) % 2 == 0 ? CGFloat(-3) : 2
                            enemySpriteOnly(eW: eW)
                                .offset(x: enemyLeft, y: bob)
                        }
                        .padding(.bottom, adventureFloorInset)
                    } else {
                        enemySpriteOnly(eW: eW)
                            .offset(x: enemyLeft)
                            .padding(.bottom, adventureFloorInset)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func enemySpriteOnly(eW: CGFloat) -> some View {
        if let name = enemyFirstFrameName, UIImage(named: name) != nil {
            Image(name)
                .resizable()
                .interpolation(.none)
                .scaledToFit()
                .frame(width: eW, height: eW)
        } else {
            Text(enemyEmoji)
                .font(.system(size: 56))
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
private let adventureFloorInset: CGFloat = 8

// MARK: - メイン

struct StudyQuestScreenView: View {
    @Environment(\.dismiss) var dismiss
    let initialStudyMinutes: Int
    let genreId: String?
    let dungeonName: String?

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

    init(initialStudyMinutes: Int, genreId: String? = nil, dungeonName: String? = nil) {
        self.initialStudyMinutes = initialStudyMinutes
        self.genreId = genreId
        self.dungeonName = dungeonName
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
            serverRewards: [],
            serverSynced: nil,
            partyLeadName: "冒険者",
            partyLeadImageUrl: "",
            dungeonName: dungeonName,
            currentFloor: 1,
            totalFloors: 10,
            floorClearCount: 0,
            playerHp: 100,
            playerMaxHp: 100,
            earnedXp: 0,
            earnedStones: 0
        ))
    }

    var body: some View {
        let isBreak = uiState.type == .break_
        let bgColor = isBreak ? breakBg : darkBg

        ZStack {
            bgColor.ignoresSafeArea()

            if uiState.status == .finished {
                resultScreen
            } else {
                mainQuestView
            }
        }
        .onAppear {
            if !didStartQuest {
                didStartQuest = true
                holder.viewModel.onIntent(intent: StudyQuestIntentStartQuest(studyMinutes: Int32(initialStudyMinutes), genreId: genreId, dungeonName: dungeonName))
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

        return VStack(spacing: 0) {
            // トップバー
            Spacer().frame(height: 56)
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    if let dn = uiState.dungeonName, !dn.isEmpty {
                        Text("🏰 \(dn)")
                            .font(.system(size: 11, weight: .heavy))
                            .foregroundColor(fireOrange)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(fireOrange.opacity(0.15))
                            .cornerRadius(10)
                    }
                    Text("📖 \(uiState.genreId ?? "総合")")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(accentIndigo)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(accentIndigo.opacity(0.15))
                        .cornerRadius(10)
                }

                Spacer()

                // ステータスバッジ
                HStack(spacing: 6) {
                    Text(phaseStatusEmoji(phase: phase, isOvertime: isOvertime, isPaused: uiState.status == .paused, isBreak: isBreak))
                        .font(.system(size: 12))
                    Text(phaseStatusText(phase: phase, isOvertime: isOvertime, isPaused: uiState.status == .paused, isBreak: isBreak))
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(phaseStatusColor(phase: phase, isOvertime: isOvertime, isBreak: isBreak))
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(phaseStatusColor(phase: phase, isOvertime: isOvertime, isBreak: isBreak).opacity(0.15))
                .cornerRadius(12)

                Spacer()

                // 階層
                Text("📍 \(uiState.currentFloor)F/\(uiState.totalFloors)F")
                    .font(.system(size: 11, weight: .heavy))
                    .foregroundColor(textWhite)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(darkSurface)
                    .cornerRadius(10)

                // 撃破数
                Text("💀 \(uiState.defeatedCount)")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(emeraldGreen)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(emeraldGreen.opacity(0.15))
                    .cornerRadius(10)
            }
            .padding(.horizontal, 24)

            Spacer()

            // プレイヤーHPバー
            if !isBreak {
                HStack(spacing: 8) {
                    Text("🧙‍♂️").font(.system(size: 16))
                    VStack(alignment: .leading, spacing: 2) {
                        HStack {
                            Text("HP")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(textMuted)
                            Spacer()
                            Text("\(uiState.playerHp)/\(uiState.playerMaxHp)")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(textWhite)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(darkSurface)
                                    .frame(height: 6)
                                let ratio = uiState.playerMaxHp > 0
                                    ? CGFloat(uiState.playerHp) / CGFloat(uiState.playerMaxHp)
                                    : 0
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(ratio > 0.5 ? emeraldGreen : (ratio > 0.25 ? fireOrange : fireRed))
                                    .frame(width: geo.size.width * ratio, height: 6)
                            }
                        }
                        .frame(height: 6)
                    }
                    if uiState.lastPlayerDamage > 0 {
                        Text("-\(uiState.lastPlayerDamage)")
                            .font(.system(size: 14, weight: .heavy))
                            .foregroundColor(fireRed)
                    }
                }
                .padding(.horizontal, 24)
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
                    let bgAssetName = dungeonBgName(uiState.dungeonName)
                    if UIImage(named: bgAssetName) != nil {
                        DungeonBackgroundView(dungeonName: uiState.dungeonName)
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

            // ボタン（一時停止 + 終了のみ）
            HStack(spacing: 12) {
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
        let hasEnemySprites = !enemyFrames.isEmpty
        let bgName = dungeonBgName(uiState.dungeonName)
        let hasBg = UIImage(named: bgName) != nil
        return ZStack {
            if hasBg {
                DungeonBackgroundView(dungeonName: uiState.dungeonName)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 24))
            }

            switch phase {
            case .walking:
                VStack {
                    Text("探索中…")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(textMuted)
                        .padding(.top, 8)
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

            case .encounter:
                BattleConfrontationIOSView(
                    isAttackPhase: false,
                    approach: confrontationApproach,
                    phaseTick: uiState.adventurePhaseTick,
                    hasPlayerSprites: hasPlayerSprites,
                    playerWalkFrames: playerWalkFrameNames(),
                    hasEnemySprites: hasEnemySprites,
                    enemyFirstFrameName: enemyFrames.first,
                    enemyEmoji: uiState.enemyEmoji,
                    enemyName: uiState.enemyName,
                    enemyHp: uiState.enemyHp,
                    enemyMaxHp: uiState.enemyMaxHp,
                    lastDamage: uiState.lastDamage
                )

            case .attacking:
                BattleConfrontationIOSView(
                    isAttackPhase: true,
                    approach: confrontationApproach,
                    phaseTick: uiState.adventurePhaseTick,
                    hasPlayerSprites: hasPlayerSprites,
                    playerWalkFrames: playerWalkFrameNames(),
                    hasEnemySprites: hasEnemySprites,
                    enemyFirstFrameName: enemyFrames.first,
                    enemyEmoji: uiState.enemyEmoji,
                    enemyName: uiState.enemyName,
                    enemyHp: uiState.enemyHp,
                    enemyMaxHp: uiState.enemyMaxHp,
                    lastDamage: uiState.lastDamage
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
            // 星
            ForEach(0..<10, id: \.self) { i in
                Text("✦")
                    .font(.system(size: CGFloat(8 + i % 4 * 3)))
                    .foregroundColor(.white.opacity(pulsePhase ? 0.3 : 0.1))
                    .offset(
                        x: CGFloat(-120 + i * 28),
                        y: CGFloat(-60 + (i % 3) * 25)
                    )
            }

            VStack(spacing: 0) {
                Text("🧙‍♂️")
                    .font(.system(size: 48))
                    .offset(x: -20)
                Text("🔥")
                    .font(.system(size: pulsePhase ? 32 : 28))
                Spacer().frame(height: 8)
                Text("休憩中… 体力を回復しています")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(breakAccent)
            }
        }
    }

    // MARK: - リザルト画面

    private var resultScreen: some View {
        let isStudy = uiState.type == .study
        let actualMinutes = max(1, Int(uiState.elapsedSeconds / 60))

        return ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                Spacer().frame(height: 80)

                Text(isStudy ? "⚔️ QUEST CLEAR!" : "🌿 REST COMPLETE!")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(isStudy ? fireOrange.opacity(0.8) : breakAccent.opacity(0.8))
                    .tracking(4)
                Spacer().frame(height: 8)
                Text(isStudy ? "クエスト達成！" : "休憩完了！")
                    .font(.system(size: 36, weight: .heavy))
                    .foregroundColor(textWhite)

                Spacer().frame(height: 24)

                ZStack {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [
                                    (isStudy ? fireOrange : breakAccent).opacity(pulsePhase ? 0.25 : 0.1),
                                    .clear
                                ],
                                center: .center,
                                startRadius: 20,
                                endRadius: 70
                            )
                        )
                        .frame(width: 140, height: 140)
                    Text(isStudy ? "🏆" : "✨")
                        .font(.system(size: 72))
                        .offset(y: pulsePhase ? -6 : 0)
                }

                Spacer().frame(height: 20)

                // 報酬カード
                VStack(spacing: 16) {
                    if isStudy {
                        Text("📊 冒険結果")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(textMuted)

                        HStack {
                            Spacer()
                            rewardItem(emoji: "⏱", label: "集中時間", value: "\(actualMinutes)分")
                            Spacer()
                            rewardItem(emoji: "⭐", label: "経験値", value: "+\(uiState.earnedXp)")
                            Spacer()
                            rewardItem(emoji: "💀", label: "討伐数", value: "\(uiState.defeatedCount)体")
                            Spacer()
                            rewardItem(emoji: "💎", label: "ダイヤ", value: "+\(uiState.earnedStones)")
                            Spacer()
                        }
                    } else {
                        Text("🌙 休憩完了")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(textMuted)
                    }

                    HStack(spacing: 10) {
                        Text("🧙‍♂️").font(.system(size: 28))
                        Text(isStudy
                            ? "「見事な集中力だ！\nその調子で強くなるぞ。」"
                            : "「いいリフレッシュになったな。\nさあ、次の冒険に備えよう！」")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(textWhite)
                            .lineSpacing(4)
                    }
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(isStudy ? darkSurface : Color(hex: 0x1A3A1A))
                    .cornerRadius(12)
                }
                .padding(24)
                .background(isStudy ? darkCard : breakCard)
                .cornerRadius(24)
                .padding(.horizontal, 32)

                Spacer().frame(height: 28)

                // ボタン
                HStack(spacing: 12) {
                    Button(action: {
                        holder.viewModel.onIntent(intent: StudyQuestIntentStopQuest())
                        dismiss()
                    }) {
                        Text("🏠 街に戻る")
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
                        Text(isStudy ? "🌿 休憩へ" : "⚔️ 冒険へ")
                            .font(.system(size: 14, weight: .heavy))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(isStudy ? emeraldGreen : accentBlue)
                            .cornerRadius(16)
                    }
                }
                .padding(.horizontal, 32)

                Spacer().frame(height: 60)
            }
        }
        .background(
            LinearGradient(
                colors: isStudy
                    ? [Color(hex: 0x1A0A2E), Color(hex: 0x0F172A)]
                    : [Color(hex: 0x0A2E1A), Color(hex: 0x0C1E0C)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        )
    }

    // MARK: - ヘルパー

    private func phaseStatusEmoji(phase: AdventurePhase, isOvertime: Bool, isPaused: Bool, isBreak: Bool) -> String {
        if isOvertime { return "⚡" }
        if isPaused { return "⏸" }
        if isBreak { return "🏕️" }
        if phase == .playerDead { return "💀" }
        if phase == .floorClear { return "🏆" }
        if phase == .walking { return "🚶" }
        if phase == .encounter { return "⚠️" }
        if phase == .attacking { return "⚔️" }
        if phase == .resting { return "🏕️" }
        return "⚔️"
    }

    private func phaseStatusText(phase: AdventurePhase, isOvertime: Bool, isPaused: Bool, isBreak: Bool) -> String {
        if isOvertime { return "限界突破中" }
        if isPaused { return "一時停止" }
        if isBreak { return "休憩中" }
        if phase == .playerDead { return "力尽きた…" }
        if phase == .floorClear { return "全階層制覇！" }
        if phase == .walking { return "探索中" }
        if phase == .encounter { return "エンカウント！" }
        if phase == .attacking { return "戦闘中" }
        if phase == .resting { return "休憩中" }
        return "冒険中"
    }

    private func phaseStatusColor(phase: AdventurePhase, isOvertime: Bool, isBreak: Bool) -> Color {
        if isOvertime { return purpleGlow }
        if isBreak { return breakAccent }
        if phase == .playerDead { return fireRed }
        if phase == .floorClear { return fireOrange }
        if phase == .attacking { return fireRed }
        if phase == .encounter { return fireOrange }
        return accentBlue
    }

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
