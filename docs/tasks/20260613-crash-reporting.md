# クラッシュレポート・監視計画

## MVP

- Render logs: stderr 出力が log drain で収集される
- `log.Fatal` / `slog.Error` で重大エラーを記録
- モバイル: 未ハンドル例外をリングバッファに保存（#114 ログ基盤参照）

## 将来

- Sentry / Firebase Crashlytics 導入
- Render → Papertrail / Logtail 連携
- Slack 通知（ERROR レベルのみ）
- クラッシュ率・ANR 率ダッシュボード
