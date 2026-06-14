# iOS / Android ストア申請とリリースチェックリスト

## 共通

- [ ] アプリアイコン (1024x1024) が設定済み
- [ ] スクリーンショット (6.5/5.5 inch) が準備済み
- [ ] プライバシーポリシー URL が有効
- [ ] サポート URL が有効
- [ ] アプリの説明文・キーワードが最適化済み

## Android (Google Play)

- [ ] release keystore を安全に保管
- [ ] google-services.json が本番用
- [ ] ProGuard/R8 が有効
- [ ] targetSdkVersion が最新
- [ ] Android App Bundle (.aab) でビルド

## iOS (App Store)

- [ ] 開発チーム・証明書が有効
- [ ] Info.plist のプライバシー記述が完了
- [ ] App Transport Security 設定
- [ ] アーカイブ → アップロードが成功
- [ ] TestFlight 配信テスト済み
