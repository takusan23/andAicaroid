# libaicaroid

`HDR`動画のフレームである`RGBA_1010102`形式のバイト配列を、`Android`の`UltraHDR`画像に変換するライブラリです。  
内部で`C++`で書かれている`google/libultrahdr`ライブラリをビルドして利用しているため、すこしだけ`JNI`と`C++`を書いています。

このアプリのコア部分のライブラリですね。

# ビルド方法
といっても`google/libultrahdr`はビルド済みのを`Git`のリポジトリに配置しているため、アプリ（とライブラリ）をビルドするために`C++`とかの知識はほぼいりません。  
内部で使われている`google/libultrahdr`そのものをビルドしたい場合は難しいです（が先述通りビルド済みのを配置しているため不要です）

ふつうに`Android Studio`で開けばよいです。`Android NDK`も必要であれば自動でインストールされるはず。  
ライブラリの生成ですが、`gradlew :libaicaroid:publishToMavenLocal`を叩くと、ホームディレクトリの`.m2`フォルダに`libaicaroid`の`.aar`が生成されます。

--- 自分用 ---

`LIB_REELASE_NOTE.md`を書いて、`libaicaroid/build.gradle.kts`を新しいバージョンにして、`git`の`release_libaicaroid`ブランチを更新してください。  
`Maven Central`への公開は`GitHub Actions`のワークフローがあり、`workflow_dispatch`なのでボタンを押せばビルドとアップロードがされます。  
ワークフローが成功したら`Maven Central`の管理画面を開いて`Publish`してください。

# 内部で使われている libultrahdr ライブラリのビルド方法
これとおなじ

https://takusan.negitoro.dev/posts/android_take_ultrahdr_picture_from_hdr_video_app_making/

`google/libultrahdr`そのものをビルドしたい場合。  
たかだかこれをビルドするためだけに`C++`環境を作る気は無いので、すぐに消せる`WSL2`でやります。

## 公式の手順
https://github.com/google/libultrahdr/blob/main/docs/building.md

## WSL2 で Ubuntu を用意する
できたら、`sudo apt update`と`sudo apt upgrade`をしてください。お作法。

## 必要なパッケージを Ubuntu にいれる

```bash
sudo apt install build-essential
sudo apt install cmake pkg-config libjpeg-dev ninja-build unzip
```

ワンライナーで書き直してもよいです。

## Android NDK をダウンロードする
`Android NDK`をダウンロードするためには`wget`とかでよいわけですが、  
`ブラウザ`か何かで開いて利用規約に同意した後にリダイレクトされる`URL`を得る必要があります。

https://developer.android.com/ndk/downloads?hl=ja

ここから`Linux`を選んで、同意規約を読んでチェックボックスを押して同意して、`ダウンロードボタン`を右クリックしてリンクをコピーします。  
普通にクリックすると`Windows`マシンにダウンロードされてしまう、、

## Android NDK を opt に配置する
よくわかりませんが`/opt`に置いた方が良いらしい。というわけで`cd /opt`して、` sudo wget https://dl.google.com/android/repository/.... `でダウンロードします。

```bash
cd /opt
sudo wget ここにコピーした URL
sudo unzip android-ndk-r30-linux.zip
```

`WSL2`、たまにダウンロードがめちゃ遅いときあるけどなんなんだ・・

## libultrahdr をビルドする
`git`のリポジトリをクローンし、ビルド用のディレクトリを作ります。

また、あえて古いソースコードを使うように`git checkout`しています。これは`HEIF`画像のサポートとともに追加された`libheif`ライブラリが、`LGPL v3.0`で配布されているからです。  
これを`Android`向けにビルドすると`libheif`が`静的リンク`となる模様です。このライセンスは静的リンクを禁止しているわけではないらしいのですが、`Android`の仕組み上`LGPL v3.0`の約束を果たすことが出来ないため、`libheif`に依存していない古いコミットを使うようにしています。  
法律に詳しくないので、本当は`libheif`を使ってもよいのかもしれない？

```bash
cd ~
git clone https://github.com/google/libultrahdr.git
cd libultrahdr
git checkout a8166d6 
mkdir build_directory
cd build_directory
```

つぎに、以下のコマンドを叩きます。  
`CPU`のアーキテクチャ分繰り返します、とりあえず`arm64-v8a (ARM 64 ビット)`をビルドしてみますか。

```bash
cmake -G Ninja -DCMAKE_TOOLCHAIN_FILE=../cmake/toolchains/android.cmake -DUHDR_ANDROID_NDK_PATH=/opt/android-ndk-r30 -DUHDR_BUILD_DEPS=1 -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-23 ../
```

成功していればこんな感じのログで終了するはず？

```bash
-- Configuring done (3.0s)
-- Generating done (0.0s)
-- Build files have been written to: /home/takusan23/libultrahdr/build_directory
```

そしたら`ninja`コマンドを叩いてビルドします。

```bash
ninja
```

案外早い！

おわったら、`libuhdr.so`をローカルの`Windows`にコピーしておきましょう。  
仮想マシンで遊んでいた数年前とは違って`WSL2`は`Windown`のエクスプローラーから直接見ることが出来るので、めちゃめちゃいい時代に生きています。

コピーしたら、このプロジェクトに配置してもよいです。`libaicaroid/src/main/jniLibs/`フォルダーを開いてください。そこに`CPU`アーキテクチャごとにフォルダがあります。  
そこに、さっき出来た`.so`を配置します。

ここの説明では`arm64-v8a`の`.so`を配置するので、`libaicaroid/src/main/jniLibs/arm64-v8a/`フォルダーに配置します。

そしてこの作業を残りの`CPU`アーキテクチャ分繰り返します。以下の四つですね。  
なんで`Intel/AMD`のアーキテクチャもビルドしないといけないのか？`Windows/Linux`の`Android Studio`の`Android エミュレーター`が使ってるからだと思います。  
たぶん`Chromebook`とかも使ってるんじゃないかなあ、、

- ARM 64 ビット: `arm64-v8a`
- ARM 32 ビット: `armeabi-v7a`
- Intel/AMD 64 ビット: `x86_64`
- Intel/AMD 32 ビット: `x86`

`ARM 64 ビット`はさっきやったので残り三つですね。  
`build_directory`を削除して、もう一回`build_directory`を作って、`cmake`コマンドの`-DANDROID_ABI=`の部分を変えてビルドして、成功したら配置する、、を繰り返します。