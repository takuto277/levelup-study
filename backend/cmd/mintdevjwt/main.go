// mintdevjwt — ローカル / Render 試験用の HS256 JWT を標準出力に 1 行で出す。
// 署名鍵はサーバーの JWT_SECRET（Supabase の JWT Secret と同一）と一致させること。
//
// 例:
//
//	cd backend && go run ./cmd/mintdevjwt
//	SUB=00000000-0000-0000-0000-000000000001 go run ./cmd/mintdevjwt
package main

import (
	"fmt"
	"os"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/joho/godotenv"
)

func main() {
	_ = godotenv.Load()
	secret := os.Getenv("JWT_SECRET")
	if secret == "" {
		fmt.Fprintln(os.Stderr, "JWT_SECRET が未設定です（backend/.env または環境変数）")
		os.Exit(1)
	}
	sub := os.Getenv("SUB")
	if sub == "" {
		sub = "00000000-0000-0000-0000-000000000001"
	}
	claims := jwt.RegisteredClaims{
		Subject:   sub,
		IssuedAt:  jwt.NewNumericDate(time.Now()),
		ExpiresAt: jwt.NewNumericDate(time.Now().Add(365 * 24 * time.Hour)),
	}
	tok := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	s, err := tok.SignedString([]byte(secret))
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
	fmt.Println(s)
}
