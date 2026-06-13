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
// トークン内の sub (= user ID) と app_metadata.role をコンテキストに格納する。
//
// 検証対象:
//   - Authorization: Bearer *** ヘッダー必須
//   - 署名アルゴリズム: HS256（Supabase のデフォルト）
//   - JWT Secret による署名検証
//   - exp (有効期限) の自動チェック
//
// コンテキストキー:
//   ContextKeyUserID → トークンの sub クレーム（文字列）
//   ContextKeyUserRole → トークンの app_metadata.role（文字列）
// ============================================================

type ctxKey string

const ContextKeyUserID ctxKey = "userID"
const ContextKeyUserRole ctxKey = "userRole"

// JWTAuth — JWT 検証ミドルウェアを返すファクトリ
//
//	jwtSecret: Supabase プロジェクトの JWT Secret
func JWTAuth(jwtSecret string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// --- Authorization ヘッダー取得 ---
			authHeader := r.Header.Get("Authorization")
			if authHeader == "" {
				if DebugAPILogEnabled() {
					APIDebugPrintf("[API] auth Authorization missing %s %s", r.Method, r.URL.Path)
				}
				http.Error(w, `{"error":"Authorization ヘッダーが必要です"}`, http.StatusUnauthorized)
				return
			}

			parts := strings.SplitN(authHeader, " ", 2)
			if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
				if DebugAPILogEnabled() {
					APIDebugPrintf("[API] auth Bearer format invalid %s %s", r.Method, r.URL.Path)
				}
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
				if DebugAPILogEnabled() {
					APIDebugPrintf("[API] auth JWT invalid or parse error %s %s: %v", r.Method, r.URL.Path, err)
				}
				http.Error(w, `{"error":"無効なトークンです"}`, http.StatusUnauthorized)
				return
			}

			// --- claims を取得 ---
			claims, ok := token.Claims.(jwt.MapClaims)
			if !ok {
				if DebugAPILogEnabled() {
					APIDebugPrintf("[API] auth JWT claims type %s %s", r.Method, r.URL.Path)
				}
				http.Error(w, `{"error":"トークンクレームの解析に失敗しました"}`, http.StatusUnauthorized)
				return
			}

			// --- sub クレームを取得 ---
			sub, err := claims.GetSubject()
			if err != nil || sub == "" {
				if DebugAPILogEnabled() {
					APIDebugPrintf("[API] auth JWT sub missing %s %s", r.Method, r.URL.Path)
				}
				http.Error(w, `{"error":"トークンに sub クレームがありません"}`, http.StatusUnauthorized)
				return
			}

			// --- role クレームを取得 (app_metadata.role または直下の role) ---
			role := ""
			if appMeta, ok := claims["app_metadata"].(map[string]interface{}); ok {
				if r, ok := appMeta["role"].(string); ok {
					role = r
				}
			}
			if role == "" {
				if r, ok := claims["role"].(string); ok {
					role = r
				}
			}

			// --- コンテキストに格納して次へ ---
			ctx := context.WithValue(r.Context(), ContextKeyUserID, sub)
			ctx = context.WithValue(ctx, ContextKeyUserRole, role)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

// UserIDFromContext — コンテキストから認証済みユーザー ID を取得する
func UserIDFromContext(ctx context.Context) (string, bool) {
	uid, ok := ctx.Value(ContextKeyUserID).(string)
	return uid, ok
}

// UserRoleFromContext — コンテキストからユーザーロールを取得する
func UserRoleFromContext(ctx context.Context) (string, bool) {
	role, ok := ctx.Value(ContextKeyUserRole).(string)
	return role, ok
}
