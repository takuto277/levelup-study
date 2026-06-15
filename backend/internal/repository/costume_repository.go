package repository

import (
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/gorm"
)

type CostumeRepository struct {
	db *gorm.DB
}

func NewCostumeRepository(db *gorm.DB) *CostumeRepository {
	return &CostumeRepository{db: db}
}

func (r *CostumeRepository) Create(tx *gorm.DB, uc *model.UserCostume) error {
	return tx.Create(uc).Error
}

func (r *CostumeRepository) ListByUser(userID uuid.UUID) ([]model.UserCostume, error) {
	var list []model.UserCostume
	err := r.db.Preload("Costume").Where("user_id = ?", userID).Find(&list).Error
	return list, err
}

func (r *CostumeRepository) GetByUserAndCostume(userID, costumeID uuid.UUID) (*model.UserCostume, error) {
	var uc model.UserCostume
	err := r.db.Where("user_id = ? AND costume_id = ?", userID, costumeID).First(&uc).Error
	if err != nil {
		return nil, err
	}
	return &uc, nil
}

func (r *CostumeRepository) GetByID(id uuid.UUID) (*model.UserCostume, error) {
	var uc model.UserCostume
	err := r.db.Preload("Costume").First(&uc, "id = ?", id).Error
	if err != nil {
		return nil, err
	}
	return &uc, nil
}
