# LevelUp Study — データベーススキーマ設計

## 概要
本ドキュメントは、LevelUp Study のバックエンド（Go + PostgreSQL）で使用する全テーブルの定義と関連を記述する。
ORM は GORM を使用し、開発環境では `AutoMigrate` でテーブルを自動作成する。

---

## ER図（概要）

```
users ─────────┬──── study_sessions ──── study_rewards
               │
               ├──── user_characters ──→ m_characters
               │         │
               │         └──→ user_weapons ──→ m_weapons
               │
               ├──── user_party_slots ──→ user_characters
               │
               ├──── user_dungeon_progresses ──→ m_dungeons
               │
               └──── gacha_histories ──→ m_gacha_banners

m_dungeons ──── m_dungeon_stages
```

---

## テーブル一覧

### ユーザー系

| テーブル名 | 用途 | 主キー |
|---|---|---|
| `users` | ユーザー基本情報（石・ゴールド・累計勉強時間） | `id (UUID)` |
| `study_sessions` | 勉強セッション記録（1回のポモドーロ） | `id (UUID)` |
| `study_rewards` | セッション報酬明細（石・ゴールド・XP等） | `id (UUID)` |

### マスタデータ系（運営管理）

| テーブル名 | 用途 | 主キー |
|---|---|---|
| `m_characters` | キャラクターマスタ（名前・レアリティ・ステータス・画像URL） | `id (UUID)` |
| `m_weapons` | 武器マスタ（名前・レアリティ・ATK・画像URL） | `id (UUID)` |
| `m_dungeons` | ダンジョンマスタ（名前・解放条件・画像URL） | `id (UUID)` |
| `m_dungeon_stages` | ダンジョンステージ（敵構成・ドロップテーブル） | `id (UUID)` |
| `m_gacha_banners` | ガチャバナー（種類・期間・天井・排出テーブル） | `id (UUID)` |

### ユーザー所持・進行系

| テーブル名 | 用途 | 主キー |
|---|---|---|
| `user_characters` | ユーザー所持キャラ（レベル・XP・装備武器） | `id (UUID)` |
| `user_weapons` | ユーザー所持武器（レベル） | `id (UUID)` |
| `user_party_slots` | パーティ編成（スロット1〜4） | `id (UUID)` |
| `user_dungeon_progresses` | ダンジョン進行状況（現在ステージ・最高クリア） | `id (UUID)` |
| `gacha_histories` | ガチャ履歴（天井カウント管理） | `id (UUID)` |

---

## 各テーブル詳細

### `users`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | ユーザーID |
| display_name | VARCHAR(50) | NOT NULL | 表示名 |
| total_study_seconds | BIGINT | NOT NULL, DEFAULT 0 | 累計勉強秒数 |
| stones | INT | NOT NULL, DEFAULT 0 | ガチャ用通貨（知識の結晶） |
| gold | INT | NOT NULL, DEFAULT 0 | 強化用通貨（ゴールド） |
| created_at | TIMESTAMP | auto | 作成日時 |
| updated_at | TIMESTAMP | auto | 更新日時 |

### `study_sessions`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | セッションID |
| user_id | UUID | NOT NULL, INDEX | FK→users |
| category | VARCHAR(50) | nullable | 勉強カテゴリ（数学, 語学等） |
| started_at | TIMESTAMP | NOT NULL | 開始日時 |
| ended_at | TIMESTAMP | NOT NULL | 終了日時 |
| duration_seconds | INT | NOT NULL | 実勉強秒数 |
| is_completed | BOOL | NOT NULL, DEFAULT false | ポモドーロ目標達成か |
| created_at | TIMESTAMP | auto | 記録日時 |

### `study_rewards`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | 報酬ID |
| session_id | UUID | NOT NULL, INDEX | FK→study_sessions |
| reward_type | VARCHAR(30) | NOT NULL | stones / gold / xp / item_drop 等 |
| amount | INT | NOT NULL | 獲得量 |
| item_id | UUID | nullable | ドロップアイテム時のマスタID |
| created_at | TIMESTAMP | auto | 記録日時 |

### `m_characters`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | キャラID |
| name | VARCHAR(100) | NOT NULL | キャラ名 |
| rarity | INT | NOT NULL | 星1〜5 |
| base_hp | INT | NOT NULL | 基本HP |
| base_atk | INT | NOT NULL | 基本攻撃力 |
| base_def | INT | NOT NULL | 基本防御力 |
| image_url | TEXT | NOT NULL | 立ち絵URL |
| idle_animation_url | TEXT | nullable | ホーム画面用アニメーションURL |
| is_active | BOOL | NOT NULL, DEFAULT true | 有効フラグ（論理削除用） |
| created_at | TIMESTAMP | auto | 登録日時 |

### `m_weapons`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | 武器ID |
| name | VARCHAR(100) | NOT NULL | 武器名 |
| rarity | INT | NOT NULL | 星1〜5 |
| base_atk | INT | NOT NULL | 基本ATK |
| image_url | TEXT | NOT NULL | 武器画像URL |
| is_active | BOOL | NOT NULL, DEFAULT true | 有効フラグ |
| created_at | TIMESTAMP | auto | 登録日時 |

### `m_dungeons`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | ダンジョンID |
| name | VARCHAR(100) | NOT NULL | ダンジョン名 |
| sort_order | INT | NOT NULL | 表示順 |
| unlock_condition | TEXT | nullable | 解放条件（JSON） |
| image_url | TEXT | NOT NULL | ダンジョン画像URL |
| is_active | BOOL | NOT NULL, DEFAULT true | 有効フラグ |
| created_at | TIMESTAMP | auto | 登録日時 |

### `m_dungeon_stages`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | ステージID |
| dungeon_id | UUID | NOT NULL, INDEX | FK→m_dungeons |
| stage_number | INT | NOT NULL | ステージ番号 |
| recommended_power | INT | NOT NULL | 推奨戦力 |
| enemy_composition | JSONB | NOT NULL | 敵構成 `[{name, emoji, hp, atk}]` |
| drop_table | JSONB | NOT NULL | ドロップ `[{item_type, amount, rate}]` |

### `m_gacha_banners`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | バナーID |
| name | VARCHAR(100) | NOT NULL | バナー名 |
| banner_type | VARCHAR(30) | NOT NULL | character / weapon / mixed |
| start_at | TIMESTAMP | NOT NULL | 開始日時 |
| end_at | TIMESTAMP | NOT NULL | 終了日時 |
| pity_threshold | INT | nullable | 天井回数（nullなら天井なし） |
| rate_table | JSONB | NOT NULL | 排出テーブル `[{item_id, result_type, rarity, rate}]` |
| is_active | BOOL | NOT NULL, DEFAULT true | 有効フラグ |

### `user_characters`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | 所持キャラID |
| user_id | UUID | NOT NULL, INDEX | FK→users |
| character_id | UUID | NOT NULL | FK→m_characters |
| level | INT | NOT NULL, DEFAULT 1 | 現在レベル |
| current_xp | INT | NOT NULL, DEFAULT 0 | 累積経験値 |
| equipped_weapon_id | UUID | nullable | FK→user_weapons |
| obtained_at | TIMESTAMP | NOT NULL | 入手日時 |

### `user_weapons`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | 所持武器ID |
| user_id | UUID | NOT NULL, INDEX | FK→users |
| weapon_id | UUID | NOT NULL | FK→m_weapons |
| level | INT | NOT NULL, DEFAULT 1 | 現在レベル |
| obtained_at | TIMESTAMP | NOT NULL | 入手日時 |

### `user_party_slots`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | スロットID |
| user_id | UUID | NOT NULL, UNIQUE(user_id, slot_position) | FK→users |
| slot_position | INT | NOT NULL | スロット番号（1〜4） |
| user_character_id | UUID | NOT NULL | FK→user_characters |

### `user_dungeon_progresses`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | 進行ID |
| user_id | UUID | NOT NULL | FK→users |
| dungeon_id | UUID | NOT NULL | FK→m_dungeons |
| current_stage | INT | NOT NULL, DEFAULT 1 | 現在挑戦中ステージ |
| max_cleared_stage | INT | NOT NULL, DEFAULT 0 | 最高クリア済みステージ |
| updated_at | TIMESTAMP | auto | 更新日時 |

### `gacha_histories`
| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| id | UUID | PK | 履歴ID |
| user_id | UUID | NOT NULL | FK→users |
| banner_id | UUID | NOT NULL | FK→m_gacha_banners |
| result_type | VARCHAR(20) | NOT NULL | character / weapon |
| result_item_id | UUID | NOT NULL | 排出されたマスタID |
| pity_count | INT | NOT NULL | バナー内累計回数 |
| created_at | TIMESTAMP | auto | ガチャ実行日時 |

---

## 画像の保管方針

| 環境 | 保管場所 | 備考 |
|---|---|---|
| 開発（ローカル） | placeholder URL (`placehold.co`) | seed.sql に設定済み |
| ステージング/本番 | Cloudflare R2 / Supabase Storage | 10GB無料枠あり。CDN経由で配信 |

画像を用意したら、DBの `image_url` カラムを更新するだけで切り替え可能。

---

## シードデータの投入

```bash
cd backend
make seed
```

詳細は `docs/tasks/20260402-master-data-setup.md` を参照。
