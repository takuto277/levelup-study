import SwiftUI
import Shared

// MARK: - Color helper

private extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}

// MARK: - ヘルパー

private func rarityColor(_ rarity: Int) -> Color {
    switch rarity {
    case 5: return Color(hex: 0xFFD700)
    case 4: return Color(hex: 0x8B5CF6)
    case 3: return Color(hex: 0x3B82F6)
    default: return Color(hex: 0x94A3B8)
    }
}

private func rarityGradient(_ rarity: Int) -> [Color] {
    switch rarity {
    case 5: return [Color(hex: 0xFFD700), Color(hex: 0xFBBF24)]
    case 4: return [Color(hex: 0x8B5CF6), Color(hex: 0xA78BFA)]
    case 3: return [Color(hex: 0x3B82F6), Color(hex: 0x60A5FA)]
    default: return [Color(hex: 0x94A3B8), Color(hex: 0xCBD5E1)]
    }
}

private func characterEmoji(_ characterId: String) -> String {
    switch characterId {
    case "char_wizard", "a0000000-0000-0000-0000-000000000005": return "🧙‍♂️"
    case "char_knight", "a0000000-0000-0000-0000-000000000001": return "⚔️"
    case "char_archer", "a0000000-0000-0000-0000-000000000003": return "🏹"
    case "char_healer", "a0000000-0000-0000-0000-000000000004": return "💚"
    case "char_ninja", "a0000000-0000-0000-0000-000000000002": return "🥷"
    case "char_dragon", "a0000000-0000-0000-0000-000000000006": return "🐉"
    default: return "👤"
    }
}

private func weaponEmoji(_ weaponId: String?) -> String {
    switch weaponId {
    case "wpn_staff", "b0000000-0000-0000-0000-000000000005": return "🪄"
    case "wpn_sword", "b0000000-0000-0000-0000-000000000001": return "⚔️"
    case "wpn_wand", "b0000000-0000-0000-0000-000000000004": return "✨"
    default: return "🗡️"
    }
}

// MARK: - メイン

/// 編成画面（タブ②: 左から2番目）
/// キャラクターの確認・パーティスロット配置・ステータス閲覧
struct PartyScreenView: View {
    @StateObject private var holder = ViewModelHolder()
    @State private var uiState: PartyUiState

    init() {
        let vm = KoinHelperKt.getPartyViewModel()
        _uiState = State(initialValue: vm.uiState.value as! PartyUiState)
    }

    var body: some View {
        ZStack {
            Color(UIColor.systemGroupedBackground).ignoresSafeArea()

            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 12) {
                    partyHeader

                    // メインキャラ
                    if let main = uiState.party?.mainCharacter {
                        mainCharacterSection(main)
                    }

                    // パーティスロット
                    partySlotSection

                    // 所持キャラ一覧
                    characterListSection

                    Spacer().frame(height: 120)
                }
                .padding(.horizontal, 16)
            }

            // キャラ詳細
            if let selected = uiState.selectedCharacter {
                characterDetailOverlay(selected)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }

            // スロット選択中バナー
            if let slot = uiState.selectedSlot, slot.intValue > 0 {
                VStack {
                    slotSelectionBanner(slot: slot.intValue)
                    Spacer()
                }
            }

            // ローディング
            if uiState.isLoading {
                Color.black.opacity(0.2).ignoresSafeArea()
                ProgressView()
                    .scaleEffect(1.5)
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.85), value: uiState.selectedCharacter != nil)
        .onReceive(Timer.publish(every: 0.3, on: .main, in: .common).autoconnect()) { _ in
            self.uiState = holder.viewModel.uiState.value as! PartyUiState
        }
        .onAppear {
            holder.viewModel.onIntent(intent: PartyIntentRefresh())
        }
    }

    // MARK: - ViewModel Holder

    private class ViewModelHolder: ObservableObject {
        let viewModel: PartyViewModel = KoinHelperKt.getPartyViewModel()
    }

    // MARK: - Header

    private var partyHeader: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                Text("🛡️").font(.system(size: 28))
                Text("編成")
                    .font(.system(size: 28, weight: .heavy))
                    .foregroundColor(Color(hex: 0x1E293B))
            }
            .padding(.top, 8)
            Text("キャラクターを配置してパーティを強化しよう")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 8)
    }

    // MARK: - メインキャラ

    private func mainCharacterSection(_ mainChar: UserCharacter) -> some View {
        let master = mainChar.character!
        let weapon = uiState.ownedWeapons.first(where: { $0.id == mainChar.equippedWeaponId })

        return VStack(spacing: 0) {
            // ヒーロー
            ZStack(alignment: .topTrailing) {
                HStack(spacing: 20) {
                    ZStack {
                        Circle()
                            .fill(rarityColor(Int(master.rarity)).opacity(0.15))
                            .frame(width: 100, height: 100)
                        Text(characterEmoji(master.id))
                            .font(.system(size: 64))
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 2) {
                            ForEach(0..<Int(master.rarity), id: \.self) { _ in
                                Text("⭐").font(.system(size: 14))
                            }
                        }
                        Text(master.name)
                            .font(.system(size: 22, weight: .heavy))
                            .foregroundColor(Color(hex: 0x1E293B))
                        Text("Lv.\(mainChar.level)")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(rarityColor(Int(master.rarity)))

                        if let w = weapon {
                            HStack(spacing: 4) {
                                Text(weaponEmoji(w.weaponId)).font(.system(size: 14))
                                Text(w.weapon?.name ?? "武器")
                                    .font(.system(size: 12))
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                    Spacer()
                }
                .padding(24)
                .frame(maxWidth: .infinity)
                .background(
                    LinearGradient(
                        colors: rarityGradient(Int(master.rarity)).map { $0.opacity(0.12) },
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

                Text("MAIN")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color(hex: 0x3B82F6))
                    .cornerRadius(8)
                    .padding(12)
            }

            // ステータスバー
            HStack {
                Spacer()
                statItem("❤️", "HP", "\(master.baseHp + mainChar.level * 20)", Color(hex: 0xEF4444))
                Spacer()
                statItem("⚔️", "ATK", "\(master.baseAtk + mainChar.level * 8)", Color(hex: 0xF59E0B))
                Spacer()
                statItem("🛡️", "DEF", "\(master.baseDef + mainChar.level * 5)", Color(hex: 0x3B82F6))
                Spacer()
            }
            .padding(.vertical, 14)
        }
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(24)
        .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 4)
    }

    private func statItem(_ emoji: String, _ label: String, _ value: String, _ color: Color) -> some View {
        VStack(spacing: 2) {
            HStack(spacing: 4) {
                Text(emoji).font(.system(size: 16))
                Text(value)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(color)
            }
            Text(label)
                .font(.system(size: 11))
                .foregroundColor(.secondary)
        }
    }

    // MARK: - パーティスロット

    private var partySlotSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("パーティ編成")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color(hex: 0x1E293B))

            HStack(spacing: 10) {
                ForEach(1..<5) { slot in
                    let partySlot = uiState.party?.slots.first(where: {
                        ($0 as? PartySlot)?.slotPosition == Int32(slot)
                    }) as? PartySlot
                    let isSelected = uiState.selectedSlot?.intValue == slot

                    partySlotCard(
                        slot: slot,
                        character: partySlot?.userCharacter,
                        isSelected: isSelected
                    )
                }
            }
        }
    }

    private func partySlotCard(slot: Int, character: UserCharacter?, isSelected: Bool) -> some View {
        Button(action: {
            holder.viewModel.onIntent(intent: PartyIntentSelectSlot(slotPosition: Int32(slot)))
        }) {
            VStack(spacing: 4) {
                if let char = character, let master = char.character {
                    HStack(spacing: 2) {
                        ForEach(0..<Int(master.rarity), id: \.self) { _ in
                            Text("⭐").font(.system(size: 6))
                        }
                    }
                    Text(characterEmoji(master.id))
                        .font(.system(size: 28))
                    Text(master.name)
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(Color(hex: 0x1E293B))
                        .lineLimit(1)
                    Text("Lv.\(char.level)")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundColor(rarityColor(Int(master.rarity)))
                } else {
                    ZStack {
                        Circle()
                            .strokeBorder(Color(hex: 0xCBD5E1), lineWidth: 2)
                            .frame(width: 36, height: 36)
                        Image(systemName: "plus")
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: 0xCBD5E1))
                    }
                    Text("空きスロット")
                        .font(.system(size: 8))
                        .foregroundColor(.secondary)
                }

                Text("Slot \(slot)")
                    .font(.system(size: 7, weight: isSelected ? .bold : .regular))
                    .foregroundColor(isSelected ? Color(hex: 0x3B82F6) : .secondary)
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(0.75, contentMode: .fit)
            .background(
                character != nil
                    ? Color(UIColor.secondarySystemGroupedBackground)
                    : Color(UIColor.tertiarySystemGroupedBackground)
            )
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? Color(hex: 0x3B82F6) : .clear, lineWidth: 2)
            )
            .shadow(color: .black.opacity(character != nil ? 0.05 : 0), radius: 4, y: 2)
        }
        .buttonStyle(.plain)
    }

    // MARK: - スロット選択バナー

    private func slotSelectionBanner(slot: Int) -> some View {
        HStack {
            Text("✨").font(.system(size: 16))
            Text("スロット\(slot)に配置するキャラクターを選んでください")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(.white)
            Spacer()
            Button("キャンセル") {
                holder.viewModel.onIntent(intent: PartyIntentSelectSlot(slotPosition: 0))
            }
            .font(.system(size: 12))
            .foregroundColor(.white.opacity(0.8))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color(hex: 0x3B82F6))
        .cornerRadius(12)
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .shadow(color: Color(hex: 0x3B82F6).opacity(0.3), radius: 8, y: 4)
    }

    // MARK: - キャラ一覧

    private var characterListSection: some View {
        let isSelecting = uiState.selectedSlot != nil && uiState.selectedSlot!.intValue > 0

        return VStack(alignment: .leading, spacing: 10) {
            Text(isSelecting ? "配置するキャラクターを選択" : "所持キャラクター")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(isSelecting ? Color(hex: 0x3B82F6) : Color(hex: 0x1E293B))

            let columns = [
                GridItem(.flexible(), spacing: 10),
                GridItem(.flexible(), spacing: 10),
                GridItem(.flexible(), spacing: 10)
            ]

            LazyVGrid(columns: columns, spacing: 10) {
                ForEach(uiState.ownedCharacters, id: \.id) { char in
                    characterGridCard(char, isSelecting: isSelecting)
                }
            }
        }
        .padding(.top, 8)
    }

    private func characterGridCard(_ character: UserCharacter, isSelecting: Bool) -> some View {
        let master = character.character!

        return Button(action: {
            if let slot = uiState.selectedSlot, slot.intValue > 0 {
                holder.viewModel.onIntent(intent: PartyIntentAssignCharacter(
                    slotPosition: slot.int32Value,
                    userCharacterId: character.id
                ))
            } else {
                holder.viewModel.onIntent(intent: PartyIntentSelectCharacter(userCharacterId: character.id))
            }
        }) {
            VStack(spacing: 0) {
                // 上部
                ZStack(alignment: .topTrailing) {
                    VStack(spacing: 4) {
                        Text(characterEmoji(master.id))
                            .font(.system(size: 36))
                        HStack(spacing: 1) {
                            ForEach(0..<Int(master.rarity), id: \.self) { _ in
                                Text("⭐").font(.system(size: 8))
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(
                        LinearGradient(
                            colors: rarityGradient(Int(master.rarity)).map { $0.opacity(0.12) },
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )

                    if isSelecting {
                        ZStack {
                            Circle()
                                .fill(Color(hex: 0x3B82F6))
                                .frame(width: 20, height: 20)
                            Image(systemName: "plus")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(.white)
                        }
                        .padding(6)
                    }
                }

                // 下部
                VStack(spacing: 2) {
                    Text(master.name)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(Color(hex: 0x1E293B))
                        .lineLimit(1)
                    Text("Lv.\(character.level)")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(rarityColor(Int(master.rarity)))

                    HStack(spacing: 6) {
                        miniStat("❤️", "\(master.baseHp + character.level * 20)")
                        miniStat("⚔️", "\(master.baseAtk + character.level * 8)")
                        miniStat("🛡️", "\(master.baseDef + character.level * 5)")
                    }
                    .padding(.top, 2)
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 8)
            }
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(16)
            .shadow(color: .black.opacity(0.05), radius: 4, y: 2)
        }
        .buttonStyle(.plain)
    }

    private func miniStat(_ emoji: String, _ value: String) -> some View {
        HStack(spacing: 1) {
            Text(emoji).font(.system(size: 8))
            Text(value)
                .font(.system(size: 8, weight: .semibold))
                .foregroundColor(.secondary)
        }
    }

    // MARK: - キャラ詳細オーバーレイ

    private func characterDetailOverlay(_ character: UserCharacter) -> some View {
        let master = character.character!
        let weapon = uiState.ownedWeapons.first(where: { $0.id == character.equippedWeaponId })
        let hp = Int(master.baseHp) + Int(character.level) * 20
        let atk = Int(master.baseAtk) + Int(character.level) * 8
        let def = Int(master.baseDef) + Int(character.level) * 5
        let totalPower = hp + atk * 2 + def

        return ZStack(alignment: .bottom) {
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture {
                    holder.viewModel.onIntent(intent: PartyIntentDismissCharacterDetail())
                }

            VStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color(hex: 0xD1D5DB))
                    .frame(width: 40, height: 4)
                    .padding(.top, 12)

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: 16) {
                        // ヒーロー
                        VStack(spacing: 8) {
                            Text(characterEmoji(master.id))
                                .font(.system(size: 72))
                            HStack(spacing: 2) {
                                ForEach(0..<Int(master.rarity), id: \.self) { _ in
                                    Text("⭐").font(.system(size: 16))
                                }
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 20)
                        .background(
                            LinearGradient(
                                colors: rarityGradient(Int(master.rarity)).map { $0.opacity(0.10) },
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .cornerRadius(20)
                        .padding(.horizontal, 16)

                        // 名前
                        VStack(spacing: 4) {
                            Text(master.name)
                                .font(.system(size: 24, weight: .heavy))
                                .foregroundColor(Color(hex: 0x1E293B))
                            Text("Lv.\(character.level)  ·  XP \(character.currentXp)")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(rarityColor(Int(master.rarity)))
                                .padding(.horizontal, 12)
                                .padding(.vertical, 4)
                                .background(rarityColor(Int(master.rarity)).opacity(0.12))
                                .cornerRadius(8)
                        }

                        // ステータス
                        VStack(alignment: .leading, spacing: 12) {
                            Text("ステータス")
                                .font(.system(size: 15, weight: .bold))
                            statBarView("❤️ HP", value: hp, maxValue: 2000, color: Color(hex: 0xEF4444))
                            statBarView("⚔️ ATK", value: atk, maxValue: 600, color: Color(hex: 0xF59E0B))
                            statBarView("🛡️ DEF", value: def, maxValue: 500, color: Color(hex: 0x3B82F6))

                            Divider()

                            HStack {
                                Text("💪 総合戦闘力")
                                    .font(.system(size: 14))
                                    .foregroundColor(.secondary)
                                Spacer()
                                Text("\(totalPower)")
                                    .font(.system(size: 20, weight: .heavy))
                                    .foregroundColor(Color(hex: 0x1E293B))
                            }
                        }
                        .padding(16)
                        .background(Color(UIColor.systemGroupedBackground))
                        .cornerRadius(16)
                        .padding(.horizontal, 16)

                        // 装備
                        VStack(alignment: .leading, spacing: 10) {
                            Text("装備中の武器")
                                .font(.system(size: 15, weight: .bold))

                            if let w = weapon, let wm = w.weapon {
                                HStack(spacing: 12) {
                                    ZStack {
                                        Circle()
                                            .fill(rarityColor(Int(wm.rarity)).opacity(0.12))
                                            .frame(width: 48, height: 48)
                                        Text(weaponEmoji(w.weaponId))
                                            .font(.system(size: 24))
                                    }
                                    VStack(alignment: .leading) {
                                        HStack(spacing: 4) {
                                            Text(wm.name)
                                                .font(.system(size: 15, weight: .bold))
                                            HStack(spacing: 1) {
                                                ForEach(0..<Int(wm.rarity), id: \.self) { _ in
                                                    Text("⭐").font(.system(size: 10))
                                                }
                                            }
                                        }
                                        Text("Lv.\(w.level)  ·  ATK +\(wm.baseAtk)")
                                            .font(.system(size: 12))
                                            .foregroundColor(.secondary)
                                    }
                                }
                            } else {
                                HStack(spacing: 12) {
                                    ZStack {
                                        Circle()
                                            .strokeBorder(Color(hex: 0xCBD5E1), lineWidth: 2)
                                            .frame(width: 48, height: 48)
                                        Text("—").font(.system(size: 20)).foregroundColor(Color(hex: 0xCBD5E1))
                                    }
                                    Text("武器未装備")
                                        .font(.system(size: 14))
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(hex: 0xFFFBEB))
                        .cornerRadius(16)
                        .padding(.horizontal, 16)

                        Spacer().frame(height: 40)
                    }
                    .padding(.top, 8)
                }
            }
            .frame(maxHeight: UIScreen.main.bounds.height * 0.70)
            .background(Color(UIColor.secondarySystemGroupedBackground))
            .cornerRadius(28, corners: [.topLeft, .topRight])
            .shadow(color: .black.opacity(0.15), radius: 20, y: -5)
        }
    }

    private func statBarView(_ label: String, value: Int, maxValue: Int, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label)
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                Spacer()
                Text("\(value)")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(color)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(Color(hex: 0xE2E8F0))
                        .frame(height: 8)
                    RoundedRectangle(cornerRadius: 4)
                        .fill(color)
                        .frame(width: geo.size.width * min(CGFloat(value) / CGFloat(maxValue), 1.0), height: 8)
                }
            }
            .frame(height: 8)
        }
    }
}

// MARK: - RoundedCorner Helper

private struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        return Path(path.cgPath)
    }
}

private extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

// MARK: - Preview

struct PartyScreenView_Previews: PreviewProvider {
    static var previews: some View {
        PartyScreenView()
    }
}
