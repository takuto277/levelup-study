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
            // Main Content
            Group {
                switch selectedTab {
                case .quest: QuestScreen()
                case .party: PartyScreen()
                case .home: HomeScreenView()
                case .gacha: GachaScreen()
                case .analytics: AnalyticsScreen()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(UIColor.systemGroupedBackground))

            // Premium Floating Tab Bar
            customTabBar
        }
        .ignoresSafeArea(.all, edges: .bottom)
        .ignoresSafeArea(.keyboard)
    }

    // MARK: - Custom Tab Bar

    private var customTabBar: some View {
        HStack(spacing: 0) {
            ForEach(Tab.allCases, id: \.rawValue) { tab in
                tabButton(tab: tab)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(
            ZStack {
                // Background blur (Glassmorphism)
                RoundedRectangle(cornerRadius: 35, style: .continuous)
                    .fill(.ultraThinMaterial)
                    .shadow(color: .black.opacity(0.1), radius: 20, x: 0, y: 10)
                
                // Fine stroke for highlights
                RoundedRectangle(cornerRadius: 35, style: .continuous)
                    .stroke(
                        LinearGradient(
                            colors: [.white.opacity(0.6), .clear, .white.opacity(0.2)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
            }
        )
        .padding(.horizontal, 16)
        .padding(.bottom, 34) // Float above safe area
    }

    private func tabButton(tab: Tab) -> some View {
        Button(action: {
            let impact = UIImpactFeedbackGenerator(style: .medium)
            impact.impactOccurred()
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                selectedTab = tab
            }
        }) {
            VStack(spacing: 4) {
                ZStack {
                    if tab == .home {
                        // Highlight central Home tab
                        Circle()
                            .fill(
                                LinearGradient(
                                    colors: [Color.blue, Color.indigo],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .frame(width: 54, height: 54)
                            .shadow(color: Color.blue.opacity(0.4), radius: 8, x: 0, y: 4)
                            .offset(y: -18)
                        
                        Image(systemName: tab.icon)
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                            .offset(y: -18)
                    } else {
                        if selectedTab == tab {
                            Capsule()
                                .fill(Color.blue.opacity(0.12))
                                .frame(width: 44, height: 32)
                                .transition(.scale.combined(with: .opacity))
                        }
                        
                        Image(systemName: tab.icon)
                            .font(.system(size: 20, weight: selectedTab == tab ? .bold : .medium))
                            .symbolVariant(selectedTab == tab ? .fill : .none)
                            .foregroundColor(selectedTab == tab ? .blue : .secondary)
                    }
                }
                .frame(height: 32)

                Text(tab.title)
                    .font(.system(size: 10, weight: selectedTab == tab ? .bold : .medium))
                    .foregroundColor(selectedTab == tab ? .primary : .secondary)
            }
            .frame(maxWidth: .infinity)
        }
    }
}

struct MainTabView_Previews: PreviewProvider {
    static var previews: some View {
        MainTabView()
    }
}
