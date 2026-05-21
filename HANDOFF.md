# QuestAdmin 引き継ぎドキュメント

## 現在のバージョン

QuestAdmin は Minecraft Forge 1.20.1 向けのクエスト管理MODです。
運営側がクエストを管理し、プレイヤーがクエストを達成すると Lightman's Currency の銀行口座へ報酬が支払われます。

現在は **Phase 11.8 完了 / v1.0.7 安定化版** です。

| 項目 | 内容 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Java | 17 |
| MOD名 | QuestAdmin |
| mod id | questadmin |
| 現在バージョン | 1.0.7 |
| 必須経済MOD | Lightman's Currency |
| Lightman's Currency mod id | lightmanscurrency |
| 開発時参照jar | lightmanscurrency-1.20.1-2.3.0.4e.jar |

このプロジェクトは **Forge 1.20.1 用** です。Fabric API、FabricLoader、ModInitializer、fabric.mod.json、net.fabricmc 系 import は使用しません。

## 完了済みPhase一覧

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
- Phase 11.6: 保存I/O計測 / 遅い保存警告 / 保存方針ドキュメント
- Phase 11.7: 保存バックアップ / バックアップ一覧 / 保存ファイル検証
- Phase 11.8: 運用ドキュメント / 実機確認手順 / 既知制限 / リリース準備

## 現在の安定化状況

v1.0.7 は新機能追加ではなく、v1.0.x 安定化版として運用前チェックを整えた状態です。

実装済みの安定化ポイント:

- `CLAIMING` 保存後に Lightman's Currency へ入金し、成功時のみ `CLAIMED` 保存
- `CLAIMING` / `CLAIMED` の再claim拒否
- 通常インベントリ/オフハンドのみを納品対象化
- ログアウト時/サーバー停止時の作成・編集セッションcleanup
- GUIページング、チャット一覧ページング
- 所持数計算キャッシュ、questId検索インデックス
- `QuestValidator` による作成/編集/読み込み時検証
- 保存I/O計測、50ms/200ms warn
- `/questadmin storage status`
- `/questadmin storage backup`
- `/questadmin storage backups`
- `/questadmin storage validate`
- 運用ランブック、テストチェックリスト、既知制限ドキュメント

## コマンド一覧

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

## Lightman's Currency jarの扱い

開発時参照jar:

```text
libs/lightmanscurrency-1.20.1-2.3.0.4e.jar
```

このjarはGit管理しません。`libs/*.jar`、生成jar、`build/`、`.gradle/` は `.gitignore` 対象です。

実行時はサーバーとクライアントの `mods/` に QuestAdmin jar と Lightman's Currency jar を別々に配置します。

```text
mods/questadmin-1.0.7.jar
mods/lightmanscurrency-1.20.1-2.3.0.4e.jar
```

QuestAdmin jar に Lightman's Currency 本体は同梱しません。

## 別PC移行手順

1. Java 17 を用意します。
2. リポジトリを取得します。
3. Gradle Wrapper 一式があることを確認します。
4. `libs/lightmanscurrency-1.20.1-2.3.0.4e.jar` を手動配置します。
5. `./gradlew clean build` を実行します。
6. `build/libs/questadmin-1.0.7.jar` が生成されることを確認します。
7. `jar tf build/libs/*.jar | grep -E "mods.toml|pack.mcmeta"` を実行します。
8. Fabric関連grepを実行します。

## ビルド手順

```bash
./gradlew clean build
```

成果物:

```text
build/libs/questadmin-1.0.7.jar
```

jar確認:

```bash
jar tf build/libs/*.jar | grep -E "mods.toml|pack.mcmeta"
unzip -p build/libs/*.jar META-INF/mods.toml | grep -E "modId|version|lightmanscurrency" -n
```

Git管理除外確認:

```bash
git ls-files ".gradle/*" "build/*" "libs/*.jar" "*.jar"
```

Fabric要素確認:

```bash
grep -R "fabric\|FabricLoader\|ModInitializer\|net.fabricmc" -n src build.gradle settings.gradle gradle.properties 2>/dev/null
```

## 実機確認手順

1. Forge 1.20.1 サーバーを用意します。
2. サーバーの `mods/` へ Lightman's Currency を入れます。
3. サーバーの `mods/` へ `questadmin-1.0.7.jar` を入れます。
4. クライアント側にも Lightman's Currency と QuestAdmin を入れます。
5. サーバーを起動して接続します。
6. `/questadmin economy status` で `available=true` を確認します。
7. `/questadmin storage status` で保存I/O計測状態を確認します。
8. `/questadmin storage backup` でバックアップを確認します。
9. `/questadmin storage backups` でバックアップ一覧を確認します。
10. `/questadmin storage validate` で正常ファイルがOK表示されることを確認します。
11. `/questadmin` で管理者GUIが開くことを確認します。
12. `/quest` でプレイヤーGUIが開くことを確認します。
13. クエスト作成、編集、enabled切替、削除を確認します。
14. `/quest list <page>` と `/questadmin list <page>` を確認します。
15. `/quest complete <questId>` と `/quest claim <questId>` を確認します。
16. 二重claimできないことを確認します。
17. `latest.log` に QuestAdmin 由来 ERROR がないことを確認します。

詳細な確認項目は `docs/TEST_CHECKLIST.md` を参照してください。

## 保存ファイル

```text
config/questadmin/quests.json
config/questadmin/player_quests.json
config/questadmin/backups/
```

`/questadmin storage backup` は `quests.json` と `player_quests.json` をバックアップします。初回直後などで `player_quests.json` が未生成の場合、そのファイルだけFAILEDになることがあります。

QuestAdminは自動復元や復元コマンドを提供しません。復元はサーバー停止中に手動で行います。

保存方針の詳細は `docs/STORAGE_IO_STRATEGY.md` です。

## 既知の制限

- 対応クエスト種別は `ITEM_DELIVERY` のみ
- `repeatable=true` は未対応
- Lightman's Currency 必須
- 他経済MOD非対応
- 村人NPC、討伐、採掘、探索、デイリー、Web管理画面、MySQL保存は未実装
- `complete` / `claim` は同期保存
- バックアップ復元は手動
- 防具欄は納品対象外
- NBT差分は未判定

詳細は `docs/KNOWN_LIMITATIONS.md` を参照してください。

## review archive生成手順

レビュー提出用アーカイブが必要な場合は、既存スクリプトを使用できます。

```bash
bash scripts/make-review-archive.sh
bash scripts/make-review-archive.sh "Document QuestAdmin operations and release readiness"
```

生成物は `questadmin-review-*.tar.gz` と `questadmin-review-latest.tar.gz` です。これらはGit管理しません。

## 次の候補Phase

- v1.0.7 運用テスト
- Phase 12 新機能検討
- 新しいクエスト種別の設計検討
- 村人/NPC連携の設計検討
- MySQL保存やWeb管理画面の設計検討

Phase 11.8時点では、新機能追加ではなく v1.0.7 の運用準備までを完了しています。
