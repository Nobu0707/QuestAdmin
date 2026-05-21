# QuestAdmin 運用ランブック

## 運用目的

QuestAdmin は、Forge 1.20.1 サーバーで運営者がアイテム納品クエストを作成し、プレイヤーが達成後に Lightman's Currency の銀行口座へ報酬を受け取るための運用向けMODです。

v1.0.7 は v1.0.0 MVP の安定化版です。50人以上の同時接続を想定し、二重支払い防止、保存ファイル検証、手動バックアップ、保存I/O計測を確認しながら運用してください。

## 対応環境

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- Lightman's Currency 必須
- QuestAdmin v1.0.7

## Lightman's Currency jar の配置

開発時は Lightman's Currency jar を以下へ手動配置します。

```text
libs/lightmanscurrency-1.20.1-2.3.0.4e.jar
```

`libs/*.jar` はGit管理しません。別PCへ移行した場合も手動で同じ場所へ配置してください。

実行時はサーバーとクライアント双方の `mods/` に以下を配置します。

```text
mods/lightmanscurrency-1.20.1-2.3.0.4e.jar
mods/questadmin-1.0.7.jar
```

QuestAdmin jar に Lightman's Currency 本体は同梱されません。

## サーバー導入手順

1. Forge 1.20.1 / Java 17 のサーバーを用意します。
2. `mods/` に Lightman's Currency jar を配置します。
3. `mods/` に `questadmin-1.0.7.jar` を配置します。
4. サーバーを起動します。
5. `latest.log` に QuestAdmin 由来の ERROR がないことを確認します。
6. OP権限レベル2以上のアカウントで接続します。
7. `/questadmin economy status` で `available: true` を確認します。
8. `/questadmin storage status` で保存I/O計測が表示されることを確認します。

Lightman's Currency が未導入の場合、`mods.toml` の必須依存により QuestAdmin は起動しません。

## クライアント導入手順

1. Forge 1.20.1 / Java 17 のクライアント環境を用意します。
2. `mods/` に Lightman's Currency jar を配置します。
3. `mods/` に `questadmin-1.0.7.jar` を配置します。
4. サーバーへ接続します。
5. `/quest` でプレイヤーGUIが開くことを確認します。

## 別PC移行手順

1. リポジトリを取得します。
2. Java 17 を用意します。
3. Gradle Wrapper があることを確認します。
4. `libs/lightmanscurrency-1.20.1-2.3.0.4e.jar` を手動配置します。
5. `./gradlew clean build` を実行します。
6. `build/libs/questadmin-1.0.7.jar` が生成されることを確認します。
7. `jar tf build/libs/*.jar | grep -E "mods.toml|pack.mcmeta"` を実行します。
8. Fabric関連grepを実行し、Forge構成であることを確認します。

## 起動確認

- Forgeサーバーが起動すること
- Lightman's Currency が読み込まれていること
- QuestAdmin が読み込まれていること
- `latest.log` に QuestAdmin 由来の ERROR がないこと
- `config/questadmin/quests.json` が読み込めること
- 必要に応じて `config/questadmin/player_quests.json` が作成されること

## 初回確認コマンド

```text
/questadmin
/quest
/questadmin economy status
/questadmin storage status
/questadmin storage validate
/questadmin storage backup
/questadmin storage backups
```

`/questadmin storage backup` は `quests.json` と `player_quests.json` の両方をバックアップ対象にします。初回直後などで `player_quests.json` が未生成の場合、そのファイルだけ FAILED と表示されることがあります。プレイヤー進行状況が一度保存された後に再実行してください。

## 通常運用時に見るコマンド

```text
/questadmin economy status
/questadmin storage status
/questadmin storage validate
/questadmin storage backup
/questadmin storage backups
/questadmin sessions
```

`storage status` では `lastSaveMs`、`success`、`failure`、`atomicFallback` を確認します。保存が50ms以上でwarn、200ms以上で強いwarnが出ます。

## クエスト作成手順

1. OP権限レベル2以上で `/questadmin` を実行します。
2. 管理者GUIの新規作成ボタンから作成を開始します。
3. チャット入力で `questId`、タイトル、説明、納品アイテムID、個数、報酬、`repeatable`、`enabled` を入力します。
4. `repeatable` は現時点では `false` のみ使用します。
5. 作成後に `/questadmin list` と `/quest list` で表示を確認します。

作成中止は `cancel` または `/questadmin create cancel` です。

## クエスト編集手順

1. `/questadmin edit <questId>`、または管理者GUIの詳細から編集を開始します。
2. チャット入力で各項目を更新します。
3. 変更しない項目は `-` を入力します。
4. 最後の確認で `true` を入力すると保存されます。
5. `/questadmin storage validate` で保存ファイルを確認します。

編集中止は `cancel` または `/questadmin edit cancel` です。`id`、`type`、`createdAt` は編集できません。

## クエスト削除手順

1. `/questadmin` で管理者GUIを開きます。
2. 対象クエストを Shiftクリックして削除確認画面へ進みます。
3. 確認画面で削除を実行します。
4. `/questadmin list` で削除されたことを確認します。

削除しても `player_quests.json` の既存進行状況は自動削除されません。必要に応じてバックアップ後に手動確認してください。

## クエスト無効化手順

1. `/questadmin` で管理者GUIを開きます。
2. 対象クエストを左クリックして enabled を切り替えます。
3. `/quest list` に無効クエストが表示されないことを確認します。

無効クエストはプレイヤーの完了と報酬受け取りも拒否されます。

## プレイヤー対応手順

- クエスト一覧が見えない場合は `/quest list` と `/quest` を確認します。
- 納品できない場合は対象アイテムID、必要個数、通常インベントリ/オフハンドの所持数を確認します。
- 防具欄は納品対象外です。
- 報酬が受け取れない場合は `/questadmin economy status` と該当プレイヤーの `/questadmin progress <player>` を確認します。
- 管理者が状態を修正する場合は `/questadmin progress mark <player> <questId> <status>` を使います。二重支払いを避けるため、残高確認後に実行してください。

## 報酬トラブル時の確認手順

1. `latest.log` の QuestAdmin ERROR / WARN を確認します。
2. `/questadmin economy status` で Lightman's Currency 連携状態を確認します。
3. `/questadmin progress <player>` で対象クエストの状態を確認します。
4. Lightman's Currency 側の残高を確認します。
5. `/questadmin storage validate` で保存ファイルを検証します。
6. 状態修正が必要な場合は、必ず `/questadmin storage backup` 後に実施します。

## CLAIMING状態が残った場合の確認方針

`CLAIMING` は報酬支払い中、または支払い結果の確認が必要な状態です。プレイヤーからの再claimは拒否されます。

確認手順:

1. `latest.log` で `CRITICAL` または claim 関連エラーを探します。
2. Lightman's Currency の銀行口座残高を確認します。
3. 支払い済みなら `/questadmin progress mark <player> <questId> CLAIMED` を検討します。
4. 支払いされていないことが確認できた場合のみ `COMPLETED` へ戻し、再claimを案内します。

判断できない場合は再claimさせず、残高とログを保全してください。

## 保存ファイル破損時の対応方針

1. サーバーを停止します。
2. 現在の `config/questadmin/quests.json` と `player_quests.json` を別場所へ退避します。
3. 直近バックアップを確認します。
4. 手動復元後、サーバー起動前にJSON構文を確認します。
5. 起動後に `/questadmin storage validate` を実行します。
6. `latest.log` に QuestAdmin 由来 ERROR がないことを確認します。

## バックアップからの手動復元方針

QuestAdmin は自動復元と復元コマンドを提供しません。

手動復元する場合は、サーバー停止中に `config/questadmin/backups/` から対象ファイルを選び、元のファイル名へ戻します。

```text
quests-YYYYMMDD-HHMMSS.json -> quests.json
player_quests-YYYYMMDD-HHMMSS.json -> player_quests.json
```

復元は進行状況の巻き戻りや報酬状態の不整合につながる可能性があります。特に `player_quests.json` を戻す場合は、Lightman's Currency 残高と `CLAIMED` / `CLAIMING` 状態を確認してください。

## 注意事項

- Lightman's Currency は必須です。
- 他経済MODには対応していません。
- 対応クエスト種別は `ITEM_DELIVERY` のみです。
- `repeatable=true` は未対応です。
- 納品対象は通常インベントリとオフハンドです。
- NBT差分は判定しません。同じアイテムIDなら同一扱いです。
- `/quest complete <questId>` と `/quest claim <questId>` は安全性優先で同期保存します。
- 保存ファイルの手動編集前にはバックアップを作成してください。
- ピーク時間帯の大量編集や手動復元は避けてください。
