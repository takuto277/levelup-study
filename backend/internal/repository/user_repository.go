package repository

import (
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

// ============================================================
// UserRepository — ユーザーの CRUD 操作
// ============================================================

type UserRepository struct {
	db *gorm.DB
}

// NewUserRepository — コンストラクタ
func NewUserRepository(db *gorm.DB) *UserRepository {
	return &UserRepository{db: db}
}

// 初期キャラ・武器の固定 UUID（seed.sql のマスタデータと一致）
var (
	InitialCharacterID = uuid.MustParse("a0000000-0000-0000-0000-000000000010") // 見習い戦士タロウ ★3
	InitialWeaponID    = uuid.MustParse("b0000000-0000-0000-0000-000000000009") // 鉄の剣 ★3
)

// UpsertWithInitialData — ユーザー作成 + 初期データ付与を 1 トランザクションで行う。
// ON CONFLICT DO NOTHING により同時リクエスト時も冪等に動作する。
func (r *UserRepository) UpsertWithInitialData(user *model.User) (bool, error) {
	created := false

	err := r.db.Transaction(func(tx *gorm.DB) error {
		result := tx.Clauses(clause.OnConflict{
			Columns:   []clause.Column{{Name: "id"}},
			DoNothing: true,
		}).Create(user)
		if result.Error != nil {
			return result.Error
		}
		if result.RowsAffected == 0 {
			return nil // 既存ユーザー、または並行リクエスト側が作成済み
		}

		created = true
		now := time.Now()

		weaponID := uuid.New()
		if err := tx.Create(&model.UserWeapon{
			ID: weaponID, UserID: user.ID, WeaponID: InitialWeaponID, Level: 1, ObtainedAt: now,
		}).Error; err != nil {
			return err
		}

		charID := uuid.New()
		if err := tx.Create(&model.UserCharacter{
			ID: charID, UserID: user.ID, CharacterID: InitialCharacterID, Level: 1, EquippedWeaponID: &weaponID, ObtainedAt: now,
		}).Error; err != nil {
			return err
		}

		if err := tx.Create(&model.UserPartySlot{
			ID: uuid.New(), UserID: user.ID, SlotPosition: 1, UserCharacterID: charID,
		}).Error; err != nil {
			return err
		}

		return nil
	})

	return created, err
}

// Create — 新規ユーザーを作成する
func (r *UserRepository) Create(user *model.User) error {
	return r.db.Create(user).Error
}

// Upsert — ユーザーを作成する。既に存在する場合は何もしない（冪等）。
func (r *UserRepository) Upsert(user *model.User) error {
	return r.db.FirstOrCreate(user, "id = ?", user.ID).Error
}

// GetByID — IDでユーザーを1件取得する
func (r *UserRepository) GetByID(id uuid.UUID) (*model.User, error) {
	var user model.User
	err := r.db.Where("id = ?", id).First(&user).Error
	if err != nil {
		return nil, err
	}
	return &user, nil
}

// Update — ユーザー情報を更新する
func (r *UserRepository) Update(user *model.User) error {
	return r.db.Save(user).Error
}

// Delete — ユーザーを削除する（CASCADE で関連データも消える）
func (r *UserRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&model.User{}, "id = ?", id).Error
}

// AddStones — 石を加算する（不正防止のためサーバー側で加算）
func (r *UserRepository) AddStones(id uuid.UUID, amount int) error {
	return r.db.Model(&model.User{}).
		Where("id = ?", id).
		Update("stones", gorm.Expr("stones + ?", amount)).Error
}

// AddGold — ゴールドを加算する
func (r *UserRepository) AddGold(id uuid.UUID, amount int) error {
	return r.db.Model(&model.User{}).
		Where("id = ?", id).
		Update("gold", gorm.Expr("gold + ?", amount)).Error
}

// ApplyCurrencyDelta — DEV_MODE 専用: 石・ゴールドを増減（0 未満には丸める）
func (r *UserRepository) ApplyCurrencyDelta(id uuid.UUID, stonesDelta, goldDelta int) (*model.User, error) {
	var u model.User
	if err := r.db.Where("id = ?", id).First(&u).Error; err != nil {
		return nil, err
	}
	stones := u.Stones + stonesDelta
	if stones < 0 {
		stones = 0
	}
	gold := u.Gold + goldDelta
	if gold < 0 {
		gold = 0
	}
	if err := r.db.Model(&model.User{}).Where("id = ?", id).Updates(map[string]interface{}{
		"stones": stones,
		"gold":   gold,
	}).Error; err != nil {
		return nil, err
	}
	return r.GetByID(id)
}

// AddStudySeconds — 累計勉強秒数を加算する
func (r *UserRepository) AddStudySeconds(id uuid.UUID, seconds int) error {
	return r.db.Model(&model.User{}).
		Where("id = ?", id).
		Update("total_study_seconds", gorm.Expr("total_study_seconds + ?", seconds)).Error
}

// IncrementCurrencies — 石・ゴールド・勉強秒数を一括で加算する（トランザクション内で使う）
func (r *UserRepository) IncrementCurrencies(tx *gorm.DB, id uuid.UUID, stones, gold, studySeconds int) error {
	res := tx.Model(&model.User{}).
		Where("id = ?", id).
		Updates(map[string]interface{}{
			"stones":              gorm.Expr("stones + ?", stones),
			"gold":                gorm.Expr("gold + ?", gold),
			"total_study_seconds": gorm.Expr("total_study_seconds + ?", studySeconds),
		})
	if res.Error != nil {
		return res.Error
	}
	if res.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}
