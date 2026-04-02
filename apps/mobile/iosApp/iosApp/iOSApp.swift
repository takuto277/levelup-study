import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        KoinHelperKt.doInitKoin()
        KoinHelperKt.setDevSession()
    }

    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
