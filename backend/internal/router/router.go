package router

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	chimw "github.com/go-chi/chi/v5/middleware"
	"github.com/takuto277/levelup-study/backend/internal/handler"
	mw "github.com/takuto277/levelup-study/backend/internal/middleware"
)

// ============================================================
// SecurityConfig — セキュリティ関連の設定値
// ============================================================

type SecurityConfig struct {
	JWTSecret      string   // Supabase JWT Secret
	APIKey         string   // クライアント識別用 API Key
	AllowedOrigins []string // CORS 許可オリジン
	DevMode        bool     // true: JWT/APIKey 検証をスキップ（ローカル開発用）
}

// ============================================================
// NewRouter — アプリケーション全体のルーティングを定義する
//
// エンドポイント一覧:
//
// [ユーザー]
//   POST   /api/v1/users                              ユーザー作成
//   GET    /api/v1/users/{userID}                     ユーザー取得
//   PUT    /api/v1/users/{userID}                     ユーザー更新
//   DELETE /api/v1/users/{userID}                     ユーザー削除
//
// [勉強]
//   POST   /api/v1/users/{userID}/study/complete      勉強完了（報酬確定）
//
// [キャラ・武器]
//   GET    /api/v1/users/{userID}/characters           所持キャラ一覧
//   GET    /api/v1/users/{userID}/characters/{id}      所持キャラ詳細
//   PUT    /api/v1/users/{userID}/characters/{id}/equip 武器装備
//   GET    /api/v1/users/{userID}/weapons              所持武器一覧
//
// [パーティ]
//   GET    /api/v1/users/{userID}/party                パーティ取得
//   PUT    /api/v1/users/{userID}/party/{slot}         スロット更新
//   DELETE /api/v1/users/{userID}/party/{slot}         スロット解除
//
// [ダンジョン]
//   GET    /api/v1/users/{userID}/dungeons             進行状況一覧
//
// [ガチャ]
//   POST   /api/v1/users/{userID}/gacha/pull           ガチャ実行
//
// [マスタデータ]
//   GET    /api/v1/master/characters                   キャラマスタ
//   GET    /api/v1/master/weapons                      武器マスタ
//   GET    /api/v1/master/dungeons                     ダンジョンマスタ
//   GET    /api/v1/master/dungeons/{dungeonID}         ダンジョン詳細
//   GET    /api/v1/master/gacha/banners                開催中バナー
//   GET    /api/v1/master/genres                       勉強ジャンルマスタ
// ============================================================

func NewRouter(
	sec SecurityConfig,
	userH *handler.UserHandler,
	studyH *handler.StudyHandler,
	gameH *handler.GameHandler,
	gachaH *handler.GachaHandler,
	masterH *handler.MasterHandler,
) *chi.Mux {
	r := chi.NewRouter()

	// --- グローバルミドルウェア ---
	r.Use(chimw.RequestID)
	r.Use(chimw.RealIP)
	r.Use(chimw.Logger)
	r.Use(chimw.Recoverer)
	r.Use(mw.CORSConfig(sec.AllowedOrigins)) // CORS
	r.Use(mw.RateLimiter(100, 200))          // グローバル: 100 rps / burst 200

	// --- ヘルスチェック（認証不要） ---
	r.Get("/", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("LevelUp Study API is running 🚀"))
	})

	// --- API v1 ---
	r.Route("/api/v1", func(r chi.Router) {
		// API Key を全 API に適用（dev mode ではスキップ）
		if !sec.DevMode {
			r.Use(mw.APIKeyAuth(sec.APIKey))
		}

		// ===== ユーザー作成（JWT なしで呼べる — サインアップ直後） =====
		r.Post("/users", userH.CreateUser)

		// ===== 認証が必要なエンドポイント =====
		r.Group(func(r chi.Router) {
			if !sec.DevMode {
				r.Use(mw.JWTAuth(sec.JWTSecret))
			}

			r.Route("/users/{userID}", func(r chi.Router) {
				// Owner Guard: トークンの sub == {userID} を強制（dev mode ではスキップ）
				if !sec.DevMode {
					r.Use(mw.OwnerGuard)
				}

				r.Get("/", userH.GetUser)
				r.Put("/", userH.UpdateUser)
				r.Delete("/", userH.DeleteUser)

			// 勉強
			r.Post("/study/complete", studyH.CompleteStudy)
			r.Get("/study/sessions", studyH.ListSessions)

				// 所持キャラ
				r.Route("/characters", func(r chi.Router) {
					r.Get("/", gameH.ListCharacters)
					r.Route("/{characterID}", func(r chi.Router) {
						r.Get("/", gameH.GetCharacter)
						r.Put("/equip", gameH.EquipWeapon)
					})
				})

				// 所持武器
				r.Get("/weapons", gameH.ListWeapons)

				// パーティ編成
				r.Route("/party", func(r chi.Router) {
					r.Get("/", gameH.GetParty)
					r.Put("/{slotPosition}", gameH.UpdatePartySlot)
					r.Delete("/{slotPosition}", gameH.RemovePartySlot)
				})

				// ダンジョン進行
				r.Get("/dungeons", gameH.ListDungeonProgress)

				// ガチャ（専用レートリミット: 5 rps / burst 10）
				r.With(mw.RateLimiter(5, 10)).Post("/gacha/pull", gachaH.Pull)
			})
		})

		// ===== マスタデータ（JWT 不要 — API Key のみで公開） =====
		r.Route("/master", func(r chi.Router) {
			r.Get("/characters", masterH.ListCharacters)
			r.Get("/weapons", masterH.ListWeapons)
			r.Get("/dungeons", masterH.ListDungeons)
			r.Get("/dungeons/{dungeonID}", masterH.GetDungeon)
			r.Get("/gacha/banners", masterH.ListActiveBanners)
			r.Get("/genres", masterH.ListStudyGenres)
			r.Post("/genres", masterH.CreateStudyGenre)
		})
	})

	return r
}
