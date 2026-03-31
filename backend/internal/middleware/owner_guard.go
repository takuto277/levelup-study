package middleware

import (
	"net/http"

	"github.com/go-chi/chi/v5"
)

// ============================================================
// Owner Guard ミドルウェア
//
// URL パス中の {userID} と JWT トークンの sub を照合し、
// 他人のリソースへのアクセスを防止する。
//
// 例: GET /api/v1/users/{userID}/characters
//   → トークンの sub と {userID} が一致しなければ 403
// ============================================================

// OwnerGuard — パスの {userID} とトークン sub の一致を検証
func OwnerGuard(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// JWT ミドルウェアで格納された認証済みユーザー ID
		tokenUID, ok := UserIDFromContext(r.Context())
		if !ok || tokenUID == "" {
			http.Error(w, `{"error":"認証情報が取得できません"}`, http.StatusUnauthorized)
			return
		}

		// URL パスから {userID} を取得
		pathUID := chi.URLParam(r, "userID")
		if pathUID == "" {
			// {userID} を含まないルートでは何もしない
			next.ServeHTTP(w, r)
			return
		}

		// 一致チェック
		if tokenUID != pathUID {
			http.Error(w, `{"error":"他のユーザーのリソースにはアクセスできません"}`, http.StatusForbidden)
			return
		}

		next.ServeHTTP(w, r)
	})
}
