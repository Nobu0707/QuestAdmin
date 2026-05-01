# QuestAdmin

QuestAdmin は Minecraft Forge 1.20.1 向けのクエスト管理MODです。
サーバー管理者がゲーム内でアイテム納品クエストを作成・編集・管理し、プレイヤーはクエストを達成して Lightman's Currency の銀行口座へ報酬を受け取れます。

## 対応環境

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- Lightman's Currency 必須
- mod id: `questadmin`
- 現在のバージョン: `0.9.0`

## 必須MOD

- Lightman's Currency
  - mod id: `lightmanscurrency`
  - 開発時参照: `lightmanscurrency-1.20.1-2.3.0.4e.jar`

QuestAdmin は Lightman's Currency を必須依存として扱います。サーバーとクライアントの両方へ導入してください。

## 導入方法

1. Forge 1.20.1 サーバーまたはクライアントを用意します。
2. `mods` フォルダに Lightman's Currency を入れます。
3. `mods` フォルダに `questadmin-0.9.0.jar` を入れます。
4. サーバーを起動します。
5. 初回起動時、必要に応じて `config/questadmin/quests.json` が生成されます。

## コマンド一覧

### プレイヤー向け

```text
/quest
/quest list
/quest complete <questId>
/quest claim <questId>
```

### 管理者向け

OP権限レベル2以上が必要です。

```text
/questadmin
/questadmin reload
/questadmin list
/questadmin edit <questId>
/questadmin edit cancel
/questadmin create cancel
/questadmin economy status
/questadmin progress <player>
/questadmin progress mark <player> <questId> <status>
```

## 管理者向け使い方

`/questadmin` で管理者用GUIを開けます。

管理者GUIでは以下を行えます。

- 登録済みクエスト一覧の確認
- クエスト詳細の確認
- enabled の有効/無効切替
- 削除確認GUIを経由したクエスト削除
- 新規クエスト作成の開始
- 既存クエスト編集の開始

GUI操作時もサーバー側で権限確認を行います。

## プレイヤー向け使い方

`/quest` でプレイヤー用GUIを開けます。

プレイヤーは以下を行えます。

- 有効なクエスト一覧の確認
- クエスト詳細の確認
- 必要アイテムを所持している場合のクエスト完了
- 完了済みクエストの報酬受け取り

チャットコマンドからも `/quest list`、`/quest complete <questId>`、`/quest claim <questId>` を実行できます。

## クエスト作成手順

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

入力値の制限:

- `questId`: 半角英数字、`_`、`-` のみ。重複不可。
- `itemId`: Minecraftに存在するアイテムIDのみ。
- `amount`: 1以上999999以下。
- `reward money`: 0以上999999999以下。
- `repeatable` / `enabled`: `true` または `false`。

## クエスト編集手順

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

編集できない項目:

- `id`
- `type`
- `createdAt`

編集完了時は `updatedAt` が更新されます。

## 保存場所

クエスト定義:

```text
config/questadmin/quests.json
```

プレイヤー進行状況:

```text
config/questadmin/player_quests.json
```

## 対応クエスト種別

現在対応しているクエスト種別は `ITEM_DELIVERY` のみです。

対応内容:

- 指定アイテムの所持数確認
- 完了時の必要アイテム消費
- Lightman's Currency 銀行口座への報酬入金

## 注意事項

- Lightman's Currency 以外の経済MODには対応していません。
- 討伐、採掘、探索、デイリー、クエストツリー、前提クエストは未実装です。
- クエスト削除や編集を行っても、既存の `player_quests.json` の進行状況は変更しません。
- 無効化されたクエストはプレイヤー用一覧やGUIに表示されず、完了・報酬受け取りもできません。
- `quests.json` が不正な場合はログにエラーを出し、サーバーがクラッシュしないように処理します。
