# QuestAdmin 引き継ぎドキュメント

## 1. 現在の状態

QuestAdmin は Minecraft Forge 1.20.1 向けのクエスト管理MODです。
運営側がクエストを管理し、プレイヤーがクエストを達成すると Lightman's Currency の銀行口座へ報酬が支払われる構成です。

現在は **Phase 6 完了扱い** です。

## 2. 開発環境

| 項目 | 内容 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x 系、確認済みは 47.4.20 |
| Java | 17 |
| 開発環境 | WSL Ubuntu + VS Code |
| MOD名 | QuestAdmin |
| mod id | questadmin |
| 現在バージョン | 0.6.0 |
| 経済MOD | Lightman's Currency |
| Lightman's Currency mod id | lightmanscurrency |
| 使用jar | lightmanscurrency-1.20.1-2.3.0.4e.jar |

## 3. 重要な前提

このプロジェクトは **Forge 1.20.1 用** です。Fabric関連は使用しません。

禁止・注意対象:

- Fabric API
- FabricLoader
- ModInitializer
- fabric.mod.json
- net.fabricmc 系 import

Forge 1.20.1 では、MOD初期化は `@Mod("questadmin")` を起点にします。

## 4. 完了済みPhase

### Phase 1: MOD基盤とデータ構造

完了済み。

実装済み内容:

- QuestDefinition
- QuestType
- QuestRequirement
- QuestReward
- QuestStatus
- PlayerQuestState
- QuestStorage
- quests.json の保存・読み込み
- サンプルクエスト生成

保存先:

```text
config/questadmin/quests.json
```

### Phase 1.5: Forge 1.20.1整合確認

完了済み。

確認済み内容:

- Forge 1.20.1 対応
- Java 17 対応
- Gradle Wrapper 正常化
- pack.mcmeta 追加
- Fabric要素除去
- Forgeサーバー起動確認

### Phase 2: コマンド基盤

完了済み。

実装済みコマンド:

```text
/questadmin reload
/questadmin list
/quest list
```

### Phase 3: プレイヤー進行状況保存

完了済み。

実装済み内容:

- PlayerQuestStorage
- player_quests.json の保存・読み込み
- プレイヤーUUIDごとの状態保存
- クエストIDごとの状態保存
- COMPLETED / CLAIMED 状態保存
- 再起動後の復元確認

保存先:

```text
config/questadmin/player_quests.json
```

### Phase 4: アイテム納品クエスト処理

完了済み。

実装済みコマンド:

```text
/quest complete <questId>
```

確認済み内容:

- 必要アイテム所持数確認
- 必要アイテム消費
- COMPLETED 状態保存
- repeatable=false の再完了防止
- 不正questIdでクラッシュしない

### Phase 5A: EconomyBridge 土台

完了済み。

実装済み内容:

- EconomyBridge
- DummyEconomyBridge
- EconomyService
- `/quest claim <questId>`
- `/questadmin economy status`
- 経済MOD未接続時は CLAIMED にしない安全処理

### Phase 5B-1: Lightman's Currency 連携

完了済み。

実装済み内容:

- LightmansCurrencyEconomyBridge
- Lightman's Currency 銀行口座への直接入金
- `/quest claim <questId>` による報酬受け取り
- 入金成功時のみ CLAIMED に変更
- 二重受け取り防止
- mods.toml に lightmanscurrency 依存追加

重要な修正履歴:

- `mods.toml` の `versionRange=""` が原因でForge起動時に依存エラーが発生した。
- `versionRange="[0,)"` に修正して起動成功。
- Lightman's Currency がサーバー `mods` フォルダに入っていない場合、QuestAdminは起動しない。

### Phase 6: プレイヤー用GUI

完了扱い。

実装済み内容:

- `/quest` でプレイヤー用GUIを開く
- GUIからクエスト一覧を確認
- GUIからクエスト完了処理
- GUIから報酬受け取り処理
- クエスト状態表示
  - 未完了
  - 完了済み
  - 報酬受取済み
- `/quest complete` 後のメッセージ修正
- MODバージョン 0.1.0 → 0.6.0

## 5. 現在の主要コマンド

### プレイヤー用

```text
/quest
/quest list
/quest complete <questId>
/quest claim <questId>
```

### 管理者用

```text
/questadmin reload
/questadmin list
/questadmin progress <player>
/questadmin progress mark <player> <questId> <status>
/questadmin economy status
```

`/questadmin economy status` の期待例:

```text
bridge: LightmansCurrencyEconomyBridge
available: true
currency: lightmanscurrency
```

## 6. 重要なファイル

リポジトリ直下:

```text
SPEC.md
ROADMAP.md
CLAUDE.md
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
~/servers/forge-1.20.1/mods/questadmin-0.6.0.jar
~/servers/forge-1.20.1/mods/lightmanscurrency-1.20.1-2.3.0.4e.jar
```

## 7. 別PCへ移行するときに必要なもの

必須:

- Java 17
- Git
- VS Code
- WSL Ubuntu
- Gradle Wrapper一式
  - gradlew
  - gradlew.bat
  - gradle/wrapper/gradle-wrapper.jar
  - gradle/wrapper/gradle-wrapper.properties
- プロジェクトソース一式
- `libs/lightmanscurrency-1.20.1-2.3.0.4e.jar`

注意:

`libs/` が `.gitignore` で除外されている場合、Lightman's Currency jar はGitHubに上がりません。
別PCでは手動で `libs/lightmanscurrency-1.20.1-2.3.0.4e.jar` を配置してください。

## 8. 別PCでの復元手順

### 1. Java 17確認

```bash
java -version
javac -version
```

### 2. リポジトリ取得

```bash
cd ~/projects
git clone <QuestAdminのGitHub URL>
cd QuestAdmin
```

### 3. Lightman's Currency jar配置

```bash
mkdir -p libs
```

以下を配置:

```text
libs/lightmanscurrency-1.20.1-2.3.0.4e.jar
```

### 4. ビルド確認

```bash
./gradlew --version
./gradlew clean build
```

### 5. jar確認

```bash
ls -la build/libs
jar tf build/libs/*.jar | grep -E "mods.toml|pack.mcmeta"
```

### 6. Fabric要素確認

```bash
grep -R "fabric\|FabricLoader\|ModInitializer\|net.fabricmc" -n src build.gradle settings.gradle gradle.properties 2>/dev/null
```

基本的にはヒットなしが理想です。

### 7. バージョン確認

```bash
grep -R "0.6.0" -n gradle.properties build.gradle src/main/resources/META-INF/mods.toml README.md 2>/dev/null
grep -R "0.1.0" -n . --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git
```

## 9. サーバー起動確認

Forgeサーバーの `mods` フォルダに以下を入れます。

```text
questadmin-0.6.0.jar
lightmanscurrency-1.20.1-2.3.0.4e.jar
```

起動後、以下を確認します。

```text
/questadmin economy status
/questadmin reload
/questadmin list
/quest
```

## 10. Phase 6後の手動確認手順

1. サーバー起動
2. クライアント接続
3. `/questadmin economy status`
4. `available=true` を確認
5. `/quest` でGUIが開くことを確認
6. 小麦不足状態で wheat_delivery_001 をクリックし、完了できないことを確認
7. `/give @p minecraft:wheat 64`
8. GUIから wheat_delivery_001 を完了
9. 状態が COMPLETED になることを確認
10. GUIから報酬受け取り
11. Lightman's Currency 銀行口座に入金されることを確認
12. 状態が CLAIMED になることを確認
13. 再度クリックして二重受け取りできないことを確認
14. サーバー再起動
15. CLAIMED 状態が維持されることを確認

## 11. 既知の注意点

### Lightman's Currency の残高表記

銀行口座で `1e5g` のような表記が出る場合があります。
これは異常とは限りません。
Lightman's Currency はコイン単位を組み合わせて表示します。

重要なのは以下です。

- claim 前後で報酬額相当だけ増えているか
- `/quest claim` の2回目で増えないか
- CLAIMED状態が保存されるか

### mods.toml の Lightman's Currency 依存

依存定義には必ず versionRange を入れてください。

推奨:

```toml
[[dependencies.questadmin]]
    modId="lightmanscurrency"
    mandatory=true
    versionRange="[0,)"
    ordering="AFTER"
    side="BOTH"
```

`versionRange=""` だと、Lightman's Currency が入っていても Forge の依存解決でクラッシュします。

### サーバーとクライアントのMOD

Lightman's Currency はサーバー・クライアント両方に入れる前提で扱ってください。
QuestAdminもGUIを実装したため、マルチ環境ではクライアント側導入が必要になる可能性が高いです。

## 12. 次のPhase候補

次は **Phase 7：管理者用クエスト一覧GUI** が候補です。

目的:

- `/questadmin` で管理者用GUIを開く
- 登録済みクエスト一覧をGUI表示
- 有効/無効状態表示
- 削除ボタン
- 新規作成画面への導線

Phase 7ではまだ本格的な作成フォームは作らず、管理者がGUIで一覧・削除・有効状態を見られるところまでにすると安全です。

## 13. 次回作業前チェック

別PCで作業を始める前に、以下を確認してください。

```bash
cd ~/projects/QuestAdmin
git status
./gradlew clean build
grep -R "fabric\|FabricLoader\|ModInitializer\|net.fabricmc" -n src build.gradle settings.gradle gradle.properties 2>/dev/null
```

サーバー起動後:

```text
/questadmin economy status
/questadmin reload
/quest
```

ここまで問題なければ、Phase 7へ進めます。
