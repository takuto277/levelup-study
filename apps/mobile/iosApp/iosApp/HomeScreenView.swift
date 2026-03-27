import SwiftUI

/// ホーム画面（タブ③: 中央）
/// キャラクター表示 + ステータス + 勉強開始の導線
struct HomeScreenView: View {
    @State private var isBouncing = false
    @State private var showStudySheet = false
    @State private var studyMinutes = 25

    var body: some View {
        VStack(spacing: 0) {
            // ヘッダー（累計勉強時間 + ガチャ石）
            headerView

            Spacer()

            // キャラクター表示エリア
            characterView

            Spacer()
            
            // 勉強時間の調整
            VStack(spacing: 8) {
                Text("今回の冒険時間")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                HStack(spacing: 20) {
                    Button(action: { if studyMinutes > 1 { studyMinutes -= 1 } }) {
                        Image(systemName: "minus.circle.fill")
                            .font(.title2)
                            .foregroundColor(.blue)
                    }
                    
                    Text("\(studyMinutes) 分")
                        .font(.title2)
                        .fontWeight(.bold)
                        .frame(width: 80)
                    
                    Button(action: { if studyMinutes < 120 { studyMinutes += 1 } }) {
                        Image(systemName: "plus.circle.fill")
                            .font(.title2)
                            .foregroundColor(.blue)
                    }
                }
                .padding(.vertical, 8)
                .padding(.horizontal, 16)
                .background(Color(UIColor.secondarySystemGroupedBackground))
                .cornerRadius(12)
            }
            .padding(.bottom, 20)

            // 勉強開始ボタン
            studyStartButton

            Spacer()

            // タップバー分の余白
            Spacer().frame(height: 90)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(UIColor.systemGroupedBackground))
        .fullScreenCover(isPresented: $showStudySheet) {
            StudyQuestScreenView(initialStudyMinutes: studyMinutes)
        }
    }

    // MARK: - Header

    private var headerView: some View {
        HStack {
            // 累計勉強時間
            HStack(spacing: 8) {
                Image(systemName: "clock.fill")
                    .foregroundColor(.blue)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Total Study")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Text("124h 30m")
                        .font(.headline)
                        .fontWeight(.bold)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 2, y: 1)

            Spacer()

            // 知識の結晶（ガチャ石）
            HStack(spacing: 8) {
                Image(systemName: "sparkles")
                    .foregroundColor(.yellow)
                VStack(alignment: .leading, spacing: 2) {
                    Text("知識の結晶")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Text("1,250")
                        .font(.headline)
                        .fontWeight(.bold)
                }
                Button(action: {}) {
                    Image(systemName: "plus.circle.fill")
                        .foregroundColor(.green)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(12)
            .shadow(color: .black.opacity(0.05), radius: 2, y: 1)
        }
        .padding()
    }

    // MARK: - Study Button

    private var studyStartButton: some View {
        Button(action: {
            showStudySheet = true
        }) {
            HStack(spacing: 12) {
                Image(systemName: "flame.fill")
                    .font(.title2)
                Text("勉強をスタートする")
                    .font(.headline)
                    .fontWeight(.bold)
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [.red, .orange]),
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .cornerRadius(20)
            .shadow(color: .orange.opacity(0.3), radius: 10, y: 5)
            .padding(.horizontal, 40)
        }
    }

    // MARK: - Character

    private var characterView: some View {
        ZStack {
            // 背景グラデーション円
            Circle()
                .fill(
                    RadialGradient(
                        gradient: Gradient(colors: [.blue.opacity(0.2), .clear]),
                        center: .center,
                        startRadius: 20,
                        endRadius: 150
                    )
                )
                .frame(width: 300, height: 300)

            VStack(spacing: 8) {
                // 吹き出し
                Text("「今日の特訓も頑張ろうな！」")
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.white)
                    .cornerRadius(20)
                    .shadow(radius: 4)

                // キャラクター（仮）
                Text("🧙‍♂️")
                    .font(.system(size: 130))
                    .offset(y: isBouncing ? -12 : 0)
                    .animation(
                        Animation.easeInOut(duration: 1.5)
                            .repeatForever(autoreverses: true),
                        value: isBouncing
                    )
                    .onAppear { isBouncing = true }
            }
        }
    }
}

struct HomeScreenView_Previews: PreviewProvider {
    static var previews: some View {
        HomeScreenView()
    }
}
