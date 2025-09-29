package io.github.takusan23.libaicaroid

/**
 * libultrahdr ライブラリを C++ から利用して、Kotlin からその関数を呼び出すための架け橋
 */
object LibUltraHdrBridge {

    init {
        // libaicaroid.cpp をロード
        System.loadLibrary("libaicaroid")
    }

    /**
     * 入力 HDR フレームのガンマカーブ
     *
     * @param cppIndex libaicaroid 側と同じ
     */
    enum class HdrColorSpaceType(val cppIndex: Int) {
        HLG(0),
        PQ(1)
    }

    /**
     * RGBA1010102 の画像データ（ピクセル配列）から UltraHDR 画像を作る
     *
     * @param width 画像の横
     * @param height 画像の縦
     * @param rgba1010102FilePath RGBA1010102 の画像のファイルパス
     * @param ultraHdrResultFilePath 完成した UltraHDR 画像のファイルパス
     * @param colorSpaceType HDR のガンマカーブ
     */
    fun encodeFromRgba1010102(
        width: Int,
        height: Int,
        rgba1010102FilePath: String,
        ultraHdrResultFilePath: String,
        colorSpaceType: HdrColorSpaceType
    ) = encodeFromRgba1010102(
        width = width,
        height = height,
        rgba1010102FilePath = rgba1010102FilePath,
        ultraHdrResultFilePath = ultraHdrResultFilePath,
        hdrColorSpaceType = colorSpaceType.cppIndex
    )

    private external fun encodeFromRgba1010102(
        width: Int,
        height: Int,
        rgba1010102FilePath: String,
        ultraHdrResultFilePath: String,
        hdrColorSpaceType: Int
    )

}