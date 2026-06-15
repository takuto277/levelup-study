package handler

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"gorm.io/gorm"
)

// ============================================================
// GameHandler — RPG関連（キャラ・武器・パーティ・ダンジョン）の API ハンドラー
// ============================================================

type GameHandler struct {
	db          *gorm.DB
	userRepo    *repository.UserRepository
	charRepo    *repository.CharacterRepository
	weaponRepo  *repository.WeaponRepository
	partyRepo   *repository.PartyRepository
	dungeonRepo *repository.DungeonProgressRepository
	costumeRepo *repository.CostumeRepository
	masterRepo  *repository.MasterRepository
}

func NewGameHandler(
	db *gorm.DB,
	userRepo *repository.UserRepository,
	charRepo *repository.CharacterRepository,
	weaponRepo *repository.WeaponRepository,
	partyRepo *repository.PartyRepository,
	dungeonRepo *repository.DungeonProgressRepository,
	costumeRepo *repository.CostumeRepository,
	masterRepo *repository.MasterRepository,
) *GameHandler {
	return &GameHandler{
		db:          db,
		userRepo:    userRepo,
		charRepo:    charRepo,
		weaponRepo:  weaponRepo,
		partyRepo:   partyRepo,
		dungeonRepo: dungeonRepo,
		costumeRepo: costumeRepo,
		masterRepo:  masterRepo,
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

	respondJSON(w, http.StatusOK, map[string]interface{}{"characters": list})
}

// GetCharacter — GET /api/v1/users/{userID}/characters/{characterID}
// 所持キャラの詳細を取得する
func (h *GameHandler) GetCharacter(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	charID, err := parseUUID(chi.URLParam(r, "characterID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なキャラクターIDです")
		return
	}

	uc, ok := h.ownedCharacter(w, userID, charID)
	if !ok {
		return
	}

	respondJSON(w, http.StatusOK, uc)
}

// EquipWeapon — PUT /api/v1/users/{userID}/characters/{characterID}/equip
// キャラクターに武器を装備する
func (h *GameHandler) EquipWeapon(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	charID, err := parseUUID(chi.URLParam(r, "characterID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なキャラクターIDです")
		return
	}

	if _, ok := h.ownedCharacter(w, userID, charID); !ok {
		return
	}

	var req struct {
		UserWeaponID *uuid.UUID `json:"user_weapon_id"` // user_weapons.id（null で装備解除）
		WeaponID     *uuid.UUID `json:"weapon_id"`      // 旧クライアント互換
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}

	userWeaponID := req.UserWeaponID
	if userWeaponID == nil {
		userWeaponID = req.WeaponID
	}

	if userWeaponID != nil {
		if _, ok := h.ownedWeapon(w, userID, *userWeaponID); !ok {
			return
		}
	}

	if err := h.charRepo.EquipWeapon(charID, userWeaponID); err != nil {
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

	respondJSON(w, http.StatusOK, map[string]interface{}{"weapons": list})
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

	respondJSON(w, http.StatusOK, map[string]interface{}{"slots": slots})
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

	if _, ok := h.ownedCharacter(w, userID, req.UserCharacterID); !ok {
		return
	}

	existingSlots, _ := h.partyRepo.GetByUser(userID)
	for _, s := range existingSlots {
		if s.SlotPosition != slotPos && s.UserCharacterID == req.UserCharacterID {
			respondError(w, http.StatusBadRequest, "このキャラはすでに別のスロットに編成されています")
			return
		}
	}

	partySlot := &model.UserPartySlot{
		UserID:          userID,
		SlotPosition:    slotPos,
		UserCharacterID: req.UserCharacterID,
	}
	if err := h.partyRepo.Upsert(partySlot); err != nil {
		respondError(w, http.StatusInternalServerError, "パーティ更新に失敗しました")
		return
	}

	slots, err := h.partyRepo.GetByUser(userID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "パーティ取得に失敗しました")
		return
	}
	for _, s := range slots {
		if s.SlotPosition == slotPos {
			respondJSON(w, http.StatusOK, s)
			return
		}
	}
	respondJSON(w, http.StatusOK, partySlot)
}

// RemovePartySlot — DELETE /api/v1/users/{userID}/party/{slotPosition}
// パーティのスロットからキャラを外す
func (h *GameHandler) RemovePartySlot(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	slotStr := chi.URLParam(r, "slotPosition")
	var slotPos int
	n, err := json.Number(slotStr).Int64()
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なスロット番号です")
		return
	}
	slotPos = int(n)

	if slotPos < 1 || slotPos > 4 {
		respondError(w, http.StatusBadRequest, "スロットは 1〜4 です")
		return
	}

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

	respondJSON(w, http.StatusOK, map[string]interface{}{"progress": list})
}

// LevelUpCharacter — POST /api/v1/users/{userID}/characters/{characterID}/level-up
func (h *GameHandler) LevelUpCharacter(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	charID, err := parseUUID(chi.URLParam(r, "characterID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なキャラクターIDです")
		return
	}

	var charIDOut uuid.UUID
	if err := h.db.Transaction(func(tx *gorm.DB) error {
		uc, err := ensureCharacterOwnedTx(tx, h.charRepo, charID, userID)
		if err != nil {
			return err
		}
		if uc.Level >= maxCharacterLevel {
			return errMaxLevel
		}
		cost := levelUpGoldCost(uc.Level)
		if err := h.deductGoldTx(tx, userID, cost); err != nil {
			return err
		}
		if err := h.charRepo.LevelUp(tx, charID, uc.Level+1); err != nil {
			return err
		}
		charIDOut = charID
		return nil
	}); err != nil {
		mapLevelUpError(w, err)
		return
	}

	updated, err := h.reloadCharacter(charIDOut)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "キャラクター取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, updated)
}

// LevelUpWeapon — POST /api/v1/users/{userID}/weapons/{weaponID}/level-up
func (h *GameHandler) LevelUpWeapon(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	weaponID, err := parseUUID(chi.URLParam(r, "weaponID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正な武器IDです")
		return
	}

	if err := h.db.Transaction(func(tx *gorm.DB) error {
		var uw model.UserWeapon
		if err := tx.First(&uw, "id = ? AND user_id = ?", weaponID, userID).Error; err != nil {
			return err
		}
		if uw.Level >= maxWeaponLevel {
			return errMaxLevel
		}
		cost := levelUpGoldCost(uw.Level)
		if err := h.deductGoldTx(tx, userID, cost); err != nil {
			return err
		}
		return h.weaponRepo.LevelUpTx(tx, weaponID, uw.Level+1)
	}); err != nil {
		mapLevelUpError(w, err)
		return
	}

	updated, err := h.reloadWeapon(weaponID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "武器取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, updated)
}

func (h *GameHandler) ListCostumes(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	list, err := h.costumeRepo.ListByUser(userID)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "衣装取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, map[string]interface{}{"costumes": list})
}

func (h *GameHandler) BuyCostume(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	var req struct {
		CostumeID uuid.UUID `json:"costume_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}
	mc, err := h.masterRepo.GetCostume(req.CostumeID)
	if err != nil {
		respondError(w, http.StatusNotFound, "衣装が見つかりません")
		return
	}
	if mc.ShopPriceStones == nil {
		respondError(w, http.StatusBadRequest, "この衣装は購入できません")
		return
	}
	price := *mc.ShopPriceStones
	err = h.db.Transaction(func(tx *gorm.DB) error {
		result := tx.Model(&model.User{}).Where("id = ? AND stones >= ?", userID, price).Update("stones", gorm.Expr("stones - ?", price))
		if result.Error != nil {
			return result.Error
		}
		if result.RowsAffected == 0 {
			return fmt.Errorf("石が足りません")
		}
		uc := &model.UserCostume{UserID: userID, CostumeID: req.CostumeID, ObtainedAt: time.Now().UTC()}
		return h.costumeRepo.Create(tx, uc)
	})
	if err != nil {
		respondError(w, http.StatusBadRequest, err.Error())
		return
	}
	respondJSON(w, http.StatusOK, map[string]string{"message": "衣装を購入しました"})
}

func (h *GameHandler) EquipCostume(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}
	charID, err := parseUUID(chi.URLParam(r, "characterID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なキャラクターIDです")
		return
	}
	if _, ok := h.ownedCharacter(w, userID, charID); !ok {
		return
	}
	var req struct {
		UserCostumeID *uuid.UUID `json:"user_costume_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}
	if req.UserCostumeID != nil {
		existing, err := h.costumeRepo.GetByID(*req.UserCostumeID)
		if err != nil || existing.UserID != userID {
			respondError(w, http.StatusBadRequest, "所持していない衣装です")
			return
		}
	}
	if err := h.charRepo.EquipCostume(charID, req.UserCostumeID); err != nil {
		respondError(w, http.StatusInternalServerError, "衣装装備に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, map[string]string{"message": "衣装を装備しました"})
}
