import SwiftUI

private let barBg = Color(red: 0.06, green: 0.09, blue: 0.16)
private let barStroke = Color(red: 0.15, green: 0.22, blue: 0.35)
private let accentBlue = Color(red: 0.23, green: 0.51, blue: 0.96)
private let accentIndigo = Color(red: 0.39, green: 0.40, blue: 0.95)
private let accentCyan = Color(red: 0.13, green: 0.83, blue: 0.93)

struct MainTabView: View {
    @State private var selectedTab: Tab = .home

    enum Tab: Int, CaseIterable {
        case quest = 0, party = 1, home = 2, gacha = 3, analytics = 4
        var title: String { switch self { case .quest: return "冒険"; case .party: return "編成"; case .home: return "ホーム"; case .gacha: return "召喚"; case .analytics: return "記録" } }
        var icon: String { switch self { case .quest: return "map.fill"; case .party: return "person.3.fill"; case .home: return "house.fill"; case .gacha: return "sparkles"; case .analytics: return "chart.bar.fill" } }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            Color(red: 0.04, green: 0.05, blue: 0.12)
                .ignoresSafeArea()

            Group {
                switch selectedTab {
                case .quest: QuestScreenView()
                case .party: PartyScreenView()
                case .home: HomeScreenView()
                case .gacha: GachaScreenView()
                case .analytics: AnalyticsScreenView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            customTabBar
        }
        .ignoresSafeArea(.all, edges: .bottom)
        .ignoresSafeArea(.keyboard)
    }

    private var customTabBar: some View {
        HStack(spacing: 0) { ForEach(Tab.allCases, id: \.rawValue) { tab in tabButton(tab: tab) } }
            .padding(.horizontal, 12).padding(.vertical, 8)
            .background(
                ZStack {
                    RoundedRectangle(cornerRadius: 30, style: .continuous).fill(barBg.opacity(0.95))
                        .shadow(color: .black.opacity(0.35), radius: 16, y: 8)
                    RoundedRectangle(cornerRadius: 30, style: .continuous)
                        .stroke(barStroke, lineWidth: 0.5)
                }
            )
            .padding(.horizontal, 16).padding(.bottom, 34)
    }

    private func tabButton(tab: Tab) -> some View {
        Button(action: {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) { selectedTab = tab }
        }) {
            VStack(spacing: 4) {
                ZStack {
                    if tab == .home {
                        Circle()
                            .fill(LinearGradient(colors: [accentBlue, accentIndigo], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 44, height: 44)
                            .shadow(color: accentBlue.opacity(0.5), radius: 6, y: 2)
                        Image(systemName: tab.icon).font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                    } else {
                        if selectedTab == tab {
                            Capsule().fill(accentBlue.opacity(0.2)).frame(width: 44, height: 30).transition(.scale.combined(with: .opacity))
                        }
                        Image(systemName: tab.icon)
                            .font(.system(size: 19, weight: selectedTab == tab ? .bold : .medium))
                            .foregroundColor(selectedTab == tab ? accentCyan : Color(red: 0.5, green: 0.55, blue: 0.65))
                    }
                }.frame(height: 32)
                Text(tab.title)
                    .font(.system(size: 10, weight: selectedTab == tab ? .bold : .medium))
                    .foregroundColor(selectedTab == tab ? Color.white : Color(red: 0.5, green: 0.55, blue: 0.65))
            }.frame(maxWidth: .infinity)
        }
    }
}
