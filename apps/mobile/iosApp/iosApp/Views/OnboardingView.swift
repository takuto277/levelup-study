import SwiftUI

struct OnboardingView: View {
    let onComplete: () -> Void

    @State private var currentPage = 0

    private let pages: [(emoji: String, title: String, description: String)] = [
        ("📚", "勉強を始めよう", "勉強時間があなたの冒険の力になります。\n集中して学べば学ぶほど、戦闘力が上がります。"),
        ("⚔️", "冒険に出かけよう", "勉強が進むと新しいダンジョンが解放されます。\n敵を倒して経験値と報酬を手に入れましょう。"),
        ("🏆", "報酬を集めよう", "冒険で得た石とゴールドで召喚や装備強化ができます。\n最強のパーティを編成しましょう。"),
        ("☁️", "いつでも同期", "オフラインでも進捗は端末に保存されます。\nネット接続時に自動でサーバーと同期します。"),
    ]

    private var isLastPage: Bool { currentPage >= pages.count - 1 }
    private var page: (emoji: String, title: String, description: String) { pages[currentPage] }

    var body: some View {
        ZStack {
            bgDark.ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                Text(page.emoji)
                    .font(.system(size: 72))

                Spacer().frame(height: 32)

                Text(page.title)
                    .font(.title2.weight(.bold))
                    .foregroundColor(textPrimary)

                Spacer().frame(height: 16)

                Text(page.description)
                    .font(.subheadline)
                    .foregroundColor(textSecondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(6)

                Spacer().frame(height: 48)

                HStack(spacing: 8) {
                    ForEach(0..<pages.count, id: \.self) { i in
                        Circle()
                            .fill(i == currentPage ? accentBlue : textSecondary.opacity(0.4))
                            .frame(width: i == currentPage ? 10 : 8, height: i == currentPage ? 10 : 8)
                    }
                }

                Spacer().frame(height: 32)

                Button(action: {
                    if isLastPage {
                        onComplete()
                    } else {
                        withAnimation { currentPage += 1 }
                    }
                }) {
                    Text(isLastPage ? "勉強を始める" : "次へ")
                        .font(.headline.weight(.bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(accentBlue)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .padding(.horizontal, 32)

                if !isLastPage {
                    Spacer().frame(height: 12)

                    Button(action: onComplete) {
                        Text("スキップ")
                            .font(.subheadline)
                            .foregroundColor(textSecondary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 44)
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(textSecondary.opacity(0.3), lineWidth: 1)
                            )
                    }
                    .padding(.horizontal, 32)
                }

                Spacer()
            }
            .padding(.horizontal, 16)
        }
    }
}

private let bgDark = Color(red: 0.04, green: 0.05, blue: 0.12)
private let textPrimary = Color(red: 0.95, green: 0.96, blue: 0.98)
private let textSecondary = Color(red: 0.58, green: 0.64, blue: 0.71)
private let accentBlue = Color(red: 0.23, green: 0.51, blue: 0.96)
