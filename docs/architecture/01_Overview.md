# LevelUp Study - 勉強RPGアプリ 基本設計 (Architecture Overview)

## プロジェクト概要 (Project Overview)
**コンセプト:** 勉強時間に応じてガチャ（キャラクターや武器）を引くことができ、獲得したキャラクタで「冒険」を行える勉強×RPGアプリ。
**目標プラットフォーム:** iOS (SwiftUI) & Android (Jetpack Compose)
**バックエンド:** Go

---

## 1. モノレポ構成 (Monorepo Structure)
このプロジェクトは、フロントエンド(iOS/Android)およびバックエンドを一つのリポジトリで管理するモノレポ構成を採用しています。

```
levelup-study/
 ├── apps/                 # モバイルアプリのディレクトリ
 │    ├── iosApp/          # iOSアプリ (SwiftUIによるネイティブUI実装)
 │    ├── androidApp/      # Androidアプリ (Jetpack Composeによる実装)
 │    └── shared/          # KMP (Kotlin Multiplatform) モジュール。ビジネスロジックやViewModel相当を配置
 ├── backend/              # バックエンドシステム (Goによる実装)
 │    ├── cmd/             # アプリケーションのエントリーポイント
 │    ├── internal/        # Goの標準的な構成に基づくプライベートなビジネスロジック群
 │    ├── pkg/             # 他プロジェクトでも利用可能な公開パッケージ群
 │    └── api/             # OpenAPI定義やプロトコルバッファ定義など
 ├── docs/                 # 要件定義や設計、機能提案書を保管するディレクトリ
 │    ├── features/        # 機能ごとの個別のフォーマットを置く
 │    ├── 00_Template_Requirements_Design.md
 │    └── 01_Architecture_Overview.md
 ├── claude/               # AI関連の設定やルール
 │    └── rules/           # ClaudeやAntigravity向けの開発ルールの定義
 └── claude.md             # AntigravityやClaude向けのプロジェクトグローバルなコンテキスト定義
```

---

## 2. フロントエンド責務分離ルール (KMP vs Native)

KMP（共有モジュール）とネイティブ（SwiftUI / Jetpack Compose）の責務境界を明確にし、**「UIとプラットフォーム固有機能以外はすべてKMPに書く」** ことを基本ルールとします。

### 📌 共有モジュール (KMP - `apps/shared/`) の責務
- **ドメインルール (UseCases / Domain):** タイマーの計算、取得ポイントの算出、条件判定などのすべてのビジネスロジック。
- **データ層 (Repository / Network / DB):** API通信（`Ktor`）、ローカルデータ保存（`SQLDelight` または `Room`）、データキャッシュ戦略。
- **プレゼンテーション層 (ViewModels):** 
  - 画面の今の状態（UI State）を `StateFlow` 等で保持する。
  - ユーザーからの操作（ボタンタップ等のIntent/Event）を受け取り、UseCaseを呼び出してStateを更新する。
  - **重要:** ここにはiOSの `UIKit/SwiftUI` や Androidの `Context` などのプラットフォーム依存のコードは一切含めない。
- **DI (依存性注入):** `Koin` などを利用して、共通モジュール内の依存関係を解決する。

### 📌 ネイティブ層 (iOS: Swift / Android: Kotlin) の責務
- **UIコンポーネントと描画:** `SwiftUI` および `Jetpack Compose` を使った画面レイアウト、ボタン、リストの構築。
- **アニメーションとUIエフェクト:** リッチな装飾や、タップ時の独自のインタラクション。
- **画面遷移 (Navigation / Routing):** `NavigationStack` やNavigation Compose を使った各画面間のルーティング処理（※Decomposeなどを利用する場合も、最終的な描画と遷移アニメーションはネイティブが担当）。
- **KMPのViewModelの購読:** KMP側から提供される `StateFlow` を `@StateObject` や `ObservedObject`, `collectAsState` 等で監視し、UIのViewにバインディングする。
- **プラットフォーム固有のAPI (Platform APIs):**
  - KMPで抽象化するのが難しい、またはネイティブで書いたほうが早いOS独自の機能。
  - *(例: ローカルプッシュ通知(APNs), Widget機能, 課金処理(StoreKit), HealthKitなど)*
  - 上記の機能は直接ネイティブ側で実装するか、KMPの `expect/actual` 構文を用いてKMPインターフェイス経由でネイティブ実装を提供する。

---

## 3. バックエンド設計方針 (Backend: Go)
- バックエンドは **Go** を使用し、モノリシックアーキテクチャから開始してスケール可能に設計します。
- **フレームワーク:** Gin or Echo もしくは標準の `net/http` とルーティングライブラリ(chi)を検討。
- **データベース:** PostgreSQL を採用し、マイグレーションツール(golang-migrate or goose)、ORM(GORMやsqlc)を使用。
- **認証機構:** JWTやFirebase Authenticationを使用して、APIの保護を行います。

---

## 4. 主な機能ドメイン (Core Domains)

### 4.1 勉強時間計測 (Study Timer)
- ユーザーは「ポモドーロ機能」や「ストップウォッチ機能」で勉強時間を計測。
- 計測時間はローカル(KMPDB)に保存され、ネットワーク通信が安定したタイミングでGoバックエンドに同期(Sync)する仕組みを構築。

### 4.2 ガチャシステム (Gacha/Summon)
- ユーザーの勉強時間/セッションに応じて「ポイント」を付与。
- ポイントを使ってバックエンドのガチャAPIをコール。レスポンスで当たったキャラクター/武器を返す。
- ※ガチャ排出率などの重要なロジックは必ず**サーバーサイド（Go）**で実装し、クライアントからチートできないようにする。

### 4.3 RPG・冒険要素 (Adventure)
- 集めたキャラや武器でパーティを編成。
- キャラクターには属性やステータスがあり、ステージを進行して敵(モンスター)を倒していく放置RPG・またはオートバトル機能などを検討。これらもバックエンド側でスタミナや結果の整合性を管理。

---

## 5. 今後のステップ (Next Steps)
1. 要件定義のブラッシュアップ（どんなガチャがあるか？RPGの戦闘システムはどうするか？）
2. `apps/` への Kotlin Multiplatform プロジェクトの初期化 (`kmp-wizard` 等の利用)
3. `backend/` のGoモジュールの初期化と最低限のAPI立ち上げ (Ping/Pongなど)
4. クライアントからの疎通確認
