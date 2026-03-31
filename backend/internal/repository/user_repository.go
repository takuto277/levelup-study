package repository

import (
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/gorm"
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

// Create — 新規ユーザーを作成する
func (r *UserRepository) Create(user *model.User) error {
	return r.db.Create(user).Error
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

// AddStudySeconds — 累計勉強秒数を加算する
func (r *UserRepository) AddStudySeconds(id uuid.UUID, seconds int) error {
	return r.db.Model(&model.User{}).
		Where("id = ?", id).
		Update("total_study_seconds", gorm.Expr("total_study_seconds + ?", seconds)).Error
}

// IncrementCurrencies — 石・ゴールド・勉強秒数を一括で加算する（トランザクション内で使う）
func (r *UserRepository) IncrementCurrencies(tx *gorm.DB, id uuid.UUID, stones, gold, studySeconds int) error {
	return tx.Model(&model.User{}).
		Where("id = ?", id).
		Updates(map[string]interface{}{
			"stones":              gorm.Expr("stones + ?", stones),
			"gold":                gorm.Expr("gold + ?", gold),
			"total_study_seconds": gorm.Expr("total_study_seconds + ?", studySeconds),
		}).Error
}
