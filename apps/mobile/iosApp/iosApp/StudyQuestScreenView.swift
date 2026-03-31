import SwiftUI
import Shared

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
private let fireRed = Color(hex: 0xEF4444)
private let fireOrange = Color(hex: 0xF59E0B)
private let emeraldGreen = Color(hex: 0x10B981)
private let purpleGlow = Color(hex: 0x8B5CF6)
private let textWhite = Color(hex: 0xF8FAFC)
private let textMuted = Color(hex: 0x94A3B8)
private let breakBg = Color(hex: 0x0C1E0C)
private let breakCard = Color(hex: 0x1A2E1A)
private let breakAccent = Color(hex: 0x34D399)
private let breakGlow = Color(hex: 0x10B981)

// MARK: - メイン

struct StudyQuestScreenView: View {
    @Environment(\.dismiss) var dismiss
    let initialStudyMinutes: Int

    private let viewModel = StudyQuestViewModel()
    @State private var uiState: StudyQuestUiState
    @State private var pulsePhase = false

    init(initialStudyMinutes: Int) {
        self.initialStudyMinutes = initialStudyMinutes
        _uiState = State(initialValue: StudyQuestUiState(
            type: .study,
            status: .ready,
            targetStudyMinutes: Int32(initialStudyMinutes),
            targetBreakMinutes: 5,
            elapsedSeconds: 0,
            isOvertime: false,
            currentLog: [],
            displayTime: "\(initialStudyMinutes < 10 ? "0" : "")\(initialStudyMinutes):00"
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
            viewModel.onIntent(intent: StudyQuestIntentStartQuest(studyMinutes: Int32(initialStudyMinutes)))
            withAnimation(.easeInOut(duration: 1.5).repeatForever(autoreverses: true)) {
                pulsePhase = true
            }
        }
        .onReceive(Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()) { _ in
            self.uiState = viewModel.uiState.value as! StudyQuestUiState
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // メインクエスト — 没入感のあるダークUI
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private var mainQuestView: some View {
        let isBreak = uiState.type == .break_
        let isOvertime = uiState.isOvertime
        let targetSec = isBreak ? Int(uiState.targetBreakMinutes) * 60 : Int(uiState.targetStudyMinutes) * 60
        let progress: Double = targetSec > 0 && !isOvertime
            ? min(Double(uiState.elapsedSeconds) / Double(targetSec), 1.0)
            : (isOvertime ? 1.0 : 0.0)
        let glowColor: Color = isOvertime ? purpleGlow : (isBreak ? breakGlow : accentBlue)

        return VStack(spacing: 0) {
            // ── トップバー ──
            Spacer().frame(height: 56)
            HStack {
                Button(action: {
                    viewModel.onIntent(intent: StudyQuestIntentStopQuest())
                    dismiss()
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(textMuted)
                        .frame(width: 40, height: 40)
                }

                Spacer()

                // ステータスバッジ
                HStack(spacing: 6) {
                    Text(isOvertime ? "⚡" : (uiState.status == .paused ? "⏸" : (isBreak ? "🌿" : "⚔️")))
                        .font(.system(size: 12))
                    Text(isOvertime ? "限界突破中" : (uiState.status == .paused ? "一時停止" : (isBreak ? "休憩中" : "冒険中")))
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(glowColor)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(glowColor.opacity(0.15))
                .cornerRadius(12)

                Spacer()
                Spacer().frame(width: 40)
            }
            .padding(.horizontal, 24)

            Spacer()

            // ── サークルタイマー ──
            ZStack {
                // リング
                Circle()
                    .stroke(glowColor.opacity(0.15), lineWidth: 10)
                    .frame(width: 220, height: 220)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(
                        glowColor.opacity(pulsePhase ? 0.9 : 0.5),
                        style: StrokeStyle(lineWidth: 10, lineCap: .round)
                    )
                    .frame(width: 220, height: 220)
                    .rotationEffect(.degrees(-90))

                VStack(spacing: 8) {
                    // キャラ & 敵
                    if isBreak {
                        Text("🏕️").font(.system(size: 48))
                    } else {
                        HStack(spacing: 16) {
                            Text("🧙‍♂️").font(.system(size: 40))
                            Text("⚔️")
                                .font(.system(size: 20))
                                .foregroundColor(fireRed.opacity(pulsePhase ? 0.9 : 0.4))
                            Text("👾").font(.system(size: 40))
                        }
                    }

                    // タイマー
                    Text(uiState.displayTime)
                        .font(.system(size: 52, weight: .heavy, design: .monospaced))
                        .foregroundColor(isOvertime ? purpleGlow : (isBreak ? breakAccent : textWhite))

                    if isOvertime {
                        Text("延長戦！")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(purpleGlow.opacity(pulsePhase ? 0.9 : 0.4))
                    }
                }
            }
            .scaleEffect(pulsePhase ? 1.03 : 1.0)

            Spacer().frame(height: 24)

            // ── バトルログ ──
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text(isBreak ? "🌙 キャンプログ" : "📜 冒険ログ")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(textWhite)
                    Spacer()
                    if isOvertime {
                        Text("超集中")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(purpleGlow)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(purpleGlow.opacity(0.2))
                            .cornerRadius(8)
                    }
                }

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 8) {
                        ForEach(Array(uiState.currentLog.enumerated()), id: \.offset) { idx, log in
                            let isLatest = idx == uiState.currentLog.count - 1
                            HStack(alignment: .top, spacing: 10) {
                                Circle()
                                    .fill(isLatest ? glowColor : textMuted)
                                    .frame(width: 6, height: 6)
                                    .padding(.top, 6)
                                Text(log)
                                    .font(.system(size: 13, weight: isLatest ? .semibold : .regular))
                                    .foregroundColor(isLatest ? textWhite : textMuted)
                                    .lineSpacing(4)
                            }
                        }
                    }
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isBreak ? breakCard : darkCard)
            .cornerRadius(24)
            .padding(.horizontal, 24)

            Spacer()

            // ── ボタン ──
            VStack(spacing: 12) {
                if !isBreak {
                    Button(action: {
                        viewModel.onIntent(intent: StudyQuestIntentFinishSession())
                    }) {
                        Text("🏁 クエスト完了")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(emeraldGreen)
                            .cornerRadius(16)
                    }
                }

                HStack(spacing: 12) {
                    Button(action: {
                        viewModel.onIntent(intent: StudyQuestIntentTogglePause())
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
                        viewModel.onIntent(intent: StudyQuestIntentStopQuest())
                        dismiss()
                    }) {
                        Text("中止")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(fireRed)
                            .padding(.vertical, 16)
                            .padding(.horizontal, 24)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(fireRed.opacity(0.3), lineWidth: 1)
                            )
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // リザルト画面 — 達成感のある演出
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private var resultScreen: some View {
        let isStudy = uiState.type == .study

        return ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 0) {
                Spacer().frame(height: 80)

                // タイトル
                Text(isStudy ? "⚔️ QUEST CLEAR!" : "🌿 REST COMPLETE!")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(isStudy ? fireOrange.opacity(0.8) : breakAccent.opacity(0.8))
                    .tracking(4)
                Spacer().frame(height: 8)
                Text(isStudy ? "クエスト達成！" : "休憩完了！")
                    .font(.system(size: 36, weight: .heavy))
                    .foregroundColor(textWhite)

                Spacer().frame(height: 32)

                // トロフィー
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
                                endRadius: 80
                            )
                        )
                        .frame(width: 160, height: 160)
                    Text(isStudy ? "🏆" : "✨")
                        .font(.system(size: 80))
                        .offset(y: pulsePhase ? -6 : 0)
                }

                Spacer().frame(height: 24)

                // 報酬カード
                VStack(spacing: 16) {
                    if isStudy {
                        Text("📊 冒険結果")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(textMuted)

                        HStack {
                            Spacer()
                            rewardItem(emoji: "⏱", label: "集中時間", value: "\(uiState.targetStudyMinutes)分")
                            Spacer()
                            rewardItem(emoji: "⭐", label: "経験値", value: "+\(uiState.targetStudyMinutes * 10)")
                            Spacer()
                            rewardItem(emoji: "💎", label: "結晶", value: "+\(uiState.targetStudyMinutes / 5)")
                            Spacer()
                        }
                    } else {
                        Text("🌙 休憩完了")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(textMuted)
                    }

                    // キャラ吹き出し
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

                Spacer().frame(height: 32)

                // ボタン
                Button(action: {
                    viewModel.onIntent(intent: StudyQuestIntentNextSession())
                }) {
                    Text(isStudy ? "🌿 休憩を開始する" : "⚔️ 次の冒険へ")
                        .font(.system(size: 16, weight: .heavy))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 18)
                        .background(isStudy ? emeraldGreen : accentBlue)
                        .cornerRadius(18)
                }
                .padding(.horizontal, 32)

                Spacer().frame(height: 12)

                Button(action: {
                    viewModel.onIntent(intent: StudyQuestIntentStopQuest())
                    dismiss()
                }) {
                    Text("🏠 街に戻る")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(textMuted)
                }

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

    private func rewardItem(emoji: String, label: String, value: String) -> some View {
        VStack(spacing: 4) {
            Text(emoji).font(.system(size: 28))
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(textMuted)
            Text(value)
                .font(.system(size: 16, weight: .heavy))
                .foregroundColor(textWhite)
        }
    }
}

struct StudyQuestScreenView_Previews: PreviewProvider {
    static var previews: some View {
        StudyQuestScreenView(initialStudyMinutes: 25)
    }
}
