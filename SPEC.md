# QuestAdmin SPEC

## 1. MOD概要

QuestAdmin は、Minecraft Forge 1.20.1 環境向けのクエスト管理MODです。

このMODの主目的は、サーバー運営者がゲーム内GUIからクエストを作成し、プレイヤーがそのクエストを達成することで、お金MODを通じて報酬を受け取れるようにすることです。

本MODは、単なるプレイヤー向けクエストMODではなく、運営側がクエストを作成・管理するための「クエスト作成システム」を目指します。

## 2. 開発環境

- Minecraft: 1.20.1
- Mod Loader: Forge 1.20.1 / Forge 47.x 系
- Java: Java 17
- 開発環境: VS Code + WSL
- 仕様・設計・プロンプト作成: GPT Thinking
- 実装・ビルド・修正: Claude / 必要に応じてCodex
- 言語: Java

## 3. 重要な前提

このプロジェクトは **FabricではなくForge 1.20.1用** です。

使用しないもの:

- Fabric API
- FabricLoader
- ModInitializer
- fabric.mod.json
- net.fabricmc 系 import

Forge側で使用する想定のもの:

- `@Mod("questadmin")`
- `mods.toml`
- Forge EventBus
- `RegisterCommandsEvent`
- ForgeのMenu / Screen
- ForgeのSimpleChannel

## 4. MVPの目的

MVPでは、以下の一連の流れが最低限成立することを目標とします。

1. 管理者がゲーム内でクエストを作成する
2. クエスト情報が保存される
3. プレイヤーがクエストを確認する
4. プレイヤーが条件を満たしてクエストを達成する
5. 報酬がお金MOD経由で支払われる
6. プレイヤーごとの達成状況が保存される

## 5. MVPで実装する機能

### 5.1 管理者向け機能

- `/questadmin` で管理画面を開く
- 登録済みクエスト一覧を確認できる
- 新規クエストを作成できる
- 既存クエストを削除できる
- クエストを有効化・無効化できる
- `/questadmin reload` でクエスト設定を再読み込みできる
- `/questadmin list` で登録済みクエスト一覧をチャットに表示できる

### 5.2 プレイヤー向け機能

- `/quest` でクエスト一覧を開く
- 受注可能なクエストを確認できる
- クエスト詳細を確認できる
- 条件を満たしていればクエストを完了できる
- 完了時に必要アイテムを消費する
- 完了時にお金MOD経由で報酬を受け取る
- 完了済みクエストは保存される

### 5.3 MVPで対応するクエスト種別

MVPでは、以下の1種類のみ対応します。

- アイテム納品クエスト

例:

- 小麦を64個納品する
- 鉄インゴットを32個納品する
- ダイヤモンドを3個納品する

### 5.4 MVPで対応する報酬

MVPでは、以下の報酬のみ対応します。

- お金MODを通じた通貨報酬

内部的には `EconomyBridge` を作成し、将来的に複数のお金MODへ対応できる設計にします。

ただし、MVPでは実際に対応するお金MODは1種類のみとします。

## 6. MVPで実装しない機能

以下はMVPでは実装しません。

- 討伐クエスト
- 採掘クエスト
- ブロック設置クエスト
- 探索クエスト
- デイリークエスト
- ウィークリークエスト
- クエストツリー
- 前提クエスト
- 専用村人NPC
- 専用村人職業
- 会話分岐
- ランキング
- Web管理画面
- MySQL / PostgreSQL 保存
- 複数のお金MOD対応
- パーティ共有クエスト
- 派手な演出
- 複雑な権限管理

## 7. データ設計

### 7.1 QuestDefinition

クエスト定義を表します。

想定フィールド:

```text
id: String
title: String
description: String
type: QuestType
requirement: QuestRequirement
reward: QuestReward
repeatable: boolean
enabled: boolean
createdAt: long
updatedAt: long
```

### 7.2 QuestType

MVPでは以下のみ使用します。

```text
ITEM_DELIVERY
```

将来的に以下を追加できる余地を残します。

```text
MOB_KILL
BLOCK_BREAK
LOCATION
COMMAND
```

### 7.3 QuestRequirement

クエスト達成条件を表します。

MVPではアイテム納品のみ対応します。

想定フィールド:

```text
itemId: String
amount: int
```

例:

```json
{
  "itemId": "minecraft:wheat",
  "amount": 64
}
```

### 7.4 QuestReward

クエスト報酬を表します。

MVPではお金報酬のみ対応します。

想定フィールド:

```text
money: long
```

例:

```json
{
  "money": 500
}
```

### 7.5 PlayerQuestState

プレイヤーごとのクエスト状態を表します。

想定フィールド:

```text
playerUuid: UUID
questId: String
status: QuestStatus
completedAt: long
claimedAt: long
```

### 7.6 QuestStatus

MVPでは以下を使用します。

```text
NOT_STARTED
COMPLETED
CLAIMED
```

MVPでは、受注状態を厳密に分けなくてもよいです。

ただし、将来的に以下を追加できるようにします。

```text
IN_PROGRESS
CANCELLED
```

## 8. 保存形式

MVPではファイル保存を使用します。

### 8.1 クエスト定義

保存先:

```text
config/questadmin/quests.json
```

例:

```json
[
  {
    "id": "wheat_delivery_001",
    "title": "小麦の納品",
    "description": "小麦を64個納品してください。",
    "type": "ITEM_DELIVERY",
    "requirement": {
      "itemId": "minecraft:wheat",
      "amount": 64
    },
    "reward": {
      "money": 500
    },
    "repeatable": false,
    "enabled": true,
    "createdAt": 0,
    "updatedAt": 0
  }
]
```

### 8.2 プレイヤー進行状況

保存先:

```text
config/questadmin/player_quests.json
```

例:

```json
{
  "player-uuid-here": {
    "wheat_delivery_001": {
      "status": "CLAIMED",
      "completedAt": 0,
      "claimedAt": 0
    }
  }
}
```

## 9. Forge 1.20.1での実装方針

### 9.1 メインMODクラス

Forgeでは、メインMODクラスは `@Mod("questadmin")` を使用します。

想定:

```java
@Mod(QuestAdminMod.MOD_ID)
public class QuestAdminMod {
    public static final String MOD_ID = "questadmin";

    public QuestAdminMod() {
        // Forge向け初期化処理
    }
}
```

### 9.2 コマンド登録

コマンドはForgeの `RegisterCommandsEvent` で登録します。

MVPで想定するコマンド:

```text
/questadmin
/questadmin reload
/questadmin list
/quest
/quest list
/quest complete <questId>
```

### 9.3 GUI

Forge 1.20.1では、GUIはMenu / Screenを使って実装します。

MVPでは最終的にGUIを使いますが、実装順序としては以下を許可します。

1. まずチャット表示で機能を確認する
2. 次にプレイヤー用GUIを作る
3. 最後に管理者用GUIを作る

### 9.4 ネットワーク通信

クライアントGUIからサーバー側処理を呼ぶ必要がある場合は、Forgeの `SimpleChannel` を使います。

ただし、ネットワーク通信はGUI実装Phaseまで追加しません。

### 9.5 configディレクトリ

保存先は最終的に以下になるようにします。

```text
config/questadmin/quests.json
config/questadmin/player_quests.json
```

Phase 1.5では、Forge環境でこのパスへ安全に読み書きできるか確認します。

## 10. コマンド仕様

### 10.1 管理者コマンド

```text
/questadmin
```

管理GUIを開きます。

```text
/questadmin reload
```

クエスト定義を再読み込みします。

```text
/questadmin list
```

デバッグ用として、登録済みクエスト一覧をチャットに表示します。

MVP初期段階ではGUIより先にチャット表示で実装してもよいです。

### 10.2 プレイヤーコマンド

```text
/quest
```

プレイヤー用クエスト画面を開きます。

MVP初期段階ではGUIより先にチャット表示で実装してもよいです。

```text
/quest complete <questId>
```

デバッグ用または初期実装用として、指定クエストの完了処理を実行します。

最終的にはGUI操作から完了できるようにします。

## 11. GUI方針

本MODの最終的なMVPではGUIを使用します。

### 11.1 プレイヤー用GUI

必要な画面:

- クエスト一覧
- クエスト詳細
- 完了ボタン

### 11.2 管理者用GUI

必要な画面:

- クエスト一覧
- 新規作成
- 削除
- 有効/無効切り替え
- クエスト作成フォーム

### 11.3 GUI実装上の注意

独自GUIを作る場合、管理者・プレイヤーのクライアント側にもMOD導入が必要です。

そのため、本MODは原則として以下の構成を想定します。

```text
サーバー: MOD必須
管理者クライアント: MOD必須
プレイヤークライアント: 原則MOD必須
```

将来的にサーバー専用化を目指す場合は、チェストGUIやチャット入力方式を検討します。

## 12. 経済MOD連携方針

お金MOD連携は直接実装せず、必ず `EconomyBridge` を経由します。

想定インターフェース:

```text
EconomyBridge
- boolean isAvailable()
- boolean deposit(UUID playerUuid, long amount)
- String getCurrencyName()
```

MVPでは1種類のお金MODのみ対応します。

経済MODが存在しない場合は、以下の挙動にします。

- サーバーログに警告を出す
- 報酬支払いを失敗扱いにする
- プレイヤーに報酬支払い失敗メッセージを出す
- クエスト完了状態の扱いは慎重にする

報酬支払いに失敗した場合、アイテムだけ消費される事故を避けます。

## 13. 権限方針

MVPでは単純なOP判定を使用します。

管理者用機能:

```text
permission level 2 以上
```

将来的にLuckPerms等の権限MODに対応できる余地を残します。

## 14. エラー処理方針

以下のケースでは、必ず安全側に倒します。

- クエスト定義の読み込み失敗
- JSONの構文エラー
- アイテムIDが不正
- 報酬金額が不正
- 経済MODが未導入
- 報酬支払い失敗
- 保存失敗

特に、以下の事故は避けます。

- アイテムだけ消費されて報酬が支払われない
- 報酬が二重支払いされる
- 削除済みクエストを完了できる
- 無効化されたクエストを完了できる
- サーバー再起動で進行状況が消える

## 15. 開発上の優先順位

優先順位は以下の通りです。

1. ビルドが通ること
2. Forge 1.20.1の構成として正しいこと
3. データ構造が破綻しないこと
4. 保存と読み込みが安定すること
5. 報酬支払いで事故が起きないこと
6. GUIが分かりやすいこと
7. 拡張しやすいこと

## 16. 命名方針

仮のMOD名:

```text
QuestAdmin
```

仮のmod id:

```text
questadmin
```

仮のpackage:

```text
net.nobu0707.questadmin
```

既存プロジェクトに別の命名規則がある場合は、既存に合わせます。
