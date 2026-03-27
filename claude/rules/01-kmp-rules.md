# Kotlin Multiplatform (KMP) アーキテクチャルール

1. **共有ロジックの最大化:** ビジネスロジックやViewModel（MVIまたはMVVMライブラリを利用）、およびネットワーク通信（Ktor）など、可能な限り多くの要素をKMPの共通(Shared)モジュールに配置してください。
2. **UIの実装:** UIは明示的にネイティブで実装してください。iOSアプリにはSwiftUIを使用し、AndroidアプリにはJetpack Composeを使用します。カスタマイズされた特定のコンポーネントでCompose Multiplatformを使用する場合を除き、ネイティブの操作感を損なうようなUIの共有は避けてください。
3. **データベース:** クロスプラットフォームでのローカルキャッシュ・保存には、SQLDelight または KMP互換バージョンの Room を使用してください。
4. **依存性注入 (DI):** 共有モジュールの依存関係の注入には Koin を使用してください。
5. **命名規則:** iOSとAndroidで対応する画面（View/Screen）のファイル名およびコンポーネント名は、プラットフォーム間で統一してください。例: `StudyQuestScreenView`（iOS: `StudyQuestScreenView.swift`, Android: `StudyQuestScreenView.kt`）。
