import SwiftUI
import Shared

/// 設定シート（デバッグビルド時のみデバッグブロックを表示）
struct SettingsScreenView: View {
    let onDismiss: () -> Void
    let onClosedRefreshHome: () -> Void

    private let vm = KoinHelperKt.getSettingsViewModel()

    @State private var state: SettingsUiState?
    private let poll = Timer.publish(every: 0.35, on: .main, in: .common).autoconnect()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("API ベース URL")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(state?.apiBaseUrl ?? "—")
                        .font(.footnote)
                        .textSelection(.enabled)

                    Divider()

                    #if DEBUG
                    debugSection
                    #else
                    Text("デバッグメニューはデバッグビルドでのみ表示されます。")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                    #endif

                    if let t = state?.toast, !t.isEmpty {
                        Text(t)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }

                    Divider()

                    Button("オンボーディングを再表示") {
                        UserDefaults.standard.set(false, forKey: "onboarding_done")
                        vm.clearToastFromPlatform()
                        onClosedRefreshHome()
                        onDismiss()
                    }
                    .font(.subheadline)

                    Button("チュートリアルをリセット") {
                        TutorialHelper.shared.resetAllTutorials()
                        NotificationCenter.default.post(name: Notification.Name("TutorialDidReset"), object: nil)
                        vm.clearToastFromPlatform()
                        onClosedRefreshHome()
                        onDismiss()
                    }
                    .font(.subheadline)
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .navigationTitle("設定")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("閉じる") {
                        vm.clearToastFromPlatform()
                        onClosedRefreshHome()
                        onDismiss()
                    }
                }
            }
        }
        .onAppear {
            state = vm.uiState.value as? SettingsUiState
        }
        .onReceive(poll) { _ in
            state = vm.uiState.value as? SettingsUiState
        }
    }

    #if DEBUG
    @ViewBuilder
    private var debugSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("デバッグ")
                .font(.headline)

            Text("ユーザー ID（API に送る値）")
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(state?.displayedUserId ?? "—")
                .font(.caption.monospaced())
                .textSelection(.enabled)

            Text("シード固定: \((state?.forceDevSeed == true) ? "ON" : "OFF")")
                .font(.caption2)
                .foregroundStyle(.secondary)

            Text("石: \(state?.stones ?? 0)")
                .font(.subheadline.weight(.semibold))
            HStack(spacing: 8) {
                ForEach([-100, -10, 10, 100], id: \.self) { d in
                    Button(d > 0 ? "+\(d)" : "\(d)") {
                        vm.patchCurrenciesFromPlatform(stonesDelta: Int32(d), goldDelta: 0)
                    }
                    .buttonStyle(.bordered)
                }
            }

            Text("ゴールド: \(state?.gold ?? 0)")
                .font(.subheadline.weight(.semibold))
            HStack(spacing: 8) {
                ForEach([-500, -100, 100, 500], id: \.self) { d in
                    Button(d > 0 ? "+\(d)" : "\(d)") {
                        vm.patchCurrenciesFromPlatform(stonesDelta: 0, goldDelta: Int32(d))
                    }
                    .buttonStyle(.bordered)
                }
            }

            Text("POST /api/v1/debug/users/{id}/currencies は Go の DEV_MODE=true のときのみ有効です。")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
    #endif
}
