#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_edwin_grabador_WhisperNative_transcribe(JNIEnv * env, jobject,
                                                   jstring modelPath, jfloatArray audio) {
    const char * path = env->GetStringUTFChars(modelPath, nullptr);
    whisper_context_params ctxParams = whisper_context_default_params();
    ctxParams.use_gpu = false;
    whisper_context * ctx = whisper_init_from_file_with_params(path, ctxParams);
    env->ReleaseStringUTFChars(modelPath, path);
    if (!ctx) return env->NewStringUTF("ERROR: No se pudo cargar el modelo Whisper");
    jsize size = env->GetArrayLength(audio);
    std::vector<float> pcm(size);
    env->GetFloatArrayRegion(audio, 0, size, pcm.data());
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = "auto";
    params.n_threads = 4;
    int rc = whisper_full(ctx, params, pcm.data(), pcm.size());
    std::string text;
    if (rc == 0) {
        for (int i = 0; i < whisper_full_n_segments(ctx); i++) {
            text += whisper_full_get_segment_text(ctx, i);
            text += "\n";
        }
    }
    whisper_free(ctx);
    if (rc != 0) return env->NewStringUTF("ERROR: Falló el reconocimiento");
    return env->NewStringUTF(text.c_str());
}
