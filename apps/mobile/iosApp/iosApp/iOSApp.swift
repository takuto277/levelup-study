import SwiftUI
import Shared

@main
struct iOSApp: App {

    private let networkMonitor = NetworkMonitor()

    init() {
        KoinHelperKt.doInitKoin()
        // DEBUG: seed.sql の user1 (00000000-...-001) を毎回セッションに固定（forceSeedUserId）。
        // Supabase の auth ユーザー UUID とは別。seed-remote / seed は public.users にこの固定 ID を入れる。
        // make run の DATABASE_URL は seed を流した DB と同じ .env にすること。
        // RELEASE: Supabase ログイン後の userId / JWT に任せる（本番 API 試験時はこちら）
#if DEBUG
        // seed.sql / seed-remote の user1 と常に一致（KeyValueStore の古い UUID を残さない）
        KoinHelperKt.setDevSession(useSeedUser: true, forceSeedUserId: true)
#else
        KoinHelperKt.setDevSession(useSeedUser: false, forceSeedUserId: false)
#endif
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
