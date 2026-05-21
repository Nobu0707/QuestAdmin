# QuestAdmin テストチェックリスト

## ビルド確認

- [ ] `./gradlew clean build`
- [ ] `build/libs/questadmin-1.0.7.jar` が生成される
- [ ] `jar tf build/libs/*.jar | grep -E "mods.toml|pack.mcmeta"` で両方が見える
- [ ] `.gradle/`、`build/`、`libs/*.jar`、リポジトリ直下の生成 `*.jar` がGit管理されていない
- [ ] `gradle/wrapper/gradle-wrapper.jar` はGradle WrapperとしてGit管理されている
- [ ] Fabric関連の import / 依存が残っていない

## 起動確認

- [ ] Forge 1.20.1 サーバーが起動する
- [ ] Lightman's Currency 導入済み
- [ ] QuestAdmin 導入済み
- [ ] `latest.log` に QuestAdmin 由来 ERROR がない
- [ ] `/questadmin economy status` で Lightman's Currency が available

## 管理者機能確認

- [ ] `/questadmin`
- [ ] `/questadmin list`
- [ ] `/questadmin list <page>`
- [ ] `/questadmin reload`
- [ ] `/questadmin economy status`
- [ ] `/questadmin sessions`
- [ ] `/questadmin storage status`
- [ ] `/questadmin storage backup`
- [ ] `/questadmin storage backups`
- [ ] `/questadmin storage validate`
- [ ] クエスト作成
- [ ] クエスト編集
- [ ] クエスト削除
- [ ] enabled 切替
- [ ] `/questadmin progress <player>`
- [ ] `/questadmin progress mark <player> <questId> <status>`

## プレイヤー機能確認

- [ ] `/quest`
- [ ] `/quest list`
- [ ] `/quest list <page>`
- [ ] `/quest complete <questId>`
- [ ] `/quest claim <questId>`
- [ ] GUIから完了
- [ ] GUIから報酬受け取り
- [ ] 二重claim不可

## 安全性確認

- [ ] Lightman's Currency 未導入時に依存エラーで起動しない
- [ ] 不正 `questId` が拒否される
- [ ] 不正 `itemId` が拒否される
- [ ] 不正 `amount` が拒否される
- [ ] `repeatable=true` が拒否または未対応表示される
- [ ] `CLAIMING` 状態の再claimが拒否される
- [ ] `CLAIMED` 状態の再claimが拒否される
- [ ] 防具欄が納品対象外
- [ ] ログアウト時に作成/編集セッションがcleanupされる
- [ ] サーバー停止時に作成/編集セッションがcleanupされる
- [ ] `/questadmin storage backup` が動作する
- [ ] `/questadmin storage validate` が正常ファイルをOK表示する
- [ ] `player_quests.json` 未生成時の `storage backup` は該当ファイルのみFAILEDになり、既存ファイルのバックアップ結果が確認できる

## 50人以上想定の確認

- [ ] 複数プレイヤーが `/quest` を開く
- [ ] 複数プレイヤーが `/quest list` を使う
- [ ] クエスト数55件以上でGUIページングを確認する
- [ ] クエスト数55件以上で `/quest list <page>` と `/questadmin list <page>` を確認する
- [ ] `complete` / `claim` の同時実行で二重支払いがない
- [ ] `/questadmin storage status` で保存時間を見る
- [ ] 保存50ms / 200ms warnの発生頻度を確認する
- [ ] `latest.log` に過剰な保存warnやQuestAdmin ERRORがない

## リリース前チェック

- [ ] README のバージョン、jar名、コマンド一覧が実装と一致している
- [ ] HANDOFF のPhase状態が最新
- [ ] RELEASE_NOTES に対象バージョンがある
- [ ] `docs/OPERATIONS_RUNBOOK.md` がある
- [ ] `docs/KNOWN_LIMITATIONS.md` がある
- [ ] `docs/STORAGE_IO_STRATEGY.md` と同期保存方針が矛盾していない
- [ ] Lightman's Currency jarをGit管理しない方針が明記されている
- [ ] バックアップ、自動復元なし、手動復元方針が明記されている
- [ ] 新機能がPhase外で追加されていない
