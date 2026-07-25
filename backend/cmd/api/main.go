package main

import (
	"log"
	"net/http"
	"os"
	"regexp"
	"strings"

	"github.com/joho/godotenv"
	"github.com/takuto277/levelup-study/backend/internal/database"
	"github.com/takuto277/levelup-study/backend/internal/handler"
	mw "github.com/takuto277/levelup-study/backend/internal/middleware"
	"github.com/takuto277/levelup-study/backend/internal/repository"
	"github.com/takuto277/levelup-study/backend/internal/router"
	"github.com/takuto277/levelup-study/backend/internal/service"
)

func main() {
	// --- .env ファイルの読み込み（開発用、なければスキップ） ---
	if err := godotenv.Load(); err != nil {
		log.Println("ℹ️  .env ファイルが見つかりません（本番環境では環境変数を直接設定してください）")
	}
	mw.InitDebugAPILogOutput()

	// --- 開発モード判定 ---
	devMode := os.Getenv("DEV_MODE") == "true"
	if devMode {
		if os.Getenv("RENDER") == "true" {
			log.Fatal("❌ DEV_MODE=true は本番環境では許可されていません。DEPLOYMENT_SAFETY_GATE")
		}
		log.Println("⚠️  DEV_MODE が有効です — JWT / API Key 認証をスキップします")
		log.Println("🛠 DEBUG: POST /api/v1/debug/users/{userID}/currencies（stones_delta / gold_delta）が利用可能です")
	}

	// --- 環境変数からセキュリティ設定を読み込む ---
	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" && !devMode {
		log.Fatal("❌ 環境変数 JWT_SECRET が未設定です")
	}
	apiKey := os.Getenv("API_KEY")
	if apiKey == "" && !devMode {
		log.Fatal("❌ 環境変数 API_KEY が未設定です")
	}
	allowedOrigins := []string{}
	if origins := os.Getenv("ALLOWED_ORIGINS"); origins != "" {
		allowedOrigins = strings.Split(origins, ",")
	}

	debugAPILog := mw.DebugAPILogEnabled()
	if debugAPILog {
		log.Println("🌱 DEBUG_API_LOG: 各リクエスト終了時に 🌱 1 行を stderr に出します（GET / や 404 も含む）。オフは DEBUG_API_LOG=false。ファイルは DEBUG_API_LOG_FILE")
		if mw.DebugAPILogVerboseEnabled() {
			log.Println("🌱 DEBUG_API_LOG_VERBOSE: 各リクエストの続けて req/resp 要約行を出します（機密に注意。オフは DEBUG_API_LOG_VERBOSE=false）")
		}
	}

	// --- Supabase JWKS の初期化（ES256/RS256 対応）---
	supabaseRef := extractSupabaseProjectRef(os.Getenv("SUPABASE_URL"), os.Getenv("DATABASE_URL"))
	if supabaseRef != "" {
		mw.InitJWKS(supabaseRef)
		log.Printf("🔑 Supabase JWKS: %s.supabase.co", supabaseRef)
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
	costumeRepo := repository.NewCostumeRepository(db)
	partyRepo := repository.NewPartyRepository(db)
	dungeonRepo := repository.NewDungeonProgressRepository(db)
	gachaRepo := repository.NewGachaRepository(db)
	masterRepo := repository.NewMasterRepository(db)
	goalRepo := repository.NewGoalRepository(db)

	// --- Service 初期化 ---
	studyService := service.NewStudyService(db, userRepo, studyRepo, charRepo, partyRepo, dungeonRepo, goalRepo)
	gachaService := service.NewGachaService(db, userRepo, gachaRepo, masterRepo, charRepo, weaponRepo, costumeRepo)

	// --- Handler 初期化 ---
	userH := handler.NewUserHandler(userRepo, masterRepo)
	studyH := handler.NewStudyHandler(studyService)
	gameH := handler.NewGameHandler(db, userRepo, charRepo, weaponRepo, partyRepo, dungeonRepo, costumeRepo, masterRepo)
	gachaH := handler.NewGachaHandler(gachaService, gachaRepo, costumeRepo)
	masterH := handler.NewMasterHandler(masterRepo)
	goalH := handler.NewGoalHandler(db, goalRepo, userRepo)

	// --- セキュリティ設定 ---
	sec := router.SecurityConfig{
		JWTSecret:      jwtSecret,
		APIKey:         apiKey,
		AllowedOrigins: allowedOrigins,
		DevMode:        devMode,
		DebugAPILog:    debugAPILog,
	}

	// --- ルーター構築 ---
	r := router.NewRouter(sec, userH, studyH, gameH, gachaH, masterH, goalH)

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

// extractSupabaseProjectRef — SUPABASE_URL または DATABASE_URL からプロジェクト参照を抽出
func extractSupabaseProjectRef(supabaseURL, databaseURL string) string {
	if supabaseURL != "" {
		re := regexp.MustCompile(`https://([a-z0-9]+)\.supabase\.co`)
		if m := re.FindStringSubmatch(supabaseURL); len(m) >= 2 {
			return m[1]
		}
	}
	if databaseURL != "" {
		re := regexp.MustCompile(`postgres\.([a-z0-9]+)[^@]*@`)
		if m := re.FindStringSubmatch(databaseURL); len(m) >= 2 {
			return m[1]
		}
	}
	return ""
}
