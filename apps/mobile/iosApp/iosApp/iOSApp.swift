import SwiftUI
import Shared

@main
struct iOSApp: App {

    private let networkMonitor = NetworkMonitor()

    init() {
        KoinHelperKt.doInitKoin()
        // DEBUG: 保存されている SessionMode を復元。未保存なら Seed（互換性維持）。
        // Supabase の auth ユーザー UUID とは別。seed-remote / seed は public.users にこの固定 ID を入れる。
        // make run の DATABASE_URL は seed を流した DB と同じ .env にすること。
        // RELEASE: 常に Guest モード相当（Seed 固定を外す）
#if DEBUG
        let isDebug = true
        KoinHelperKt.initializeSessionMode(isDebug: isDebug)
        // 保存されたデバッグ環境を復元。未保存なら dev（http://localhost:8080）がデフォルト
        let savedEnv = UserSessionStore.shared.getDebugEnvironment() ?? "dev"
        switch savedEnv {
        case "stg":
            ApiRoutes.shared.BASE_URL = "https://levelup-study-api-stg.onrender.com"
        default:
            ApiRoutes.shared.BASE_URL = "http://localhost:8080"
        }
        DevJwtSelector.shared.selectForEnvironment(envName: savedEnv)
        SupabaseConfigSelector.shared.selectForEnvironment(envName: savedEnv)
#else
        let isDebug = false
        KoinHelperKt.initializeSessionMode(isDebug: isDebug)
#endif
        // Guest Session を初期化（Release では必須、Debug Guest 時も実行）
        KoinHelperKt.initializeSessionManagerAsync(isDebug: isDebug)
    }

    var body: some Scene {
        WindowGroup {
            SessionGateView {
                MainTabView()
            }
        }
    }
}
