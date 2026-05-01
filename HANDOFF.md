# QuestAdmin 引き継ぎドキュメント

## 1. 現在の状態

QuestAdmin は Minecraft Forge 1.20.1 向けのクエスト管理MODです。
運営側がクエストを管理し、プレイヤーがクエストを達成すると Lightman's Currency の銀行口座へ報酬が支払われます。

現在は **Phase 10 完了 / v1.0.0 MVP完成版** です。

## 2. 対応環境

| 項目 | 内容 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Java | 17 |
| MOD名 | QuestAdmin |
| mod id | questadmin |
| 現在バージョン | 1.0.0 |
| 必須経済MOD | Lightman's Currency |
| Lightman's Currency mod id | lightmanscurrency |
| 開発時参照jar | lightmanscurrency-1.20.1-2.3.0.4e.jar |

## 3. 開発環境

- WSL Ubuntu
- VS Code
- Gradle Wrapper
- Java 17
- ForgeGradle 6.x

このプロジェクトは **Forge 1.20.1 用** です。Fabric関連は使用しません。

禁止・注意対象:

- Fabric API
- FabricLoader
- ModInitializer
- fabric.mod.json
- net.fabricmc 系 import

Forge 1.20.1 では、MOD初期化は `@Mod("questadmin")` を起点にします。

## 4. 完了済みPhase一覧

- Phase 1: MOD基盤とデータ構造
- Phase 1.5: Forge 1.20.1 整合確認
- Phase 2: コマンド基盤
- Phase 3: プレイヤー進行状況保存
- Phase 4: アイテム納品クエスト処理
- Phase 5A: EconomyBridge 土台
- Phase 5B-1: Lightman's Currency 連携
- Phase 6: プレイヤー用GUI
- Phase 7: 管理者用クエスト一覧GUI
- Phase 8: ITEM_DELIVERY クエスト作成
- Phase 9: 既存クエスト編集とMVP安定化
- Phase 10: v1.0.0 MVP配布準備

## 5. 実装済み機能一覧

- `QuestDefinition` / `QuestRequirement` / `QuestReward` / `QuestType` / `QuestStatus`
- `quests.json` の保存・読み込み
- `player_quests.json` の保存・読み込み
- ITEM_DELIVERY クエスト
- 必要アイテム所持数確認
- 完了時の必要アイテム消費
- COMPLETED / CLAIMED 状態保存
- repeatable=false の再完了防止
- Lightman's Currency 銀行口座への報酬入金
- 入金成功時のみ CLAIMED へ変更
- 二重claim防止
- プレイヤー用GUI
- 管理者用GUI
- enabled の有効/無効切替
- 削除確認GUI経由のクエスト削除
- チャット入力ステップ方式のクエスト作成
- チャット入力ステップ方式のクエスト編集
- 作成/編集キャンセル
- 管理者操作のOP権限レベル2確認

## 6. 未実装機能一覧

- 新しいクエスト種別
- 討伐クエスト
- 採掘クエスト
- 探索クエスト
- デイリークエスト
- クエストツリー
- 前提クエスト
- 村人/NPC連携
- Web管理画面
- MySQL保存
- 複数経済MOD対応
- Lightman's Currency 以外の経済MOD対応

## 7. コマンド一覧

### プレイヤー用

```text
/quest
/quest list
/quest complete <questId>
/quest claim <questId>
```

### 管理者用

OP権限レベル2以上が必要です。

```text
/questadmin
/questadmin reload
/questadmin list
/questadmin economy status
/questadmin progress <player>
/questadmin progress mark <player> <questId> <status>
/questadmin edit <questId>
/questadmin edit cancel
/questadmin create cancel
```

`/questadmin economy status` の期待例:

```text
bridge: LightmansCurrencyEconomyBridge
available: true
currency: lightmanscurrency
```

## 8. 保存ファイル

クエスト定義:

```text
config/questadmin/quests.json
```

プレイヤー進行状況:

```text
config/questadmin/player_quests.json
```

手動でJSONを編集する場合はバックアップを推奨します。

## 9. 重要なファイル

リポジトリ直下:

```text
SPEC.md
ROADMAP.md
CLAUDE.md
README.md
HANDOFF.md
RELEASE_NOTES.md
```

リソース:

```text
src/main/resources/META-INF/mods.toml
src/main/resources/pack.mcmeta
```

Lightman's Currency 開発時参照:

```text
libs/lightmanscurrency-1.20.1-2.3.0.4e.jar
```

サーバー側 mods 例:

```text
questadmin-1.0.0.jar
lightmanscurrency-1.20.1-2.3.0.4e.jar
```

QuestAdmin jar に Lightman's Currency 本体は同梱しません。

## 10. ビルド手順

```bash
./gradlew clean build
```

成果物:

```text
build/libs/questadmin-1.0.0.jar
```

jar確認:

```bash
jar tf build/libs/*.jar | grep -E "mods.toml|pack.mcmeta"
unzip -p build/libs/*.jar META-INF/mods.toml | grep -E "modId|version|lightmanscurrency" -n
```

Fabric要素確認:

```bash
grep -R "fabric\|FabricLoader\|ModInitializer\|net.fabricmc" -n src build.gradle settings.gradle gradle.properties 2>/dev/null
```

## 11. 別PC移行時の確認手順

1. Java 17 を用意します。
2. リポジトリを取得します。
3. Gradle Wrapper 一式があることを確認します。
4. `libs/lightmanscurrency-1.20.1-2.3.0.4e.jar` を配置します。
5. `./gradlew clean build` を実行します。
6. `build/libs/questadmin-1.0.0.jar` が生成されることを確認します。
7. `jar tf` で `META-INF/mods.toml` と `pack.mcmeta` を確認します。
8. Fabric要素確認grepを実行します。

`libs/` が `.gitignore` で除外されている場合、Lightman's Currency jar はGitHubに上がりません。
別PCでは手動で配置してください。

## 12. 実機確認手順

1. Forge 1.20.1 サーバーを用意します。
2. サーバーの `mods` フォルダへ Lightman's Currency を入れます。
3. サーバーの `mods` フォルダへ `questadmin-1.0.0.jar` を入れます。
4. クライアント側にも Lightman's Currency と QuestAdmin を入れます。
5. サーバーを起動して接続します。
6. `/questadmin economy status` で `available=true` を確認します。
7. `/questadmin` で管理者GUIが開くことを確認します。
8. `/quest` でプレイヤーGUIが開くことを確認します。
9. クエストを新規作成できることを確認します。
10. 作成したクエストを編集できることを確認します。
11. enabled 切替が反映されることを確認します。
12. 削除確認付きで削除できることを確認します。
13. `/quest list` に有効クエストのみ表示されることを確認します。
14. `/quest complete <questId>` で完了できることを確認します。
15. `/quest claim <questId>` で報酬が入金されることを確認します。
16. 二重claimできないことを確認します。
17. 再起動後も `quests.json` と `player_quests.json` が維持されることを確認します。
18. `latest.log` に questadmin 由来の ERROR が出ていないことを確認します。

## 13. Lightman's Currency連携状況

- `mods.toml` に `lightmanscurrency` の必須依存を定義済みです。
- `versionRange="[0,)"` を指定しています。
- Lightman's Currency がサーバー `mods` フォルダに入っていない場合、QuestAdminは起動しません。
- 報酬は Lightman's Currency 銀行口座へ直接入金します。
- 入金成功時のみ `CLAIMED` に変更します。

## 14. 既知の制限

- 対応クエスト種別は `ITEM_DELIVERY` のみです。
- 報酬は Lightman's Currency の銀行口座入金のみです。
- 既存クエストの `id` と `type` は編集できません。
- 既存 progress はクエスト削除・編集時にも変更しません。
- Lightman's Currency 以外の経済MODには対応していません。
- Web管理画面、MySQL保存、村人/NPC連携は未実装です。
- Lightman's Currency の銀行口座表示で `1e5g` のような表記になる場合がありますが、残高増減と二重claim防止を確認してください。

## 15. 次期Phase候補

- 実機テスト結果に基づく不具合修正
- 管理者GUIのページングや検索
- クエスト一覧の表示改善
- JSONバックアップ/リストア補助
- 新しいクエスト種別の設計検討
- 村人/NPC連携の設計検討

Phase 10時点では、新機能追加ではなく v1.0.0 MVP 配布準備までを完了しています。
