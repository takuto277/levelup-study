# Issue: 認可・LvUP・4スロット編成・BGタイマー・ホームタップ

Fixes #9
Fixes #12
Fixes #14
Fixes #17
Fixes #24

## 背景

- #9: キャラ/武器/パーティ操作の所有者検証不足、マスタジャンル書き込みが API Key のみ
- #12: キャラ・武器 LvUP API / モバイル TODO
- #14: iOS 4 スロット編成 parity（Android も PartySlotSection 未配線）
- #17: 勉強タイマーがフォアグラウンド前提
- #24: ホームの TapMainCharacter が空実装

## スコープ

### #9
- game_handler で user_id 所有検証
- マスタジャンル POST/DELETE に Admin API Key

### #12
- POST level-up API（キャラ・武器）
- Repository / PartyViewModel / 詳細 UI

### #14
- Android/iOS 4 スロット編成 UI

### #17
- StudyQuestViewModel ウォールクロック + 一時停止補正

### #24
- ホームキャラタップでセリフ表示

## 受け入れ条件

- [ ] 他ユーザー resource ID で 403/404
- [ ] LvUP がゴールド消費してレベル更新
- [ ] iOS/Android で 4 スロット編集可能
- [ ] バックグラウンド復帰後も経過時間が正しい
- [ ] ホームタップでセリフが出る
- [ ] CI green
