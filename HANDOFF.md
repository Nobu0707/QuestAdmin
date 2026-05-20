# QuestAdmin 引き継ぎドキュメント

## 1. 現在の状態

QuestAdmin は Minecraft Forge 1.20.1 向けのクエスト管理MODです。
運営側がクエストを管理し、プレイヤーがクエストを達成すると Lightman's Currency の銀行口座へ報酬が支払われます。

現在は **Phase 11.5 完了 / v1.0.4 安定化版** です。

## 2. 対応環境

| 項目 | 内容 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Java | 17 |
| MOD名 | QuestAdmin |
| mod id | questadmin |
| 現在バージョン | 1.0.4 |
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
- Phase 11.1: 報酬claim安全化 / CLAIMING 状態導入
- Phase 11.2: リポジトリ衛生修正 / 保存I/O安全化
- Phase 11.3: クエストデータ検証強化 / repeatable未対応明確化
- Phase 11.4: GUIページング / 所持数計算キャッシュ / questId検索index
- Phase 11.5: セッションcleanup / 納品対象安全化 / チャット一覧ページング

## 5. 実装済み機能一覧

- `QuestDefinition` / `QuestRequirement` / `QuestReward` / `QuestType` / `QuestStatus`
- `quests.json` の保存・読み込み
- `player_quests.json` の保存・読み込み
- ITEM_DELIVERY クエスト
- 必要アイテム所持数確認
- 完了時の必要アイテム消費
- COMPLETED / CLAIMING / CLAIMED 状態保存
- repeatable=false の再完了防止
- Lightman's Currency 銀行口座への報酬入金
- 報酬claim時は先に CLAIMING を保存し、入金成功時のみ CLAIMED へ変更
- 二重claim防止
- プレイヤー用GUI
- 管理者用GUI
- プレイヤー用GUI/管理者用GUIのページング
- プレイヤーGUI表示用の所持数計算キャッシュ
- QuestStorageのquestId検索インデックス
- ログアウト時/サーバー停止時の作成・編集セッションcleanup
- 通常インベントリ/オフハンドのみを対象にしたアイテム納品
- `/quest list` / `/questadmin list` のチャット一覧ページング
- enabled の有効/無効切替
- 削除確認GUI経由のクエスト削除
- チャット入力ステップ方式のクエスト作成
- チャット入力ステップ方式のクエスト編集
- クエスト作成/編集時の共通バリデーション
- `quests.json` 読み込み時の不正クエストスキップとwarnログ出力
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
/quest list <page>
/quest complete <questId>
/quest claim <questId>
```

### 管理者用

OP権限レベル2以上が必要です。

```text
/questadmin
/questadmin reload
/questadmin list
/questadmin list <page>
/questadmin sessions
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

このjarはGit管理しません。別PCへ移行した場合は、開発環境の `libs/` に手動配置してください。

サーバー側 mods 例:

```text
questadmin-1.0.4.jar
lightmanscurrency-1.20.1-2.3.0.4e.jar
```

QuestAdmin jar に Lightman's Currency 本体は同梱しません。配布時は QuestAdmin jar と Lightman's Currency jar を別々に `mods` へ入れます。

## 10. ビルド手順

```bash
./gradlew clean build
```

成果物:

```text
build/libs/questadmin-1.0.4.jar
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
6. `build/libs/questadmin-1.0.4.jar` が生成されることを確認します。
7. `jar tf` で `META-INF/mods.toml` と `pack.mcmeta` を確認します。
8. Fabric要素確認grepを実行します。

`libs/` が `.gitignore` で除外されている場合、Lightman's Currency jar はGitHubに上がりません。
別PCでは手動で配置してください。

## 12. 実機確認手順

1. Forge 1.20.1 サーバーを用意します。
2. サーバーの `mods` フォルダへ Lightman's Currency を入れます。
3. サーバーの `mods` フォルダへ `questadmin-1.0.4.jar` を入れます。
4. クライアント側にも Lightman's Currency と QuestAdmin を入れます。
5. サーバーを起動して接続します。
6. `/questadmin economy status` で `available=true` を確認します。
7. `/questadmin` で管理者GUIが開くことを確認します。
8. `/quest` でプレイヤーGUIが開くことを確認します。
9. クエストを新規作成できることを確認します。
10. 作成したクエストを編集できることを確認します。
11. enabled 切替が反映されることを確認します。
12. 削除確認付きで削除できることを確認します。
13. `/quest list` に有効クエストのみがページ表示されることを確認します。
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
- 報酬claim時は先に `CLAIMING` を保存し、入金成功時のみ `CLAIMED` に変更します。

## 14. 既知の制限

- 対応クエスト種別は `ITEM_DELIVERY` のみです。
- `repeatable=true` は現時点では未対応です。クエスト定義では `repeatable=false` を使用してください。
- 報酬は Lightman's Currency の銀行口座入金のみです。
- 既存クエストの `id` と `type` は編集できません。
- 既存 progress はクエスト削除・編集時にも変更しません。
- Lightman's Currency 以外の経済MODには対応していません。
- Web管理画面、MySQL保存、村人/NPC連携は未実装です。
- Lightman's Currency の銀行口座表示で `1e5g` のような表記になる場合がありますが、残高増減と二重claim防止を確認してください。

## 15. Phase 11.2 メモ

- `quests.json` / `player_quests.json` の保存処理は一時ファイルへ書き込んでから置換します。
- `ATOMIC_MOVE` 非対応環境ではログを出して非atomic置換へfallbackします。
- 保存失敗時は例外つきでログに出し、サーバーをクラッシュさせない既存方針を維持します。
- `build/`、`.gradle/`、外部MOD jar、生成jar、レビュー用生成物はGit管理しません。

## 16. Phase 11.3 メモ

- `QuestValidator` でクエスト定義、作成入力、編集入力、保存前検証を共通化しています。
- `quests.json` 読み込み時、不正なクエストはスキップし、理由をwarnログへ出します。
- JSON構文不正やファイルI/O失敗時は、既存のメモリ上クエストを維持します。
- `questId` 重複、不正な `itemId`、範囲外の `amount` / `reward money`、`ITEM_DELIVERY` 以外のtype、`repeatable=true` は無効扱いです。

## 17. Phase 11.4 メモ

- `/quest` と `/questadmin` の一覧GUIは45件ごとのページングに対応しています。
- プレイヤーGUIの所持数表示は、GUI更新時にインベントリを1回集計したSnapshotを参照します。
- `QuestStorage` は読み込み・保存後にquestId検索インデックスを再構築し、主要なID検索は `findById` / `exists` を使います。

## 18. Phase 11.5 メモ

- プレイヤーログアウト時に、未保存のクエスト作成/編集セッションを破棄します。
- サーバー停止時に、作成/編集セッションMapをclearします。
- アイテム納品対象は通常インベントリとオフハンドです。防具欄はGUI所持数表示、完了判定、消費処理の対象外です。
- `/quest list` と `/questadmin list` は1ページ10件表示です。ページ指定は `/quest list 2` / `/questadmin list 2` です。
- `/questadmin sessions` で現在の作成/編集セッション数を確認できます。

## 19. 次期Phase候補

- Phase 11.6 非同期保存 / debounce保存の設計検討
- JSONバックアップ/リストア補助
- 新しいクエスト種別の設計検討
- 村人/NPC連携の設計検討

Phase 11.5時点では、新機能追加ではなく v1.0.4 安定化対応までを完了しています。
