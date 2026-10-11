## 背景
ガチャ `POST /users/{userID}/gacha/pull` は既にキャラを抽選して `name`/`rarity` を返すが、**画像URLを返していない**。結果カードは汎用アイコン表示のみ。

## やること
- API レスポンスに `image_url` を追加（キャラ/武器/衣装）
- モバイルで結果カードに画像を表示（無ければアイコンfallback）

## 方針
- 画像の実体は Storage から直DL（APIはメタのみ）。base64埋め込みやAPIでのバイト配信はしない。
- まずは代表1枚を表示。複数フレーム配信は別途。

## 完了条件
- [ ] `POST /gacha/pull` に `image_url` が含まれる
- [ ] character 00002 で名前＋画像が出る
- [ ] 単発・10連の両方で表示
