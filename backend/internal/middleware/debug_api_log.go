package middleware

import (
	"bytes"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"regexp"
	"strings"
	"time"

	chimw "github.com/go-chi/chi/v5/middleware"
)

// DebugAPIPrefix — API デバッグログ行の先頭に付ける統一記号（grep しやすい）
const DebugAPIPrefix = "🌱 "

const (
	debugLogBodySoftMax = 4096  // ログ1行あたりの本文上限（超えたら省略表示）
	debugLogBodyHardMax = 256 * 1024
)

var (
	bearerRE            = regexp.MustCompile(`(?i)Bearer\s+[A-Za-z0-9\-_\.~+/]+=*`)
	jsonAccessTokenRE   = regexp.MustCompile(`"(access_token|refresh_token|id_token)"\s*:\s*"[^"]*"`)
	jsonPasswordFieldRE = regexp.MustCompile(`"(password|token)"\s*:\s*"[^"]*"`)
)

// apiDebugLog は標準エラーへ出す（ターミナルで make run したとき必ず見える）。
// InitDebugAPILogOutput で DEBUG_API_LOG_FILE が指定されていればファイルにも同じ内容を追記する。
var apiDebugLog = log.New(os.Stderr, "", log.LstdFlags|log.Lmicroseconds)

// InitDebugAPILogOutput — godotenv.Load の直後に呼ぶこと。
// DEBUG_API_LOG_FILE があれば、そのファイルにも 🌱 ログを追記する。
func InitDebugAPILogOutput() {
	path := strings.TrimSpace(os.Getenv("DEBUG_API_LOG_FILE"))
	if path == "" {
		return
	}
	f, err := os.OpenFile(path, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		log.Printf("DEBUG_API_LOG_FILE を開けません %s: %v", path, err)
		return
	}
	apiDebugLog = log.New(io.MultiWriter(os.Stderr, f), "", log.LstdFlags|log.Lmicroseconds)
	log.Printf("DEBUG_API_LOG_FILE: %s にも追記します（ターミナルには従来どおり stderr に出力）", path)
}

// DebugAPILogEnabled — DEBUG_API_LOG が false/0/off/no のときだけオフ。未設定はオン（ローカルで何も出ない問題を避ける）。
// 本番では DEBUG_API_LOG=false を推奨。
func DebugAPILogEnabled() bool {
	v := strings.ToLower(strings.TrimSpace(os.Getenv("DEBUG_API_LOG")))
	switch v {
	case "0", "false", "no", "off":
		return false
	default:
		return true
	}
}

// DebugAPILogVerboseEnabled — true のとき、リクエスト／レスポンス本文の要約を 🌱 で追加出力（ローカル・AI 調査用）。
// 本番では絶対にオンにしないこと（機密が残る可能性あり）。DEBUG_API_LOG がオフのときは意味がない。
func DebugAPILogVerboseEnabled() bool {
	v := strings.ToLower(strings.TrimSpace(os.Getenv("DEBUG_API_LOG_VERBOSE")))
	switch v {
	case "1", "true", "yes", "on":
		return true
	default:
		return false
	}
}

// APIDebugPrintf — 🌱 接頭辞付きで apiDebugLog に書く（stderr ＋ 任意のファイル）。
func APIDebugPrintf(format string, args ...any) {
	apiDebugLog.Printf(DebugAPIPrefix+format, args...)
}

// redactSecrets — ログ用に Bearer や JSON 内のトークン風フィールドをマスクする。
func redactSecrets(s string) string {
	s = bearerRE.ReplaceAllString(s, `Bearer <redacted>`)
	s = jsonAccessTokenRE.ReplaceAllString(s, `"$1":"<redacted>"`)
	s = jsonPasswordFieldRE.ReplaceAllString(s, `"$1":"<redacted>"`)
	return s
}

func compactOneLine(s string) string {
	s = strings.ReplaceAll(s, "\r", " ")
	s = strings.ReplaceAll(s, "\n", " ")
	return strings.Join(strings.Fields(s), " ")
}

func truncateDisplay(s string, softMax int) string {
	s = compactOneLine(s)
	if len(s) <= softMax {
		return s
	}
	return s[:softMax] + fmt.Sprintf("… (%d chars)", len(s))
}

func readRequestBodyForLog(r *http.Request) string {
	if r.Body == nil {
		return ""
	}
	switch r.Method {
	case http.MethodGet, http.MethodHead, http.MethodOptions, http.MethodTrace:
		return ""
	}
	cl := r.ContentLength
	if cl < 0 {
		return "(req body: length unknown — not logged)"
	}
	if cl == 0 {
		return ""
	}
	if cl > debugLogBodyHardMax {
		return fmt.Sprintf("(req body too large: %d bytes)", cl)
	}
	data, err := io.ReadAll(r.Body)
	_ = r.Body.Close()
	if err != nil {
		return "(req body read error)"
	}
	r.Body = io.NopCloser(bytes.NewReader(data))
	raw := string(data)
	return truncateDisplay(redactSecrets(raw), debugLogBodySoftMax)
}

// respCapture — レスポンス先頭のみ保持（JSON 想定）。
type respCapture struct {
	chimw.WrapResponseWriter
	buf   []byte
	limit int
}

func (rc *respCapture) Write(b []byte) (int, error) {
	if len(rc.buf) < rc.limit {
		space := rc.limit - len(rc.buf)
		if space > len(b) {
			rc.buf = append(rc.buf, b...)
		} else if space > 0 {
			rc.buf = append(rc.buf, b[:space]...)
		}
	}
	return rc.WrapResponseWriter.Write(b)
}

func (rc *respCapture) ReadFrom(src io.Reader) (int64, error) {
	if rf, ok := rc.WrapResponseWriter.(io.ReaderFrom); ok {
		// 本体は下流へ。先頭バイトだけ手で読むのは難しいので、ReaderFrom 経路では buf を諦める。
		return rf.ReadFrom(src)
	}
	buf := make([]byte, 32*1024)
	var n int64
	for {
		nr, er := src.Read(buf)
		if nr > 0 {
			rc.Write(buf[:nr])
			n += int64(nr)
		}
		if er != nil {
			if er != io.EOF {
				return n, er
			}
			break
		}
	}
	return n, nil
}

func (rc *respCapture) Flush() {
	if f, ok := rc.WrapResponseWriter.(http.Flusher); ok {
		f.Flush()
	}
}

// DebugAPILog — 1 リクエストごとに終了時ステータスと所要時間を 🌱 でログする（全パス対象）。
// chi の RequestID が付いていれば req_id も出す。
// DEBUG_API_LOG_VERBOSE=true のときは続けて req/resp 本文の要約行を出す（stderr ＝ make run 中のターミナル）。
func DebugAPILog(next http.Handler) http.Handler {
	verbose := DebugAPILogVerboseEnabled()
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		base := chimw.NewWrapResponseWriter(w, r.ProtoMajor)
		var ww chimw.WrapResponseWriter = base
		var rc *respCapture
		if verbose {
			rc = &respCapture{WrapResponseWriter: base, limit: debugLogBodySoftMax}
			ww = rc
		}

		var reqDump string
		if verbose {
			reqDump = readRequestBodyForLog(r)
		}

		next.ServeHTTP(ww, r)

		status := ww.Status()
		if status == 0 {
			status = http.StatusOK
		}
		path := r.URL.Path
		if q := r.URL.RawQuery; q != "" {
			path += "?" + q
		}
		elapsed := time.Since(start).Round(time.Millisecond)
		reqID := chimw.GetReqID(r.Context())
		if reqID == "" {
			reqID = "-"
		}

		tag := "OK "
		if status >= http.StatusBadRequest {
			tag = "FAIL"
		}
		APIDebugPrintf("[API] %s %s %s -> %d (%s) req_id=%s",
			tag, r.Method, path, status, elapsed, reqID)

		if !verbose {
			return
		}
		ua := r.UserAgent()
		if len(ua) > 200 {
			ua = ua[:200] + "…"
		}
		respDump := ""
		if rc != nil && len(rc.buf) > 0 {
			respDump = truncateDisplay(redactSecrets(string(rc.buf)), debugLogBodySoftMax)
		}
		if respDump == "" {
			respDump = "(empty or non-captured)"
		}
		APIDebugPrintf("[API] dump req_id=%s ua=%q req=%q resp=%q", reqID, ua, reqDump, respDump)
	})
}
