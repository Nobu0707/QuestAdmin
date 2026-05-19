# Release Notes

## QuestAdmin 1.0.2

### 概要

v1.0.0 MVP の安定化版です。
Phase 11.3 として、クエストデータ検証と `quests.json` 読み込み時の安全性を強化しました。

### 変更内容

- `QuestValidator` / `QuestValidationResult` / `QuestValidationError` を追加
- `questId` 形式、重複、title、description、itemId、amount、reward money の検証を共通化
- `quests.json` 読み込み時、不正なクエストをスキップしてwarnログへ理由を出力
- JSON構文不正やファイルI/O失敗時は既存のメモリ上クエストを維持
- `ITEM_DELIVERY` 以外のtypeをMVP未対応として安全にスキップ
- `repeatable=true` を現時点では未対応として拒否
- reward money `0` を検証上の有効値として扱うよう調整

### 配布物

- `build/libs/questadmin-1.0.2.jar`

QuestAdmin jar に Lightman's Currency 本体は同梱していません。
Lightman's Currency はサーバーとクライアント双方の `mods` フォルダへ別途配置してください。

## QuestAdmin 1.0.1

### 概要

v1.0.0 MVP の安定化版です。
Phase 11.2 として、リポジトリ衛生修正と保存I/O安全化を行いました。

### 変更内容

- `quests.json` / `player_quests.json` の保存処理を共通化
- 一時ファイル書き込み後の置換に `ATOMIC_MOVE` fallback を追加
- 保存失敗時のログを例外つきで出力するよう改善
- `QuestStorage` の `System.err.println` をLoggerへ置換
- 外部MOD jar、生成jar、`build/`、`.gradle/`、レビュー用生成物をGit管理しない方針を明記
- Linux / WSL 用の `gradlew` 実行権限を確認

### 配布物

- `build/libs/questadmin-1.0.1.jar`

QuestAdmin jar に Lightman's Currency 本体は同梱していません。
Lightman's Currency はサーバーとクライアント双方の `mods` フォルダへ別途配置してください。

## QuestAdmin 1.0.0

### 概要

初期MVP完成版です。

QuestAdmin は Forge 1.20.1 向けのクエスト管理MODです。
サーバー運営者はゲーム内でアイテム納品クエストを作成・編集・削除でき、プレイヤーはクエストを完了することで Lightman's Currency の銀行口座へ報酬を受け取れます。

### 追加済み機能

- アイテム納品クエスト
- プレイヤー用GUI
- 管理者用GUI
- クエスト作成
- クエスト編集
- クエスト削除
- 有効/無効切替
- Lightman's Currency 報酬支払い
- プレイヤーごとの進行状況保存
- `quests.json` によるクエスト定義保存
- `player_quests.json` による完了/受け取り状態保存

### 対応環境

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- Lightman's Currency 1.20.1 対応版

### 配布物

- `build/libs/questadmin-1.0.0.jar`

QuestAdmin jar に Lightman's Currency 本体は同梱していません。
Lightman's Currency はサーバーとクライアント双方の `mods` フォルダへ別途配置してください。

### 既知の制限

- `ITEM_DELIVERY` のみ対応
- Lightman's Currency 必須
- 報酬は Lightman's Currency 銀行口座入金のみ
- 複数経済MOD非対応
- Lightman's Currency 以外の経済MOD非対応
- 村人/NPC未実装
- Web管理画面未実装
- MySQL保存未実装
- 討伐、採掘、探索、デイリー、クエストツリー、前提クエストは未実装
