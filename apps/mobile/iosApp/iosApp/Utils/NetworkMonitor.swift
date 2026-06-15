import Network
import Shared

final class NetworkMonitor: ObservableObject {
    @Published var isOnline = true
    private var monitor: NWPathMonitor?

    init() {
        monitor = NWPathMonitor()
        monitor?.pathUpdateHandler = { [weak self] path in
            let online = path.status == .satisfied
            DispatchQueue.main.async {
                self?.isOnline = online
                DeviceOnline_iosKt.iosNetworkMonitorOnline = online
            }
        }
        monitor?.start(queue: DispatchQueue.global())
    }
}
