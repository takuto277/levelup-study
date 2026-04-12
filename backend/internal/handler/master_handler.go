package handler

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/takuto277/levelup-study/backend/internal/model"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"gorm.io/gorm"
)

// ============================================================
// MasterHandler — マスタデータ取得用の API ハンドラー
// クライアントが起動時にマスタデータを取得するためのエンドポイント。
// ============================================================

type MasterHandler struct {
	repo *repository.MasterRepository
}

func NewMasterHandler(repo *repository.MasterRepository) *MasterHandler {
	return &MasterHandler{repo: repo}
}

// ListCharacters — GET /api/v1/master/characters
// 有効なキャラクターマスタ一覧を返す
func (h *MasterHandler) ListCharacters(w http.ResponseWriter, r *http.Request) {
	list, err := h.repo.ListCharacters()
	if err != nil {
		respondError(w, http.StatusInternalServerError, "キャラクターマスタ取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, map[string]interface{}{"characters": list})
}

// ListWeapons — GET /api/v1/master/weapons
// 有効な武器マスタ一覧を返す
func (h *MasterHandler) ListWeapons(w http.ResponseWriter, r *http.Request) {
	list, err := h.repo.ListWeapons()
	if err != nil {
		respondError(w, http.StatusInternalServerError, "武器マスタ取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, map[string]interface{}{"weapons": list})
}

// ListDungeons — GET /api/v1/master/dungeons
// 有効なダンジョンマスタ一覧を返す（ステージ情報含む）
func (h *MasterHandler) ListDungeons(w http.ResponseWriter, r *http.Request) {
	list, err := h.repo.ListDungeons()
	if err != nil {
		respondError(w, http.StatusInternalServerError, "ダンジョンマスタ取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, map[string]interface{}{"dungeons": list})
}

// ListActiveBanners — GET /api/v1/master/gacha/banners
// 現在開催中のガチャバナー一覧を返す（各バナーに featured[] とマスタ解決済みの表示用フィールドを含む）
func (h *MasterHandler) ListActiveBanners(w http.ResponseWriter, r *http.Request) {
	list, err := h.repo.ListActiveBanners()
	if err != nil {
		respondError(w, http.StatusInternalServerError, "バナー取得に失敗しました")
		return
	}
	ids := make([]uuid.UUID, len(list))
	for i := range list {
		ids[i] = list[i].ID
	}
	featuredRows, err := h.repo.ListFeaturedByBannerIDs(ids)
	if err != nil {
		respondError(w, http.StatusInternalServerError, "ピックアップ情報の取得に失敗しました")
		return
	}
	byBanner := make(map[uuid.UUID][]model.MasterGachaBannerFeatured)
	for _, row := range featuredRows {
		byBanner[row.BannerID] = append(byBanner[row.BannerID], row)
	}
	out := make([]map[string]interface{}, 0, len(list))
	for i := range list {
		out = append(out, h.buildGachaBannerJSON(list[i], byBanner[list[i].ID]))
	}
	respondJSON(w, http.StatusOK, map[string]interface{}{"banners": out})
}

func (h *MasterHandler) buildGachaBannerJSON(b model.MasterGachaBanner, feats []model.MasterGachaBannerFeatured) map[string]interface{} {
	m := map[string]interface{}{
		"id":              b.ID.String(),
		"name":            b.Name,
		"banner_type":     b.BannerType,
		"start_at":        b.StartAt,
		"end_at":          b.EndAt,
		"pity_threshold":  b.PityThreshold,
		"rate_table":      json.RawMessage(b.RateTable),
		"is_active":       b.IsActive,
	}
	arr := make([]map[string]interface{}, 0, len(feats))
	for _, f := range feats {
		arr = append(arr, h.enrichGachaFeaturedJSON(f))
	}
	m["featured"] = arr
	return m
}

func (h *MasterHandler) enrichGachaFeaturedJSON(f model.MasterGachaBannerFeatured) map[string]interface{} {
	out := map[string]interface{}{
		"id":        f.ID.String(),
		"banner_id": f.BannerID.String(),
		"item_id":   f.ItemID.String(),
		"item_type": f.ItemType,
		"rate_up":   f.RateUp,
	}
	switch strings.ToLower(f.ItemType) {
	case "character":
		if c, err := h.repo.GetCharacter(f.ItemID); err == nil && c != nil {
			out["item_name"] = c.Name
			out["rarity"] = c.Rarity
			out["image_url"] = c.ImageURL
		}
	case "weapon":
		if w, err := h.repo.GetWeapon(f.ItemID); err == nil && w != nil {
			out["item_name"] = w.Name
			out["rarity"] = w.Rarity
			out["image_url"] = w.ImageURL
		}
	case "costume":
		// 衣装マスタ連携後に item_name 等を埋める
	}
	return out
}

// ListStudyGenres — GET /api/v1/master/genres
// 勉強ジャンルマスタ一覧を返す
func (h *MasterHandler) ListStudyGenres(w http.ResponseWriter, r *http.Request) {
	list, err := h.repo.ListStudyGenres()
	if err != nil {
		respondError(w, http.StatusInternalServerError, "ジャンルマスタ取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, map[string]interface{}{"genres": list})
}

// CreateStudyGenre — POST /api/v1/master/genres
func (h *MasterHandler) CreateStudyGenre(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Slug     string `json:"slug"`
		Label    string `json:"label"`
		Emoji    string `json:"emoji"`
		ColorHex string `json:"color_hex"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.Label == "" || req.Slug == "" {
		respondError(w, http.StatusBadRequest, "slug と label は必須です")
		return
	}
	if req.Emoji == "" {
		req.Emoji = "📖"
	}
	if req.ColorHex == "" {
		req.ColorHex = "#6B7280"
	}

	genre := &model.MasterStudyGenre{
		Slug:      req.Slug,
		Label:     req.Label,
		Emoji:     req.Emoji,
		ColorHex:  req.ColorHex,
		SortOrder: 99,
		IsDefault: false,
		IsActive:  true,
	}
	if err := h.repo.CreateStudyGenre(genre); err != nil {
		respondError(w, http.StatusInternalServerError, "ジャンル作成に失敗しました")
		return
	}
	respondJSON(w, http.StatusCreated, genre)
}

// DeleteStudyGenre — DELETE /api/v1/master/genres/{genreID}
// 論理削除（is_active=false）。アクティブなジャンルが1件だけのときは削除不可。
func (h *MasterHandler) DeleteStudyGenre(w http.ResponseWriter, r *http.Request) {
	id, err := parseUUID(chi.URLParam(r, "genreID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なジャンルIDです")
		return
	}
	g, err := h.repo.GetStudyGenre(id)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			respondError(w, http.StatusNotFound, "ジャンルが見つかりません")
			return
		}
		respondError(w, http.StatusInternalServerError, "ジャンル取得に失敗しました")
		return
	}
	if !g.IsActive {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	activeCount, err := h.repo.CountActiveStudyGenres()
	if err != nil {
		respondError(w, http.StatusInternalServerError, "ジャンル件数の取得に失敗しました")
		return
	}
	if activeCount <= 1 {
		respondError(w, http.StatusBadRequest, "ジャンルは最低1件必要です")
		return
	}
	if err := h.repo.DeactivateStudyGenre(id); err != nil {
		respondError(w, http.StatusInternalServerError, "ジャンル削除に失敗しました")
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// GetDungeon — GET /api/v1/master/dungeons/{dungeonID}
// ダンジョン詳細を返す（ステージ情報含む）
func (h *MasterHandler) GetDungeon(w http.ResponseWriter, r *http.Request) {
	id, err := parseUUID(chi.URLParam(r, "dungeonID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なダンジョンIDです")
		return
	}

	dungeon, err := h.repo.GetDungeon(id)
	if err != nil {
		respondError(w, http.StatusNotFound, "ダンジョンが見つかりません")
		return
	}

	respondJSON(w, http.StatusOK, dungeon)
}
