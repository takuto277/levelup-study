package middleware_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/golang-jwt/jwt/v5"
	"github.com/takuto277/levelup-study/backend/internal/middleware"
)

const testJWTSecret = "test-jwt-secret-for-middleware"

func signTestJWT(t *testing.T, sub string) string {
	t.Helper()
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"sub": sub,
		"exp": time.Now().Add(time.Hour).Unix(),
		"iat": time.Now().Unix(),
	})
	s, err := token.SignedString([]byte(testJWTSecret))
	if err != nil {
		t.Fatalf("sign jwt: %v", err)
	}
	return s
}

func TestAPIKeyAuth_ValidKey(t *testing.T) {
	const key = "secret-api-key"
	h := middleware.APIKeyAuth(key)(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/master/genres", nil)
	req.Header.Set("X-API-Key", key)
	rr := httptest.NewRecorder()
	h.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200 body=%q", rr.Code, rr.Body.String())
	}
}

func TestAPIKeyAuth_MissingKey(t *testing.T) {
	h := middleware.APIKeyAuth("secret")(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/master/genres", nil)
	rr := httptest.NewRecorder()
	h.ServeHTTP(rr, req)

	if rr.Code != http.StatusForbidden {
		t.Fatalf("status = %d, want 403", rr.Code)
	}
}

func TestAPIKeyAuth_InvalidKey(t *testing.T) {
	h := middleware.APIKeyAuth("secret")(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/master/genres", nil)
	req.Header.Set("X-API-Key", "wrong")
	rr := httptest.NewRecorder()
	h.ServeHTTP(rr, req)

	if rr.Code != http.StatusForbidden {
		t.Fatalf("status = %d, want 403", rr.Code)
	}
}

func TestJWTAuth_ValidToken(t *testing.T) {
	sub := "00000000-0000-0000-0000-000000000001"
	h := middleware.JWTAuth(testJWTSecret)(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		uid, ok := middleware.UserIDFromContext(r.Context())
		if !ok || uid != sub {
			t.Fatalf("context userID = %q ok=%v", uid, ok)
		}
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/users/"+sub, nil)
	req.Header.Set("Authorization", "Bearer "+signTestJWT(t, sub))
	rr := httptest.NewRecorder()
	h.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200 body=%q", rr.Code, rr.Body.String())
	}
}

func TestJWTAuth_MissingAuthorization(t *testing.T) {
	h := middleware.JWTAuth(testJWTSecret)(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/api/v1/users/x", nil)
	rr := httptest.NewRecorder()
	h.ServeHTTP(rr, req)

	if rr.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, want 401", rr.Code)
	}
}

func TestOwnerGuard_MatchingUserID(t *testing.T) {
	sub := "00000000-0000-0000-0000-000000000001"
	inner := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	r := chi.NewRouter()
	r.Route("/api/v1/users/{userID}", func(r chi.Router) {
		r.Use(func(next http.Handler) http.Handler {
			return http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
				ctx := context.WithValue(req.Context(), middleware.ContextKeyUserID, sub)
				next.ServeHTTP(w, req.WithContext(ctx))
			})
		})
		r.Use(middleware.OwnerGuard)
		r.Get("/", inner)
	})

	req := httptest.NewRequest(http.MethodGet, "/api/v1/users/"+sub, nil)
	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200 body=%q", rr.Code, rr.Body.String())
	}
}

func TestOwnerGuard_MismatchedUserID(t *testing.T) {
	sub := "00000000-0000-0000-0000-000000000001"
	other := "00000000-0000-0000-0000-000000000002"
	inner := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	r := chi.NewRouter()
	r.Route("/api/v1/users/{userID}", func(r chi.Router) {
		r.Use(func(next http.Handler) http.Handler {
			return http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
				ctx := context.WithValue(req.Context(), middleware.ContextKeyUserID, sub)
				next.ServeHTTP(w, req.WithContext(ctx))
			})
		})
		r.Use(middleware.OwnerGuard)
		r.Get("/", inner)
	})

	req := httptest.NewRequest(http.MethodGet, "/api/v1/users/"+other, nil)
	rr := httptest.NewRecorder()
	r.ServeHTTP(rr, req)

	if rr.Code != http.StatusForbidden {
		t.Fatalf("status = %d, want 403", rr.Code)
	}
}
