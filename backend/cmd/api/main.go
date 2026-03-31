package main

import (
	"log"
	"net/http"
	"os"
	"strings"

	"github.com/joho/godotenv"
	"github.com/takuto277/levelup-study/backend/internal/database"
	"github.com/takuto277/levelup-study/backend/internal/handler"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"github.com/takuto277/levelup-study/backend/internal/router"
	"github.com/takuto277/levelup-study/backend/internal/service"
)

func main() {
	// --- .env ファイルの読み込み（開発用、なければスキップ） ---
	if err := godotenv.Load(); err != nil {
		log.Println("ℹ️  .env ファイルが見つかりません（本番環境では環境変数を直接設定してください）")
	}

	// --- 環境変数からセキュリティ設定を読み込む ---
	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		log.Fatal("❌ 環境変数 JWT_SECRET が未設定です")
	}
	apiKey := os.Getenv("API_KEY")
	if apiKey == "" {
		log.Fatal("❌ 環境変数 API_KEY が未設定です")
	}
	allowedOrigins := []string{}
	if origins := os.Getenv("ALLOWED_ORIGINS"); origins != "" {
		allowedOrigins = strings.Split(origins, ",")
	}

	// --- データベース接続 ---
	db, err := database.Connect()
	if err != nil {
		log.Fatalf("❌ DB接続失敗: %v", err)
	}

	// --- マイグレーション（開発用） ---
	if err := database.AutoMigrate(db); err != nil {
		log.Fatalf("❌ マイグレーション失敗: %v", err)
	}

	// --- Repository 初期化 ---
	userRepo := repository.NewUserRepository(db)
	studyRepo := repository.NewStudyRepository(db)
	charRepo := repository.NewCharacterRepository(db)
	weaponRepo := repository.NewWeaponRepository(db)
	partyRepo := repository.NewPartyRepository(db)
	dungeonRepo := repository.NewDungeonProgressRepository(db)
	gachaRepo := repository.NewGachaRepository(db)
	masterRepo := repository.NewMasterRepository(db)

	// --- Service 初期化 ---
	studyService := service.NewStudyService(db, userRepo, studyRepo, charRepo, partyRepo, dungeonRepo)
	gachaService := service.NewGachaService(db, userRepo, gachaRepo, masterRepo, charRepo, weaponRepo)

	// --- Handler 初期化 ---
	userH := handler.NewUserHandler(userRepo)
	studyH := handler.NewStudyHandler(studyService)
	gameH := handler.NewGameHandler(charRepo, weaponRepo, partyRepo, dungeonRepo)
	gachaH := handler.NewGachaHandler(gachaService)
	masterH := handler.NewMasterHandler(masterRepo)

	// --- セキュリティ設定 ---
	sec := router.SecurityConfig{
		JWTSecret:      jwtSecret,
		APIKey:         apiKey,
		AllowedOrigins: allowedOrigins,
	}

	// --- ルーター構築 ---
	r := router.NewRouter(sec, userH, studyH, gameH, gachaH, masterH)

	// --- サーバー起動 ---
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	log.Printf("🚀 LevelUp Study API を起動中... http://localhost:%s\n", port)
	if err := http.ListenAndServe(":"+port, r); err != nil {
		log.Fatalf("❌ サーバー起動失敗: %v", err)
	}
}
