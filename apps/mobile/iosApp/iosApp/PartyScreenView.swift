import SwiftUI
import Shared

private extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255, opacity: alpha)
    }
}

private let bgDark = Color(hex: 0x0B1120)
private let bgCard = Color(hex: 0x111B2E)
private let bgSurface = Color(hex: 0x1A2744)
private let accentCyan = Color(hex: 0x22D3EE)
private let accentBlue = Color(hex: 0x3B82F6)
private let accentIndigo = Color(hex: 0x6366F1)
private let textW = Color(hex: 0xF1F5F9)
private let textSub = Color(hex: 0x94A3B8)

private func rarityColor(_ r: Int) -> Color {
    switch r { case 5: return Color(hex: 0xFFD700); case 4: return Color(hex: 0xA78BFA); case 3: return Color(hex: 0x60A5FA); default: return Color(hex: 0x94A3B8) }
}

/// Kotlin `UserCharacter.combatHp` / `combatAtk` / `combatDef` と同じ式（Lv1 はマスタ基準、以降 HP+100 / ATK・DEF+10）
private func breakthroughHpMult(_ bt: Int32) -> Double { 1.0 + Double(bt) * 0.05 }
private func breakthroughAtkMult(_ bt: Int32) -> Double {
    let b = Int(bt)
    let inc = b <= 1 ? 0.03 : 0.04
    return 1.0 + Double(b) * inc
}
private func combatHp(_ c: UserCharacter, _ m: MasterCharacter) -> Int {
    let bonus = Swift.max(0, Int(c.level) - 1) * 100
    return Int(Double(Int(m.baseHp) + bonus) * breakthroughHpMult(c.breakthroughLevel))
}
private func combatAtk(_ c: UserCharacter, _ m: MasterCharacter) -> Int {
    let bonus = Swift.max(0, Int(c.level) - 1) * 10
    return Int(Double(Int(m.baseAtk) + bonus) * breakthroughAtkMult(c.breakthroughLevel))
}
private func combatDef(_ c: UserCharacter, _ m: MasterCharacter) -> Int {
    Int(m.baseDef) + Swift.max(0, Int(c.level) - 1) * 10
}
/// 編成画面ではとりあえずユーザー（冒険者）スプライトを表示
@ViewBuilder
private func partyPlayerAvatar(size: CGFloat) -> some View {
    if UIImage(named: "sprite_player_idle_1") != nil {
        Image("sprite_player_idle_1")
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size)
    } else if UIImage(named: "sprite_player_prep_1") != nil {
        Image("sprite_player_prep_1")
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size)
    } else if UIImage(named: "sprite_player_walk_1") != nil {
        Image("sprite_player_walk_1")
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .frame(width: size, height: size)
    } else {
        Text("🧙‍♂️").font(.system(size: size * 0.52))
    }
}

struct PartyScreenView: View {
    @StateObject private var holder = ViewModelHolder()
    @State private var uiState: PartyUiState
    @State private var showPicker = false

    init() { let vm = KoinHelperKt.getPartyViewModel(); _uiState = State(initialValue: vm.uiState.value as! PartyUiState) }

    var body: some View {
        ZStack {
            LinearGradient(colors: [bgDark, Color(hex: 0x0F172A)], startPoint: .top, endPoint: .bottom).ignoresSafeArea()

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("編 成").font(.system(size: 28, weight: .black)).foregroundColor(textW)
                            Text("メインキャラクターを選択しよう").font(.caption).foregroundColor(textSub)
                        }
                        Spacer()
                        Button(action: { showPicker = true }) {
                            HStack(spacing: 4) {
                                Image(systemName: "arrow.triangle.2.circlepath").font(.system(size: 12, weight: .bold))
                                Text("変更").font(.system(size: 13, weight: .bold))
                            }
                            .foregroundColor(.white).padding(.horizontal, 14).padding(.vertical, 8)
                            .background(LinearGradient(colors: [accentBlue, accentIndigo], startPoint: .leading, endPoint: .trailing)).cornerRadius(10)
                        }
                    }
                    .padding(.horizontal, 20).padding(.top, 16)

                    if let main = uiState.party?.mainCharacter { mainCard(main) }

                    Text("所持キャラクター").font(.system(size: 14, weight: .bold)).foregroundColor(accentCyan).padding(.horizontal, 20)
                    charGrid
                    Spacer().frame(height: 120)
                }
            }

            if let sel = uiState.selectedCharacter { detailOverlay(sel) }
            if showPicker { pickerOverlay }
            if uiState.isLoading { Color.black.opacity(0.3).ignoresSafeArea(); ProgressView().scaleEffect(1.5).progressViewStyle(CircularProgressViewStyle(tint: .white)) }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: uiState.selectedCharacter != nil)
        .onReceive(Timer.publish(every: 0.3, on: .main, in: .common).autoconnect()) { _ in self.uiState = holder.viewModel.uiState.value as! PartyUiState }
        .onAppear { holder.viewModel.onIntent(intent: PartyIntentRefresh()) }
    }

    private class ViewModelHolder: ObservableObject { let viewModel: PartyViewModel = KoinHelperKt.getPartyViewModel() }

    // MARK: - Main Card
    private func mainCard(_ mc: UserCharacter) -> some View {
        let m = mc.character!
        return VStack(spacing: 0) {
            ZStack(alignment: .topTrailing) {
                HStack(spacing: 18) {
                    ZStack {
                        Circle().fill(rarityColor(Int(m.rarity)).opacity(0.2)).frame(width: 90, height: 90)
                        partyPlayerAvatar(size: 80)
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 2) { ForEach(0..<Int(m.rarity), id: \.self) { _ in Text("⭐").font(.system(size: 12)) } }
                        Text(m.name).font(.system(size: 20, weight: .heavy)).foregroundColor(textW)
                        Text("Lv.\(mc.level)").font(.system(size: 15, weight: .bold)).foregroundColor(rarityColor(Int(m.rarity)))
                    }
                    Spacer()
                }
                .padding(20).frame(maxWidth: .infinity)
                .background(LinearGradient(colors: [rarityColor(Int(m.rarity)).opacity(0.15), bgCard], startPoint: .topLeading, endPoint: .bottomTrailing))
                Text("MAIN").font(.system(size: 9, weight: .bold)).foregroundColor(.white)
                    .padding(.horizontal, 8).padding(.vertical, 3).background(accentBlue).cornerRadius(6).padding(10)
            }
            HStack {
                Spacer()
                stat("❤️", "HP", "\(combatHp(mc, m))", Color(hex: 0xEF4444))
                Spacer()
                stat("⚔️", "ATK", "\(combatAtk(mc, m))", Color(hex: 0xF59E0B))
                Spacer()
                stat("🛡️", "DEF", "\(combatDef(mc, m))", accentBlue)
                Spacer()
            }.padding(.vertical, 12)
        }
        .background(bgCard).cornerRadius(22).padding(.horizontal, 16)
    }

    private func stat(_ e: String, _ l: String, _ v: String, _ c: Color) -> some View {
        VStack(spacing: 2) { HStack(spacing: 3) { Text(e).font(.system(size: 14)); Text(v).font(.system(size: 16, weight: .bold)).foregroundColor(c) }; Text(l).font(.system(size: 10)).foregroundColor(textSub) }
    }

    // MARK: - Grid
    private var charGrid: some View {
        let cols = [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]
        return LazyVGrid(columns: cols, spacing: 10) {
            ForEach(uiState.ownedCharacters, id: \.id) { c in
                let m = c.character!
                Button(action: { holder.viewModel.onIntent(intent: PartyIntentSelectCharacter(userCharacterId: c.id)) }) {
                    VStack(spacing: 0) {
                        VStack(spacing: 4) { partyPlayerAvatar(size: 44); HStack(spacing: 1) { ForEach(0..<Int(m.rarity), id: \.self) { _ in Text("⭐").font(.system(size: 7)) } } }
                            .frame(maxWidth: .infinity).padding(.vertical, 10).background(rarityColor(Int(m.rarity)).opacity(0.1))
                        VStack(spacing: 2) {
                            Text(m.name).font(.system(size: 11, weight: .bold)).foregroundColor(textW).lineLimit(1)
                            Text("Lv.\(c.level)").font(.system(size: 10, weight: .semibold)).foregroundColor(rarityColor(Int(m.rarity)))
                        }.padding(6)
                    }.background(bgCard).cornerRadius(14)
                }.buttonStyle(.plain)
            }
        }.padding(.horizontal, 16)
    }

    // MARK: - Picker
    private var pickerOverlay: some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.5).ignoresSafeArea().onTapGesture { showPicker = false }
            VStack(spacing: 0) {
                HStack { Text("メインキャラクターを選択").font(.system(size: 16, weight: .heavy)).foregroundColor(textW); Spacer(); Button("閉じる") { showPicker = false }.font(.system(size: 13, weight: .bold)).foregroundColor(accentCyan) }.padding(18)
                ScrollView {
                    let cols = [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]
                    LazyVGrid(columns: cols, spacing: 10) {
                        ForEach(uiState.ownedCharacters, id: \.id) { c in
                            let m = c.character!
                            Button(action: { holder.viewModel.onIntent(intent: PartyIntentAssignCharacter(slotPosition: 1, userCharacterId: c.id)); showPicker = false }) {
                                VStack(spacing: 4) { partyPlayerAvatar(size: 40); Text(m.name).font(.system(size: 11, weight: .bold)).foregroundColor(textW); Text("Lv.\(c.level)").font(.system(size: 10)).foregroundColor(rarityColor(Int(m.rarity))) }
                                    .frame(maxWidth: .infinity).padding(.vertical, 10).background(bgSurface).cornerRadius(12)
                            }.buttonStyle(.plain)
                        }
                    }.padding(.horizontal, 18)
                }.frame(maxHeight: 380)
            }.background(bgCard).cornerRadius(26, corners: [.topLeft, .topRight])
        }
    }

    // MARK: - Detail
    private func detailOverlay(_ c: UserCharacter) -> some View {
        let m = c.character!
        let hp = combatHp(c, m)
        let atk = combatAtk(c, m)
        let def = combatDef(c, m)
        return ZStack(alignment: .bottom) {
            Color.black.opacity(0.5).ignoresSafeArea().onTapGesture { holder.viewModel.onIntent(intent: PartyIntentDismissCharacterDetail()) }
            VStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 2).fill(Color(hex: 0x475569)).frame(width: 40, height: 4).padding(.top, 12)
                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 14) {
                        VStack(spacing: 6) { partyPlayerAvatar(size: 100); HStack(spacing: 2) { ForEach(0..<Int(m.rarity), id: \.self) { _ in Text("⭐").font(.system(size: 14)) } } }
                            .frame(maxWidth: .infinity).padding(.vertical, 18).background(rarityColor(Int(m.rarity)).opacity(0.12)).cornerRadius(18).padding(.horizontal, 16)
                        VStack(spacing: 4) {
                            Text(m.name).font(.system(size: 22, weight: .heavy)).foregroundColor(textW)
                            Text("Lv.\(c.level)  ·  XP \(c.currentXp)").font(.system(size: 12, weight: .bold)).foregroundColor(rarityColor(Int(m.rarity))).padding(.horizontal, 12).padding(.vertical, 4).background(rarityColor(Int(m.rarity)).opacity(0.15)).cornerRadius(8)
                        }
                        VStack(alignment: .leading, spacing: 10) {
                            Text("ステータス").font(.system(size: 14, weight: .bold)).foregroundColor(textW)
                            statBar("❤️ HP", val: hp, max: 2000, c: Color(hex: 0xEF4444))
                            statBar("⚔️ ATK", val: atk, max: 600, c: Color(hex: 0xF59E0B))
                            statBar("🛡️ DEF", val: def, max: 500, c: accentBlue)
                            Divider().background(bgSurface)
                            HStack { Text("💪 総合戦闘力").font(.system(size: 13)).foregroundColor(textSub); Spacer(); Text("\(hp + atk * 2 + def)").font(.system(size: 18, weight: .heavy)).foregroundColor(textW) }
                        }.padding(14).background(bgSurface).cornerRadius(14).padding(.horizontal, 16)
                        Spacer().frame(height: 40)
                    }.padding(.top, 8)
                }
            }
            .frame(maxHeight: UIScreen.main.bounds.height * 0.7).background(bgCard).cornerRadius(26, corners: [.topLeft, .topRight])
        }
    }

    private func statBar(_ label: String, val: Int, max: Int, c: Color) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack { Text(label).font(.system(size: 12)).foregroundColor(textSub); Spacer(); Text("\(val)").font(.system(size: 12, weight: .bold)).foregroundColor(c) }
            GeometryReader { g in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3).fill(bgDark).frame(height: 6)
                    RoundedRectangle(cornerRadius: 3).fill(c).frame(width: g.size.width * min(CGFloat(val) / CGFloat(max), 1), height: 6)
                }
            }.frame(height: 6)
        }
    }
}

private struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity; var corners: UIRectCorner = .allCorners
    func path(in rect: CGRect) -> Path { Path(UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius)).cgPath) }
}
private extension View { func cornerRadius(_ r: CGFloat, corners: UIRectCorner) -> some View { clipShape(RoundedCorner(radius: r, corners: corners)) } }
