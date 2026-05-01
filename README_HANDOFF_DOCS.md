# QuestAdmin Handoff Docs

このフォルダは、QuestAdmin を別PCへ移行するための引き継ぎ資料です。

## ファイル一覧

- `HANDOFF.md`
  - 現在の開発状況、完了Phase、環境、移行手順、注意点をまとめた引き継ぎ資料です。

- `PHASE7_PROMPT.md`
  - 次のPhaseである「管理者用クエスト一覧GUI」実装用のプロンプトです。

## 使い方

1. `HANDOFF.md` をリポジトリ直下に置く
2. 別PCで `HANDOFF.md` を読みながら環境を復元する
3. 復元後に `./gradlew clean build` を実行する
4. 問題なければ `PHASE7_PROMPT.md` をClaudeまたはCodexに渡す

## 重要

Lightman's Currency の jar はGit管理されていない可能性があります。

別PCでも以下を配置してください。

```text
libs/lightmanscurrency-1.20.1-2.3.0.4e.jar
```
