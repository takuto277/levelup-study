import SwiftUI

/// 冒険画面（タブ①: 左端）
/// 勉強中に自動進行するダンジョンの状況を確認する
struct QuestScreenView: View {
    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "map.fill")
                .font(.system(size: 60))
                .foregroundColor(.orange)

            Text("冒険")
                .font(.title)
                .fontWeight(.bold)

            Text("勉強を開始するとパーティがダンジョンを探索します")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            // 仮のダンジョン情報
            VStack(spacing: 12) {
                HStack {
                    Text("現在のダンジョン")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("はじまりの森 - Stage 3")
                        .fontWeight(.semibold)
                }
                HStack {
                    Text("パーティ状態")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("💤 休息中")
                }
                HStack {
                    Text("最高到達ステージ")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("Stage 5")
                        .fontWeight(.semibold)
                }
            }
            .padding()
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(16)
            .padding(.horizontal, 24)

            Spacer()
            Spacer().frame(height: 90) // タブバー分の余白
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(UIColor.systemGroupedBackground))
    }
}

struct QuestScreenView_Previews: PreviewProvider {
    static var previews: some View {
        QuestScreenView()
    }
}
