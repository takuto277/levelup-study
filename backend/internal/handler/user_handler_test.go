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
)

func TestUpdateUser_SelectedDungeonID(t *testing.T) {
	db := testutil.SetupTestDB(t)
	userRepo := repository.NewUserRepository(db)
	masterRepo := repository.NewMasterRepository(db)
	h := handler.NewUserHandler(userRepo, masterRepo)

	// テスト用ダンジョン
	dungeon := &model.MasterDungeon{
		ID:       uuid.New(),
		Name:     "テストダンジョン",
		ImageURL: "https://example.com/dungeon.png",
	}
	if err := masterRepo.CreateDungeon(dungeon); err != nil {
		t.Fatalf("ダンジョン作成失敗: %v", err)
	}

	inactiveDungeon := &model.MasterDungeon{
		ID:       uuid.New(),
		Name:     "無効ダンジョン",
		ImageURL: "https://example.com/inactive.png",
	}
	if err := masterRepo.CreateDungeon(inactiveDungeon); err != nil {
		t.Fatalf("無効ダンジョン作成失敗: %v", err)
	}
	if err := masterRepo.DeactivateDungeon(inactiveDungeon.ID); err != nil {
		t.Fatalf("ダンジョン無効化失敗: %v", err)
	}

	// テスト用ユーザー
	user := &model.User{DisplayName: "テスト"}
	if err := userRepo.Create(user); err != nil {
		t.Fatalf("ユーザー作成失敗: %v", err)
	}

	tests := []struct {
		name           string
		body           map[string]interface{}
		wantStatus     int
		wantDungeonNil bool
	}{
		{
			name:       "invalid uuid",
			body:       map[string]interface{}{"selected_dungeon_id": "not-a-uuid"},
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "non-existent dungeon",
			body:       map[string]interface{}{"selected_dungeon_id": uuid.New().String()},
			wantStatus: http.StatusBadRequest,
		},
		{
			name:       "inactive dungeon",
			body:       map[string]interface{}{"selected_dungeon_id": inactiveDungeon.ID.String()},
			wantStatus: http.StatusBadRequest,
		},
		{
			name:           "empty string clears selection",
			body:           map[string]interface{}{"selected_dungeon_id": ""},
			wantStatus:     http.StatusOK,
			wantDungeonNil: true,
		},
		{
			name:       "valid dungeon id",
			body:       map[string]interface{}{"selected_dungeon_id": dungeon.ID.String()},
			wantStatus: http.StatusOK,
		},
		{
			name:       "no selected_dungeon_id in body",
			body:       map[string]interface{}{"display_name": "更新"},
			wantStatus: http.StatusOK,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			bodyBytes, _ := json.Marshal(tt.body)
			r := chi.NewRouter()
			r.Put("/users/{userID}", h.UpdateUser)

			req := httptest.NewRequest(http.MethodPut, "/users/"+user.ID.String(), strings.NewReader(string(bodyBytes)))
			req.Header.Set("Content-Type", "application/json")
			rr := httptest.NewRecorder()
			r.ServeHTTP(rr, req)

			if rr.Code != tt.wantStatus {
				t.Errorf("status = %d, want %d", rr.Code, tt.wantStatus)
			}

			if tt.wantStatus == http.StatusOK {
				updated, _ := userRepo.GetByID(user.ID)
				if tt.wantDungeonNil && updated.SelectedDungeonID != nil {
					t.Errorf("SelectedDungeonID should be nil, got %v", *updated.SelectedDungeonID)
				}
			}
		})
	}
}
