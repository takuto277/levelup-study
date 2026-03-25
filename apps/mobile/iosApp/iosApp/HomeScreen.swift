import SwiftUI
import Shared

struct HomeScreen: View {
    // 選択されたタブのステータス管理
    @State private var selectedTab = 0
    // キャラクタのアニメーション用
    @State private var isBouncing = false

    var body: some View {
        ZStack(alignment: .bottom) {
            // メインコンテンツ部分
            VStack(spacing: 0) {
                // Header Bar (ステータス表示)
                headerView
                
                // Character Area (中央)
                characterView
                
                Spacer()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(UIColor.systemGroupedBackground))
            
            // 下部タブバー＆Study開始ボタン
            bottomTabBar
        }
    }
    
    // MARK: - Subviews
    
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
                    Text("Crystals")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Text("1,250")
                        .font(.headline)
                        .fontWeight(.bold)
                }
                
                Button(action: {
                    // Storeやガチャ詳細などへの遷移
                }) {
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
    
    // 中央のキャラクターアニメーション領域
    private var characterView: some View {
        VStack {
            Spacer()
            
            // 推しキャラクター（仮で魔法使いの絵文字を使用）
            ZStack {
                Circle()
                    .fill(
                        RadialGradient(gradient: Gradient(colors: [.blue.opacity(0.3), .clear]), center: .center, startRadius: 20, endRadius: 150)
                    )
                    .frame(width: 300, height: 300)
                
                VStack {
                    Text("「今日の特訓も頑張ろうな！」")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .padding()
                        .background(Color.white)
                        .cornerRadius(20)
                        .shadow(radius: 5)
                        .offset(y: 20)
                        .zIndex(1)

                    Text("🧙‍♂️")
                        .font(.system(size: 150))
                        .offset(y: isBouncing ? -15 : 0)
                        .animation(Animation.easeInOut(duration: 1.5).repeatForever(autoreverses: true), value: isBouncing)
                        .onAppear {
                            isBouncing = true
                        }
                }
            }
            
            Spacer()
            Spacer() // 下部タブに被らないようにする余白
        }
    }
    
    // 5タブと中央のFloating Action Button
    private var bottomTabBar: some View {
        ZStack(alignment: .bottom) {
            // 背景のタブバー
            HStack {
                tabBarItem(icon: "house.fill", title: "Home", tabIndex: 0)
                Spacer(minLength: 0)
                tabBarItem(icon: "person.3.fill", title: "Party", tabIndex: 1)
                Spacer(minLength: 0)
                
                // 冒険（Adventure）
                tabBarItem(icon: "map.fill", title: "Quest", tabIndex: 2)
                    .padding(.trailing, 20)
                
                // ここはStartボタンの分空ける
                Color.clear.frame(width: 80, height: 60)
                
                tabBarItem(icon: "diamond.fill", title: "Gacha", tabIndex: 3)
                    .padding(.leading, 20)
                Spacer(minLength: 0)
                tabBarItem(icon: "chart.bar.fill", title: "Data", tabIndex: 4)
            }
            .padding(.horizontal, 15)
            .padding(.top, 10)
            .padding(.bottom, 30) // SafeAreaの代わり
            .background(Color.white)
            .shadow(color: .black.opacity(0.1), radius: 10, y: -5)
            
            // 中央のStudy Startボタン
            Button(action: {
                // タイマー画面へ遷移処理を書く
            }) {
                ZStack {
                    Circle()
                        .fill(LinearGradient(gradient: Gradient(colors: [Color.red, Color.orange]), startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 80, height: 80)
                        .shadow(color: .orange.opacity(0.5), radius: 10, y: 5)
                    
                    VStack(spacing: 4) {
                        Image(systemName: "timer")
                            .font(.system(size: 28, weight: .bold))
                            .foregroundColor(.white)
                        Text("START")
                            .font(.system(size: 11, weight: .heavy))
                            .foregroundColor(.white)
                    }
                }
            }
            .offset(y: -45) // タブバーに少し被せる
        }
        .ignoresSafeArea(edges: .bottom)
    }
    
    // タブ要素
    private func tabBarItem(icon: String, title: String, tabIndex: Int) -> some View {
        Button(action: {
            withAnimation(.spring()) {
                selectedTab = tabIndex
            }
        }) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 22, weight: selectedTab == tabIndex ? .bold : .regular))
                Text(title)
                    .font(.system(size: 10, weight: selectedTab == tabIndex ? .bold : .semibold))
            }
            .foregroundColor(selectedTab == tabIndex ? .blue : .gray)
            .frame(maxWidth: 50)
        }
    }
}

struct HomeScreen_Previews: PreviewProvider {
    static var previews: some View {
        HomeScreen()
    }
}
