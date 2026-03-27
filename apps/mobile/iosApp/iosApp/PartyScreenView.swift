import SwiftUI

/// 編成画面（タブ②: 左から2番目）
/// キャラクターや武器の装備・強化を行う
struct PartyScreenView: View {
    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "person.3.fill")
                .font(.system(size: 60))
                .foregroundColor(.purple)

            Text("編成")
                .font(.title)
                .fontWeight(.bold)

            Text("キャラクターや武器を装備してパーティを強化しよう")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)

            // 仮のパーティスロット
            HStack(spacing: 16) {
                ForEach(0..<4) { i in
                    VStack {
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(UIColor.tertiarySystemGroupedBackground))
                            .frame(width: 70, height: 70)
                            .overlay(
                                Text(["🧙‍♂️", "⚔️", "🛡️", "➕"][i])
                                    .font(.system(size: 30))
                            )
                        Text(i < 3 ? "Lv.\(i * 5 + 1)" : "空き")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding()
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(16)

            Spacer()
            Spacer().frame(height: 90)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(UIColor.systemGroupedBackground))
    }
}

struct PartyScreenView_Previews: PreviewProvider {
    static var previews: some View {
        PartyScreenView()
    }
}
