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
// GameHandler — RPG関連（キャラ・武器・パーティ・ダンジョン）の API ハンドラー
// ============================================================

type GameHandler struct {
	charRepo    *repository.CharacterRepository
	weaponRepo  *repository.WeaponRepository
	partyRepo   *repository.PartyRepository
	dungeonRepo *repository.DungeonProgressRepository
}

func NewGameHandler(
	charRepo *repository.CharacterRepository,
	weaponRepo *repository.WeaponRepository,
	partyRepo *repository.PartyRepository,
	dungeonRepo *repository.DungeonProgressRepository,
) *GameHandler {
	return &GameHandler{
		charRepo:    charRepo,
		weaponRepo:  weaponRepo,
		partyRepo:   partyRepo,
		dungeonRepo: dungeonRepo,
	}
}

// ============================================================
// キャラクター
// ============================================================

// ListCharacters — GET /api/v1/users/{userID}/characters
// ユーザーの所持キャラ一覧を取得する
func (h *GameHandler) ListCharacters(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	list, err := h.charRepo.ListByUser(userID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "キャラクター取得に失敗しました")
		return
	}

	respondJSON(w, http.StatusOK, list)
}

// GetCharacter — GET /api/v1/users/{userID}/characters/{characterID}
// 所持キャラの詳細を取得する
func (h *GameHandler) GetCharacter(w http.ResponseWriter, r *http.Request) {
	charID, err := parseUUID(chi.URLParam(r, "characterID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なキャラクターIDです")
		return
	}

	uc, err := h.charRepo.GetByID(charID)
	if err != nil {
		respondError(w, http.StatusNotFound, "キャラクターが見つかりません")
		return
	}

	respondJSON(w, http.StatusOK, uc)
}

// EquipWeapon — PUT /api/v1/users/{userID}/characters/{characterID}/equip
// キャラクターに武器を装備する
func (h *GameHandler) EquipWeapon(w http.ResponseWriter, r *http.Request) {
	charID, err := parseUUID(chi.URLParam(r, "characterID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なキャラクターIDです")
		return
	}

	var req struct {
		WeaponID *uuid.UUID `json:"weapon_id"` // null で装備解除
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}

	if err := h.charRepo.EquipWeapon(charID, req.WeaponID); err != nil {
		respondError(w, http.StatusInternalServerError, "武器装備に失敗しました")
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{"message": "武器を装備しました"})
}

// ============================================================
// 武器
// ============================================================

// ListWeapons — GET /api/v1/users/{userID}/weapons
// ユーザーの所持武器一覧を取得する
func (h *GameHandler) ListWeapons(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	list, err := h.weaponRepo.ListByUser(userID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "武器取得に失敗しました")
		return
	}

	respondJSON(w, http.StatusOK, list)
}

// ============================================================
// パーティ編成
// ============================================================

// GetParty — GET /api/v1/users/{userID}/party
// 現在のパーティ編成を取得する
func (h *GameHandler) GetParty(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	slots, err := h.partyRepo.GetByUser(userID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "パーティ取得に失敗しました")
		return
	}

	respondJSON(w, http.StatusOK, slots)
}

// UpdatePartySlot — PUT /api/v1/users/{userID}/party/{slotPosition}
// パーティのスロットにキャラを配置する
func (h *GameHandler) UpdatePartySlot(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	slotStr := chi.URLParam(r, "slotPosition")
	var slotPos int
	if _, err := json.Number(slotStr).Int64(); err != nil {
		respondError(w, http.StatusBadRequest, "不正なスロット番号です")
		return
	} else {
		n, _ := json.Number(slotStr).Int64()
		slotPos = int(n)
	}

	if slotPos < 1 || slotPos > 4 {
		respondError(w, http.StatusBadRequest, "スロットは 1〜4 です")
		return
	}

	var req struct {
		UserCharacterID uuid.UUID `json:"user_character_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}

	// Upsert 用の model を作成
	partySlot := &model.UserPartySlot{
		UserID:          userID,
		SlotPosition:    slotPos,
		UserCharacterID: req.UserCharacterID,
	}
	if err := h.partyRepo.Upsert(partySlot); err != nil {
		respondError(w, http.StatusInternalServerError, "パーティ更新に失敗しました")
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{"message": "パーティを更新しました"})
}

// RemovePartySlot — DELETE /api/v1/users/{userID}/party/{slotPosition}
// パーティのスロットからキャラを外す
func (h *GameHandler) RemovePartySlot(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	n, _ := json.Number(chi.URLParam(r, "slotPosition")).Int64()
	slotPos := int(n)

	if err := h.partyRepo.RemoveSlot(userID, slotPos); err != nil {
		respondError(w, http.StatusInternalServerError, "スロット解除に失敗しました")
		return
	}

	respondJSON(w, http.StatusOK, map[string]string{"message": "スロットを解除しました"})
}

// ============================================================
// ダンジョン進行
// ============================================================

// ListDungeonProgress — GET /api/v1/users/{userID}/dungeons
// ユーザーの全ダンジョン進行状況を取得する
func (h *GameHandler) ListDungeonProgress(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	list, err := h.dungeonRepo.ListByUser(userID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "ダンジョン進行取得に失敗しました")
		return
	}

	respondJSON(w, http.StatusOK, list)
}
