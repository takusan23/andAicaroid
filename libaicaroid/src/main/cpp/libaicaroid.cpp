#include <jni.h>
#include <malloc.h>
#include <iostream>
#include <fstream>
#include <android/log.h>
#include "ultrahdr_api.h"

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("libaicaroid");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("libaicaroid")
//      }
//    }

#define READ_BYTES(DESC, ADDR, LEN)                                                             \
 DESC.read(static_cast<char*>(ADDR), (LEN));                                                   \
 if (DESC.gcount() != (LEN)) {                                                                 \
   std::cerr << "failed to read : " << (LEN) << " bytes, read : " << DESC.gcount() << " bytes" \
             << std::endl;                                                                     \
   return false;                                                                               \
 }

static bool loadFile(const char *filename, uhdr_raw_image_t *handle) {
    std::ifstream ifd(filename, std::ios::binary);
    if (ifd.good()) {
        if (handle->fmt == UHDR_IMG_FMT_24bppYCbCrP010) {
            const size_t bpp = 2;
            READ_BYTES(ifd, handle->planes[UHDR_PLANE_Y], bpp * handle->w * handle->h)
            READ_BYTES(ifd, handle->planes[UHDR_PLANE_UV],
                       bpp * (handle->w / 2) * (handle->h / 2) * 2)
            return true;
        } else if (handle->fmt == UHDR_IMG_FMT_32bppRGBA1010102 ||
                   handle->fmt == UHDR_IMG_FMT_32bppRGBA8888) {
            const size_t bpp = 4;
            READ_BYTES(ifd, handle->planes[UHDR_PLANE_PACKED], bpp * handle->w * handle->h)
            return true;
        } else if (handle->fmt == UHDR_IMG_FMT_64bppRGBAHalfFloat) {
            const size_t bpp = 8;
            READ_BYTES(ifd, handle->planes[UHDR_PLANE_PACKED], bpp * handle->w * handle->h)
            return true;
        } else if (handle->fmt == UHDR_IMG_FMT_12bppYCbCr420) {
            READ_BYTES(ifd, handle->planes[UHDR_PLANE_Y], (size_t) handle->w * handle->h)
            READ_BYTES(ifd, handle->planes[UHDR_PLANE_U],
                       (size_t) (handle->w / 2) * (handle->h / 2))
            READ_BYTES(ifd, handle->planes[UHDR_PLANE_V],
                       (size_t) (handle->w / 2) * (handle->h / 2))
            return true;
        }
        return false;
    }
    return false;
}

static bool writeFile(const char *filename, void *&result, size_t length) {
    std::ofstream ofd(filename, std::ios::binary);
    if (ofd.is_open()) {
        ofd.write(static_cast<char *>(result), length);
        return true;
    }
    std::cerr << "unable to write to file : " << filename << std::endl;
    return false;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_takusan23_libaicaroid_LibUltraHdrBridge_encodeFromRgba1010102(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jint width,
                                                                             jint height,
                                                                             jstring rgba1010102_file_path,
                                                                             jstring ultra_hdr_result_file_path,
                                                                             jint hdr_color_space_type) {
    // pointer
    const char *native_rgba1010102_path = env->GetStringUTFChars(rgba1010102_file_path, 0);
    const char *native_ultra_hdr_path = env->GetStringUTFChars(ultra_hdr_result_file_path, 0);

    // HLG / PQ
    // HdrColorSpaceType 参照
    uhdr_color_transfer range;
    switch (hdr_color_space_type) {
        case 0:
            range = UHDR_CT_HLG;
            break;
        case 1:
            range = UHDR_CT_PQ;
            break;
        default:
            range = UHDR_CT_HLG;
            break;
    }

    // Load rgba1010102
    const size_t bpp = 4;
    uhdr_raw_image_t mRawRgba1010102Image{};
    mRawRgba1010102Image.fmt = UHDR_IMG_FMT_32bppRGBA1010102;
    mRawRgba1010102Image.cg = UHDR_CG_DISPLAY_P3;
    mRawRgba1010102Image.ct = range;
    mRawRgba1010102Image.range = UHDR_CR_FULL_RANGE;
    mRawRgba1010102Image.w = width;
    mRawRgba1010102Image.h = height;
    mRawRgba1010102Image.planes[UHDR_PLANE_PACKED] = malloc(bpp * width * height);
    mRawRgba1010102Image.planes[UHDR_PLANE_UV] = nullptr;
    mRawRgba1010102Image.planes[UHDR_PLANE_V] = nullptr;
    mRawRgba1010102Image.stride[UHDR_PLANE_PACKED] = width;
    mRawRgba1010102Image.stride[UHDR_PLANE_UV] = 0;
    mRawRgba1010102Image.stride[UHDR_PLANE_V] = 0;
    loadFile(native_rgba1010102_path, &mRawRgba1010102Image);

    // https://github.com/google/libultrahdr/blob/6db3a83ee2b1f79850f3f597172289808dc6a331/examples/ultrahdr_app.cpp#L776-L781
    uhdr_codec_private_t *handle = uhdr_create_encoder();
    uhdr_enc_set_raw_image(handle, &mRawRgba1010102Image, UHDR_HDR_IMG);
    uhdr_enc_set_quality(handle, 95, UHDR_BASE_IMG);
    uhdr_enc_set_quality(handle, 95, UHDR_GAIN_MAP_IMG);
    uhdr_enc_set_using_multi_channel_gainmap(handle, true);
    uhdr_enc_set_gainmap_scale_factor(handle, 1);
    uhdr_enc_set_gainmap_gamma(handle, 1.f);
    uhdr_enc_set_preset(handle, UHDR_USAGE_BEST_QUALITY);
    uhdr_encode(handle);

    auto output = uhdr_get_encoded_stream(handle);

    // for decoding
    uhdr_compressed_image_t mUhdrImage{};
    mUhdrImage.data = malloc(output->data_sz);
    memcpy(mUhdrImage.data, output->data, output->data_sz);
    mUhdrImage.capacity = mUhdrImage.data_sz = output->data_sz;
    mUhdrImage.cg = output->cg;
    mUhdrImage.ct = output->ct;
    mUhdrImage.range = output->range;
    uhdr_release_encoder(handle);

    writeFile(native_ultra_hdr_path, mUhdrImage.data, mUhdrImage.data_sz);
    free(mUhdrImage.data);
}