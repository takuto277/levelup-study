## Issues

- Close #51

## 背景

モバイルには `PendingStudyQueueStore` があり、オフライン時に勉強完了をローカル保存して復帰後に同期する仕組みがあるが、ユーザーに未同期件数や失敗状態が見えず、手動リトライ手段もなかった。

## 概要

PendingStudyCompletion に syncStatus/retryCount/lastError フィールドを追加し、syncPendingSessions でアイテム単位の状態追跡を実装。記録画面に未同期件数表示と手動リトライボタンを追加（Android/iOS 両方）。

## 変更内容

### KMP Shared
- **`PendingStudyCompletion`**: `syncStatus`, `retryCount`, `lastError`, `lastAttemptAt` フィールド追加（デフォルト値付きで後方互換）
- **`SyncStatus`**: `PENDING`/`SYNCING`/`FAILED`/`MAX_RETRIES=3` 定数
- **`PendingStudyQueueStore`**: `count()`, `countByStatus()`, `hasFailedItems()`, `updateStatus()` メソッド追加
- **`StudyRepository`**: `getPendingCount()`, `hasFailedPendingSessions()`, `retryFailedSessions()` 追加
- **`StudyRepositoryImpl`**: syncPendingSessions をアイテム単位で状態追跡するよう改善（失敗時も break せず継続、3回失敗で FAILED）。retryFailedSessions 実装
- **`RecordUiState`**: `pendingCount`, `hasFailedPending`, `isSyncingPending` フィールド追加
- **`RecordIntent`**: `RetryPending` 追加
- **`RecordViewModel`**: refreshData で pending 状態読み込み、retryPending 処理追加

### Android / iOS UI
- 記録画面のヘッダ直下に未同期バナー表示
- 通常時: 「📡 未同期の勉強セッション」+ 件数
- 失敗時: 「⚠️ 同期に失敗したセッションがあります」+ 再試行ボタン（赤）
- 同期中は ProgressIndicator 表示
- タップで手動リトライ実行

## 確認項目

- [x] フォーマット
- [x] Go vet pass
- [ ] KMP コンパイル（CI で検証、Java 未インストール環境のため未実行）
- [ ] 手動確認: オフライン勉強 → 記録画面に未同期表示 → オンライン復帰で自動同期 → 表示消滅
- [ ] 手動確認: 3回失敗 → 赤バナー表示 → 手動リトライで成功
- [ ] 手動確認: pending データ破損時もクラッシュしない（readAll の try-catch 継続）
