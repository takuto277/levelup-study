package handler

import (
	"errors"
	"net/http"

	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"gorm.io/gorm"
)

const (
	maxCharacterLevel = 100
	maxWeaponLevel    = 100
)

func levelUpGoldCost(currentLevel int) int {
	if currentLevel < 1 {
		return 50
	}
	return currentLevel * 50
}

var (
	errInsufficientGold = errors.New("insufficient gold")
	errMaxLevel         = errors.New("max level reached")
)

func (h *GameHandler) ownedCharacter(w http.ResponseWriter, userID, charID uuid.UUID) (*model.UserCharacter, bool) {
	uc, err := h.charRepo.GetByID(charID)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			respondError(w, http.StatusNotFound, "キャラクターが見つかりません")
		} else {
			respondError(w, http.StatusInternalServerError, "キャラクター取得に失敗しました")
		}
		return nil, false
	}
	if uc.UserID != userID {
		respondError(w, http.StatusForbidden, "このキャラクターにアクセスする権限がありません")
		return nil, false
	}
	return uc, true
}

func (h *GameHandler) ownedWeapon(w http.ResponseWriter, userID, weaponID uuid.UUID) (*model.UserWeapon, bool) {
	uw, err := h.weaponRepo.GetByIDForUser(weaponID, userID)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			respondError(w, http.StatusNotFound, "武器が見つかりません")
		} else {
			respondError(w, http.StatusInternalServerError, "武器取得に失敗しました")
		}
		return nil, false
	}
	return uw, true
}

func (h *GameHandler) deductGoldTx(tx *gorm.DB, userID uuid.UUID, amount int) error {
	res := tx.Model(&model.User{}).
		Where("id = ? AND gold >= ?", userID, amount).
		Update("gold", gorm.Expr("gold - ?", amount))
	if res.Error != nil {
		return res.Error
	}
	if res.RowsAffected == 0 {
		return errInsufficientGold
	}
	return nil
}

func mapLevelUpError(w http.ResponseWriter, err error) {
	switch {
	case errors.Is(err, errInsufficientGold):
		respondError(w, http.StatusBadRequest, "ゴールドが不足しています")
	case errors.Is(err, errMaxLevel):
		respondError(w, http.StatusBadRequest, "これ以上レベルアップできません")
	case errors.Is(err, gorm.ErrRecordNotFound):
		respondError(w, http.StatusNotFound, "対象が見つかりません")
	default:
		respondError(w, http.StatusInternalServerError, "レベルアップに失敗しました")
	}
}

// reloadCharacter — トランザクション後に Preload 付きで再取得
func (h *GameHandler) reloadCharacter(id uuid.UUID) (*model.UserCharacter, error) {
	return h.charRepo.GetByID(id)
}

// reloadWeapon — トランザクション後に Preload 付きで再取得
func (h *GameHandler) reloadWeapon(id uuid.UUID) (*model.UserWeapon, error) {
	return h.weaponRepo.GetByID(id)
}

// ensureCharacterOwnedTx — トランザクション内で user_id を検証
func ensureCharacterOwnedTx(tx *gorm.DB, charRepo *repository.CharacterRepository, charID, userID uuid.UUID) (*model.UserCharacter, error) {
	return charRepo.GetByIDForUserTx(tx, charID, userID)
}
