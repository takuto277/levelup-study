import SwiftUI
import Shared

struct TutorialHintBanner: View {
    let topic: String
    let emoji: String
    let message: String

    @State private var dismissed = false

    var body: some View {
        if dismissed || TutorialHelper.shared.isTutorialCompleted(topic: topic) {
            EmptyView()
        } else {
            Button(action: {
                TutorialHelper.shared.markTutorialCompleted(topic: topic)
                dismissed = true
            }) {
                HStack(spacing: 8) {
                    Text(emoji)
                        .font(.system(size: 16))
                    Text(message)
                        .foregroundColor(Color(hex: "#22D3EE"))
                        .font(.system(size: 12, weight: .medium))
                    Spacer()
                    Text("✕")
                        .foregroundColor(Color(hex: "#64748B"))
                        .font(.system(size: 12))
                }
                .padding(10)
                .frame(maxWidth: .infinity)
                .background(Color(hex: "#1A2744"))
                .cornerRadius(12)
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 16)
        }
    }
}

private extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6:
            (a, r, g, b) = (255, (int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = ((int >> 24) & 0xFF, (int >> 16) & 0xFF, (int >> 8) & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
