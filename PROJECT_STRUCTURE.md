# AbyssTimer Project Structure

このプロジェクトは、WEB版のロジックとデザインをリファレンスとし、Androidネイティブ（Kotlin/Compose/Room）で再構築しているプロジェクトです。

## モジュール構成

### 1. `app/` (メインモジュール)
- **役割**: ネイティブAndroidアプリケーションの実装。
- **技術スタック**: 
  - **UI**: Jetpack Compose (Material 3)
  - **データ保存**: Room Database (SQLite)
  - **アーキテクチャ**: MVVM (ViewModel + Repository)
- **特徴**: WEB版の挙動を可能な限りネイティブコンポーネントで再現しています。

### 2. `webview_app/`
- **役割**: WEB版をそのままWebViewでラップしたバージョン（TWA/PWAベース）。
- **用途**: ネイティブ版との挙動比較や、急ぎの動作確認用。

### 3. `assets/` (リファレンス用)
- **場所**: `app/src/main/assets/` およびプロジェクトルート。
- **内容**: WEB版のJS/CSS/HTMLファイル。
- **役割**: 現在、ネイティブ版のロジックはKotlinで書き直されていますが、一部の複雑な計算やスタイル、または将来的なWebView併用時のために保持されています。
- **注意**: ネイティブ版の開発時は、これらのJSファイル内のロジックをリファレンスとして参照します。

## 主要なファイル (ネイティブ版)
- `MainActivity.kt`: UIのメインエントリポイント、Composeによる画面構成。
- `TimerViewModel.kt`: タイマーの状態管理、更新ロジック、データ操作。
- `TimerRepository.kt`: RoomデータベースとViewModelの橋渡し。
- `AppDatabase.kt` / `ItemEntity.kt`: データのスキーマ定義。

## 開発のガイドライン
- **Source of Truth**: 基本的な仕様やUIの正解はWEB版（`index.html`, `engine.js`等）にあります。
- **ネイティブ優先**: 動作の安定性と軽さを重視し、極力JSエンジンに頼らずKotlin側でロジックを完結させます。
- **UI再現**: Material 3を使いつつ、WEB版の「深淵」を感じさせるダークでスタイリッシュなデザインを継承します。
