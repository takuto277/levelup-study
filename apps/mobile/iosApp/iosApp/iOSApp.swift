import SwiftUI
import Shared

@main
struct iOSApp: App {

    private let networkMonitor = NetworkMonitor()

    init() {
        KoinHelperKt.doInitKoin()
        KoinHelperKt.initializeSessionMode(isDebug: isDebugBuild)
        // 環境解決: Edit Scheme → Run → Environment Variables の LEVELUP_ENV（dev/stg/prod）を最優先。
        //   - prod: 本番 API + 本番 Supabase（Release ビルドでもスキームで明示的に prod を選べる）
        //   - dev/stg: デバッグ環境を切り替え。未設定なら保存済み環境、それも無ければ dev。
        let schemeEnv = ProcessInfo.processInfo.environment["LEVELUP_ENV"]
        let savedEnv = UserSessionStore.shared.getDebugEnvironment()
        let resolvedEnv: String
        if let schemeEnv, !schemeEnv.isEmpty {
            resolvedEnv = schemeEnv
        } else if isReleaseBuild {
            resolvedEnv = "prod"
        } else {
            resolvedEnv = savedEnv ?? "dev"
        }
        UserSessionStore.shared.setDebugEnvironment(resolvedEnv)
        initializeEnvironment(resolvedEnv)
        // Guest Session を初期化（Release では必須、Debug Guest 時も実行）
        KoinHelperKt.initializeSessionManagerAsync(isDebug: isDebugBuild)
    }

    private var isReleaseBuild: Bool {
#if DEBUG
        return false
#else
        return true
#endif
    }

    private var isDebugBuild: Bool {
#if DEBUG
        return true
#else
        return false
#endif
    }

    /// 環境名（dev / stg / prod）に応じて API URL・Supabase・JWT を設定する。
    private func initializeEnvironment(_ env: String) {
        switch env {
        case "stg":
            ApiRoutes.shared.BASE_URL = "https://levelup-study-api-stg.onrender.com"
            DevJwtSelector.shared.selectForEnvironment(envName: env)
            SupabaseConfigSelector.shared.initialize(isDebug: true)
            SupabaseConfigSelector.shared.selectForEnvironment(envName: env)
        case "prod":
            ApiRoutes.shared.BASE_URL = ApiRoutes.shared.PROD_URL
            DevJwtSelector.shared.selectForEnvironment(envName: "prod")
            SupabaseConfigSelector.shared.initialize(isDebug: false)
        default:
            ApiRoutes.shared.BASE_URL = "http://localhost:8080"
            DevJwtSelector.shared.selectForEnvironment(envName: "dev")
            SupabaseConfigSelector.shared.initialize(isDebug: true)
            SupabaseConfigSelector.shared.selectForEnvironment(envName: "dev")
        }
    }

    var body: some Scene {
        WindowGroup {
            SessionGateView {
                MainTabView()
            }
        }
    }
}
