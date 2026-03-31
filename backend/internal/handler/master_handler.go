package handler

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/takuto277/levelup-study/backend/internal/repository"
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
	respondJSON(w, http.StatusOK, list)
}

// ListWeapons — GET /api/v1/master/weapons
// 有効な武器マスタ一覧を返す
func (h *MasterHandler) ListWeapons(w http.ResponseWriter, r *http.Request) {
	list, err := h.repo.ListWeapons()
	if err != nil {
		respondError(w, http.StatusInternalServerError, "武器マスタ取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, list)
}

// ListDungeons — GET /api/v1/master/dungeons
// 有効なダンジョンマスタ一覧を返す（ステージ情報含む）
func (h *MasterHandler) ListDungeons(w http.ResponseWriter, r *http.Request) {
	list, err := h.repo.ListDungeons()
	if err != nil {
		respondError(w, http.StatusInternalServerError, "ダンジョンマスタ取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, list)
}

// ListActiveBanners — GET /api/v1/master/gacha/banners
// 現在開催中のガチャバナー一覧を返す
func (h *MasterHandler) ListActiveBanners(w http.ResponseWriter, r *http.Request) {
	list, err := h.repo.ListActiveBanners()
	if err != nil {
		respondError(w, http.StatusInternalServerError, "バナー取得に失敗しました")
		return
	}
	respondJSON(w, http.StatusOK, list)
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
