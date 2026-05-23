package handler

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"github.com/takuto277/levelup-study/backend/internal/service"
)

// ============================================================
// GachaHandler — ガチャ関連の API ハンドラー
// ============================================================

type GachaHandler struct {
	gachaService *service.GachaService
	gachaRepo    *repository.GachaRepository
}

func NewGachaHandler(gachaService *service.GachaService, gachaRepo *repository.GachaRepository) *GachaHandler {
	return &GachaHandler{
		gachaService: gachaService,
		gachaRepo:    gachaRepo,
	}
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

// ListHistory — GET /api/v1/users/{userID}/gacha/history
// ガチャ履歴一覧（新しい順）。query: limit, offset, banner_id
func (h *GachaHandler) ListHistory(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	limit := 20
	if v := r.URL.Query().Get("limit"); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n > 0 {
			limit = n
		}
	}
	if limit > 100 {
		limit = 100
	}

	offset := 0
	if v := r.URL.Query().Get("offset"); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n >= 0 {
			offset = n
		}
	}

	var list interface{}
	if bannerIDStr := r.URL.Query().Get("banner_id"); bannerIDStr != "" {
		bannerID, err := uuid.Parse(bannerIDStr)
		if err != nil {
			respondError(w, http.StatusBadRequest, "不正な banner_id です")
			return
		}
		history, err := h.gachaRepo.ListByBanner(userID, bannerID)
		if err != nil {
			respondError(w, http.StatusInternalServerError, "ガチャ履歴の取得に失敗しました")
			return
		}
		list = history
	} else {
		history, err := h.gachaRepo.ListByUser(userID, limit, offset)
		if err != nil {
			respondError(w, http.StatusInternalServerError, "ガチャ履歴の取得に失敗しました")
			return
		}
		list = history
	}

	respondJSON(w, http.StatusOK, map[string]interface{}{
		"history": list,
		"limit":   limit,
		"offset":  offset,
	})
}
