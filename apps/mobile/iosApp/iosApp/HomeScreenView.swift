import SwiftUI
import Shared

// MARK: - 冒険時間（1〜60 分・刻み）＋ UserDefaults（キーは Android KeyValueStore と同じ）
private enum HomeStudyMinutesPersisted {
    static let key = "home_study_minutes"

    static func snapToValid(_ m: Int) -> Int {
        let c = min(max(m, 1), 60)
        if c <= 1 { return 1 }
        if c < 5 { return 5 }
        return min((c + 2) / 5 * 5, 60)
    }

    static func increase(_ current: Int) -> Int {
        let c = min(max(current, 1), 60)
        if c >= 60 { return 60 }
        if c <= 1 { return 5 }
        let ceil5 = (c + 4) / 5 * 5
        return ceil5 > c ? ceil5 : min(c + 5, 60)
    }

    static func decrease(_ current: Int) -> Int {
        let c = min(max(current, 1), 60)
        if c <= 1 { return 1 }
        if c <= 5 { return 1 }
        let floor5 = c / 5 * 5
        return floor5 < c ? floor5 : max(c - 5, 5)
    }

    static func load() -> Int {
        guard let s = UserDefaults.standard.string(forKey: key), let v = Int(s) else { return 25 }
        return snapToValid(v)
    }

    static func save(_ minutes: Int) {
        UserDefaults.standard.set(String(minutes), forKey: key)
    }
}

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
private let fireGradient = [Color(hex: 0xEF4444), Color(hex: 0xF59E0B)]

struct HomeScreenView: View {
    @State private var showStudySheet = false
    @State private var studyMinutes: Int
    @State private var selectedGenreSlug = "general"
    @State private var isBouncing = false
    @State private var messageIndex = 0
    @State private var showAddGenreSheet = false
    @State private var newGenreLabel = ""
    @State private var newGenreEmoji = "📖"
    @State private var genrePendingDelete: MasterStudyGenre? = nil

    private let homeViewModel = KoinHelperKt.getHomeViewModel()
    @State private var homeState: HomeUiState?

    private let messages = ["今日の特訓も頑張ろうな！", "知識こそ最強の武器だ。", "お前の成長、楽しみにしてるぞ。", "さぁ、冒険の時間だ！", "集中すれば、何でもできる。"]
    let messageTimer = Timer.publish(every: 4, on: .main, in: .common).autoconnect()

    init() {
        _showStudySheet = State(initialValue: false)
        _studyMinutes = State(initialValue: HomeStudyMinutesPersisted.load())
        _selectedGenreSlug = State(initialValue: "general")
        _isBouncing = State(initialValue: false)
        _messageIndex = State(initialValue: 0)
        _showAddGenreSheet = State(initialValue: false)
        _newGenreLabel = State(initialValue: "")
        _newGenreEmoji = State(initialValue: "📖")
        _homeState = State(initialValue: nil)
    }

    private var genreList: [(slug: String, emoji: String, label: String)] {
        let fromServer = (homeState?.genres ?? []).map { (slug: $0.slug, emoji: $0.emoji, label: $0.label) }
        return fromServer.isEmpty ? [("general", "📚", "総合")] : fromServer
    }

    var body: some View {
        VStack(spacing: 0) {
            homeHeader
            adventureContextRow
            Spacer()
            characterArea
            Spacer()
            timeSelector
            Spacer().frame(height: 28)
            startButton
            Spacer().frame(height: 24)
            Spacer().frame(height: 90)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(LinearGradient(colors: [bgDark, Color(hex: 0x0F172A)], startPoint: .top, endPoint: .bottom).ignoresSafeArea())
        .fullScreenCover(isPresented: $showStudySheet, onDismiss: {
            homeViewModel.onIntent(intent: HomeIntentRefresh())
        }) {
            StudyQuestScreenView(initialStudyMinutes: studyMinutes, genreId: selectedGenreSlug, dungeonName: homeState?.selectedDungeonName)
        }
        .sheet(isPresented: $showAddGenreSheet) { genreManageSheet }
        .alert("削除の確認", isPresented: Binding(
            get: { genrePendingDelete != nil },
            set: { if !$0 { genrePendingDelete = nil } }
        )) {
            Button("キャンセル", role: .cancel) { genrePendingDelete = nil }
            Button("削除", role: .destructive) {
                if let g = genrePendingDelete {
                    homeViewModel.onIntent(intent: HomeIntentDeleteGenre(genreId: g.id))
                }
                genrePendingDelete = nil
            }
        } message: {
            if let g = genrePendingDelete {
                Text("\(g.emoji) \(g.label) を削除しますか？\n記録の勉強時間は「削除済み課題」として残ります。")
            }
        }
        .onReceive(messageTimer) { _ in withAnimation(.easeInOut(duration: 0.3)) { messageIndex = (messageIndex + 1) % messages.count } }
        .onAppear { homeViewModel.onIntent(intent: HomeIntentRefresh()) }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in self.homeState = homeViewModel.uiState.value as? HomeUiState }
        .onChange(of: studyMinutes) { _, newValue in
            HomeStudyMinutesPersisted.save(newValue)
        }
    }

    // MARK: - ダンジョン / ジャンル（横スクロール・勉強時間はヘッダー）
    private var adventureContextRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                contextStatChip(title: "ダンジョン", value: homeState?.selectedDungeonName ?? "—")
                genreMenuChip
                Button(action: { showAddGenreSheet = true }) {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(accentIndigo)
                        .frame(width: 40, height: 40)
                        .background(bgCard)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(accentCyan.opacity(0.45), lineWidth: 1))
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 6)
        }
    }

    private func contextStatChip(title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.system(size: 9)).foregroundColor(textSub).lineLimit(1)
            Text(value).font(.system(size: 11, weight: .bold)).foregroundColor(textW).lineLimit(1)
        }
        .frame(minWidth: title == "ダンジョン" ? 96 : (title == "回想" ? 56 : 64), alignment: .leading)
        .padding(.horizontal, 10).padding(.vertical, 8)
        .background(bgCard).cornerRadius(12)
    }

    private var genreMenuChip: some View {
        let selected = genreList.first { $0.slug == selectedGenreSlug } ?? genreList.first!
        return Menu {
            ForEach(genreList, id: \.slug) { g in
                Button(action: { selectedGenreSlug = g.slug }) {
                    HStack {
                        if selectedGenreSlug == g.slug {
                            Image(systemName: "checkmark").font(.system(size: 11, weight: .bold)).foregroundColor(accentCyan)
                        }
                        Text("\(g.emoji) \(g.label)").foregroundColor(textW)
                    }
                }
            }
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                Text("ジャンル").font(.system(size: 9)).foregroundColor(textSub)
                HStack(spacing: 4) {
                    Text("\(selected.emoji) \(selected.label)").font(.system(size: 11, weight: .bold)).foregroundColor(textW).lineLimit(1)
                    Image(systemName: "chevron.down").font(.system(size: 10, weight: .bold)).foregroundColor(accentCyan)
                }
            }
            .frame(minWidth: 108, maxWidth: 168, alignment: .leading)
            .padding(.horizontal, 10).padding(.vertical, 8)
            .background(bgCard).cornerRadius(12)
        }
    }

    // MARK: - Header
    private var homeHeader: some View {
        HStack {
            HStack(spacing: 6) {
                Text("📖").font(.system(size: 16))
                VStack(alignment: .leading, spacing: 1) {
                    Text("累計勉強").font(.system(size: 9)).foregroundColor(textSub)
                    Text(homeState?.formattedStudyTime ?? "0h 0m").font(.system(size: 14, weight: .heavy)).foregroundColor(textW)
                }
            }
            .padding(.horizontal, 12).padding(.vertical, 8)
            .background(bgCard).cornerRadius(14)

            Spacer()

            HStack(spacing: 6) {
                Text("💎").font(.system(size: 16))
                VStack(alignment: .leading, spacing: 1) {
                    Text("知識の結晶").font(.system(size: 9)).foregroundColor(textSub)
                    Text("\(homeState?.stones ?? 0)").font(.system(size: 14, weight: .heavy)).foregroundColor(textW)
                }
            }
            .padding(.horizontal, 12).padding(.vertical, 8)
            .background(bgCard).cornerRadius(14)
        }
        .padding(16)
    }

    // MARK: - Character
    private var characterArea: some View {
        ZStack {
            Circle()
                .fill(RadialGradient(colors: [accentBlue.opacity(0.2), accentIndigo.opacity(0.08), .clear], center: .center, startRadius: 20, endRadius: 140))
                .frame(width: 260, height: 260)

            VStack(spacing: 0) {
                VStack(spacing: 0) {
                    HStack(spacing: 6) {
                        Text("💬").font(.system(size: 12))
                        Text("「\(messages[messageIndex])」").font(.system(size: 13, weight: .bold)).foregroundColor(textW)
                    }
                    .padding(.horizontal, 16).padding(.vertical, 10)
                    .background(bgCard).cornerRadius(18)

                    Triangle().fill(bgCard).frame(width: 14, height: 8)
                }
                Spacer().frame(height: 6)
                Group {
                    if UIImage(named: "sprite_player_idle_1") != nil {
                        Image("sprite_player_idle_1")
                            .resizable()
                            .interpolation(.none)
                            .scaledToFit()
                            .frame(width: 160, height: 160)
                    } else {
                        Text("🧙‍♂️").font(.system(size: 90))
                    }
                }
                    .offset(y: isBouncing ? -10 : 0)
                    .animation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true), value: isBouncing)
                    .onAppear { isBouncing = true }

                Spacer().frame(height: 4)
                HStack(spacing: 6) {
                    Text(homeState?.mainCharacter?.character?.name ?? "冒険者").font(.system(size: 15, weight: .bold)).foregroundColor(textW)
                    Text("Lv.\(homeState?.mainCharacter?.level ?? 1)")
                        .font(.system(size: 11, weight: .bold)).foregroundColor(accentCyan)
                        .padding(.horizontal, 8).padding(.vertical, 2).background(accentCyan.opacity(0.15)).cornerRadius(8)
                }
            }
        }
    }

    // MARK: - Time（1〜60 分、Android と同じ ± ルール）
    private var timeSelector: some View {
        VStack(spacing: 8) {
            Text("⏱ 冒険時間").font(.system(size: 12, weight: .bold)).foregroundColor(textSub)
            HStack(spacing: 0) {
                Button(action: { studyMinutes = HomeStudyMinutesPersisted.decrease(studyMinutes) }) {
                    Circle().fill(accentBlue.opacity(studyMinutes > 1 ? 0.2 : 0.08)).frame(width: 34, height: 34)
                        .overlay(
                            Image(systemName: "chevron.left")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(studyMinutes > 1 ? accentBlue : textSub.opacity(0.35))
                        )
                }
                .disabled(studyMinutes <= 1)
                VStack(spacing: 0) {
                    Text("\(studyMinutes)").font(.system(size: 38, weight: .black, design: .rounded)).foregroundColor(textW)
                    Text("分").font(.system(size: 13, weight: .bold)).foregroundColor(textSub)
                }
                .frame(width: 90)
                Button(action: { studyMinutes = HomeStudyMinutesPersisted.increase(studyMinutes) }) {
                    Circle().fill(accentBlue.opacity(studyMinutes < 60 ? 0.2 : 0.08)).frame(width: 34, height: 34)
                        .overlay(
                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(studyMinutes < 60 ? accentBlue : textSub.opacity(0.35))
                        )
                }
                .disabled(studyMinutes >= 60)
            }
            .padding(.horizontal, 8).padding(.vertical, 4).background(bgCard).cornerRadius(18)
        }
        .padding(.horizontal, 32)
    }

    // MARK: - Genre manage sheet（追加・削除）
    private var deletableGenres: [MasterStudyGenre] {
        (homeState?.genres ?? []).filter { !$0.isDefault }
    }

    private var genreManageSheet: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("追加・削除はここから").font(.system(size: 12)).foregroundColor(textSub)

                    Text("新しいジャンルを追加").font(.system(size: 13, weight: .bold)).foregroundColor(accentCyan)
                    Text("ジャンル名").font(.system(size: 12, weight: .semibold)).foregroundColor(textSub)
                    TextField("例: 英語、物理", text: $newGenreLabel)
                        .foregroundColor(textW)
                        .padding(12)
                        .background(bgCard)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(accentCyan.opacity(0.35), lineWidth: 1))

                    Text("絵文字").font(.system(size: 12, weight: .semibold)).foregroundColor(textSub)
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 44))], spacing: 10) {
                        ForEach(["📖", "📐", "🧪", "🌍", "🎵", "⚽", "🎮", "✏️"], id: \.self) { e in
                            Button(action: { newGenreEmoji = e }) {
                                Text(e).font(.system(size: 28)).frame(maxWidth: .infinity).padding(8)
                                    .background(newGenreEmoji == e ? accentCyan.opacity(0.22) : Color.clear).cornerRadius(10)
                            }
                        }
                    }

                    Button(action: {
                        guard !newGenreLabel.isEmpty else { return }
                        homeViewModel.onIntent(intent: HomeIntentAddGenre(label: newGenreLabel, emoji: newGenreEmoji, colorHex: "#6B7280"))
                        newGenreLabel = ""; newGenreEmoji = "📖"; showAddGenreSheet = false
                    }) {
                        Text("ジャンルを追加").font(.system(size: 16, weight: .bold)).foregroundColor(.white).frame(maxWidth: .infinity).padding(.vertical, 14)
                            .background(
                                LinearGradient(
                                    colors: newGenreLabel.isEmpty ? [Color.gray.opacity(0.5), Color.gray.opacity(0.4)] : [accentBlue, accentIndigo],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .cornerRadius(14)
                    }.disabled(newGenreLabel.isEmpty)

                    if !deletableGenres.isEmpty {
                        Divider().background(Color(hex: 0x263859))
                        Text("追加したジャンルを削除").font(.system(size: 13, weight: .bold)).foregroundColor(accentCyan)
                        ForEach(deletableGenres, id: \.id) { g in
                            HStack {
                                Text("\(g.emoji) \(g.label)").font(.system(size: 15, weight: .medium)).foregroundColor(textW).lineLimit(1)
                                Spacer()
                                Button("削除") { genrePendingDelete = g }
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(Color(hex: 0xEF4444))
                            }
                            .padding(12)
                            .background(bgCard)
                            .cornerRadius(12)
                        }
                    }
                }
                .padding(24)
            }
            .background(LinearGradient(colors: [bgDark, Color(hex: 0x0F172A)], startPoint: .top, endPoint: .bottom))
            .navigationTitle("ジャンル管理")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("閉じる") { showAddGenreSheet = false }
                }
            }
        }
    }

    // MARK: - Start Button
    private var startButton: some View {
        Button(action: { showStudySheet = true }) {
            HStack(spacing: 8) {
                Text("⚔️").font(.system(size: 20))
                Text("冒険に出発する").font(.system(size: 17, weight: .heavy))
            }
            .foregroundColor(.white).frame(maxWidth: .infinity).padding(.vertical, 16)
            .background(LinearGradient(colors: fireGradient, startPoint: .leading, endPoint: .trailing))
            .cornerRadius(22).shadow(color: Color(hex: 0xF59E0B).opacity(0.4), radius: 10, y: 5)
        }
        .padding(.horizontal, 32)
    }
}

private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        Path { p in p.move(to: CGPoint(x: rect.midX - 7, y: 0)); p.addLine(to: CGPoint(x: rect.midX, y: rect.maxY)); p.addLine(to: CGPoint(x: rect.midX + 7, y: 0)); p.closeSubpath() }
    }
}
