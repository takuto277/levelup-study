import SwiftUI

/// 召喚画面（タブ④: 右から2番目）
/// ガチャ石を使ってキャラクターや武器を引く
struct GachaScreenView: View {
    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "sparkles")
                .font(.system(size: 60))
                .foregroundColor(.yellow)

            Text("召喚")
                .font(.title)
                .fontWeight(.bold)

            Text("知識の結晶を使って仲間や武器を召喚しよう")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            // 所持石の表示
            HStack {
                Image(systemName: "sparkles")
                    .foregroundColor(.yellow)
                Text("所持: 1,250個")
                    .fontWeight(.bold)
            }
            .padding()
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(12)

            // ガチャボタン
            HStack(spacing: 16) {
                Button(action: {}) {
                    VStack(spacing: 4) {
                        Text("単発")
                            .fontWeight(.bold)
                        Text("50個")
                            .font(.caption)
                    }
                    .foregroundColor(.white)
                    .frame(width: 120, height: 60)
                    .background(
                        LinearGradient(
                            colors: [.blue, .purple],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .cornerRadius(16)
                }

                Button(action: {}) {
                    VStack(spacing: 4) {
                        Text("10連")
                            .fontWeight(.bold)
                        Text("450個")
                            .font(.caption)
                    }
                    .foregroundColor(.white)
                    .frame(width: 120, height: 60)
                    .background(
                        LinearGradient(
                            colors: [.orange, .red],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .cornerRadius(16)
                }
            }

            Spacer()
            Spacer().frame(height: 90)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(UIColor.systemGroupedBackground))
    }
}

struct GachaScreenView_Previews: PreviewProvider {
    static var previews: some View {
        GachaScreenView()
    }
}
