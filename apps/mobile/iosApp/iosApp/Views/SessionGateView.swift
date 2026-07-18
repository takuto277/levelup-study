import SwiftUI
import Shared

struct SessionGateView<Content: View>: View {
    let content: () -> Content

    @State private var state: String = "Initializing"
    @State private var errorMessage: String?

    private var isReady: Bool { state == "Ready" }
    private var isError: Bool { state == "RecoverableError" || state == "ResetRequired" }

    var body: some View {
        ZStack {
            bgDark.ignoresSafeArea()

            if isReady {
                content()
            } else {
                VStack(spacing: 16) {
                    ProgressView()
                        .scaleEffect(1.5)
                        .tint(accentBlue)

                    Text("セッションを準備中...")
                        .font(.subheadline)
                        .foregroundColor(textSecondary)

                    if isError {
                        VStack(spacing: 12) {
                            if let message = errorMessage {
                                Text(message)
                                    .font(.footnote)
                                    .foregroundColor(textSecondary)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 32)
                            }

                            Button(action: {
                                KoinHelperKt.retrySessionManagerAsync()
                            }) {
                                Text("再試行")
                                    .font(.headline.weight(.bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: 200)
                                    .frame(height: 44)
                                    .background(accentBlue)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                        .padding(.top, 8)
                    }
                }
            }
        }
        .onAppear {
            KoinHelperKt.observeSessionGateState { stateStr, message in
                DispatchQueue.main.async {
                    withAnimation(.easeInOut(duration: 0.15)) {
                        state = stateStr
                        errorMessage = message as? String
                    }
                }
            }
        }
    }
}

private let bgDark = Color(red: 0.04, green: 0.05, blue: 0.12)
private let textSecondary = Color(red: 0.58, green: 0.64, blue: 0.71)
private let accentBlue = Color(red: 0.23, green: 0.51, blue: 0.96)
