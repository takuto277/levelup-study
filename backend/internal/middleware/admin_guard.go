package middleware

import (
	"net/http"
)

// ============================================================
// Admin Guard ミドルウェア
//
// JWT トークンの app_metadata.role が "admin" かどうかを検証する。
// JWTAuth ミドルウェアの後に配置すること。
//
// 使い方:
//
//	r.Use(mw.JWTAuth(sec.JWTSecret))
//	r.Use(mw.AdminGuard)
// ============================================================

// AdminGuard — 管理者ロールを検証するミドルウェア
func AdminGuard(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		role, ok := UserRoleFromContext(r.Context())
		if !ok || role == "" {
			if DebugAPILogEnabled() {
				APIDebugPrintf("[API] admin guard: no role in context %s %s", r.Method, r.URL.Path)
			}
			http.Error(w, `{"error":"認証情報が取得できません"}`, http.StatusUnauthorized)
			return
		}

		if role != "admin" {
			if DebugAPILogEnabled() {
				APIDebugPrintf("[API] admin guard: role=%s not admin %s %s", role, r.Method, r.URL.Path)
			}
			http.Error(w, `{"error":"管理者権限が必要です"}`, http.StatusForbidden)
			return
		}

		next.ServeHTTP(w, r)
	})
}
