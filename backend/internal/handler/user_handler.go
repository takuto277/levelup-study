package handler

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
)

// ============================================================
// UserHandler — ユーザー関連の API ハンドラー
// ============================================================

type UserHandler struct {
	repo      *repository.UserRepository
	masterRepo *repository.MasterRepository
}

func NewUserHandler(repo *repository.UserRepository, masterRepo *repository.MasterRepository) *UserHandler {
	return &UserHandler{repo: repo, masterRepo: masterRepo}
}

// CreateUser — POST /api/v1/users
// 新規ユーザーを作成する
func (h *UserHandler) CreateUser(w http.ResponseWriter, r *http.Request) {
	var req struct {
		DisplayName string `json:"display_name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}
	if req.DisplayName == "" {
		respondError(w, http.StatusBadRequest, "display_name は必須です")
		return
	}

	user := model.User{DisplayName: req.DisplayName}
	if err := h.repo.Create(&user); err != nil {
		respondError(w, http.StatusInternalServerError, "ユーザー作成に失敗しました")
		return
	}

	respondJSON(w, http.StatusCreated, user)
}

// GetUser — GET /api/v1/users/{userID}
// ユーザー情報を取得する
func (h *UserHandler) GetUser(w http.ResponseWriter, r *http.Request) {
	id, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	user, err := h.repo.GetByID(id)
	if err != nil {
		respondError(w, http.StatusNotFound, "ユーザーが見つかりません")
		return
	}

	respondJSON(w, http.StatusOK, user)
}

// UpdateUser — PUT /api/v1/users/{userID}
// ユーザー情報を更新する
func (h *UserHandler) UpdateUser(w http.ResponseWriter, r *http.Request) {
	id, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	user, err := h.repo.GetByID(id)
	if err != nil {
		respondError(w, http.StatusNotFound, "ユーザーが見つかりません")
		return
	}

	var req struct {
		DisplayName       string  `json:"display_name"`
		SelectedDungeonID *string `json:"selected_dungeon_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}
	if req.DisplayName != "" {
		user.DisplayName = req.DisplayName
	}
	if req.SelectedDungeonID != nil {
		if *req.SelectedDungeonID == "" {
			user.SelectedDungeonID = nil
		} else {
			parsed, err := uuid.Parse(*req.SelectedDungeonID)
			if err != nil {
				respondError(w, http.StatusBadRequest, "不正な selected_dungeon_id です")
				return
			}
			if h.masterRepo != nil {
				d, err := h.masterRepo.GetDungeon(parsed)
				if err != nil {
					respondError(w, http.StatusBadRequest, "指定されたダンジョンが見つかりません")
					return
				}
				if !d.IsActive {
					respondError(w, http.StatusBadRequest, "このダンジョンは現在利用できません")
					return
				}
			}
			user.SelectedDungeonID = &parsed
		}
	}

	if err := h.repo.Update(user); err != nil {
		respondError(w, http.StatusInternalServerError, "ユーザー更新に失敗しました")
		return
	}

	respondJSON(w, http.StatusOK, user)
}

// DeleteUser — DELETE /api/v1/users/{userID}
// ユーザーを削除する（CASCADE で関連データも削除）
func (h *UserHandler) DeleteUser(w http.ResponseWriter, r *http.Request) {
	id, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	if err := h.repo.Delete(id); err != nil {
		respondError(w, http.StatusInternalServerError, "ユーザー削除に失敗しました")
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{"message": "ユーザーを削除しました"})
}

// DebugPatchCurrencies — POST /api/v1/debug/users/{userID}/currencies（DEV_MODE のみルート登録）
// 石・ゴールドを増減する。本番では無効。
func (h *UserHandler) DebugPatchCurrencies(w http.ResponseWriter, r *http.Request) {
	id, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	var req struct {
		StonesDelta int `json:"stones_delta"`
		GoldDelta   int `json:"gold_delta"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}
	if req.StonesDelta == 0 && req.GoldDelta == 0 {
		respondError(w, http.StatusBadRequest, "stones_delta または gold_delta を指定してください")
		return
	}
	user, err := h.repo.ApplyCurrencyDelta(id, req.StonesDelta, req.GoldDelta)
	if err != nil {
		respondError(w, http.StatusNotFound, "ユーザーが見つかりません")
		return
	}
	respondJSON(w, http.StatusOK, user)
}
