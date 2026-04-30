# Phase 1.5 Forge 1.20.1 整合確認プロンプト

あなたはこのForge 1.20.1 MODプロジェクトの実装担当です。

このプロジェクトでは、仕様整理・設計・作業指示はGPT Thinkingが担当し、実装・ビルド・修正はClaudeまたはCodexが担当します。

まず、リポジトリ直下にある以下のファイルを必ず読んでください。

- `SPEC.md`
- `ROADMAP.md`
- `CLAUDE.md`

## 重要

このプロジェクトは **FabricではなくForge 1.20.1用** です。

以下は使用しないでください。

- Fabric API
- FabricLoader
- ModInitializer
- fabric.mod.json
- net.fabricmc 系 import

Phase 1 はすでに完了しており、ビルドも成功しています。

今回の作業は **Phase 1.5** です。

## 今回の目的

Phase 1で実装済みのクエストデータ構造と保存処理が、Forge 1.20.1環境に正しく適合しているか確認し、必要最小限の修正を行ってください。

今回は、Forge移行に必要な確認と軽微な修正だけを行います。

## 確認すること

以下を確認してください。

- `build.gradle` がForge 1.20.1前提になっているか
- Java 17前提になっているか
- `mods.toml` が存在するか
- mod id が `questadmin` になっているか
- メインMODクラスが `@Mod("questadmin")` で定義されているか
- FabricLoader / ModInitializer / net.fabricmc 系 import が残っていないか
- Fabric API依存が残っていないか
- `QuestStorage` がForge環境のconfigディレクトリ配下に `quests.json` を作成できるか
- 保存先が `config/questadmin/quests.json` になっているか
- `quests.json` が存在しない場合、サンプルクエストを生成できるか
- `./gradlew build` が成功するか

## 実装してよい内容

以下は実装・修正してよいです。

- Forge用メインMODクラスの修正
- `@Mod("questadmin")` の整備
- configディレクトリ取得処理のForge向け修正
- 不要なFabric参照の削除
- `mods.toml` の軽微な修正
- `build.gradle` の軽微な修正
- Phase 1で作った `QuestStorage` のForge向け調整
- 起動時に `QuestStorage` を初期化する処理

## 実装しない内容

以下は今回実装しないでください。

- GUI
- Menu
- Screen
- クライアント側画面
- ネットワーク通信
- SimpleChannel
- `/quest` コマンド
- `/questadmin` コマンド
- 経済MOD連携
- EconomyBridge
- アイテム消費処理
- 報酬支払い処理
- プレイヤーごとの進行状況保存処理
- 独自NPC
- 村人連携
- 討伐クエスト
- 採掘クエスト
- 探索クエスト
- 新しいクエスト種別

## 保存先

クエスト定義の保存先は以下にしてください。

```text
config/questadmin/quests.json
```

ファイルが存在しない場合は、既存のPhase 1仕様通り、サンプルクエストを1件生成してください。

サンプルクエスト例:

```text
id: wheat_delivery_001
title: 小麦の納品
description: 小麦を64個納品してください。
type: ITEM_DELIVERY
requirement.itemId: minecraft:wheat
requirement.amount: 64
reward.money: 500
repeatable: false
enabled: true
```

## 注意点

- 既存のpackage構成がある場合は、それを優先してください。
- 既存のmod idがある場合は、それを優先してください。
- ただし、既存構成がない場合は package を `net.nobu0707.questadmin` としてください。
- JSON読み込みに失敗してもサーバーをクラッシュさせないでください。
- 不正なJSONを勝手に上書きしないでください。
- 保存処理は `QuestStorage` に分離してください。
- コマンドやGUIはまだ作らないでください。
- Phase 1でビルドが通っているため、大規模な作り直しは避けてください。
- 必要最小限の修正に留めてください。

## 完了条件

- `./gradlew build` が成功する
- Fabric関連のimportや依存が残っていない
- Forge 1.20.1用MODとして起動できる構成になっている
- `config/questadmin/quests.json` の保存方針がForge向けになっている
- `quests.json` が存在しない場合、サンプルを生成できる
- 実装範囲がPhase 1.5に収まっている

## 作業後の報告形式

最後に以下の形式で報告してください。

```text
実装した内容:
- ...

変更したファイル:
- ...

ビルド結果:
- 成功 / 失敗

補足:
- ...
```
