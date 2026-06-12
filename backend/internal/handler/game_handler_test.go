package handler_test

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/handler"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"github.com/takuto277/levelup-study/backend/internal/testutil"
)

func TestRemovePartySlot_InvalidSlotPosition(t *testing.T) {
	db := testutil.SetupTestDB(t)

	userRepo := repository.NewUserRepository(db)
	charRepo := repository.NewCharacterRepository(db)
	weaponRepo := repository.NewWeaponRepository(db)
	partyRepo := repository.NewPartyRepository(db)
	dungeonRepo := repository.NewDungeonProgressRepository(db)
	h := handler.NewGameHandler(db, userRepo, charRepo, weaponRepo, partyRepo, dungeonRepo)

	userID := uuid.New().String()

	tests := []struct {
		name         string
		slotPosition string
		wantStatus   int
	}{
		{"non-numeric", "abc", http.StatusBadRequest},
		{"zero", "0", http.StatusBadRequest},
		{"negative", "-1", http.StatusBadRequest},
		{"out-of-range-upper", "5", http.StatusBadRequest},
		{"valid", "2", http.StatusOK},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := chi.NewRouter()
			r.Delete("/users/{userID}/party/{slotPosition}", h.RemovePartySlot)

			req := httptest.NewRequest(http.MethodDelete, "/users/"+userID+"/party/"+tt.slotPosition, nil)
			rr := httptest.NewRecorder()
			r.ServeHTTP(rr, req)

			if rr.Code != tt.wantStatus {
				t.Errorf("status = %d, want %d (slotPosition=%q)", rr.Code, tt.wantStatus, tt.slotPosition)
			}
		})
	}
}
