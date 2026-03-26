import SwiftUI

/// 勉強・冒険中の画面
/// タイマーとキャラクターの戦闘/探索描写を表示する
struct StudyQuestView: View {
    @Environment(\.dismiss) var dismiss
    @State private var timeElapsed: TimeInterval = 0
    @State private var isQuesting = true
    @State private var logs: [String] = ["はじまりの森に入った！", "スライムがあらわれた！"]
    
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // タイマー表示
                VStack(spacing: 8) {
                    Text("勉強中...")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    
                    Text(formatTime(timeElapsed))
                        .font(.system(size: 60, weight: .bold, design: .monospaced))
                        .foregroundColor(.primary)
                }
                .padding(.top, 40)
                
                // 冒険の描写エリア
                ZStack {
                    RoundedRectangle(cornerRadius: 24)
                        .fill(Color(UIColor.secondarySystemBackground))
                        .shadow(radius: 10)
                    
                    VStack {
                        // 戦闘/探索アニメーション（仮）
                        HStack(spacing: 40) {
                            VStack {
                                Text("🧙‍♂️")
                                    .font(.system(size: 80))
                                Text("プレイヤー")
                                    .font(.caption)
                                    .fontWeight(.bold)
                            }
                            
                            Text("VS")
                                .font(.title)
                                .fontWeight(.heavy)
                                .foregroundColor(.red)
                            
                            VStack {
                                Text("👾")
                                    .font(.system(size: 80))
                                Text("モンスター")
                                    .font(.caption)
                                    .fontWeight(.bold)
                            }
                        }
                        .padding(.top, 20)
                        
                        Divider().padding(.vertical)
                        
                        // ログ表示
                        ScrollView {
                            VStack(alignment: .leading, spacing: 8) {
                                ForEach(logs.reversed(), id: \.self) { log in
                                    Text("> \(log)")
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
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
                
                // 終了ボタン
                Button(action: {
                    // TODO: KMP側の勉強終了ロジックを呼ぶ
                    dismiss()
                }) {
                    Text("勉強を終了する")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.red)
                        .cornerRadius(16)
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 40)
            }
            .navigationTitle("冒険中")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("キャンセル") {
                        dismiss()
                    }
                }
            }
            .onReceive(timer) { _ in
                timeElapsed += 1
                if Int(timeElapsed) % 10 == 0 {
                    addRandomLog()
                }
            }
        }
    }
    
    private func formatTime(_ seconds: TimeInterval) -> String {
        let h = Int(seconds) / 3600
        let m = (Int(seconds) % 3600) / 60
        let s = Int(seconds) % 60
        return String(format: "%02d:%02d:%02d", h, m, s)
    }
    
    private func addRandomLog() {
        let messages = [
            "モンスターに10のダメージを与えた！",
            "経験値を5獲得した！",
            "ゴールドを2手に入れた！",
            "少し喉が渇いた気がする...",
            "深い霧が立ち込めている..."
        ]
        if let randomMessage = messages.randomElement() {
            logs.append(randomMessage)
        }
    }
}

struct StudyQuestView_Previews: PreviewProvider {
    static var previews: some View {
        StudyQuestView()
    }
}
