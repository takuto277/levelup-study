package middleware

import (
	"net/http"

	"golang.org/x/time/rate"
)

// ============================================================
// Rate Limiter ミドルウェア
//
// グローバル（IP 単位ではなくサーバー全体）のトークンバケット方式。
// 本格的な IP 別制限が必要な場合は Redis ベースに置き換え可。
//
// 用途:
//   - 一般エンドポイント: 緩めに設定
//   - ガチャ等の重要エンドポイント: 専用のリミッターで厳しく制限
// ============================================================

// RateLimiter — 指定レート以上のリクエストを 429 で拒否する
//
//	rps: 1秒あたりの許可リクエスト数
//	burst: バースト許容数（瞬間的に許可する最大数）
func RateLimiter(rps float64, burst int) func(http.Handler) http.Handler {
	limiter := rate.NewLimiter(rate.Limit(rps), burst)

	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if !limiter.Allow() {
				w.Header().Set("Retry-After", "1")
				http.Error(w, `{"error":"リクエストが多すぎます。しばらくしてから再試行してください"}`, http.StatusTooManyRequests)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
