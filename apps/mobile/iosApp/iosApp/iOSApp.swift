import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        // KMP 共有モジュールの DI コンテナを初期化
        KoinHelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
