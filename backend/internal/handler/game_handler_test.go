package handler_test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/handler"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"github.com/takuto277/levelup-study/backend/internal/testutil"
	"gorm.io/gorm"
)

func createTestUserWithChar(t *testing.T, db *gorm.DB, userRepo *repository.UserRepository, charRepo *repository.CharacterRepository) (uuid.UUID, uuid.UUID) {
	t.Helper()
	user := &model.User{DisplayName: "テスト"}
	if err := userRepo.Create(user); err != nil {
		t.Fatalf("ユーザー作成失敗: %v", err)
	}
	masterChar := &model.MasterCharacter{
		ID:       uuid.New(),
		Name:     "テストキャラ",
		Rarity:   5,
		ImageURL: "https://example.com/char.png",
	}
	if err := db.Create(masterChar).Error; err != nil {
		t.Fatalf("マスターキャラ作成失敗: %v", err)
	}
	userChar := &model.UserCharacter{
		UserID:      user.ID,
		CharacterID: masterChar.ID,
		Level:       1,
	}
	if err := charRepo.Create(db, userChar); err != nil {
		t.Fatalf("所持キャラ作成失敗: %v", err)
	}
	return user.ID, userChar.ID
}

func TestUpdatePartySlot_DuplicateCharacter(t *testing.T) {
	db := testutil.SetupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	charRepo := repository.NewCharacterRepository(db)
	weaponRepo := repository.NewWeaponRepository(db)
	partyRepo := repository.NewPartyRepository(db)
	dungeonRepo := repository.NewDungeonProgressRepository(db)
	h := handler.NewGameHandler(db, userRepo, charRepo, weaponRepo, partyRepo, dungeonRepo, nil, nil)

	userID, userCharID := createTestUserWithChar(t, db, userRepo, charRepo)

	body := func(charID string) string {
		b, _ := json.Marshal(map[string]string{"user_character_id": charID})
		return string(b)
	}

	// 1st put: slot 1 with this character → should succeed
	r := chi.NewRouter()
	r.Put("/users/{userID}/party/{slotPosition}", h.UpdatePartySlot)
	req := httptest.NewRequest(http.MethodPut, "/users/"+userID.String()+"/party/1", strings.NewReader(body(userCharID.String())))
	req.Header.Set("Content-Type", "application/json")
	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)
	if rr.Code != http.StatusOK {
		t.Fatalf("1st put: status = %d, want 200", rr.Code)
	}

	// 2nd put: same character in slot 2 → should fail with 400
	req2 := httptest.NewRequest(http.MethodPut, "/users/"+userID.String()+"/party/2", strings.NewReader(body(userCharID.String())))
	req2.Header.Set("Content-Type", "application/json")
	rr2 := httptest.NewRecorder()
	r.ServeHTTP(rr2, req2)
	if rr2.Code != http.StatusBadRequest {
		t.Errorf("duplicate put: status = %d, want 400", rr2.Code)
	}
}
