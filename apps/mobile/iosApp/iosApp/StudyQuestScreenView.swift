import SwiftUI
import Shared

struct StudyQuestScreenView: View {
    @Environment(\.dismiss) var dismiss
    let initialStudyMinutes: Int

    /// ViewModel は KMP 側で状態をすべて管理する。View はポーリングで取得するだけ。
    private let viewModel = StudyQuestViewModel()
    @State private var uiState: StudyQuestUiState

    init(initialStudyMinutes: Int) {
        self.initialStudyMinutes = initialStudyMinutes
        // 初期値は ViewModel のデフォルトに合わせる（onAppear で即上書きされる）
        _uiState = State(initialValue: StudyQuestUiState(
            type: .study,
            status: .ready,
            targetStudyMinutes: Int32(initialStudyMinutes),
            targetBreakMinutes: 5,
            elapsedSeconds: 0,
            isOvertime: false,
            currentLog: [],
            displayTime: "25:00"
        ))
    }
    
    var body: some View {
        NavigationView {
            Group {
                if uiState.status == .finished {
                    resultScreen
                } else {
                    mainQuestView
                }
            }
            .navigationTitle(uiState.type == .study ? "冒険中" : "休憩中")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("離脱") {
                        viewModel.onIntent(intent: StudyQuestIntentStopQuest())
                        dismiss()
                    }
                }
            }
            .onAppear {
                viewModel.onIntent(intent: StudyQuestIntentStartQuest(studyMinutes: Int32(initialStudyMinutes)))
            }
            .onReceive(Timer.publish(every: 0.5, on: .main, in: .common).autoconnect()) { _ in
                self.uiState = viewModel.uiState.value as! StudyQuestUiState
            }
        }
    }
    
    // MARK: - メインクエスト画面
    private var mainQuestView: some View {
        VStack(spacing: 20) {
            // タイマー表示
            VStack(spacing: 8) {
                Text(uiState.isOvertime ? "限界突破中! (延長)" : (uiState.type == .study ? "今回の特訓は..." : "リフレッシュ中"))
                    .font(.headline)
                    .foregroundColor(uiState.isOvertime ? .purple : .secondary)
                
                Text(uiState.displayTime)
                    .font(.system(size: 80, weight: .bold, design: .monospaced))
                    .foregroundColor(uiState.isOvertime ? .pink : (uiState.type == .study ? .primary : .green))
                    .shadow(color: uiState.isOvertime ? .pink.opacity(0.4) : .clear, radius: 10)
            }
            .padding(.top, 40)
            
            // 冒険の描写エリア
            ZStack {
                RoundedRectangle(cornerRadius: 32, style: .continuous)
                    .fill(uiState.isOvertime ? Color.purple.opacity(0.1) : Color(UIColor.secondarySystemBackground))
                    .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)
                
                VStack {
                    HStack(spacing: 40) {
                        VStack {
                            Text(uiState.type == .study ? "🧙‍♂️" : "🏕️")
                                .font(.system(size: uiState.isOvertime ? 90 : 80))
                                .scaleEffect(uiState.isOvertime ? 1.1 : 1.0)
                            Text(uiState.type == .study ? "プレイヤー" : "キャンプ")
                                .font(.caption).fontWeight(.bold)
                        }
                        
                        Text(uiState.type == .study ? "VS" : "&&")
                            .font(.title).fontWeight(.heavy)
                            .foregroundColor(uiState.type == .study ? .red : .blue)
                        
                        VStack {
                            Text(uiState.type == .study ? "👾" : "🍵")
                                .font(.system(size: uiState.isOvertime ? 90 : 80))
                            Text(uiState.type == .study ? "モンスター" : "お茶")
                                .font(.caption).fontWeight(.bold)
                        }
                    }
                    .padding(.top, 20)
                    
                    Divider().padding(.vertical)
                    
                    // ログ表示
                    ScrollView {
                        VStack(alignment: .leading, spacing: 8) {
                            if uiState.isOvertime {
                                Text(">> 超集中モード発動中！ <<")
                                    .font(.caption).bold()
                                    .foregroundColor(.purple)
                            }
                            ForEach(uiState.currentLog, id: \.self) { log in
                                Text("> \(log)")
                                    .font(.subheadline)
                                    .foregroundColor(uiState.isOvertime ? .purple : .secondary)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(.horizontal)
                }
                .padding()
            }
            .frame(maxHeight: 350)
            .padding(.horizontal)
            
            Spacer()
            
            // 操作ボタン
            VStack(spacing: 16) {
                if uiState.type == .study {
                    Button(action: {
                        viewModel.onIntent(intent: StudyQuestIntentFinishSession())
                    }) {
                        Text("休憩する (結果を見る)")
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .cornerRadius(20)
                            .shadow(radius: 5)
                    }
                }
                
                HStack(spacing: 30) {
                    Button(action: {
                        viewModel.onIntent(intent: StudyQuestIntentTogglePause())
                    }) {
                        HStack {
                            Image(systemName: uiState.status == .running ? "pause.fill" : "play.fill")
                            Text(uiState.status == .running ? "一時停止" : "再開する")
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(uiState.status == .running ? Color.orange : Color.blue)
                        .cornerRadius(20)
                    }
                    
                    Button(action: {
                        viewModel.onIntent(intent: StudyQuestIntentStopQuest())
                        dismiss()
                    }) {
                        Text("中止する")
                            .font(.headline)
                            .foregroundColor(.red)
                            .padding(.horizontal, 20)
                    }
                }
            }
            .padding(.horizontal, 40)
            .padding(.bottom, 40)
        }
    }
    
    // MARK: - 結果画面
    private var resultScreen: some View {
        VStack(spacing: 30) {
            Text(uiState.type == .study ? "クエスト達成！" : "休憩完了！")
                .font(.largeTitle)
                .fontWeight(.bold)
                .foregroundColor(uiState.type == .study ? .orange : .green)
            
            ZStack {
                Circle()
                    .fill(uiState.type == .study ? Color.orange.opacity(0.1) : Color.green.opacity(0.1))
                    .frame(width: 200, height: 200)
                
                Text(uiState.type == .study ? "🏁" : "✨")
                    .font(.system(size: 100))
            }
            
            Text(uiState.type == .study ? "25分間の猛特訓に成功した！\n経験値と素材を手に入れたぞ。" : "リフレッシュできたようだ。\n次の冒険の準備はいいか？")
                .font(.body)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            
            Button(action: {
                withAnimation {
                    viewModel.onIntent(intent: StudyQuestIntentNextSession())
                }
            }) {
                Text(uiState.type == .study ? "休憩を開始する" : "次の冒険へ出発する")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(uiState.type == .study ? Color.green : Color.blue)
                    .cornerRadius(20)
            }
            .padding(.horizontal, 40)
            
            Button(action: {
                viewModel.onIntent(intent: StudyQuestIntentStopQuest())
                dismiss()
            }) {
                Text("街に戻る")
                    .foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(UIColor.systemBackground))
    }
}

struct StudyQuestScreenView_Previews: PreviewProvider {
    static var previews: some View {
        StudyQuestScreenView(initialStudyMinutes: 25)
    }
}
