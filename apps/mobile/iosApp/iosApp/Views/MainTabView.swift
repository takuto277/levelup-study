import SwiftUI

struct MainTabView: View {
    @State private var selectedTab: Tab = .home

    enum Tab: Int, CaseIterable {
        case quest = 0
        case party = 1
        case home = 2
        case gacha = 3
        case analytics = 4

        var title: String {
            switch self {
            case .quest: return "冒険"
            case .party: return "編成"
            case .home: return "ホーム"
            case .gacha: return "召喚"
            case .analytics: return "記録"
            }
        }

        var icon: String {
            switch self {
            case .quest: return "map.fill"
            case .party: return "person.3.fill"
            case .home: return "house.fill"
            case .gacha: return "sparkles"
            case .analytics: return "chart.bar.fill"
            }
        }
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            // メインコンテンツ（選択中のタブに応じて切り替え）
            Group {
                switch selectedTab {
                case .quest:
                    QuestScreen()
                case .party:
                    PartyScreen()
                case .home:
                    HomeScreenView()
                case .gacha:
                    GachaScreen()
                case .analytics:
                    AnalyticsScreen()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // カスタムタブバー
            customTabBar
        }
        .ignoresSafeArea(.keyboard)
    }

    // MARK: - カスタムタブバー

    private var customTabBar: some View {
        HStack {
            ForEach(Tab.allCases, id: \.rawValue) { tab in
                tabButton(tab: tab)
            }
        }
        .padding(.horizontal, 8)
        .padding(.top, 12)
        .padding(.bottom, 28)
        .background(
            RoundedRectangle(cornerRadius: 0)
                .fill(Color(UIColor.systemBackground))
                .shadow(color: .black.opacity(0.1), radius: 8, y: -4)
        )
    }

    private func tabButton(tab: Tab) -> some View {
        Button(action: {
            withAnimation(.easeInOut(duration: 0.2)) {
                selectedTab = tab
            }
        }) {
            VStack(spacing: 4) {
                Image(systemName: tab.icon)
                    .font(.system(size: 20, weight: selectedTab == tab ? .bold : .regular))
                Text(tab.title)
                    .font(.system(size: 10, weight: selectedTab == tab ? .bold : .medium))
            }
            .foregroundColor(selectedTab == tab ? .blue : .gray)
            .frame(maxWidth: .infinity)
        }
    }
}

struct MainTabView_Previews: PreviewProvider {
    static var previews: some View {
        MainTabView()
    }
}
