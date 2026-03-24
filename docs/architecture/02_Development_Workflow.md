# LevelUp Study - 開発フロー定義 (Development Workflow)

このドキュメントは、本プロジェクト（モノレポ構成：iOS Native + KMP + Go backend）において、最も効率良く無駄のない開発を進めるための「標準実装ステップ」を定義しています。

---

## 🚀 基本方針: UI主導型（Outside-in アプローチ）

当プロジェクトでは、バックエンドのDB設計やインフラから構築を始める「バックエンド主導の手法（ウォーターフォール）」を極力避け、**「最もユーザー体験に近いUI（スイートスポット）」から構築を始める** ことを推奨します。
特にiOSエンジニアが主導する際は、このステップを踏むことでUIの完成度と手戻りのない設計が担保されます。

### 1. 📱 モックUI構築 (SwiftUI / Jetpack Compose)
- **やること:** データのことは一切忘れ、ダミーのテキストや画像（ハードコード）を使って、画面のレイアウトを完成度100%まで作り上げます。
- **目的:** 画面に必要なデータ構造と、ユーザーの操作（ボタンタップ等のイベント）を**UI側から**確定させるため。

### 2. 📦 状態定義・ViewModel構築 (KMP Shared)
- **やること:** UIが完成したら、共通モジュールに移動し、UIを描画するために必要なデータクラス（State）と、アクションを受け取る関数（Intent）だけを持った `ViewModel` を定義します。
  - この段階でUI（SwiftUI等）をこのKMPのViewModelにバインディング（結合）させます。
- **目的:** クライアント側（iOS/Android）のアプリからバックエンドに対しての「入力と出力の要件」をここで完全にロックするため。

### 3. 🤔 GoのAPI・DB設計 (Backend)
- **やること:** KMP側が欲しいデータ構造（JSON）が明確になった段階で、Goプロジェクト（`backend/`）へと移動します。
- **手順:** 
  1. `docs/templates/` を使い、APIとDBスキーマ（ER図）の仕様書を書く。
  2. PostgreSQL等のDDL（テーブル）を作成し、GoでルーティングとDB処理を実装する。
  3. UIが必要としている形そのままのJSONを返すAPIエンドポイントを作る。

### 4. 🤝 データ結合 (KMP & Backend連携)
- **やること:** 再びKMP側に移動し、`Ktor` (HTTPクライアントライブラリ)とリポジトリパターンを使ってGoのAPIを叩く処理を書きます。
- **結果:** 叩いたレスポンス（本物のデータ）をSTEP 2で作ったViewModelに流し込むことで、STEP 1で作ったダミーの画面が**「本物のデータを使って動く」**状態になります。

---

## 🛠️ 機能追加時のチェックリスト (Checklist)

新しい機能を実装する際は、以下の順番でドキュメント化と実装を進めてください。

- [ ] `docs/planning/01_Features_and_Roadmap.md` の「考案」から機能を選ぶか追加する
- [ ] `docs/templates/Requirements_Design_Template.md` を使い、`docs/features/` 配下に「どういう機能か・どんな画面か」の仕様書を作成する
- [ ] **(STEP 1)** iOS / Android のネイティブUIをモックで作る
- [ ] **(STEP 2)** KMP層で ViewModel (StateFlow) と UseCase のインターフェースを切る
- [ ] **(STEP 3)** GoにてDB実装・APIエンドポイントの追加 (`docs/templates/API_Design_Template.md`等を活用)
- [ ] **(STEP 4)** KMP層にて Repository を実装し、APIと通信して ViewModel へ流し込む
- [ ] テスト動作確認後、Roadmapドキュメントを「実装済み」に更新する
