import SwiftUI

/// 記録画面（タブ⑤: 右端）
/// 勉強時間の統計データを日/週/月で可視化する
struct AnalyticsScreen: View {
    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "chart.bar.fill")
                .font(.system(size: 60))
                .foregroundColor(.green)

            Text("記録")
                .font(.title)
                .fontWeight(.bold)

            Text("これまでの勉強時間を振り返ろう")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            // 仮の統計カード
            VStack(spacing: 12) {
                HStack {
                    Text("今日")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("2h 15m")
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                }
                HStack {
                    Text("今週")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("12h 45m")
                        .fontWeight(.bold)
                }
                HStack {
                    Text("今月")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("48h 30m")
                        .fontWeight(.bold)
                }
                Divider()
                HStack {
                    Text("🔥 連続日数")
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("7日")
                        .fontWeight(.bold)
                        .foregroundColor(.orange)
                }
            }
            .padding()
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(16)
            .padding(.horizontal, 24)

            // 仮の棒グラフ（簡易表示）
            HStack(alignment: .bottom, spacing: 8) {
                ForEach(["月", "火", "水", "木", "金", "土", "日"], id: \.self) { day in
                    VStack(spacing: 4) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(day == "水" ? Color.green : Color.green.opacity(0.4))
                            .frame(width: 30, height: CGFloat.random(in: 20...80))
                        Text(day)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding()

            Spacer()
            Spacer().frame(height: 90)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(UIColor.systemGroupedBackground))
    }
}

struct AnalyticsScreen_Previews: PreviewProvider {
    static var previews: some View {
        AnalyticsScreen()
    }
}
