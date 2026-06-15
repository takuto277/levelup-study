package repository

import (
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/gorm"
)

// ============================================================
// CostumeRepository — ユーザー所持衣装の CRUD
// ============================================================

type CostumeRepository struct {
	db *gorm.DB
}

func NewCostumeRepository(db *gorm.DB) *CostumeRepository {
	return &CostumeRepository{db: db}
}

// Create — 衣装をユーザーに付与する（ガチャ結果の保存等）
func (r *CostumeRepository) Create(tx *gorm.DB, uc *model.UserCostume) error {
	return tx.Create(uc).Error
}

// ListByUser — ユーザーの所持衣装一覧を取得する
func (r *CostumeRepository) ListByUser(userID uuid.UUID) ([]model.UserCostume, error) {
	var list []model.UserCostume
	err := r.db.Preload("Costume").Where("user_id = ?", userID).Find(&list).Error
	return list, err
}

// GetByUserAndCostume — ユーザーが特定の衣装を所持しているか確認する
func (r *CostumeRepository) GetByUserAndCostume(userID, costumeID uuid.UUID) (*model.UserCostume, error) {
	var uc model.UserCostume
	err := r.db.Where("user_id = ? AND costume_id = ?", userID, costumeID).First(&uc).Error
	if err != nil {
		return nil, err
	}
	return &uc, nil
}
