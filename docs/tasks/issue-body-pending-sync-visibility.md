# Issue: 未同期セッションの状態表示・手動リトライ

## 背景

モバイルには `PendingStudyQueueStore` があり、オフラインまたは送信失敗時の勉強完了をローカルに保存して復帰後に同期する仕組みがあります。一方で、ユーザーに対して「何件未同期か」「失敗したか」「いつ再試行されるか」を見せる UI は薄く、失敗が長引いた場合に報酬が未確定であることに気づきにくいです。

DB 設計書には `local_pending_sessions` の `sync_status` / retry 方針が書かれていますが、実装は Key-Value の配列保存のみです。

## 目的

未同期の勉強セッションをユーザーが把握でき、必要に応じて手動リトライできるようにする。報酬の仮表示と確定表示の差も分かるようにする。

## スコープ

### Mobile

- [ ] pending queue に同期状態を持たせる
  - `pending`, `syncing`, `failed`
  - `retry_count`, `last_error`, `last_attempt_at`
- [ ] 記録タブまたは設定画面に未同期件数を表示する
- [ ] 手動リトライ操作を追加する
- [ ] 3回以上失敗した場合にユーザーへ明示する
- [ ] 同期成功後に確定報酬を反映する

### Docs / QA

- [ ] オフライン開始・オンライン復帰・連続失敗・手動リトライの確認項目を追加する
- [ ] pending 中の勉強時間を記録画面に含める/含めない方針を明記する

## 受け入れ条件

- オフラインで完了した勉強セッションが未同期として表示される
- オンライン復帰後に自動同期され、成功後に未同期表示から消える
- 同期失敗時に失敗状態と再試行手段が表示される
- 手動リトライで同期成功した場合、報酬とユーザー情報が最新化される
- pending データが壊れていてもアプリがクラッシュしない

## 参照

- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/data/local/PendingStudyQueueStore.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/domain/model/PendingStudyCompletion.kt`
- `apps/mobile/shared/src/commonMain/kotlin/org/example/project/data/repository/StudyRepositoryImpl.kt`
- `docs/database/01_Database_Schema.md` §4.1
