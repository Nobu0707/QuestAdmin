# QuestAdmin

QuestAdmin は Forge 1.20.1 向けのクエスト管理MODです。サーバー運営者がゲーム内GUIとチャット入力でアイテム納品クエストを作成・編集・削除し、プレイヤーは達成後に Lightman's Currency の銀行口座へ報酬を受け取れます。

## MOD概要

QuestAdmin v1.0.7 は v1.0.0 MVP の安定化版です。
Phase 11.8 では新機能追加ではなく、運用ドキュメント、実機確認手順、既知制限、リリース前チェックを整理しました。

主な安定化内容:

- 報酬claim安全化と `CLAIMING` 状態
- GUIページングとチャット一覧ページング
- 所持数計算キャッシュとquestId検索最適化
- セッションcleanupと納品対象安全化
- 保存I/O計測、遅い保存warn、保存方針ドキュメント
- 手動バックアップ、バックアップ一覧、保存ファイル検証

## 対応環境

- Minecraft 1.20.1
- Forge 47.x
- Java 17
- mod id: `questadmin`
- 現在のバージョン: `1.0.7`

## 必須MOD

- Lightman's Currency
  - mod id: `lightmanscurrency`
- QuestAdmin

Lightman's Currency は QuestAdmin jar に同梱されません。サーバーとクライアント双方の `mods/` へ別途配置してください。

開発環境でビルドする場合は、以下のjarを手動配置します。

```text
libs/lightmanscurrency-1.20.1-2.3.0.4e.jar
```

`libs/*.jar` はGit管理しません。別PCへ移行した場合も手動で配置してください。

## 導入方法

### サーバー

1. Forge 1.20.1 / Java 17 のサーバーを用意します。
2. `mods/` へ Lightman's Currency jar を入れます。
3. `mods/` へ `questadmin-1.0.7.jar` を入れます。
4. サーバーを起動します。
5. `/questadmin economy status` で Lightman's Currency 連携を確認します。
6. `/questadmin storage validate` で保存ファイルを確認します。

### クライアント

1. Forge 1.20.1 / Java 17 のクライアントを用意します。
2. `mods/` へ Lightman's Currency jar を入れます。
3. `mods/` へ `questadmin-1.0.7.jar` を入れます。
4. サーバーへ接続し、`/quest` でGUIが開くことを確認します。

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
/questadmin storage backup
/questadmin storage backups
/questadmin storage validate
/questadmin economy status
/questadmin progress <player>
/questadmin progress mark <player> <questId> <status>
/questadmin edit <questId>
/questadmin edit cancel
/questadmin create cancel
```

## 管理者向け使い方

`/questadmin` で管理者GUIを開きます。

管理者GUIでできること:

- 登録済みクエスト一覧の確認
- クエスト詳細の確認
- 新規クエスト作成の開始
- 既存クエスト編集の開始
- enabled の有効/無効切替
- 削除確認GUIを経由したクエスト削除

クエスト作成は管理者GUIから開始し、チャット入力で以下を順番に入力します。

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

`repeatable` は現時点では `false` のみ使用できます。作成中止は `cancel` または `/questadmin create cancel` です。

既存クエストは `/questadmin edit <questId>` または管理者GUIから編集できます。変更しない項目は `-` を入力し、最後の確認で `true` を入力すると保存されます。編集中止は `cancel` または `/questadmin edit cancel` です。

保存・経済連携の通常確認:

```text
/questadmin economy status
/questadmin storage status
/questadmin storage validate
/questadmin storage backup
/questadmin storage backups
/questadmin sessions
```

## プレイヤー向け使い方

1. `/quest` でプレイヤーGUIを開きます。
2. 有効なクエスト一覧と詳細を確認します。
3. 必要アイテムを通常インベントリまたはオフハンドに用意します。
4. GUIまたは `/quest complete <questId>` でクエストを完了します。
5. GUIまたは `/quest claim <questId>` で報酬を受け取ります。

`/quest list` は1ページ10件表示です。ページ指定は `/quest list 2` のように実行します。
報酬は Lightman's Currency の銀行口座へ入金され、二重claimはできません。

## 保存ファイル

```text
config/questadmin/quests.json
config/questadmin/player_quests.json
```

`quests.json` にはクエスト定義、`player_quests.json` にはプレイヤーごとの完了/受け取り状態が保存されます。

保存処理は一時ファイルへ書き込んでから置換します。`ATOMIC_MOVE` 非対応環境では非atomic置換へfallbackします。安全性優先のため、`/quest complete <questId>`、`/quest claim <questId>`、`/questadmin progress mark ...` は同期保存を維持しています。

保存に50ms以上かかった場合はwarnログ、200ms以上ではより強いwarnログを出します。`/questadmin storage status` で保存時間、成功/失敗回数、atomic fallback回数を確認できます。

## バックアップ機能

```text
/questadmin storage backup
/questadmin storage backups
```

手動バックアップは `config/questadmin/backups/` にタイムスタンプ付きで保存されます。`quests.json` と `player_quests.json` はそれぞれ最新10件程度に制限され、古いバックアップは削除されます。

初回直後などで `player_quests.json` が未生成の場合、`/questadmin storage backup` は `player_quests` だけFAILEDを表示することがあります。プレイヤー進行状況が一度保存された後に再実行してください。

QuestAdmin は自動復元や復元コマンドを提供しません。復元が必要な場合はサーバーを停止し、現在のJSONを別場所へ退避してから、バックアップファイルを手動で元のファイル名へコピーしてください。

## 検証機能

```text
/questadmin storage validate
```

現在の `quests.json` と `player_quests.json` を読み込み専用で検証します。

確認対象:

- JSON構文
- `quests.json` のルート形式
- questId重複
- `ITEM_DELIVERY` 制限
- `repeatable=false` 制限
- itemId / amount / reward money
- `player_quests.json` のUUID、status、不明questId警告

## 既知の制限

- 対応クエスト種別は `ITEM_DELIVERY` のみです。
- `repeatable=true` は現時点では未対応です。
- Lightman's Currency が必須です。
- Lightman's Currency 以外の経済MODには対応していません。
- 討伐、採掘、探索、デイリー、村人NPC、Web管理画面、MySQL保存は未実装です。
- 納品対象は通常インベントリとオフハンドです。防具欄は対象外です。
- NBT差分は現時点では判定しません。
- バックアップ復元は自動ではなく手動です。

詳細は `docs/KNOWN_LIMITATIONS.md` を参照してください。

## 詳細ドキュメント

- `docs/OPERATIONS_RUNBOOK.md`: 導入、移行、障害対応、リリース前運用確認
- `docs/TEST_CHECKLIST.md`: ビルド、起動、管理者/プレイヤー機能、50人以上想定確認
- `docs/KNOWN_LIMITATIONS.md`: 既知の制限
- `docs/STORAGE_IO_STRATEGY.md`: 保存I/O方針、同期保存維持理由、将来の非同期化メモ
- `HANDOFF.md`: 開発引き継ぎ情報
- `RELEASE_NOTES.md`: リリース履歴
