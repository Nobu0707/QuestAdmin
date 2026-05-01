# Phase 7 実装プロンプト

あなたはForge 1.20.1 MOD「QuestAdmin」の実装担当です。

このプロジェクトでは、仕様整理・設計・作業指示はGPT Thinkingが担当し、実装・ビルド・修正はClaudeが担当します。

まず、リポジトリ直下にある以下のファイルを必ず読んでください。

- SPEC.md
- ROADMAP.md
- CLAUDE.md
- HANDOFF.md

Phase 1、Phase 1.5、Phase 2、Phase 3、Phase 4、Phase 5A、Phase 5B-1、Phase 6 は完了済みです。

今回の作業は Phase 7 です。

# 今回の目的

管理者用クエスト一覧GUIを実装してください。

Phase 7では、管理者が `/questadmin` でGUIを開き、登録済みクエストを一覧確認できるようにします。

今回はまだ本格的なクエスト作成フォーム・編集フォームは実装しないでください。

# 重要な前提

- Minecraft: 1.20.1
- Forge: 47.x
- Java: 17
- mod id: questadmin
- 現在バージョン: 0.9.0
- 使用経済MOD: Lightman's Currency
- Lightman's Currency modId: lightmanscurrency
- プレイヤー用GUIはPhase 6で実装済み
- Lightman's Currency 銀行口座入金は確認済み
- 二重受け取り防止は確認済み

# 実装してよい内容

以下を実装してください。

- `/questadmin` で管理者用GUIを開く
- 登録済みクエスト一覧をGUI表示
- クエストID表示
- タイトル表示
- type表示
- enabled状態表示
- repeatable状態表示
- required item表示
- required amount表示
- reward money表示
- クエスト削除ボタン
- enabled切り替えボタン
- 新規作成画面への導線ボタン

ただし、新規作成画面の中身は今回作らなくて構いません。
クリック時に「クエスト作成GUIは次のPhaseで実装予定です」と表示するだけでも可です。

# コマンド仕様

既存の `/questadmin reload` と `/questadmin list` は維持してください。

`/questadmin` 単体は管理者用GUIを開くようにしてください。

管理者コマンドは permission level 2 以上にしてください。

# GUI方針

Phase 6 のGUI実装方針に合わせてください。

可能であればチェスト風GUIを使用してください。

表示例:

- 紙: 通常クエスト
- エメラルド: enabled=true
- バリア: enabled=false
- レバー相当アイコン: 有効/無効切り替え
- 本: 新規作成導線

過剰に凝ったGUIにはしないでください。

# 削除機能

クエスト削除を実装する場合、誤削除防止のため、以下のどちらかにしてください。

案A:
クリック1回目で確認状態にし、もう一度クリックで削除

案B:
Shiftクリック時のみ削除

案C:
今回は削除ボタンは表示だけにして、削除実装は次Phaseへ回す

安全性を優先するなら、案Aまたは案Cで構いません。

# enabled切り替え

enabled=true / false をGUIから切り替えられるようにしてください。

切り替え後は quests.json に保存してください。

保存失敗時は、状態を戻すかエラー表示してください。

# 実装しない内容

以下は今回実装しないでください。

- 本格的なクエスト新規作成GUI
- クエスト編集GUI
- テキスト入力GUI
- 討伐クエスト
- 採掘クエスト
- 探索クエスト
- デイリークエスト
- クエストツリー
- 前提クエスト
- Web連携
- MySQL保存
- 複数経済MOD対応
- Lightman's Currency以外の経済MOD対応
- 村人/NPC連携

# 既存機能を壊さないこと

以下は必ず維持してください。

- `/quest`
- `/quest list`
- `/quest complete <questId>`
- `/quest claim <questId>`
- `/questadmin reload`
- `/questadmin list`
- `/questadmin progress`
- `/questadmin economy status`
- Lightman's Currency 銀行口座入金
- player_quests.json 保存
- quests.json 保存
- プレイヤー用GUI

# 安全ルール

- 管理者GUIはOP権限レベル2以上のみ開ける
- 一般プレイヤーが `/questadmin` を実行しても開けない
- GUI操作時もサーバー側で権限確認する
- 削除やenabled切り替え時に不正questIdでクラッシュしない
- quests.json 保存失敗時にサーバーをクラッシュさせない
- 不正JSONを勝手に上書きしない
- Phase外の機能を追加しない

# 推奨するクラス構成

既存構成に合わせつつ、必要であれば以下を追加してください。

- QuestAdminMenu
- QuestAdminMenuProvider
- QuestAdminMenuScreen
- QuestAdminGuiService
- QuestAdminMenuItemFactory

ただし、Phase 6で既に共通GUI基盤がある場合はそれを再利用してください。

# バージョン

今回、バージョン変更は必須ではありません。

Phase 9で 0.9.0 に更新済みです。

Phase 9の更新指示に合わせて 0.9.0 にしてください。

# 完了条件

- ./gradlew clean build が成功する
- `/questadmin` で管理者用GUIが開く
- 一般プレイヤーは管理者用GUIを開けない
- 登録済みクエストがGUIに表示される
- enabled状態が表示される
- enabled切り替えができる、または未実装として明確に表示される
- 削除機能を実装した場合、誤削除防止がある
- 新規作成導線がある
- 既存のプレイヤー用GUIが壊れていない
- Lightman's Currency連携が壊れていない
- latest.log に questadmin由来のERRORが出ない
- Phase外の機能を追加していない
- Fabric関連のimportや依存が残っていない

# 作業後に実行するコマンド

必ず以下を実行してください。

```bash
./gradlew clean build
```

可能であれば以下も確認してください。

```bash
grep -R "fabric\|FabricLoader\|ModInitializer\|net.fabricmc" -n src build.gradle settings.gradle gradle.properties 2>/dev/null
```

jarの中身も確認してください。

```bash
jar tf build/libs/*.jar | grep -E "mods.toml|pack.mcmeta"
```

# 実機確認の観点

ビルド後、Forge 1.20.1環境で以下を確認してください。

1. Lightman's Currency を mods フォルダに入れる
2. QuestAdmin を mods フォルダに入れる
3. サーバーを起動する
4. クライアント側にも必要なMODを入れて接続する
5. OP権限で `/questadmin` を実行する
6. 管理者GUIが開くことを確認する
7. wheat_delivery_001 が表示されることを確認する
8. enabled状態が分かることを確認する
9. enabled切り替えを実装した場合、切り替え後に `/quest list` の表示が変わることを確認する
10. 一般プレイヤーで `/questadmin` を実行し、拒否されることを確認する
11. `/quest` のプレイヤー用GUIが壊れていないことを確認する
12. `/quest claim` が壊れていないことを確認する

# 作業後の報告形式

最後に以下の形式で報告してください。

実装した内容:
- ...

変更したファイル:
- ...

追加したGUI:
- ...

追加・変更したコマンド:
- ...

削除機能:
- 実装 / 未実装
- 実装した場合の誤削除防止方法:

enabled切り替え:
- 実装 / 未実装

Lightman's Currency連携:
- 維持 / 問題あり
- 補足:

ビルド結果:
- 成功 / 失敗

実行した確認:
- ...

未確認事項:
- ...

補足:
- ...
