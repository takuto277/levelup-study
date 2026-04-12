import SwiftUI
import Shared

private extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255, opacity: alpha)
    }
}

private let bgDark = Color(hex: 0x0B1120)
private let bgCard = Color(hex: 0x111B2E)
private let bgSurface = Color(hex: 0x1A2744)
private let accentCyan = Color(hex: 0x22D3EE)
private let accentBlue = Color(hex: 0x3B82F6)
private let accentIndigo = Color(hex: 0x6366F1)
private let textW = Color(hex: 0xF1F5F9)
private let textSub = Color(hex: 0x94A3B8)
private let textDim = Color(hex: 0x64748B)

private func genreColor(_ g: GenreInfo) -> Color { Color(hex: UInt(g.colorHex)) }
private func fmtMin(_ m: Int) -> String { let h = m/60, r = m%60; return h > 0 ? "\(h)h \(r)m" : "\(r)m" }
private func fmtHM(_ m: Int) -> (String, String) { (String(m/60), String(format: "%02d", m%60)) }

private let weekSunRed = Color(hex: 0xEF4444)
private let weekSatBlue = Color(hex: 0x3B82F6)
private let weekDayGray = Color(hex: 0x64748B)

private func parseYmdLocal(_ s: String) -> Date? {
    let f = DateFormatter()
    f.calendar = Calendar(identifier: .gregorian)
    f.locale = Locale(identifier: "en_US_POSIX")
    f.timeZone = TimeZone.current
    f.dateFormat = "yyyy-MM-dd"
    return f.date(from: s)
}

/// Calendar.current weekday: 1=日 … 7=土（ロケールの一般的な並び）
private func weekdayJpFromIso(_ iso: String) -> String {
    guard let d = parseYmdLocal(iso) else { return "" }
    let wd = Calendar.current.component(.weekday, from: d)
    let names = ["", "日", "月", "火", "水", "木", "金", "土"]
    return (wd >= 1 && wd <= 7) ? names[wd] : ""
}

private func weekdayAxisColor(iso: String) -> Color {
    guard let d = parseYmdLocal(iso) else { return textDim }
    let wd = Calendar.current.component(.weekday, from: d)
    if wd == 1 { return weekSunRed }
    if wd == 7 { return weekSatBlue }
    return weekDayGray
}

struct AnalyticsScreenView: View {
    private let viewModel: RecordViewModel
    @State private var uiState: RecordUiState

    init() { let vm = KoinHelperKt.getRecordViewModel(); self.viewModel = vm; _uiState = State(initialValue: vm.uiState.value as! RecordUiState) }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 14) {
                header
                totalCard
                periodTabs
                if uiState.selectedPeriod == .monthly { monthSelector }
                Spacer().frame(height: 4)
                summaryCard
                Spacer().frame(height: 4)
                chartCard
                Spacer().frame(height: 4)
                genreCard
                Spacer().frame(height: 120)
            }
        }
        .background(LinearGradient(colors: [bgDark, Color(hex: 0x0F172A)], startPoint: .top, endPoint: .bottom).ignoresSafeArea())
        .onReceive(Timer.publish(every: 0.3, on: .main, in: .common).autoconnect()) { _ in self.uiState = viewModel.uiState.value as! RecordUiState }
    }

    // MARK: - Header
    private var header: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text("記 録").font(.system(size: 28, weight: .black)).foregroundColor(textW)
                Text("あなたの冒険の軌跡").font(.caption).foregroundColor(textSub)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 3) {
                HStack(spacing: 3) { Text("📅").font(.system(size: 8)); Text("今日").font(.system(size: 8, weight: .bold)).foregroundColor(textSub); Text(fmtMin(Int(uiState.todayStudyMinutes))).font(.system(size: 10, weight: .heavy)).foregroundColor(textW) }
                HStack(spacing: 3) { Text("📆").font(.system(size: 8)); Text("今週").font(.system(size: 8, weight: .bold)).foregroundColor(textSub); Text(fmtMin(Int(uiState.weekStudyMinutes))).font(.system(size: 10, weight: .heavy)).foregroundColor(textW) }
                HStack(spacing: 3) { Text("🗓️").font(.system(size: 8)); Text("月間").font(.system(size: 8, weight: .bold)).foregroundColor(textSub); Text(fmtMin(Int(uiState.monthStudyMinutes))).font(.system(size: 10, weight: .heavy)).foregroundColor(textW) }
            }
            .padding(8).background(bgCard).cornerRadius(10)
            Text(uiState.characterEmoji).font(.system(size: 26))
        }
        .padding(.horizontal, 20).padding(.vertical, 12)
    }

    // MARK: - Total
    private var totalCard: some View {
        let (h, m) = fmtHM(Int(uiState.totalStudyMinutes))
        return VStack(alignment: .leading, spacing: 8) {
            Text("🏆 累計勉強時間").font(.system(size: 12, weight: .bold)).foregroundColor(.white.opacity(0.8))
            HStack(alignment: .bottom, spacing: 0) {
                Text(h).font(.system(size: 52, weight: .black, design: .rounded)).foregroundColor(.white)
                Text("h").font(.system(size: 20, weight: .bold)).foregroundColor(.white.opacity(0.7)).padding(.bottom, 7)
                Spacer().frame(width: 6)
                Text(m).font(.system(size: 52, weight: .black, design: .rounded)).foregroundColor(.white)
                Text("m").font(.system(size: 20, weight: .bold)).foregroundColor(.white.opacity(0.7)).padding(.bottom, 7)
            }
            HStack(spacing: 14) {
                pill("🔥", "\(uiState.streakDays)日連続")
                pill("📖", "今日 \(uiState.todaySessions)回")
            }
        }
        .padding(22).frame(maxWidth: .infinity, alignment: .leading)
        .background(LinearGradient(colors: [accentBlue, accentIndigo, Color(hex: 0x8B5CF6)], startPoint: .topLeading, endPoint: .bottomTrailing))
        .cornerRadius(22).padding(.horizontal, 16)
    }

    private func pill(_ e: String, _ t: String) -> some View {
        HStack(spacing: 3) { Text(e).font(.system(size: 11)); Text(t).font(.system(size: 11, weight: .bold)).foregroundColor(.white) }
            .padding(.horizontal, 10).padding(.vertical, 5).background(Color.white.opacity(0.2)).cornerRadius(18)
    }

    // MARK: - Period Tabs
    private var periodTabs: some View {
        HStack(spacing: 4) {
            ForEach([RecordPeriod.today, .weekly, .monthly], id: \.self) { p in
                let sel = uiState.selectedPeriod == p
                Button(action: { viewModel.onIntent(intent: RecordIntentSelectPeriod(period: p)) }) {
                    Text(p.label).font(.system(size: 13, weight: sel ? .bold : .medium)).foregroundColor(sel ? accentCyan : textSub)
                        .frame(maxWidth: .infinity).padding(.vertical, 9).background(sel ? bgCard : .clear).cornerRadius(10)
                }.buttonStyle(.plain)
            }
        }
        .padding(4).background(bgSurface).cornerRadius(14).padding(.horizontal, 16)
    }

    // MARK: - Month Selector
    private var monthSelector: some View {
        let ms = uiState.availableMonths as! [YearMonth_]
        let idx = ms.firstIndex(where: { $0.year == uiState.selectedYear && $0.month == uiState.selectedMonth }) ?? 0
        return HStack {
            Button(action: { if idx + 1 < ms.count { let y = ms[idx+1]; viewModel.onIntent(intent: RecordIntentSelectMonth(year: y.year, month: y.month)) } }) {
                Image(systemName: "chevron.left").font(.system(size: 13, weight: .bold)).foregroundColor(idx < ms.count - 1 ? accentCyan : textDim)
            }.disabled(idx >= ms.count - 1)
            Spacer()
            Text("\(uiState.selectedYear)年\(uiState.selectedMonth)月").font(.system(size: 15, weight: .heavy)).foregroundColor(textW)
            Spacer()
            Button(action: { if idx > 0 { let y = ms[idx-1]; viewModel.onIntent(intent: RecordIntentSelectMonth(year: y.year, month: y.month)) } }) {
                Image(systemName: "chevron.right").font(.system(size: 13, weight: .bold)).foregroundColor(idx > 0 ? accentCyan : textDim)
            }.disabled(idx <= 0)
        }
        .padding(.horizontal, 24).padding(.vertical, 6)
    }

    // MARK: - Summary
    private var summaryCard: some View {
        let lbl: String = {
            switch uiState.selectedPeriod {
            case .today: return "今日の勉強時間"; case .weekly: return "今週の勉強時間"
            case .monthly: return "\(uiState.selectedYear)/\(uiState.selectedMonth) の勉強時間"; default: return "勉強時間"
            }
        }()
        return HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(lbl).font(.system(size: 12)).foregroundColor(textSub)
                Text(fmtMin(Int(uiState.periodTotalMinutes))).font(.system(size: 28, weight: .heavy)).foregroundColor(textW)
            }
            Spacer()
            if !uiState.genreBreakdown.isEmpty && uiState.selectedPeriod != .today { donut }
        }
        .padding(18).background(bgCard).cornerRadius(18).padding(.horizontal, 16)
    }

    private var donut: some View {
        Canvas { ctx, sz in
            let c = CGPoint(x: sz.width/2, y: sz.height/2), r = min(sz.width, sz.height)/2 - 5
            var s = Angle.degrees(-90)
            for i in uiState.genreBreakdown {
                let sw = Angle.degrees(Double(i.ratio) * 360)
                let p = Path { p in p.addArc(center: c, radius: r, startAngle: s, endAngle: s + sw, clockwise: false) }
                ctx.stroke(p, with: .color(genreColor(i.genre)), lineWidth: 7); s = s + sw
            }
        }.frame(width: 56, height: 56)
    }

    // MARK: - Chart
    private var chartCard: some View {
        Group {
            if uiState.selectedPeriod == .today {
                todayPieChartCard
            } else {
                barChartCardInner
            }
        }
    }

    private var todayPieChartCard: some View {
        let bd = uiState.genreBreakdown as! [GenreStudyTime]
        let total = Int(uiState.periodTotalMinutes)
        return VStack(alignment: .leading, spacing: 14) {
            Text("今日のジャンル別").font(.system(size: 14, weight: .bold)).foregroundColor(textW)
            if bd.isEmpty || total <= 0 {
                Text("この日の記録はまだありません")
                    .font(.system(size: 13))
                    .foregroundColor(textSub)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 28)
            } else {
                ZStack {
                    Canvas { ctx, sz in
                        let c = CGPoint(x: sz.width / 2, y: sz.height / 2), r = min(sz.width, sz.height) / 2 - 8
                        var s = Angle.degrees(-90)
                        for i in bd {
                            let sw = Angle.degrees(Double(i.ratio) * 360)
                            let p = Path { p in p.addArc(center: c, radius: r, startAngle: s, endAngle: s + sw, clockwise: false) }
                            ctx.stroke(p, with: .color(genreColor(i.genre)), lineWidth: 12)
                            s = s + sw
                        }
                    }
                    .frame(height: 200)
                    VStack(spacing: 2) {
                        Text(fmtMin(total)).font(.system(size: 22, weight: .heavy)).foregroundColor(textW)
                        Text("合計").font(.system(size: 10)).foregroundColor(textSub)
                    }
                }
                ForEach(Array(bd.enumerated()), id: \.offset) { _, i in
                    HStack(spacing: 8) {
                        Circle().fill(genreColor(i.genre)).frame(width: 10, height: 10)
                        Text(i.genre.emoji).font(.system(size: 14))
                        Text(i.genre.label).font(.system(size: 12, weight: .semibold)).foregroundColor(textW)
                        Spacer(minLength: 0)
                        Text("\(fmtMin(Int(i.minutes))) (\(Int(i.ratio * 100))%)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(genreColor(i.genre))
                    }
                }
            }
        }
        .padding(.horizontal, 12).padding(.vertical, 14).background(bgCard).cornerRadius(18).padding(.horizontal, 16)
    }

    private var barChartCardInner: some View {
        let bars = uiState.chartBars as! [ChartBar]
        let mx = bars.map { Int($0.minutes) }.max() ?? 1
        let sg = uiState.selectedGenre
        let useEqualWeek = uiState.selectedPeriod == .weekly && bars.count == 7
        return VStack(alignment: .leading, spacing: 10) {
            Text("学習時間の推移").font(.system(size: 14, weight: .bold)).foregroundColor(textW)
            if !bars.isEmpty {
                Group {
                    if useEqualWeek {
                        HStack(alignment: .bottom, spacing: 3) {
                            ForEach(Array(bars.enumerated()), id: \.offset) { _, b in
                                barItem(b, mx: mx, sg: sg, cnt: bars.count, fillCell: true)
                            }
                        }
                        .frame(height: 150)
                    } else {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(alignment: .bottom, spacing: 4) {
                                ForEach(Array(bars.enumerated()), id: \.offset) { _, b in barItem(b, mx: mx, sg: sg, cnt: bars.count, fillCell: false) }
                            }
                            .frame(height: 150)
                            .padding(.bottom, 2)
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 12).padding(.vertical, 14).background(bgCard).cornerRadius(18).padding(.horizontal, 16)
    }

    @ViewBuilder
    private func barItem(_ b: ChartBar, mx: Int, sg: GenreInfo?, cnt: Int, fillCell: Bool = false) -> some View {
        let fr = CGFloat(b.minutes) / CGFloat(max(mx, 1))
        let bh = max(120 * fr, 3)
        let bw: CGFloat = cnt <= 7 ? 34 : (cnt <= 14 ? 26 : 20)
        let iso = "\(b.isoDate)"
        let inner = VStack(spacing: 0) {
            if b.minutes > 0 { Text("\(b.minutes)").font(.system(size: 8, weight: .bold)).foregroundColor(textSub); Spacer().frame(height: 2) }
            Spacer()
            if let g = sg { RoundedRectangle(cornerRadius: 5).fill(genreColor(g)).frame(height: bh) }
            else {
                let gm = b.genreMinutes as! [GenreInfo: KotlinInt]; let t = gm.values.reduce(0) { $0 + $1.intValue }; let st = max(t, 1)
                VStack(spacing: 0) {
                    let sr = gm.sorted { $0.value.intValue > $1.value.intValue }
                    ForEach(Array(sr.enumerated()), id: \.offset) { _, e in Rectangle().fill(genreColor(e.key)).frame(height: max(bh * CGFloat(e.value.intValue) / CGFloat(st), 2)) }
                }.clipShape(RoundedRectangle(cornerRadius: 5))
            }
            Spacer().frame(height: 3)
            if !iso.isEmpty {
                VStack(spacing: 1) {
                    Text(weekdayJpFromIso(iso))
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(weekdayAxisColor(iso: iso))
                    Text(b.label).font(.system(size: 7)).foregroundColor(textDim).lineLimit(1)
                }
            } else {
                Text(b.label).font(.system(size: 7)).foregroundColor(textDim).lineLimit(1)
            }
        }
        if fillCell {
            inner.frame(maxWidth: .infinity)
        } else {
            inner.frame(width: bw)
        }
    }

    // MARK: - Genre
    private var genreCard: some View {
        let bd = uiState.genreBreakdown
        return VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("ジャンル別内訳").font(.system(size: 14, weight: .bold)).foregroundColor(textW)
                Spacer()
                if uiState.selectedGenre != nil {
                    Button(action: { viewModel.onIntent(intent: RecordIntentSelectGenre(genre: nil)) }) {
                        Text("全表示").font(.system(size: 10, weight: .bold)).foregroundColor(accentCyan).padding(.horizontal, 8).padding(.vertical, 3).background(bgSurface).cornerRadius(6)
                    }.buttonStyle(.plain)
                }
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(Array(bd.enumerated()), id: \.offset) { _, i in
                        let sel = uiState.selectedGenre == i.genre; let c = genreColor(i.genre)
                        Button(action: { viewModel.onIntent(intent: RecordIntentSelectGenre(genre: i.genre)) }) {
                            HStack(spacing: 3) { Text(i.genre.emoji).font(.system(size: 12)); Text(i.genre.label).font(.system(size: 11, weight: sel ? .bold : .medium)).foregroundColor(sel ? c : textSub) }
                                .padding(.horizontal, 12).padding(.vertical, 6).background(sel ? c.opacity(0.2) : bgSurface)
                                .overlay(RoundedRectangle(cornerRadius: 18).stroke(sel ? c : .clear, lineWidth: 1.5)).cornerRadius(18)
                        }.buttonStyle(.plain)
                    }
                }
            }
            ForEach(Array(bd.enumerated()), id: \.offset) { _, i in genreRow(i, hi: uiState.selectedGenre == nil || uiState.selectedGenre == i.genre) }
        }
        .padding(18).background(bgCard).cornerRadius(18).padding(.horizontal, 16)
    }

    private func genreRow(_ i: GenreStudyTime, hi: Bool) -> some View {
        let c = genreColor(i.genre); let a: Double = hi ? 1 : 0.3
        return HStack(spacing: 6) {
            Text(i.genre.emoji).font(.system(size: 14))
            VStack(alignment: .leading, spacing: 3) {
                HStack { Text(i.genre.label).font(.system(size: 12, weight: .semibold)).foregroundColor(textW.opacity(a)); Spacer(); Text("\(fmtMin(Int(i.minutes))) (\(Int(i.ratio * 100))%)").font(.system(size: 11, weight: .bold)).foregroundColor(c.opacity(a)) }
                GeometryReader { g in ZStack(alignment: .leading) { RoundedRectangle(cornerRadius: 3).fill(bgDark).frame(height: 6); RoundedRectangle(cornerRadius: 3).fill(c.opacity(a)).frame(width: g.size.width * min(CGFloat(i.ratio), 1), height: 6) } }.frame(height: 6)
            }
        }
    }
}

private typealias YearMonth_ = YearMonth
