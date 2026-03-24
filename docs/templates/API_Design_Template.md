# 連携API 仕様書フォーマット (API Design Template)

このフォーマットは、Goバックエンドとモバイル向けKMP（フロントエンド）間の通信インターフェイス（API）を定義・設計するためのものです。
新しいAPIエンドポイントを作成する際は、この構成に沿って仕様を定義してください。

---

## 1. エンドポイント概要
- **API名:** [例: 学習セッション登録 API]
- **パス (Path):** `POST /api/v1/study-sessions`
- **目的 (Purpose):**
  - アプリ（KMP）側から、終了したポモドーロセッションの記録を送信し、ガチャポイントを受け取る。
- **認証 (Auth Required):** [例: 必須 (Bearer Token)]

## 2. リクエスト仕様 (Request)
- **Content-Type:** `application/json`
- **パラメータ (Path/Query Parameter):**
  - なし

- **ボディ (Body Payload):**
```json
{
  "user_id": "string (UUID)",
  "started_at": "string (ISO 8601)",
  "ended_at": "string (ISO 8601)",
  "duration_seconds": 1500,
  "focus_mode": "pomodoro"
}
```

## 3. レスポンス仕様 (Response)
- **ステータスコード (Success):** `201 Created` または `200 OK`
- **ボディ (Body Payload):**
```json
{
  "session_id": "string (UUID)",
  "earned_points": 50,
  "total_points": 250
}
```

## 4. エラー処理 (Error Handling)
| Status Code | Error Code | Description |
|---|---|---|
| `400 Bad Request` | `invalid_duration` | 計測時間が不正、または短すぎる場合 |
| `401 Unauthorized` | `unauthorized` | トークンが無効な場合 |
| `500 Server Error` | `internal_error` | サーバー側でのDB保存失敗時など |

## 5. 備考 (Notes/Open Questions)
- [例: オフライン時のローカル保存と、オンライン復帰時のバッチ送信（バルクインサート）は別APIにするべきか？]
