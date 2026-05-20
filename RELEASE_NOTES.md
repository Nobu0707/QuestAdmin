# Release Notes

## QuestAdmin 1.0.5

### 概要

v1.0.0 MVP の安定化版です。
Phase 11.6 として、50人以上の同時接続運用に備え、保存I/O計測、遅い保存の警告ログ、保存方針ドキュメントを追加しました。

### 変更内容

- `quests.json` / `player_quests.json` の保存時間を計測
- 保存成功回数、保存失敗回数、atomic fallback回数を軽量なstatic metricsで保持
- 保存が50ms以上かかった場合にwarnログを出力
- 保存が200ms以上かかった場合により強いwarnログを出力
- 保存失敗時に対象ファイルと所要時間が分かるログを追加
- `/questadmin storage status` を追加し、管理者が保存状態を確認可能
- `docs/STORAGE_IO_STRATEGY.md` を追加し、現行の同期保存方針と将来の非同期/debounce設計メモを整理
- 安全性優先のため、`/quest complete <questId>`、`/quest claim <questId>`、`/questadmin progress mark ...` は同期保存を維持

### 配布物

- `build/libs/questadmin-1.0.5.jar`

QuestAdmin jar に Lightman's Currency 本体は同梱していません。
Lightman's Currency はサーバーとクライアント双方の `mods` フォルダへ別途配置してください。

## QuestAdmin 1.0.4

### 概要

v1.0.0 MVP の安定化版です。
Phase 11.5 として、50人以上の同時接続や長時間運用に備え、セッションcleanupと納品対象の安全化、チャット一覧ページングを追加しました。

### 変更内容

- プレイヤーログアウト時にクエスト作成/編集セッションを破棄
- サーバー停止時に作成/編集セッションMapをclear
- `/questadmin sessions` で作成/編集セッション数を確認可能
- アイテム納品対象を通常インベントリとオフハンドに限定し、防具欄を対象外化
- GUI所持数表示も通常インベントリとオフハンドのみを集計
- `/quest list <page>` を追加し、チャット一覧を1ページ10件に制限
- `/questadmin list <page>` を追加し、管理者向けチャット一覧を1ページ10件に制限

### 配布物

- `build/libs/questadmin-1.0.4.jar`

QuestAdmin jar に Lightman's Currency 本体は同梱していません。
Lightman's Currency はサーバーとクライアント双方の `mods` フォルダへ別途配置してください。

## QuestAdmin 1.0.3

### 概要

v1.0.0 MVP の安定化版です。
Phase 11.4 として、50人以上の同時接続やクエスト数増加に備え、GUI表示とquestId検索を軽量化しました。

### 変更内容

- `/quest` のプレイヤー用GUIに45件ごとのページングを追加
- `/questadmin` の管理者用GUIに45件ごとのページングを追加
- プレイヤーGUI表示時の所持数計算をインベントリ1回集計のSnapshot参照へ変更
- `QuestStorage` にquestId検索インデックス、`findById`、`exists` を追加
- 主要なquestId線形検索を `QuestStorage.findById` へ置換
- ページング後のGUIクリック時もquestIdからサーバー側で再検証する流れを維持

### 配布物

- `build/libs/questadmin-1.0.3.jar`

QuestAdmin jar に Lightman's Currency 本体は同梱していません。
Lightman's Currency はサーバーとクライアント双方の `mods` フォルダへ別途配置してください。

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
