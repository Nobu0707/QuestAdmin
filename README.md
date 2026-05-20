# QuestAdmin

QuestAdmin は、Forge 1.20.1 向けのクエスト管理MODです。
サーバー運営者がゲーム内でアイテム納品クエストを作成・編集・削除でき、プレイヤーはクエストを完了することで Lightman's Currency の銀行口座へ報酬を受け取れます。

## MOD概要

QuestAdmin v1.0.5 は ITEM_DELIVERY クエストに対応した v1.0.0 MVP の安定化版です。
Phase 11.6 では保存I/O計測、遅い保存の警告ログ、管理者向け保存状態確認コマンドを追加しています。

管理者は `/questadmin` のGUIとチャット入力ステップでクエストを管理できます。
プレイヤーは `/quest` のGUIまたはコマンドからクエストの確認、完了、報酬受け取りを行えます。

## 対応環境

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- Lightman's Currency 1.20.1-2.3.0.4e などの Forge 1.20.1 対応版
- mod id: `questadmin`
- 現在のバージョン: `1.0.5`

## 必須MOD

- Lightman's Currency
  - mod id: `lightmanscurrency`
- QuestAdmin

Lightman's Currency は QuestAdmin jar に同梱されません。サーバーとクライアント双方の `mods` フォルダへ別途配置してください。

## 開発環境での依存jar

Lightman's Currency のjarはGit管理しません。
開発環境でビルドする場合は、以下のファイルを手動で配置してください。

```text
libs/lightmanscurrency-1.20.1-2.3.0.4e.jar
```

配布時は QuestAdmin jar と Lightman's Currency jar を別々に `mods` フォルダへ入れてください。

## 導入方法

### サーバー

1. Forge 1.20.1 サーバーを用意します。
2. `mods` フォルダへ Lightman's Currency を入れます。
3. `mods` フォルダへ `questadmin-1.0.5.jar` を入れます。
4. サーバーを起動します。
5. 初回起動時、必要に応じて `config/questadmin/quests.json` と `config/questadmin/player_quests.json` が生成されます。

### クライアント

1. Forge 1.20.1 クライアントを用意します。
2. `mods` フォルダへ Lightman's Currency を入れます。
3. `mods` フォルダへ `questadmin-1.0.5.jar` を入れます。
4. サーバーへ接続します。

## 主な機能

- アイテム納品クエスト
- クエスト作成
- クエスト編集
- クエスト削除
- 有効/無効切替
- プレイヤー用GUI
- 管理者用GUI
- GUIページング
- プレイヤーGUI表示用の所持数計算キャッシュ
- QuestStorageのquestId検索インデックス
- ログアウト時/サーバー停止時の作成・編集セッションcleanup
- チャット一覧ページング
- 保存I/O計測と遅い保存の警告ログ
- 管理者向け保存状態確認
- Lightman's Currency 銀行口座への報酬支払い
- プレイヤーごとの完了/受け取り状態保存

## コマンド一覧

### プレイヤー向け

```text
/quest
/quest list
/quest list <page>
/quest complete <questId>
/quest claim <questId>
```

### 管理者向け

OP権限レベル2以上が必要です。

```text
/questadmin
/questadmin reload
/questadmin list
/questadmin list <page>
/questadmin sessions
/questadmin storage status
/questadmin economy status
/questadmin progress <player>
/questadmin progress mark <player> <questId> <status>
/questadmin edit <questId>
/questadmin edit cancel
/questadmin create cancel
```

## 管理者向け使い方

### クエスト作成

1. OP権限レベル2以上で `/questadmin` を実行します。
2. 管理者GUIの「新規クエスト作成」をクリックします。
3. チャット入力で以下を順番に入力します。

```text
questId
title
description
required itemId
required amount
reward money
repeatable
enabled
```

作成中に `cancel` と入力するか、`/questadmin create cancel` を実行すると作成をキャンセルできます。
作成途中でログアウトした場合、その作成セッションは破棄されます。

### クエスト編集

既存クエストは `/questadmin edit <questId>` または管理者GUIの詳細画面から編集できます。

編集では以下を順番に入力します。

```text
title
description
required itemId
required amount
reward money
repeatable
enabled
confirm
```

各入力で `-` を入力すると現在値を維持します。
最後の確認で `true` を入力すると保存し、`false` または `cancel` でキャンセルします。
編集中に `/questadmin edit cancel` を実行してもキャンセルできます。

編集できない項目は `id`、`type`、`createdAt` です。編集完了時は `updatedAt` が更新されます。
編集中にログアウトした場合、その編集セッションは破棄されます。

### 管理者GUI

`/questadmin` で管理者用GUIを開けます。

- 登録済みクエスト一覧の確認
- クエスト詳細の確認
- enabled の有効/無効切替
- 削除確認GUIを経由したクエスト削除
- 新規クエスト作成の開始
- 既存クエスト編集の開始

GUI操作時もサーバー側で権限確認を行います。

### reload / storage status / economy status

- `/questadmin reload` で `quests.json` を再読み込みします。
- `/questadmin list` は1ページ10件表示です。ページ指定は `/questadmin list 2` のように実行します。
- `/questadmin sessions` で現在の作成/編集セッション数を確認できます。
- `/questadmin storage status` で `quests.json` / `player_quests.json` の保存時間、成功/失敗回数、atomic fallback回数を確認できます。
- `/questadmin economy status` で Lightman's Currency 連携状態を確認します。

## プレイヤー向け使い方

1. `/quest` でプレイヤー用GUIを開きます。
2. 有効なクエスト一覧と詳細を確認します。
3. 必要アイテムを用意します。
4. GUIまたは `/quest complete <questId>` でクエストを完了します。
5. GUIまたは `/quest claim <questId>` で報酬を受け取ります。

`/quest list` は1ページ10件表示です。ページ指定は `/quest list 2` のように実行します。
納品対象は通常インベントリとオフハンドです。防具欄は対象外です。

報酬は Lightman's Currency の銀行口座へ入金されます。二重claimはできません。

## 保存ファイル

- `config/questadmin/quests.json`
- `config/questadmin/player_quests.json`

`quests.json` にはクエスト定義が保存されます。
`player_quests.json` にはプレイヤーごとの完了/受け取り状態が保存されます。
保存処理は一時ファイルへ書き込んでから置換し、`ATOMIC_MOVE` 非対応環境では非atomic置換へfallbackします。
読み込み・保存後は `QuestStorage` 内部でquestId検索インデックスを再構築します。
保存に50ms以上かかった場合は遅い保存としてwarnログを出し、200ms以上ではより強いwarnログを出します。ログ過多を避けるため、同種の警告は短時間で抑制されます。
安全性優先のため、`/quest complete <questId>`、`/quest claim <questId>`、`/questadmin progress mark ...` は現時点でも同期保存を維持しています。
保存方針の詳細は `docs/STORAGE_IO_STRATEGY.md` を参照してください。

## 入力値の制限

- `questId`: 半角英数字、`_`、`-` のみ。重複不可。
- `title`: 1文字以上64文字以下。
- `description`: 256文字以下。未指定時は空文字として扱います。
- `itemId`: Minecraftに存在するアイテムIDのみ。
- `amount`: 1以上999999以下。
- `reward money`: 0以上999999999以下。
- `repeatable`: 現時点では未対応です。`false` のみ使用できます。
- `enabled`: `true` または `false`。

## 注意事項

- 現在対応しているクエスト種別は `ITEM_DELIVERY` のみです。
- `repeatable=true` の繰り返しクエストは現時点では未対応です。
- 報酬は Lightman's Currency 銀行口座入金のみです。
- Lightman's Currency が無いと起動しません。
- QuestAdmin jar に Lightman's Currency は同梱しません。
- Lightman's Currency 以外の経済MODには対応していません。
- アイテム納品の対象は通常インベントリとオフハンドです。防具欄のアイテムは所持数表示にも完了判定にも消費にも含まれません。
- 討伐、採掘、探索、デイリー、クエストツリー、前提クエストは未実装です。
- クエスト削除や編集を行っても、既存の `player_quests.json` の進行状況は変更しません。
- 無効化されたクエストはプレイヤー用一覧やGUIに表示されず、完了・報酬受け取りもできません。
- 手動でJSONを編集する場合は事前バックアップを推奨します。
