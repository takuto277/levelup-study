import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        KoinHelperKt.doInitKoin()
        // DEBUG: seed.sql の固定ユーザーでセッションを埋める（Go DEV_MODE=true 向け）
        // RELEASE: Supabase ログイン後の userId / JWT に任せる（本番 API 試験時はこちら）
#if DEBUG
        KoinHelperKt.setDevSession(useSeedUser: true)
#else
        KoinHelperKt.setDevSession(useSeedUser: false)
#endif
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
