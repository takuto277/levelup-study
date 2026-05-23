# Issue: agent-skills ワークフロー統合

## 背景

友人の [u7chan/agent-skills](https://github.com/u7chan/agent-skills) は、PR 前の lint/test をエージェントがローカル実行し、PR 本文テンプレをスキルに閉じ込める設計。LevelUp Study は Auto open PR + GA 依存が強く、iOS コンパイル漏れなど GA 穴もあった。

## 目的

- push **前** の validate を `./scripts/validate-pr.sh` + `AGENTS.md` で標準化
- PR 本文テンプレを `.cursor/skills/github-pr-create` に集約
- `feature-delivery` / `github-implement-pr` / `qa-test-design` を LevelUp 向けに追加

## スコープ

- `AGENTS.md`、 `scripts/validate-pr.sh`
- `.cursor/skills/` に github-pr-create, github-implement-pr, qa-test-design, git-*
- `feature-delivery` / `pr-review.mdc` 更新
- GA ワークフロー自体の削除はしない（セーフティネットとして維持）

## 受け入れ条件

- [ ] `./scripts/validate-pr.sh --all` がローカルで動く
- [ ] スキルから validate → PR 本文テンプレの流れがドキュメント化されている
- [ ] CI green
