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

private let bgColor = Color(hex: 0xF0F4FF)
private let cardWhite = Color.white
private let textPrimary = Color(hex: 0x1E293B)
private let textSecondary = Color(hex: 0x64748B)
private let accentBlue = Color(hex: 0x3B82F6)
private let accentIndigo = Color(hex: 0x6366F1)
private let fireRed = Color(hex: 0xEF4444)
private let fireOrange = Color(hex: 0xF59E0B)
private let emeraldGreen = Color(hex: 0x10B981)

/// ホーム画面（タブ③: 中央）
struct HomeScreenView: View {
    @State private var showStudySheet = false
    @State private var studyMinutes = 25
    @State private var selectedGenre = "総合"
    @State private var isBouncing = false
    @State private var messageIndex = 0

    // KMP HomeViewModel — Koin から取得
    private let homeViewModel = KoinHelperKt.getHomeViewModel()
    @State private var homeState: HomeUiState?

    private let messages = [
        "今日の特訓も頑張ろうな！",
        "知識こそ最強の武器だ。",
        "お前の成長、楽しみにしてるぞ。",
        "さぁ、冒険の時間だ！",
        "集中すれば、何でもできる。"
    ]

    private let genres: [(emoji: String, label: String)] = [
        ("🔢", "数学"),
        ("🔬", "理科"),
        ("📝", "語学"),
        ("💻", "プログラミング"),
        ("📚", "総合"),
        ("🎨", "クリエイティブ")
    ]

    let messageTimer = Timer.publish(every: 4, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(spacing: 0) {
            homeHeader
            Spacer()
            characterArea
            Spacer()
            timeSelector
            Spacer().frame(height: 24)
            genreSelector
            Spacer().frame(height: 24)
            startButton
            Spacer().frame(height: 24)
            Spacer().frame(height: 90)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(bgColor)
        .fullScreenCover(isPresented: $showStudySheet) {
            StudyQuestScreenView(initialStudyMinutes: studyMinutes, genreId: selectedGenre)
        }
        .onReceive(messageTimer) { _ in
            withAnimation(.easeInOut(duration: 0.3)) {
                messageIndex = (messageIndex + 1) % messages.count
            }
        }
        .onAppear {
            homeViewModel.onIntent(intent: HomeIntentRefresh())
        }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
            self.homeState = homeViewModel.uiState.value as? HomeUiState
        }
    }

    // MARK: - Header

    private var homeHeader: some View {
        HStack {
            HStack(spacing: 8) {
                Text("📖").font(.system(size: 18))
                VStack(alignment: .leading, spacing: 1) {
                    Text("累計勉強")
                        .font(.system(size: 10))
                        .foregroundColor(textSecondary)
                    Text(homeState?.formattedStudyTime ?? "0h 0m")
                        .font(.system(size: 15, weight: .heavy))
                        .foregroundColor(textPrimary)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(cardWhite)
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.06), radius: 4, y: 2)

            Spacer()

            HStack(spacing: 8) {
                Text("💎").font(.system(size: 18))
                VStack(alignment: .leading, spacing: 1) {
                    Text("知識の結晶")
                        .font(.system(size: 10))
                        .foregroundColor(textSecondary)
                    Text("\(homeState?.stones ?? 0)")
                        .font(.system(size: 15, weight: .heavy))
                        .foregroundColor(textPrimary)
                }
                Circle()
                    .fill(emeraldGreen)
                    .frame(width: 24, height: 24)
                    .overlay(
                        Image(systemName: "plus")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.white)
                    )
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(cardWhite)
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.06), radius: 4, y: 2)
        }
        .padding(16)
    }

    // MARK: - Character Area

    private var characterArea: some View {
        ZStack {
            // オーラ
            Circle()
                .fill(
                    RadialGradient(
                        colors: [accentBlue.opacity(0.12), accentIndigo.opacity(0.06), .clear],
                        center: .center,
                        startRadius: 20,
                        endRadius: 140
                    )
                )
                .frame(width: 280, height: 280)

            VStack(spacing: 0) {
                // 吹き出し
                VStack(spacing: 0) {
                    HStack(spacing: 8) {
                        Text("💬").font(.system(size: 14))
                        Text("「\(messages[messageIndex])」")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(textPrimary)
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 12)
                    .background(cardWhite)
                    .cornerRadius(20)
                    .shadow(color: .black.opacity(0.08), radius: 8, y: 4)

                    // 三角
                    Triangle()
                        .fill(cardWhite)
                        .frame(width: 16, height: 10)
                }

                Spacer().frame(height: 6)

                // キャラクター
                Text("🧙‍♂️")
                    .font(.system(size: 100))
                    .offset(y: isBouncing ? -12 : 0)
                    .animation(
                        .easeInOut(duration: 1.2).repeatForever(autoreverses: true),
                        value: isBouncing
                    )
                    .onAppear { isBouncing = true }

                Spacer().frame(height: 4)

                // 名前 + Lv
                HStack(spacing: 8) {
                    Text("マーリン")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(textPrimary)
                    Text("Lv.24")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(accentBlue)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(accentBlue.opacity(0.12))
                        .cornerRadius(8)
                }
            }
        }
    }

    // MARK: - Time Selector

    private var timeSelector: some View {
        VStack(spacing: 8) {
            Text("⏱ 冒険時間")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(textSecondary)

            HStack(spacing: 0) {
                Button(action: { if studyMinutes > 5 { studyMinutes -= 5 } }) {
                    Circle()
                        .fill(accentBlue.opacity(0.1))
                        .frame(width: 36, height: 36)
                        .overlay(
                            Image(systemName: "chevron.left")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(accentBlue)
                        )
                }

                VStack(spacing: 0) {
                    Text("\(studyMinutes)")
                        .font(.system(size: 40, weight: .black, design: .rounded))
                        .foregroundColor(textPrimary)
                    Text("分")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(textSecondary)
                }
                .frame(width: 100)

                Button(action: { if studyMinutes < 120 { studyMinutes += 5 } }) {
                    Circle()
                        .fill(accentBlue.opacity(0.1))
                        .frame(width: 36, height: 36)
                        .overlay(
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(accentBlue)
                        )
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(cardWhite)
            .cornerRadius(20)
            .shadow(color: .black.opacity(0.06), radius: 4, y: 2)

            // クイック選択
            HStack(spacing: 8) {
                ForEach([15, 25, 45, 60], id: \.self) { min in
                    let isSelected = studyMinutes == min
                    Button(action: { studyMinutes = min }) {
                        Text("\(min)分")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(isSelected ? .white : textSecondary)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 6)
                            .background(isSelected ? accentBlue : Color(hex: 0xE2E8F0))
                            .cornerRadius(12)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(.horizontal, 32)
    }

    // MARK: - Genre Selector

    private var genreSelector: some View {
        VStack(spacing: 8) {
            Text("📖 ジャンル")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(textSecondary)

            // 3列×2行
            VStack(spacing: 8) {
                ForEach(0..<2, id: \.self) { row in
                    HStack(spacing: 8) {
                        ForEach(0..<3, id: \.self) { col in
                            let index = row * 3 + col
                            if index < genres.count {
                                let genre = genres[index]
                                let isSelected = selectedGenre == genre.label
                                Button(action: { selectedGenre = genre.label }) {
                                    HStack(spacing: 4) {
                                        Text(genre.emoji).font(.system(size: 14))
                                        Text(genre.label)
                                            .font(.system(size: 12, weight: .bold))
                                            .foregroundColor(isSelected ? .white : textSecondary)
                                    }
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(isSelected ? accentIndigo : Color(hex: 0xE2E8F0))
                                    .cornerRadius(12)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 32)
    }

    // MARK: - Start Button

    private var startButton: some View {
        Button(action: { showStudySheet = true }) {
            HStack(spacing: 10) {
                Text("⚔️").font(.system(size: 22))
                Text("冒険に出発する")
                    .font(.system(size: 18, weight: .heavy))
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(
                LinearGradient(
                    colors: [fireRed, fireOrange],
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .cornerRadius(24)
            .shadow(color: fireOrange.opacity(0.4), radius: 10, y: 5)
        }
        .padding(.horizontal, 32)
    }
}

// MARK: - 吹き出し三角

private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        Path { p in
            p.move(to: CGPoint(x: rect.midX - 8, y: 0))
            p.addLine(to: CGPoint(x: rect.midX, y: rect.maxY))
            p.addLine(to: CGPoint(x: rect.midX + 8, y: 0))
            p.closeSubpath()
        }
    }
}

struct HomeScreenView_Previews: PreviewProvider {
    static var previews: some View {
        HomeScreenView()
    }
}
