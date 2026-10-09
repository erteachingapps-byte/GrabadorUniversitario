#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"

struct ProgressBridge { JavaVM *vm; jobject callback; jmethodID method; };
static void onProgress(struct whisper_context *, struct whisper_state *, int progress, void *user_data) {
    auto *bridge=static_cast<ProgressBridge *>(user_data);
    JNIEnv *env=nullptr;
    bool attached=false;
    if(bridge->vm->GetEnv(reinterpret_cast<void **>(&env),JNI_VERSION_1_6)!=JNI_OK) {
        if(bridge->vm->AttachCurrentThread(&env,nullptr)!=JNI_OK) return;
        attached=true;
    }
    env->CallVoidMethod(bridge->callback,bridge->method,static_cast<jint>(progress));
    if(env->ExceptionCheck()) env->ExceptionClear();
    if(attached) bridge->vm->DetachCurrentThread();
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_edwin_grabador_WhisperNative_transcribe(JNIEnv * env, jobject,
                                                   jstring modelPath, jfloatArray audio, jobject progressCallback) {
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
    ProgressBridge bridge{};
    if(progressCallback) {
        env->GetJavaVM(&bridge.vm);
        bridge.callback=env->NewGlobalRef(progressCallback);
        jclass klass=env->GetObjectClass(progressCallback);
        bridge.method=env->GetMethodID(klass,"onProgress","(I)V");
        env->DeleteLocalRef(klass);
        if(bridge.method) {
            params.progress_callback=onProgress;
            params.progress_callback_user_data=&bridge;
        } else if(env->ExceptionCheck()) env->ExceptionClear();
    }
    int rc = whisper_full(ctx, params, pcm.data(), pcm.size());
    if(bridge.callback) env->DeleteGlobalRef(bridge.callback);
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
