package repository

import (
	"time"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"gorm.io/gorm"
)

type GoalRepository struct {
	db *gorm.DB
}

func NewGoalRepository(db *gorm.DB) *GoalRepository {
	return &GoalRepository{db: db}
}

func (r *GoalRepository) Create(goal *model.UserGoal) error {
	return r.db.Create(goal).Error
}

func (r *GoalRepository) ListByUser(userID uuid.UUID) ([]model.UserGoal, error) {
	var goals []model.UserGoal
	err := r.db.Where("user_id = ?", userID).Order("created_at DESC").Find(&goals).Error
	return goals, err
}

func (r *GoalRepository) GetByID(id uuid.UUID) (*model.UserGoal, error) {
	var goal model.UserGoal
	err := r.db.First(&goal, "id = ?", id).Error
	return &goal, err
}

func (r *GoalRepository) Update(goal *model.UserGoal) error {
	return r.db.Save(goal).Error
}

func (r *GoalRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&model.UserGoal{}, "id = ?", id).Error
}

func (r *GoalRepository) ListActiveByUser(userID uuid.UUID) ([]model.UserGoal, error) {
	var goals []model.UserGoal
	err := r.db.Where("user_id = ? AND is_claimed = false", userID).Find(&goals).Error
	return goals, err
}

func (r *GoalRepository) UpdateProgressTx(tx *gorm.DB, goalID uuid.UUID, newValue int, isCompleted bool) error {
	updates := map[string]interface{}{
		"current_value": newValue,
		"is_completed":  isCompleted,
	}
	if isCompleted {
		now := time.Now().UTC()
		updates["completed_at"] = &now
	}
	return tx.Model(&model.UserGoal{}).Where("id = ?", goalID).Updates(updates).Error
}
