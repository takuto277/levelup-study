# キャラ画像のストレージ/リポジトリ レイアウト変更（per-character フォルダ）

作成日: 2026-10-12
ブランチ: `refactor/character-asset-layout`

## 目的

キャラのフレーム画像が `master/characters/character_{code}_{mode}_{frame}.webp` のフラット構成で、
`characters/` と `character_` が重複して見づらい。**1キャラ = 1フォルダ**に変更し、リポジトリ・Storage・アプリ・DB を揃える。

## 新レイアウト

```
master/characters/{code}/{mode}_{frame}.webp
例: master/characters/00002/idle_1.webp
    master/characters/00003/attack_3.webp
```

- `m_characters.image_url` は代表1枚（`.../master/characters/00002/idle_1.webp`）。
- アプリは image_url の**ディレクトリ**をベースに各フレームURLを導出。
- **00001 はアプリ同梱スプライトのため Storage 不要**（削除）。

## 実装詳細

- **リポジトリ**: `backend/assets/master/source/characters/{code}/{mode}_{frame}.webp`（Storage と同一構成）。旧 `character_00001_*`（=00002 青魔導士）を `00002/` へ、`character_00003_*` を `00003/` へ整理。
- **アプリ** `CharacterFrames.kt`: `baseFromImageUrl` を「ファイル名を除いたディレクトリ」に変更し、`frameUrls` は `{base}/{mode}_{frame}.webp`。`storageKey` は従来どおり（`/object/public/{bucket}/{key}` から key 抽出）→ ローカル保存パスも新スキームに追従。
- **入稿** `upload_storage.py` は `{PREFIX}/{filename}` なので、`DIR=.../{code}` `PREFIX=master/characters/{code}` で1キャラずつ。
- **透過スクリプト** `make_player_sprites_transparent.py --kind character`: `iterdir()` → `rglob("*")` にして per-character サブフォルダを対象化。
- **DB/seed**: `...0002` の `image_url` を新URL（`.../characters/00002/idle_1.webp`）に更新（seed.sql + stg DB）。

## 完了条件

- [x] Storage を `master/characters/{code}/...` に再入稿、旧フラット24件を削除
- [x] `00002` の `image_url` を新URLに（seed + stg DB）
- [x] アプリのフレームURL導出・ローカル保存キーを新スキームに
- [ ] アプリ再ビルドで 00002 のフレーム表示確認

## 影響範囲

- `apps/mobile/shared/.../features/character/CharacterFrames.kt`
- `scripts/assets/make_player_sprites_transparent.py`
- `backend/assets/master/source/characters/{00002,00003}/`
- `backend/db/seed.sql`, `docs/assets/02_Supabase_Master_Images.md`

## 後続（未対応）

- 00003（緑の剣士）の `m_characters` 追加・ガチャ投入。
- 00001（同梱）と Storage キャラの扱いの一本化（同梱はそのまま）。
