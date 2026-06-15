import Foundation
import Network

@objc final class NetworkMonitor: NSObject {
    @objc static let shared = NetworkMonitor()
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkMonitor")
    @objc var isOnline = true

    override private init() {
        super.init()
        monitor.pathUpdateHandler = { [weak self] path in
            self?.isOnline = path.status == .satisfied
        }
        monitor.start(queue: queue)
    }
}
