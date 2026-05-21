# QuestAdmin 既知の制限

## クエスト機能

- 対応クエスト種別は `ITEM_DELIVERY` のみです。
- `repeatable=true` は現時点で未対応です。クエスト作成/読み込み時は `false` を使用してください。
- 村人NPC連携は未実装です。
- 討伐クエストは未実装です。
- 採掘クエストは未実装です。
- 探索クエストは未実装です。
- デイリークエストは未実装です。
- クエストツリー、前提クエスト、ランキングは未実装です。

## 経済MOD

- Lightman's Currency が必須です。
- 他経済MODには対応していません。
- Lightman's Currency 以外の経済MODには対応していません。
- 報酬は Lightman's Currency の銀行口座入金のみです。

## 保存と運用

- `complete` / `claim` は安全性優先で同期保存します。
- 非同期保存キューとdebounce保存は未実装です。
- MySQL保存は未実装です。
- Web管理画面は未実装です。
- バックアップ復元は自動ではなく手動です。
- 自動復元コマンドはありません。
- `player_quests.json` の手動編集は慎重に行ってください。編集前に `/questadmin storage backup` を実行し、編集後に `/questadmin storage validate` を実行してください。
- `player_quests.json` の巻き戻しは報酬状態と Lightman's Currency 残高の不整合につながる可能性があります。

## アイテム判定

- 納品対象は通常インベントリとオフハンドです。
- 防具欄は所持数表示、完了判定、消費処理の対象外です。
- NBT差分は現時点では判定しません。同じアイテムIDは同一アイテムとして扱います。

## 管理操作

- 既存クエストの `id`、`type`、`createdAt` は編集できません。
- クエスト削除や編集を行っても、既存のプレイヤー進行状況は自動削除されません。
- `CLAIMING` 状態が残った場合、管理者がログと Lightman's Currency 残高を確認してから状態修正してください。
