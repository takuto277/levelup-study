package handler

import (
	"encoding/json"
	"net/http"

	"github.com/google/uuid"
)

// ============================================================
// 共通ヘルパー — ハンドラー間で使い回すユーティリティ
// ============================================================

// respondJSON — JSON レスポンスを返す
func respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

// respondError — エラーレスポンスを返す
func respondError(w http.ResponseWriter, status int, message string) {
	respondJSON(w, status, map[string]string{"error": message})
}

// parseUUID — パスパラメータ等から UUID をパースする
func parseUUID(s string) (uuid.UUID, error) {
	return uuid.Parse(s)
}
