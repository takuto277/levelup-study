package handler

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/takuto277/levelup-study/backend/internal/service"
)

// ============================================================
// GachaHandler — ガチャ関連の API ハンドラー
// ============================================================

type GachaHandler struct {
	gachaService *service.GachaService
}

func NewGachaHandler(gachaService *service.GachaService) *GachaHandler {
	return &GachaHandler{gachaService: gachaService}
}

// Pull — POST /api/v1/users/{userID}/gacha/pull
// ガチャを引く（単発 or 10連）
func (h *GachaHandler) Pull(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	var req service.GachaPullRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}

	resp, err := h.gachaService.Pull(userID, req)
	if err != nil {
		respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	respondJSON(w, http.StatusOK, resp)
}
