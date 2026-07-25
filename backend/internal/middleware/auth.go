package middleware

import (
	"context"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rsa"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"math/big"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// ============================================================
// JWT 認証ミドルウェア
//
// HS256 (JWT_SECRET) と ES256/RS256 (Supabase JWKS) の両方に対応。
// ============================================================

type ctxKey string

const ContextKeyUserID ctxKey = "userID"

// jwksKey — JWKS の公開鍵キャッシュ用
type jwksKey struct {
	Kid string `json:"kid"`
	Kty string `json:"kty"`
	Alg string `json:"alg"`
	Use string `json:"use"`
	N   string `json:"n"`
	E   string `json:"e"`
	X   string `json:"x"`
	Y   string `json:"y"`
	Crv string `json:"crv"`
}

var (
	jwksCache     map[string]interface{}
	jwksMu        sync.RWMutex
	jwksFetchedAt time.Time
	jwksURL       string
)

// InitJWKS — 起動時に JWKS エンドポイントを設定する
func InitJWKS(supabaseProjectRef string) {
	if supabaseProjectRef == "" {
		return
	}
	jwksURL = fmt.Sprintf("https://%s.supabase.co/auth/v1/.well-known/jwks.json", supabaseProjectRef)
}

func fetchJWKS() (map[string]interface{}, error) {
	jwksMu.RLock()
	if jwksCache != nil && time.Since(jwksFetchedAt) < 5*time.Minute {
		c := jwksCache
		jwksMu.RUnlock()
		return c, nil
	}
	jwksMu.RUnlock()

	jwksMu.Lock()
	defer jwksMu.Unlock()

	if jwksCache != nil && time.Since(jwksFetchedAt) < 5*time.Minute {
		return jwksCache, nil
	}

	if jwksURL == "" {
		return nil, fmt.Errorf("JWKS URL not configured")
	}

	resp, err := http.Get(jwksURL)
	if err != nil {
		return nil, fmt.Errorf("JWKS fetch failed: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("JWKS read failed: %w", err)
	}

	var result struct {
		Keys []jwksKey `json:"keys"`
	}
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, fmt.Errorf("JWKS parse failed: %w", err)
	}

	keys := map[string]interface{}{}
	for _, k := range result.Keys {
		switch k.Kty {
		case "EC":
			x := new(big.Int)
			y := new(big.Int)
			x.SetString(k.X, 0)
			y.SetString(k.Y, 0)
			var curve elliptic.Curve
			switch k.Crv {
			case "P-256":
				curve = elliptic.P256()
			case "P-384":
				curve = elliptic.P384()
			case "P-521":
				curve = elliptic.P521()
			default:
				continue
			}
			keys[k.Kid] = &ecdsa.PublicKey{
				Curve: curve,
				X:     x,
				Y:     y,
			}
		case "RSA":
			n := new(big.Int)
			n.SetString(k.N, 0)
			eBytes, _ := base64.RawURLEncoding.DecodeString(k.E)
			e := 0
			for _, b := range eBytes {
				e = e<<8 | int(b)
			}
			keys[k.Kid] = &rsa.PublicKey{
				N: n,
				E: e,
			}
		}
	}

	jwksCache = keys
	jwksFetchedAt = time.Now()
	log.Printf("[auth] JWKS fetched: %d keys from %s", len(keys), jwksURL)
	return keys, nil
}

// JWTAuth — JWT 検証ミドルウェア（HS256 + ES256/RS256 JWKS 対応）
func JWTAuth(jwtSecret string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
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

			// 署名方式に応じたキー選択
			token, err := jwt.Parse(tokenString, func(t *jwt.Token) (interface{}, error) {
				switch t.Method.Alg() {
				case "HS256", "HS384", "HS512":
					if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
						return nil, fmt.Errorf("想定外の署名方式: %v", t.Header["alg"])
					}
					return []byte(jwtSecret), nil
			case "ES256", "ES384", "ES512", "RS256", "RS384", "RS512":
				kid, _ := t.Header["kid"].(string)
				keys, err := fetchJWKS()
				if err != nil {
					return nil, fmt.Errorf("JWKS取得エラー: %w", err)
				}
				key, ok := keys[kid]
				if !ok {
					var availableKids []string
					for k := range keys {
						availableKids = append(availableKids, k)
					}
					return nil, fmt.Errorf("JWKS にキー %s が見つかりません（利用可能: %v）", kid, availableKids)
				}
				return key, nil
				default:
					return nil, fmt.Errorf("未対応の署名方式: %v", t.Header["alg"])
				}
			})
			if err != nil || !token.Valid {
				errMsg := "不明なエラー"
				if err != nil {
					errMsg = err.Error()
				} else if !token.Valid {
					errMsg = "トークンが無効です"
				}
				log.Printf("[auth] JWT検証失敗: %s", errMsg)
				http.Error(w, fmt.Sprintf(`{"error":"無効なトークンです","detail":"%s"}`, errMsg), http.StatusUnauthorized)
				return
			}

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

			ctx := context.WithValue(r.Context(), ContextKeyUserID, sub)
			ctx = context.WithValue(ctx, JWTClaimsKey{}, claims)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

// UserIDFromContext — コンテキストから認証済みユーザー ID を取得する
func UserIDFromContext(ctx context.Context) (string, bool) {
	uid, ok := ctx.Value(ContextKeyUserID).(string)
	return uid, ok
}

type JWTClaimsKey struct{}

// RequireAdminRole — JWT に role: admin クレームがあることを要求
func RequireAdminRole(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		claims, ok := r.Context().Value(JWTClaimsKey{}).(jwt.MapClaims)
		if !ok {
			http.Error(w, `{"error":"管理者権限が必要です"}`, http.StatusForbidden)
			return
		}
		role, _ := claims["role"].(string)
		if role != "admin" {
			http.Error(w, `{"error":"管理者権限が必要です"}`, http.StatusForbidden)
			return
		}
		next.ServeHTTP(w, r)
	})
}
