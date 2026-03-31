package handler

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
	"github.com/takuto277/levelup-study/backend/internal/service"
)

// ============================================================
// StudyHandler — 勉強セッション関連の API ハンドラー
// ============================================================

type StudyHandler struct {
	studyService *service.StudyService
}

func NewStudyHandler(studyService *service.StudyService) *StudyHandler {
	return &StudyHandler{studyService: studyService}
}

// CompleteStudy — POST /api/v1/users/{userID}/study/complete
// 勉強セッションを完了し、報酬を確定する
func (h *StudyHandler) CompleteStudy(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	var req service.CompleteStudyRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondError(w, http.StatusBadRequest, "リクエストの形式が不正です")
		return
	}

	resp, err := h.studyService.CompleteStudy(userID, req)
	if err != nil {
		respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	respondJSON(w, http.StatusOK, resp)
}

// ListSessions — GET /api/v1/users/{userID}/study/sessions?limit=20&offset=0
// ユーザーの勉強セッション一覧を取得する
func (h *StudyHandler) ListSessions(w http.ResponseWriter, r *http.Request) {
	userID, err := parseUUID(chi.URLParam(r, "userID"))
	if err != nil {
		respondError(w, http.StatusBadRequest, "不正なユーザーIDです")
		return
	}

	limit := 20
	offset := 0
	if l := r.URL.Query().Get("limit"); l != "" {
		if v, err := strconv.Atoi(l); err == nil && v > 0 {
			limit = v
		}
	}
	if o := r.URL.Query().Get("offset"); o != "" {
		if v, err := strconv.Atoi(o); err == nil && v >= 0 {
			offset = v
		}
	}

	// StudyService 内の studyRepo を直接使いたいが、Handler→Service の設計を守るため
	// ここでは簡易的にServiceを経由せずrepositoryを呼ぶパターンも許容する。
	// 今回は ListSessions はビジネスロジックが不要なので直接返す。
	_ = limit
	_ = offset
	_ = userID
	// TODO: service にリスト取得メソッドを追加するか、repository を直接注入する
	respondJSON(w, http.StatusOK, map[string]string{"message": "実装予定"})
}
