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

private func weaponDisplayName(_ weapon: UserWeapon) -> String {
    if let name = weapon.weapon?.name, !name.isEmpty { return name }
    return "武器（Lv.\(weapon.level)）"
}

private func weaponEmoji(_ weapon: UserWeapon) -> String {
    let name = weapon.weapon?.name ?? ""
    if name.contains("杖") || name.contains("スタッフ") { return "🪄" }
    if name.contains("剣") || name.contains("刀") { return "⚔️" }
    if name.contains("弓") { return "🏹" }
    if name.contains("聖") || name.contains("ワンド") { return "✨" }
    return "🗡️"
}

private func kotlinSlotPosition(_ slot: KotlinInt) -> Int32 {
    Int32(truncatingIfNeeded: slot.intValue)
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
                    TutorialHintBanner(topic: "party_setup", emoji: "\u{1F9E0}", message: "キャラや武器を編成して冒険の戦力をアップ！")
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

                    partySlotSection

                    Text("所持キャラクター").font(.system(size: 14, weight: .bold)).foregroundColor(accentCyan).padding(.horizontal, 20)
                    charGrid
                    Spacer().frame(height: 120)
                }
            }

            if showPicker { pickerOverlay }
            if let slot = uiState.selectedSlot {
                slotSelectionBanner(slot: slot.intValue)
            }
            if uiState.isLoading { Color.black.opacity(0.3).ignoresSafeArea(); ProgressView().scaleEffect(1.5).progressViewStyle(CircularProgressViewStyle(tint: .white)) }
        }
        .fullScreenCover(isPresented: Binding(
            get: { uiState.selectedCharacter != nil },
            set: { if !$0 { holder.viewModel.onIntent(intent: PartyIntentDismissCharacterDetail()) } }
        )) {
            if let sel = uiState.selectedCharacter {
                CharacterDetailFullScreen(
                    character: sel,
                    weapons: uiState.ownedWeapons,
                    onDismiss: { holder.viewModel.onIntent(intent: PartyIntentDismissCharacterDetail()) },
                    onEquip: { weaponId in
                        holder.viewModel.onIntent(intent: PartyIntentEquipWeapon(userCharacterId: sel.id, userWeaponId: weaponId))
                    },
                    onLevelUpCharacter: {
                        holder.viewModel.onIntent(intent: PartyIntentLevelUpCharacter(userCharacterId: sel.id))
                    },
                    onLevelUpWeapon: { weaponId in
                        holder.viewModel.onIntent(intent: PartyIntentLevelUpWeapon(userWeaponId: weaponId))
                    }
                )
                .id("\(sel.id)-\(sel.equippedWeaponId ?? "none")")
            }
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

    private func levelUpGoldCost(_ level: Int32) -> Int {
        Swift.max(1, Int(level)) * 50
    }

    // MARK: - Party Slots
    private var partySlotSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("パーティ編成").font(.system(size: 15, weight: .bold)).foregroundColor(textW).padding(.horizontal, 16)
            HStack(spacing: 10) {
                ForEach(1...4, id: \.self) { slot in
                    let partySlot = uiState.party?.slots.first { $0.slotPosition == slot }
                    let isSelected = uiState.selectedSlot?.int32Value == Int32(slot)
                    Button(action: { holder.viewModel.onIntent(intent: PartyIntentSelectSlot(slotPosition: Int32(slot))) }) {
                        VStack(spacing: 4) {
                            if let uc = partySlot?.userCharacter, let m = uc.character {
                                partyPlayerAvatar(size: 34)
                                Text(m.name).font(.system(size: 9, weight: .bold)).foregroundColor(textW).lineLimit(1)
                                Text("Lv.\(uc.level)").font(.system(size: 9)).foregroundColor(rarityColor(Int(m.rarity)))
                            } else {
                                Image(systemName: "plus").font(.system(size: 18)).foregroundColor(textSub)
                                Text("空き").font(.system(size: 9)).foregroundColor(textSub)
                            }
                            Text("Slot \(slot)").font(.system(size: 8, weight: isSelected ? .bold : .regular)).foregroundColor(isSelected ? accentBlue : textSub)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(isSelected ? accentBlue.opacity(0.15) : bgCard)
                        .cornerRadius(14)
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(isSelected ? accentBlue : Color.clear, lineWidth: 2))
                    }
                    .buttonStyle(.plain)
                    .simultaneousGesture(LongPressGesture(minimumDuration: 0.5).onEnded { _ in
                        if partySlot != nil {
                            holder.viewModel.onIntent(intent: PartyIntentRemoveFromSlot(slotPosition: Int32(slot)))
                        }
                    })
                }
            }
            .padding(.horizontal, 16)
        }
    }

    private func slotSelectionBanner(slot: Int) -> some View {
        VStack {
            HStack {
                Text("スロット\(slot)に配置するキャラを選んでください")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(.white)
                Spacer()
                Button("キャンセル") {
                    holder.viewModel.onIntent(intent: PartyIntentSelectSlot(slotPosition: Int32(slot)))
                }
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(.white.opacity(0.85))
            }
            .padding(.horizontal, 16).padding(.vertical, 10)
            .background(accentBlue)
            .cornerRadius(12)
            .padding(.horizontal, 16)
            .padding(.top, 8)
            Spacer()
        }
    }

    // MARK: - Grid
    private var charGrid: some View {
        let cols = [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)]
        return LazyVGrid(columns: cols, spacing: 10) {
            ForEach(uiState.ownedCharacters, id: \.id) { c in
                let m = c.character!
                Button(action: {
                    if let slot = uiState.selectedSlot {
                        let slotPos = kotlinSlotPosition(slot)
                        holder.viewModel.onIntent(intent: PartyIntentAssignCharacter(slotPosition: slotPos, userCharacterId: c.id))
                        holder.viewModel.onIntent(intent: PartyIntentSelectSlot(slotPosition: slotPos))
                    } else {
                        holder.viewModel.onIntent(intent: PartyIntentSelectCharacter(userCharacterId: c.id))
                    }
                }) {
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
}

private struct CharacterDetailFullScreen: View {
    let character: UserCharacter
    let weapons: [UserWeapon]
    let onDismiss: () -> Void
    let onEquip: (String?) -> Void
    let onLevelUpCharacter: () -> Void
    let onLevelUpWeapon: (String) -> Void

    @State private var showWeaponPicker = false

    var body: some View {
        ZStack {
            LinearGradient(colors: [bgDark, Color(hex: 0x0F172A)], startPoint: .top, endPoint: .bottom).ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    Button(action: {
                        if showWeaponPicker { showWeaponPicker = false } else { onDismiss() }
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(textW)
                            .padding(10)
                    }
                    Text(showWeaponPicker ? "武器を選択" : "キャラクター詳細")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(textW)
                    Spacer()
                }
                .padding(.horizontal, 8)
                .padding(.top, 8)

                if showWeaponPicker {
                    weaponPickerContent
                } else {
                    detailContent
                }
            }
        }
    }

    private var detailContent: some View {
        let m = character.character!
        let hp = combatHp(character, m)
        let atk = combatAtk(character, m)
        let def = combatDef(character, m)
        let weapon = weapons.first { $0.id == character.equippedWeaponId }
        return ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 14) {
                VStack(spacing: 6) {
                    partyPlayerAvatar(size: 100)
                    HStack(spacing: 2) { ForEach(0..<Int(m.rarity), id: \.self) { _ in Text("⭐").font(.system(size: 14)) } }
                }
                .frame(maxWidth: .infinity).padding(.vertical, 18)
                .background(rarityColor(Int(m.rarity)).opacity(0.12)).cornerRadius(18).padding(.horizontal, 16)
                VStack(spacing: 4) {
                    Text(m.name).font(.system(size: 22, weight: .heavy)).foregroundColor(textW)
                    Text("Lv.\(character.level)  ·  XP \(character.currentXp)")
                        .font(.system(size: 12, weight: .bold)).foregroundColor(rarityColor(Int(m.rarity)))
                        .padding(.horizontal, 12).padding(.vertical, 4)
                        .background(rarityColor(Int(m.rarity)).opacity(0.15)).cornerRadius(8)
                }
                VStack(alignment: .leading, spacing: 10) {
                    Text("ステータス").font(.system(size: 14, weight: .bold)).foregroundColor(textW)
                    statBar("❤️ HP", val: hp, max: 2000, c: Color(hex: 0xEF4444))
                    statBar("⚔️ ATK", val: atk, max: 600, c: Color(hex: 0xF59E0B))
                    statBar("🛡️ DEF", val: def, max: 500, c: accentBlue)
                    Divider().background(bgSurface)
                    HStack {
                        Text("💪 総合戦闘力").font(.system(size: 13)).foregroundColor(textSub)
                        Spacer()
                        Text("\(hp + atk * 2 + def)").font(.system(size: 18, weight: .heavy)).foregroundColor(textW)
                    }
                }.padding(14).background(bgSurface).cornerRadius(14).padding(.horizontal, 16)
                weaponSection(weapon: weapon)
                Button(action: onLevelUpCharacter) {
                    Text("LvUP（\(Swift.max(1, Int(character.level)) * 50) G）")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(accentIndigo)
                        .cornerRadius(14)
                }
                .padding(.horizontal, 16)
                if let w = weapon {
                    Button(action: { onLevelUpWeapon(w.id) }) {
                        Text("武器 LvUP（\(Swift.max(1, Int(w.level)) * 50) G）")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(Color(hex: 0xF59E0B))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 11)
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0xF59E0B).opacity(0.5), lineWidth: 1))
                    }
                    .padding(.horizontal, 16)
                }
                Spacer().frame(height: 40)
            }.padding(.top, 8)
        }
    }

    private func weaponSection(weapon: UserWeapon?) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("装備中の武器").font(.system(size: 14, weight: .bold)).foregroundColor(textW)
                Spacer()
                if !weapons.isEmpty {
                    Button("変更") { showWeaponPicker = true }
                        .font(.system(size: 13, weight: .bold)).foregroundColor(accentBlue)
                }
            }
            if let w = weapon {
                HStack(spacing: 12) {
                    Text(weaponEmoji(w)).font(.system(size: 28))
                    VStack(alignment: .leading, spacing: 4) {
                        Text(weaponDisplayName(w))
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(textW)
                            .lineLimit(2)
                            .fixedSize(horizontal: false, vertical: true)
                        HStack(spacing: 4) {
                            if let rarity = w.weapon?.rarity {
                                ForEach(0..<Int(rarity), id: \.self) { _ in
                                    Text("★").font(.system(size: 10)).foregroundColor(Color(hex: 0xFFD700))
                                }
                            }
                            Text("Lv.\(w.level)  ·  ATK +\(w.weapon?.baseAtk ?? 0)")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(textSub)
                        }
                    }
                }
            } else {
                Text("武器未装備").font(.system(size: 13)).foregroundColor(textSub)
            }
        }
        .padding(14).background(bgSurface).cornerRadius(14)
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color(hex: 0xF59E0B).opacity(0.35), lineWidth: 1))
        .padding(.horizontal, 16)
    }

    private var weaponPickerContent: some View {
        VStack(spacing: 12) {
            Button("装備を外す") {
                onEquip(nil)
                showWeaponPicker = false
            }.foregroundColor(accentBlue)
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(weapons, id: \.id) { w in
                        let selected = w.id == character.equippedWeaponId
                        Button(action: {
                            onEquip(w.id)
                            showWeaponPicker = false
                        }) {
                            HStack(spacing: 12) {
                                Text(weaponEmoji(w)).font(.system(size: 28))
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(weaponDisplayName(w))
                                        .font(.system(size: 16, weight: .bold))
                                        .foregroundColor(textW)
                                        .multilineTextAlignment(.leading)
                                        .lineLimit(2)
                                    HStack(spacing: 4) {
                                        if let rarity = w.weapon?.rarity {
                                            ForEach(0..<Int(rarity), id: \.self) { _ in
                                                Text("★").font(.system(size: 10)).foregroundColor(Color(hex: 0xFFD700))
                                            }
                                        }
                                        Text("Lv.\(w.level)  ·  ATK +\(w.weapon?.baseAtk ?? 0)")
                                            .font(.system(size: 12))
                                            .foregroundColor(textSub)
                                    }
                                }
                                Spacer()
                                if selected {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(accentCyan)
                                        .font(.system(size: 20))
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 14).padding(.vertical, 12)
                            .background(selected ? accentBlue.opacity(0.18) : bgSurface)
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(selected ? accentBlue : Color(hex: 0x334155), lineWidth: selected ? 2 : 1)
                            )
                        }.buttonStyle(.plain)
                    }
                }.padding(.horizontal, 16)
            }
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
