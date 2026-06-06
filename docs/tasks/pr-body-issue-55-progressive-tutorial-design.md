## Issues

- Refs #55

## 背景

#55 は要件定義まで完了していたため、既存の #50 オンボーディング実装と Android/iOS の画面構成に照らして、段階的チュートリアルの実装前設計を作成しました。

## 概要

段階的チュートリアルのトピック、表示条件、ローカル進捗ストア、OS 別 UI 接続方針、テスト計画を `docs/tasks` に追加します。この PR は設計 PR のため、Issue はクローズせずステータスを `status:設計済み` に更新する前提です。

## 変更内容

- #55 の実行計画・設計書を追加
- 既存 `onboarding_done` と `core_loop_onboarding` の互換移行方針を整理
- Android Compose / iOS SwiftUI の表示トリガーと設定再表示導線を設計
- `goal_bonus` / `pending_sync` の予約トピック方針と QA 観点を整理

## 確認項目

- [x] `./scripts/validate-pr.sh`

## 詳細

実装は後続 PR で行います。今回の PR ではアプリコードを変更していません。
