package middleware

import (
	"context"
	"fmt"
	"net/http"
	"strings"

	"github.com/golang-jwt/jwt/v5"
)

// ============================================================
// JWT 認証ミドルウェア
//
// Supabase Auth が発行する JWT を検証し、
// トークン内の sub (= user ID) をコンテキストに格納する。
//
// 検証対象:
//   - Authorization: Bearer <token> ヘッダー必須
//   - 署名アルゴリズム: HS256（Supabase のデフォルト）
//   - JWT Secret による署名検証
//   - exp (有効期限) の自動チェック
//
// コンテキストキー:
//   ContextKeyUserID → トークンの sub クレーム（文字列）
// ============================================================

type ctxKey string

const ContextKeyUserID ctxKey = "userID"

// JWTAuth — JWT 検証ミドルウェアを返すファクトリ
//
//	jwtSecret: Supabase プロジェクトの JWT Secret
func JWTAuth(jwtSecret string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// --- Authorization ヘッダー取得 ---
			authHeader := r.Header.Get("Authorization")
			if authHeader == "" {
				http.Error(w, `{"error":"Authorization ヘッダーが必要です"}`, http.StatusUnauthorized)
				return
			}

			parts := strings.SplitN(authHeader, " ", 2)
			if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
				http.Error(w, `{"error":"Bearer トークン形式で指定してください"}`, http.StatusUnauthorized)
				return
			}
			tokenString := parts[1]

			// --- トークン検証 ---
			token, err := jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
				if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
					return nil, fmt.Errorf("想定外の署名方式: %v", t.Header["alg"])
				}
				return []byte(jwtSecret), nil
			})
			if err != nil || !token.Valid {
				http.Error(w, `{"error":"無効なトークンです"}`, http.StatusUnauthorized)
				return
			}

			// --- sub クレームを取得 ---
			claims, ok := token.Claims.(jwt.MapClaims)
			if !ok {
				http.Error(w, `{"error":"トークンクレームの解析に失敗しました"}`, http.StatusUnauthorized)
				return
			}
			sub, err := claims.GetSubject()
			if err != nil || sub == "" {
				http.Error(w, `{"error":"トークンに sub クレームがありません"}`, http.StatusUnauthorized)
				return
			}

			// --- コンテキストに格納して次へ ---
			ctx := context.WithValue(r.Context(), ContextKeyUserID, sub)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

// UserIDFromContext — コンテキストから認証済みユーザー ID を取得する
func UserIDFromContext(ctx context.Context) (string, bool) {
	uid, ok := ctx.Value(ContextKeyUserID).(string)
	return uid, ok
}
