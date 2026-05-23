---
name: qa-test-design
description: >
  QA観点・テストケース・手動 UI 確認項目を洗い出す。
  「何を確認すればいい」「テスト観点」「QAしたい」で使う。LevelUp Study 向け。
---

# qa-test-design（LevelUp Study）

テストコードや **手動 UI 確認** の前に、観点一覧を作る。エージェントが「CI green = 完了」と誤認しないためのスキル。

## 1. 対象を特定

- Issue の受け入れ条件
- PR 差分（backend / shared / composeApp / iosApp）
- 触った API・画面・CI ワークフロー

## 2. LevelUp 向けチェックリスト

### Backend（Go）

- [ ] 認可: 他ユーザー resource ID → 403/404
- [ ] DEV_MODE / 本番（JWT + API Key）の差
- [ ] `go test ./...` + `go vet`

### Mobile（KMP + UI）

- [ ] Android Compose 画面
- [ ] iOS Swift 画面（**KotlinInt ↔ Int32** 等の型）
- [ ] `./scripts/validate-pr.sh` + macOS なら `--ios`
- [ ] Render API + `local.properties`（api.key / dev.jwt）

### 手動 UI（ユーザー確認が必要な典型）

| 観点 | 確認方法 |
|------|----------|
| タップ・ナビ | 実機/シミュレータで操作 |
| バックグラウンド | アプリを裏にして復帰 |
| オフライン | 機内モードで訓練場等 |

エージェントは **自動で UI を保証できない** 場合、`github-pr-create` の Checklist に「手元 UI 確認 — ユーザー」を残す。

## 3. 出力形式

```markdown
## テスト設計: {機能名}

### 自動（validate-pr / CI）
- [ ] コマンド — 期待

### 手動（ユーザー）
- [ ] 手順 — 期待

### スコープ外
- （理由）
```

## 4. ルール

- 観点提示後、ユーザーが依頼するまでテストコードを大量追加しない
-  trivial な変更（ typo 等）では省略可

## 参考

- 友人版の詳細技法: https://github.com/u7chan/agent-skills/tree/main/qa-test-design
