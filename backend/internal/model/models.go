package model

import (
	"encoding/json"
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

// ============================================================
// BeforeCreate フック共通 — UUID が未設定の場合に Go 側で生成する
// PostgreSQL の gen_random_uuid() に依存しない（SQLite テスト互換）
// ============================================================

func ensureUUID(id *uuid.UUID) {
	if *id == uuid.Nil {
		*id = uuid.New()
	}
}

// ============================================================
// users — ユーザー基本情報
// 石・ゴールドの正はサーバー側。クライアントで加算させない。
// ============================================================

type User struct {
	ID                uuid.UUID `gorm:"type:uuid;primaryKey" json:"id"`
	DisplayName       string    `gorm:"type:varchar(50);not null"                       json:"display_name"`
	TotalStudySeconds int64     `gorm:"not null;default:0"                              json:"total_study_seconds"`
	Stones            int       `gorm:"not null;default:0"                              json:"stones"`
	Gold              int       `gorm:"not null;default:0"                              json:"gold"`
	CreatedAt         time.Time `gorm:"autoCreateTime"                                  json:"created_at"`
	UpdatedAt         time.Time `gorm:"autoUpdateTime"                                  json:"updated_at"`
}

func (u *User) BeforeCreate(tx *gorm.DB) error { ensureUUID(&u.ID); return nil }

// ============================================================
// study_sessions — 勉強セッション
// 1回の勉強（ポモドーロ or ストップウォッチ）の記録。
// ============================================================

type StudySession struct {
	ID              uuid.UUID `gorm:"type:uuid;primaryKey" json:"id"`
	UserID          uuid.UUID `gorm:"type:uuid;not null;index"                       json:"user_id"`
	Category        *string   `gorm:"type:varchar(50)"                               json:"category"`         // 勉強カテゴリ（英語, 数学 等）
	StartedAt       time.Time `gorm:"not null"                                       json:"started_at"`       // 開始日時
	EndedAt         time.Time `gorm:"not null"                                       json:"ended_at"`         // 終了日時
	DurationSeconds int       `gorm:"not null"                                       json:"duration_seconds"` // 実勉強秒数
	IsCompleted     bool      `gorm:"not null;default:false"                         json:"is_completed"`     // ポモドーロ目標達成か
	CreatedAt       time.Time `gorm:"autoCreateTime"                                 json:"created_at"`

	// リレーション
	Rewards []StudyReward `gorm:"foreignKey:SessionID" json:"rewards,omitempty"`
}

func (s *StudySession) BeforeCreate(tx *gorm.DB) error { ensureUUID(&s.ID); return nil }

// ============================================================
// study_rewards — セッション報酬明細
// 1セッションに対し複数行が入る（石, ゴールド, XP, ドロップ等）。
// ボーナス（30分連続 +10石）も別行として記録する。
// ============================================================

type StudyReward struct {
	ID         uuid.UUID  `gorm:"type:uuid;primaryKey" json:"id"`
	SessionID  uuid.UUID  `gorm:"type:uuid;not null;index"                       json:"session_id"`
	RewardType string     `gorm:"type:varchar(30);not null"                      json:"reward_type"` // stones / gold / xp / item_drop
	Amount     int        `gorm:"not null"                                       json:"amount"`      // 獲得量
	ItemID     *uuid.UUID `gorm:"type:uuid"                                      json:"item_id"`     // ドロップアイテム時のマスタID
	CreatedAt  time.Time  `gorm:"autoCreateTime"                                 json:"created_at"`
}

func (r *StudyReward) BeforeCreate(tx *gorm.DB) error { ensureUUID(&r.ID); return nil }

// ============================================================
// m_characters — キャラクターマスタ
// アプリ更新なしで追加可能。is_active で論理削除。
// ============================================================

type MasterCharacter struct {
	ID               uuid.UUID `gorm:"type:uuid;primaryKey" json:"id"`
	Name             string    `gorm:"type:varchar(100);not null"                      json:"name"`
	Rarity           int       `gorm:"not null"                                       json:"rarity"`             // 星1〜5
	BaseHP           int       `gorm:"not null"                                       json:"base_hp"`            // 基本HP
	BaseATK          int       `gorm:"not null"                                       json:"base_atk"`           // 基本攻撃力
	BaseDEF          int       `gorm:"not null"                                       json:"base_def"`           // 基本防御力
	ImageURL         string    `gorm:"type:text;not null"                              json:"image_url"`         // 立ち絵URL
	IdleAnimationURL *string   `gorm:"type:text"                                      json:"idle_animation_url"` // ホーム画面用アニメーション
	IsActive         bool      `gorm:"not null;default:true"                           json:"is_active"`
	CreatedAt        time.Time `gorm:"autoCreateTime"                                  json:"created_at"`
}

// TableName — GORMにテーブル名を明示
func (MasterCharacter) TableName() string                 { return "m_characters" }
func (c *MasterCharacter) BeforeCreate(tx *gorm.DB) error { ensureUUID(&c.ID); return nil }

// ============================================================
// m_weapons — 武器マスタ
// ============================================================

type MasterWeapon struct {
	ID        uuid.UUID `gorm:"type:uuid;primaryKey" json:"id"`
	Name      string    `gorm:"type:varchar(100);not null"                      json:"name"`
	Rarity    int       `gorm:"not null"                                       json:"rarity"`
	BaseATK   int       `gorm:"not null"                                       json:"base_atk"`
	ImageURL  string    `gorm:"type:text;not null"                              json:"image_url"`
	IsActive  bool      `gorm:"not null;default:true"                           json:"is_active"`
	CreatedAt time.Time `gorm:"autoCreateTime"                                  json:"created_at"`
}

func (MasterWeapon) TableName() string                 { return "m_weapons" }
func (w *MasterWeapon) BeforeCreate(tx *gorm.DB) error { ensureUUID(&w.ID); return nil }

// ============================================================
// m_dungeons — ダンジョンマスタ
// ============================================================

type MasterDungeon struct {
	ID              uuid.UUID `gorm:"type:uuid;primaryKey" json:"id"`
	Name            string    `gorm:"type:varchar(100);not null"                      json:"name"`
	SortOrder       int       `gorm:"not null"                                       json:"sort_order"`
	UnlockCondition *string   `gorm:"type:text"                                      json:"unlock_condition"` // JSON or 式
	ImageURL        string    `gorm:"type:text;not null"                              json:"image_url"`
	IsActive        bool      `gorm:"not null;default:true"                           json:"is_active"`
	CreatedAt       time.Time `gorm:"autoCreateTime"                                  json:"created_at"`

	// リレーション
	Stages []MasterDungeonStage `gorm:"foreignKey:DungeonID" json:"stages,omitempty"`
}

func (MasterDungeon) TableName() string                 { return "m_dungeons" }
func (d *MasterDungeon) BeforeCreate(tx *gorm.DB) error { ensureUUID(&d.ID); return nil }

// ============================================================
// m_dungeon_stages — ダンジョンステージマスタ
// 敵構成・ドロップは JSON で柔軟に定義。
// ============================================================

type MasterDungeonStage struct {
	ID               uuid.UUID       `gorm:"type:uuid;primaryKey" json:"id"`
	DungeonID        uuid.UUID       `gorm:"type:uuid;not null;index"                       json:"dungeon_id"`
	StageNumber      int             `gorm:"not null"                                       json:"stage_number"`
	RecommendedPower int             `gorm:"not null"                                       json:"recommended_power"` // 推奨戦力
	EnemyComposition json.RawMessage `gorm:"type:jsonb;not null"                            json:"enemy_composition"` // [{name, hp, atk}]
	DropTable        json.RawMessage `gorm:"type:jsonb;not null"                            json:"drop_table"`        // [{item_id, rate}]
}

func (MasterDungeonStage) TableName() string                 { return "m_dungeon_stages" }
func (s *MasterDungeonStage) BeforeCreate(tx *gorm.DB) error { ensureUUID(&s.ID); return nil }

// ============================================================
// m_gacha_banners — ガチャバナーマスタ
// ============================================================

type MasterGachaBanner struct {
	ID            uuid.UUID       `gorm:"type:uuid;primaryKey" json:"id"`
	Name          string          `gorm:"type:varchar(100);not null"                      json:"name"`
	BannerType    string          `gorm:"type:varchar(30);not null"                       json:"banner_type"` // character / weapon / mixed
	StartAt       time.Time       `gorm:"not null"                                       json:"start_at"`
	EndAt         time.Time       `gorm:"not null"                                       json:"end_at"`
	PityThreshold *int            `gorm:""                                               json:"pity_threshold"` // 天井回数（nullなら天井なし）
	RateTable     json.RawMessage `gorm:"type:jsonb;not null"                            json:"rate_table"`     // [{item_id, rarity, rate}]
	IsActive      bool            `gorm:"not null;default:true"                           json:"is_active"`
}

func (MasterGachaBanner) TableName() string                 { return "m_gacha_banners" }
func (b *MasterGachaBanner) BeforeCreate(tx *gorm.DB) error { ensureUUID(&b.ID); return nil }

// ============================================================
// user_characters — ユーザー所持キャラ
// ガチャで引いたキャラをここに保存。レベルや装備も管理。
// ============================================================

type UserCharacter struct {
	ID               uuid.UUID  `gorm:"type:uuid;primaryKey" json:"id"`
	UserID           uuid.UUID  `gorm:"type:uuid;not null;index"                       json:"user_id"`
	CharacterID      uuid.UUID  `gorm:"type:uuid;not null"                             json:"character_id"` // → m_characters
	Level            int        `gorm:"not null;default:1"                              json:"level"`
	CurrentXP        int        `gorm:"not null;default:0"                              json:"current_xp"`       // 累積経験値
	EquippedWeaponID *uuid.UUID `gorm:"type:uuid"                                     json:"equipped_weapon_id"` // → user_weapons（null = なし）
	ObtainedAt       time.Time  `gorm:"not null"                                       json:"obtained_at"`

	// リレーション（読み取り用）
	Character *MasterCharacter `gorm:"foreignKey:CharacterID"     json:"character,omitempty"`
	Weapon    *UserWeapon      `gorm:"foreignKey:EquippedWeaponID" json:"weapon,omitempty"`
}

func (uc *UserCharacter) BeforeCreate(tx *gorm.DB) error { ensureUUID(&uc.ID); return nil }

// ============================================================
// user_weapons — ユーザー所持武器
// ============================================================

type UserWeapon struct {
	ID         uuid.UUID `gorm:"type:uuid;primaryKey" json:"id"`
	UserID     uuid.UUID `gorm:"type:uuid;not null;index"                       json:"user_id"`
	WeaponID   uuid.UUID `gorm:"type:uuid;not null"                             json:"weapon_id"` // → m_weapons
	Level      int       `gorm:"not null;default:1"                              json:"level"`
	ObtainedAt time.Time `gorm:"not null"                                       json:"obtained_at"`

	// リレーション
	Weapon *MasterWeapon `gorm:"foreignKey:WeaponID" json:"weapon,omitempty"`
}

func (uw *UserWeapon) BeforeCreate(tx *gorm.DB) error { ensureUUID(&uw.ID); return nil }

// ============================================================
// user_party_slots — パーティ編成（スロット1〜4）
// ============================================================

type UserPartySlot struct {
	ID              uuid.UUID `gorm:"type:uuid;primaryKey" json:"id"`
	UserID          uuid.UUID `gorm:"type:uuid;not null;uniqueIndex:idx_user_slot"   json:"user_id"`
	SlotPosition    int       `gorm:"not null;uniqueIndex:idx_user_slot"             json:"slot_position"`     // 1〜4
	UserCharacterID uuid.UUID `gorm:"type:uuid;not null"                             json:"user_character_id"` // → user_characters

	// リレーション
	UserCharacter *UserCharacter `gorm:"foreignKey:UserCharacterID" json:"user_character,omitempty"`
}

func (ps *UserPartySlot) BeforeCreate(tx *gorm.DB) error { ensureUUID(&ps.ID); return nil }

// ============================================================
// user_dungeon_progress — ダンジョン進行状況
// ダンジョンごとに1レコード。勉強中にステージが自動進行する。
// ============================================================

type UserDungeonProgress struct {
	ID              uuid.UUID `gorm:"type:uuid;primaryKey" json:"id"`
	UserID          uuid.UUID `gorm:"type:uuid;not null"                             json:"user_id"`
	DungeonID       uuid.UUID `gorm:"type:uuid;not null"                             json:"dungeon_id"`         // → m_dungeons
	CurrentStage    int       `gorm:"not null;default:1"                              json:"current_stage"`     // 現在挑戦中
	MaxClearedStage int       `gorm:"not null;default:0"                              json:"max_cleared_stage"` // 最高クリア済み
	UpdatedAt       time.Time `gorm:"autoUpdateTime"                                  json:"updated_at"`

	// リレーション
	Dungeon *MasterDungeon `gorm:"foreignKey:DungeonID" json:"dungeon,omitempty"`
}

func (dp *UserDungeonProgress) BeforeCreate(tx *gorm.DB) error { ensureUUID(&dp.ID); return nil }

// ============================================================
// gacha_history — ガチャ履歴
// 天井カウントの改ざん防止のためサーバー管理必須。
// ============================================================

type GachaHistory struct {
	ID           uuid.UUID `gorm:"type:uuid;primaryKey" json:"id"`
	UserID       uuid.UUID `gorm:"type:uuid;not null"                             json:"user_id"`
	BannerID     uuid.UUID `gorm:"type:uuid;not null"                             json:"banner_id"`      // → m_gacha_banners
	ResultType   string    `gorm:"type:varchar(20);not null"                      json:"result_type"`    // character / weapon
	ResultItemID uuid.UUID `gorm:"type:uuid;not null"                             json:"result_item_id"` // 排出されたマスタID
	PityCount    int       `gorm:"not null"                                       json:"pity_count"`     // バナー内累計回数
	CreatedAt    time.Time `gorm:"autoCreateTime"                                 json:"created_at"`
}

func (gh *GachaHistory) BeforeCreate(tx *gorm.DB) error { ensureUUID(&gh.ID); return nil }

// ============================================================
// AllModels — マイグレーション対象の全モデルリスト
// ============================================================

func AllModels() []interface{} {
	return []interface{}{
		&User{},
		&StudySession{},
		&StudyReward{},
		&MasterCharacter{},
		&MasterWeapon{},
		&MasterDungeon{},
		&MasterDungeonStage{},
		&MasterGachaBanner{},
		&UserCharacter{},
		&UserWeapon{},
		&UserPartySlot{},
		&UserDungeonProgress{},
		&GachaHistory{},
	}
}
