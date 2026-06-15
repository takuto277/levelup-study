package handler

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"gorm.io/gorm"
)

type GoalHandler struct {
	db       *gorm.DB
	goalRepo *repository.GoalRepository
	userRepo *repository.UserRepository
}

func NewGoalHandler(db *gorm.DB, goalRepo *repository.GoalRepository, userRepo *repository.UserRepository) *GoalHandler {
	return &GoalHandler{db: db, goalRepo: goalRepo, userRepo: userRepo}
}

func (h *GoalHandler) ListGoals(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	goals, err := h.goalRepo.ListByUser(userID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "目標取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, map[string]interface{}{"goals": goals})
}

func (h *GoalHandler) CreateGoal(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	var req struct {
		GoalType     string     `json:"goal_type"`
		Period       string     `json:"period"`
		TargetValue  int        `json:"target_value"`
		GenreID      *uuid.UUID `json:"genre_id"`
		RewardStones int        `json:"reward_stones"`
		RewardGold   int        `json:"reward_gold"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}
	if req.GoalType == "" || req.Period == "" || req.TargetValue <= 0 {
		respondError(w, http.StatusBadRequest, "goal_type, period, target_value は必須です")
		return
	}
	validTypes := map[string]bool{"pomodoro_count": true, "study_minutes": true, "genre_study_minutes": true}
	if !validTypes[req.GoalType] {
		respondError(w, http.StatusBadRequest, "goal_type が不正です")
		return
	}
	if req.Period != "daily" && req.Period != "weekly" {
		respondError(w, http.StatusBadRequest, "period は daily または weekly です")
		return
	}
	goal := &model.UserGoal{
		UserID:       userID,
		GoalType:     req.GoalType,
		Period:       req.Period,
		TargetValue:  req.TargetValue,
		GenreID:      req.GenreID,
		RewardStones: req.RewardStones,
		RewardGold:   req.RewardGold,
	}
	if err := h.goalRepo.Create(goal); err != nil {
		respondError(w, http.StatusInternalServerError, "目標作成に失敗しました")
		return
	}
	respondJSON(w, http.StatusCreated, goal)
}

func (h *GoalHandler) DeleteGoal(w http.ResponseWriter, r *http.Request) {
	goalID, err := parseUUID(chi.URLParam(r, "goalID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正な目標IDです")
		return
	}
	if err := h.goalRepo.Delete(goalID); err != nil {
		respondError(w, http.StatusInternalServerError, "目標削除に失敗しました")
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *GoalHandler) ClaimGoalReward(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	goalID, err := parseUUID(chi.URLParam(r, "goalID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正な目標IDです")
		return
	}
	goal, err := h.goalRepo.GetByID(goalID)
	if err != nil {
		respondError(w, http.StatusNotFound, "目標が見つかりません")
		return
	}
	if goal.UserID != userID {
		respondError(w, http.StatusForbidden, "アクセス権限がありません")
		return
	}
	if !goal.IsCompleted {
		respondError(w, http.StatusBadRequest, "目標はまだ達成されていません")
		return
	}
	if goal.IsClaimed {
		respondError(w, http.StatusBadRequest, "報酬はすでに受け取り済みです")
		return
	}
	err = h.db.Transaction(func(tx *gorm.DB) error {
		if goal.RewardStones > 0 {
			if err := tx.Model(&model.User{}).Where("id = ?", userID).Update("stones", gorm.Expr("stones + ?", goal.RewardStones)).Error; err != nil {
				return err
			}
		}
		if goal.RewardGold > 0 {
			if err := tx.Model(&model.User{}).Where("id = ?", userID).Update("gold", gorm.Expr("gold + ?", goal.RewardGold)).Error; err != nil {
				return err
			}
		}
		goal.IsClaimed = true
		return tx.Save(goal).Error
	})
	if err != nil {
		respondError(w, http.StatusInternalServerError, "報酬受け取りに失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, goal)
}
