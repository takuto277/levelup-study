import SwiftUI

/// ホーム画面（タブ③: 中央）
/// キャラクター表示 + ステータス + 勉強開始の導線
struct HomeScreenView: View {
    @State private var isBouncing = false

    var body: some View {
        VStack(spacing: 0) {
            // ヘッダー（累計勉強時間 + ガチャ石）
            headerView

            Spacer()

            // キャラクター表示エリア
            characterView

            Spacer()

            // タブバー分の余白
            Spacer().frame(height: 90)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(UIColor.systemGroupedBackground))
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
