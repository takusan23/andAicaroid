# andAicaroid

10ビット HDR 動画から UltraHDR 画像を作る、もしくはその逆をするためのアプリ

# ライブラリ

`RGBA1010102 のピクセルバイト配列`から`UltraHDR`を作ることが出来る関数があるライブラリです。  
`UltraHDR`を作っている`C++`製`libuhdr`を`Kotlin`から呼び出せるようにしたものです。なので利用側は`C++`関係なくこれを追加すれば`UltraHDR`画像が作れちゃう。

```kotlin
implementation("io.github.takusan23:libaicaroid:1.0.0")
```

ライブラリ自体は`sdk=26`以上を必要にしています。これは多分`libultrahdr`がそうしているから。  
実際のところは`UltraHDR`が導入された`Android 14`あたりが最低バージョンな気がします。

```kotlin
// 一時的なファイルを作る
val inputRgba1010102File = context.getExternalFilesDir(null)!!.resolve("input_rgba_1010102").apply {
    writeBytes(readPixels)
}
val outputUltraHdrJpegFile = context.getExternalFilesDir(null)!!.resolve("output_uhdr.jpeg")

// ライブラリの関数
LibUltraHdrBridge.encodeFromRgba1010102(
    width = 1920, // width
    height = 1080, // height
    rgba1010102FilePath = inputRgba1010102File.path, // rgba1010102 のバイト配列があるファイルパス
    ultraHdrResultFilePath = outputUltraHdrJpegFile.path, // UltraHDR 画像の保存先
    colorSpaceType = LibUltraHdrBridge.HdrColorSpaceType.HLG // HLG か PQ か
)

// Bitmap 取得
val ultraHdrBitmap = BitmapFactory.decodeFile(outputUltraHdrJpegFile.path)
inputRgba1010102File.delete()
outputUltraHdrJpegFile.delete()
```

## どこでつかうの

`10-bit HDR`がすでに描画できていて、かつ`OpenGL ES`経由の場合は、`glReadPixels`を使い、HDR動画の映像フレームが`RGBA 1010102`で取得できる。  
が、このままだと`JPEG`とかではなく、`RGBA1010102`が並んだバイト配列になる。`glReadPixels`してるんだからそりゃそう。

これを、`UltraHDR`に出来るのがこのライブラリです。

カメラアプリを作ってて`HDR`撮影ができるなら`UltraHDR`撮影機能がほしいよ～。でも`C++`は何としてでも書きたくない！！！そこのあなた。

# 私向け ライブラリ公開手順
GitHubActions まだ作ってない

- local.properties に認証情報を書く or 環境変数
  - AkariDroid/akari-core と同じ
- バージョンアップする + LIB_RELEASE_NOTE.md を更新させる
- `gradle :libaicaroid:publishToSonatype closeSonatypeStagingRepository`
  - を実行
- https://central.sonatype.com/publishing/deployments
  - から Publish を押して公開