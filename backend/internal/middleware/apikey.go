package middleware

import (
	"crypto/subtle"
	"net/http"
)

// ============================================================
// API Key ミドルウェア
//
// クライアントアプリからのリクエストであることを簡易確認する。
// ヘッダー X-API-Key に事前共有キーが設定されていなければ 403。
//
// ⚠ JWT 認証が主な認証手段であり、
//   API Key はモバイルアプリの識別と最低限のゲートとして機能する。
//   リバースエンジニアリングで抜かれる可能性があるため、
//   これ単体で信頼しないこと。
// ============================================================

// APIKeyAuth — X-API-Key ヘッダーを検証するミドルウェア
func APIKeyAuth(apiKey string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			provided := r.Header.Get("X-API-Key")
			if provided == "" {
				http.Error(w, `{"error":"X-API-Key ヘッダーが必要です"}`, http.StatusForbidden)
				return
			}

			// タイミング攻撃対策: constant-time compare
			if subtle.ConstantTimeCompare([]byte(provided), []byte(apiKey)) != 1 {
				http.Error(w, `{"error":"無効な API Key です"}`, http.StatusForbidden)
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}
