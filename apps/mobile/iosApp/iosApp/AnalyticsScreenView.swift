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

private let textPrimary = Color(hex: 0x1E293B)
private let textSecondary = Color(hex: 0x64748B)
private let textTertiary = Color(hex: 0x94A3B8)
private let accentBlue = Color(hex: 0x3B82F6)

private func genreColor(_ genre: GenreInfo) -> Color {
    Color(hex: UInt(genre.colorHex))
}

private func formatMinutes(_ minutes: Int) -> String {
    let h = minutes / 60
    let m = minutes % 60
    return h > 0 ? "\(h)h \(m)m" : "\(m)m"
}

private func formatHoursMinutes(_ minutes: Int) -> (String, String) {
    let h = minutes / 60
    let m = minutes % 60
    return (String(h), String(format: "%02d", m))
}

// MARK: - メイン

/// 記録画面（タブ⑤: 右端）
/// KMP RecordViewModel を使用して勉強時間を可視化
struct AnalyticsScreenView: View {
    private let viewModel: RecordViewModel
    @State private var uiState: RecordUiState

    init() {
        let vm = KoinHelperKt.getRecordViewModel()
        self.viewModel = vm
        _uiState = State(initialValue: vm.uiState.value as! RecordUiState)
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 12) {
                recordHeader
                totalStudyCard
                periodTabs

                Spacer().frame(height: 4)
                periodSummaryCard

                Spacer().frame(height: 4)
                barChartCard

                Spacer().frame(height: 4)
                genreBreakdownCard

                Spacer().frame(height: 120)
            }
        }
        .background(Color(UIColor.systemGroupedBackground))
        .onReceive(Timer.publish(every: 0.3, on: .main, in: .common).autoconnect()) { _ in
            self.uiState = viewModel.uiState.value as! RecordUiState
        }
    }

    // MARK: - ヘッダー + キャラ吹き出し

    private var recordHeader: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 8) {
                    Text("📊").font(.system(size: 24))
                    Text("記録")
                        .font(.system(size: 28, weight: .heavy))
                        .foregroundColor(textPrimary)
                }
                Text("あなたの冒険の軌跡")
                    .font(.subheadline)
                    .foregroundColor(textSecondary)
            }
            Spacer()

            // キャラ吹き出し（小さめ）
            HStack(alignment: .bottom, spacing: 4) {
                Text(uiState.characterMessage)
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(textPrimary)
                    .lineLimit(2)
                    .frame(maxWidth: 140, alignment: .trailing)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color(UIColor.secondarySystemGroupedBackground))
                    .cornerRadius(12)
                    .shadow(color: .black.opacity(0.06), radius: 2, y: 1)

                Text(uiState.characterEmoji)
                    .font(.system(size: 36))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    // MARK: - 累計勉強時間ヒーロー

    private var totalStudyCard: some View {
        let (hours, mins) = formatHoursMinutes(Int(uiState.totalStudyMinutes))

        return VStack(alignment: .leading, spacing: 8) {
            Text("🏆 累計勉強時間")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(.white.opacity(0.85))

            HStack(alignment: .bottom, spacing: 0) {
                Text(hours)
                    .font(.system(size: 56, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                Text("h")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white.opacity(0.75))
                    .padding(.bottom, 8)
                Spacer().frame(width: 8)
                Text(mins)
                    .font(.system(size: 56, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                Text("m")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white.opacity(0.75))
                    .padding(.bottom, 8)
            }

            HStack(spacing: 16) {
                statPill(emoji: "🔥", text: "\(uiState.streakDays)日連続")
                statPill(emoji: "📖", text: "今日 \(uiState.todaySessions)回")
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            LinearGradient(
                colors: [Color(hex: 0x3B82F6), Color(hex: 0x6366F1), Color(hex: 0x8B5CF6)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(24)
        .shadow(color: Color(hex: 0x3B82F6).opacity(0.3), radius: 10, y: 6)
        .padding(.horizontal, 16)
    }

    private func statPill(emoji: String, text: String) -> some View {
        HStack(spacing: 4) {
            Text(emoji).font(.system(size: 12))
            Text(text).font(.system(size: 12, weight: .bold)).foregroundColor(.white)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.white.opacity(0.2))
        .cornerRadius(20)
    }

    // MARK: - 期間タブ

    private var periodTabs: some View {
        HStack(spacing: 4) {
            ForEach([RecordPeriod.today, RecordPeriod.weekly, RecordPeriod.monthly], id: \.self) { period in
                let isSelected = uiState.selectedPeriod == period
                Button(action: {
                    viewModel.onIntent(intent: RecordIntentSelectPeriod(period: period))
                }) {
                    Text(period.label)
                        .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                        .foregroundColor(isSelected ? accentBlue : textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(isSelected ? Color(UIColor.secondarySystemGroupedBackground) : .clear)
                        .cornerRadius(12)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(Color(hex: 0xE2E8F0))
        .cornerRadius(16)
        .padding(.horizontal, 16)
    }

    // MARK: - 期間サマリー

    private var periodSummaryCard: some View {
        let periodLabel: String = {
            switch uiState.selectedPeriod {
            case .today: return "今日の勉強時間"
            case .weekly: return "今週の勉強時間"
            case .monthly: return "今月の勉強時間"
            default: return "勉強時間"
            }
        }()

        return HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(periodLabel)
                    .font(.system(size: 13))
                    .foregroundColor(textSecondary)
                Text(formatMinutes(Int(uiState.periodTotalMinutes)))
                    .font(.system(size: 32, weight: .heavy))
                    .foregroundColor(textPrimary)
            }
            Spacer()
            if !uiState.genreBreakdown.isEmpty {
                miniDonutChart
            }
        }
        .padding(20)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(20)
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
        .padding(.horizontal, 16)
    }

    private var miniDonutChart: some View {
        Canvas { context, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = min(size.width, size.height) / 2 - 5
            var startAngle = Angle.degrees(-90)

            for item in uiState.genreBreakdown {
                let sweep = Angle.degrees(Double(item.ratio) * 360)
                let path = Path { p in
                    p.addArc(center: center, radius: radius,
                             startAngle: startAngle,
                             endAngle: startAngle + sweep,
                             clockwise: false)
                }
                context.stroke(path, with: .color(genreColor(item.genre)), lineWidth: 8)
                startAngle = startAngle + sweep
            }
        }
        .frame(width: 64, height: 64)
    }

    // MARK: - 棒グラフ

    private var barChartCard: some View {
        let bars = uiState.chartBars
        let maxMin = bars.map { Int($0.minutes) }.max() ?? 1
        let selectedGenre = uiState.selectedGenre

        return VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text("学習時間の推移")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(textPrimary)
                if let sg = selectedGenre {
                    Text("\(sg.emoji) \(sg.label)のみ表示中")
                        .font(.system(size: 11))
                        .foregroundColor(textSecondary)
                } else {
                    Text("全ジャンル")
                        .font(.system(size: 11))
                        .foregroundColor(textSecondary)
                }
            }

            if !bars.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(alignment: .bottom, spacing: 6) {
                        ForEach(Array(bars.enumerated()), id: \.offset) { _, bar in
                            barItem(bar: bar, maxMin: maxMin, selectedGenre: selectedGenre, barCount: bars.count)
                        }
                    }
                    .frame(height: 160)
                    .padding(.bottom, 4)
                }
            }
        }
        .padding(20)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(20)
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
        .padding(.horizontal, 16)
    }

    private func barItem(bar: ChartBar, maxMin: Int, selectedGenre: GenreInfo?, barCount: Int) -> some View {
        let fraction = CGFloat(bar.minutes) / CGFloat(max(maxMin, 1))
        let barHeight = max(130.0 * fraction, 4.0)
        let barWidth: CGFloat = barCount <= 7 ? 36 : (barCount <= 14 ? 28 : 22)

        return VStack(spacing: 0) {
            if bar.minutes > 0 {
                Text("\(bar.minutes)")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(textSecondary)
                Spacer().frame(height: 2)
            }

            Spacer()

            if let sg = selectedGenre {
                RoundedRectangle(cornerRadius: 6)
                    .fill(genreColor(sg))
                    .frame(height: barHeight)
            } else {
                stackedBar(bar: bar, barHeight: barHeight)
            }

            Spacer().frame(height: 4)

            Text(bar.label)
                .font(.system(size: 8))
                .foregroundColor(textTertiary)
                .lineLimit(1)
        }
        .frame(width: barWidth)
    }

    private func stackedBar(bar: ChartBar, barHeight: CGFloat) -> some View {
        let genreMinutes = bar.genreMinutes as! [GenreInfo: KotlinInt]
        let total = genreMinutes.values.reduce(0) { $0 + $1.intValue }
        let safeTot = max(total, 1)

        return VStack(spacing: 0) {
            let sorted = genreMinutes.sorted { $0.value.intValue > $1.value.intValue }
            ForEach(Array(sorted.enumerated()), id: \.offset) { _, entry in
                let segmentH = max(barHeight * CGFloat(entry.value.intValue) / CGFloat(safeTot), 2)
                Rectangle()
                    .fill(genreColor(entry.key))
                    .frame(height: segmentH)
            }
        }
        .clipShape(AnalyticsRoundedCorner(radius: 6, corners: [.topLeft, .topRight]))
    }

    // MARK: - ジャンル別内訳

    private var genreBreakdownCard: some View {
        let breakdown = uiState.genreBreakdown

        return VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("ジャンル別内訳")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(textPrimary)
                Spacer()
                if uiState.selectedGenre != nil {
                    Button(action: {
                        viewModel.onIntent(intent: RecordIntentSelectGenre(genre: nil))
                    }) {
                        Text("全表示")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(accentBlue)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color(hex: 0xF1F5F9))
                            .cornerRadius(8)
                    }
                    .buttonStyle(.plain)
                }
            }

            // ジャンルチップ
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(breakdown.enumerated()), id: \.offset) { _, item in
                        genreChip(item: item, isSelected: uiState.selectedGenre == item.genre)
                    }
                }
            }

            // バー詳細
            ForEach(Array(breakdown.enumerated()), id: \.offset) { _, item in
                genreBarRow(
                    item: item,
                    isHighlighted: uiState.selectedGenre == nil || uiState.selectedGenre == item.genre
                )
            }
        }
        .padding(20)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(20)
        .shadow(color: .black.opacity(0.04), radius: 4, y: 2)
        .padding(.horizontal, 16)
    }

    private func genreChip(item: GenreStudyTime, isSelected: Bool) -> some View {
        let color = genreColor(item.genre)
        return Button(action: {
            viewModel.onIntent(intent: RecordIntentSelectGenre(genre: item.genre))
        }) {
            HStack(spacing: 4) {
                Text(item.genre.emoji).font(.system(size: 14))
                Text(item.genre.label)
                    .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                    .foregroundColor(isSelected ? color : textSecondary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(isSelected ? color.opacity(0.15) : Color(hex: 0xF1F5F9))
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(isSelected ? color : .clear, lineWidth: 1.5)
            )
            .cornerRadius(20)
        }
        .buttonStyle(.plain)
    }

    private func genreBarRow(item: GenreStudyTime, isHighlighted: Bool) -> some View {
        let color = genreColor(item.genre)
        let alpha: Double = isHighlighted ? 1.0 : 0.35
        let percentage = Int(item.ratio * 100)

        return HStack(spacing: 8) {
            Text(item.genre.emoji).font(.system(size: 16))
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(item.genre.label)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(textPrimary.opacity(alpha))
                    Spacer()
                    Text("\(formatMinutes(Int(item.minutes))) (\(percentage)%)")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(color.opacity(alpha))
                }
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color(hex: 0xE2E8F0))
                            .frame(height: 8)
                        RoundedRectangle(cornerRadius: 4)
                            .fill(color.opacity(alpha))
                            .frame(width: geo.size.width * min(CGFloat(item.ratio), 1.0), height: 8)
                    }
                }
                .frame(height: 8)
            }
        }
    }
}

// MARK: - RoundedCorner Helper

private struct AnalyticsRoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}

// MARK: - Preview

struct AnalyticsScreenView_Previews: PreviewProvider {
    static var previews: some View {
        AnalyticsScreenView()
    }
}
