package middleware

import (
	"net/http"

	"github.com/go-chi/cors"
)

// ============================================================
// CORS ミドルウェア
//
// モバイルアプリからの直接リクエストでは CORS は本来不要だが、
// 将来の Web ダッシュボードや管理画面を見据えて設定しておく。
// ============================================================

// CORSConfig — 許可オリジンのリストから CORS ミドルウェアを返す
//
//	allowedOrigins: 例 []string{"https://levelup-study.example.com"}
//	空スライスの場合は安全なデフォルト（オリジン拒否）で動作する。
func CORSConfig(allowedOrigins []string) func(http.Handler) http.Handler {
	if len(allowedOrigins) == 0 {
		allowedOrigins = []string{} // 明示的に空 → 全拒否
	}

	return cors.Handler(cors.Options{
		AllowedOrigins:   allowedOrigins,
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type", "X-API-Key"},
		ExposedHeaders:   []string{"X-Request-ID"},
		AllowCredentials: false,
		MaxAge:           3600, // preflight キャッシュ（秒）
	})
}
