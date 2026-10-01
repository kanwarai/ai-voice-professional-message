#include <jni.h>

#include <atomic>
#include <cstdint>
#include <cstdio>
#include <fstream>
#include <limits>
#include <memory>
#include <string>
#include <vector>

#include "whisper.h"

namespace {

struct Engine {
    whisper_context * context = nullptr;
    std::atomic<bool> abort_requested{false};
};

void quiet_log_callback(ggml_log_level, const char *, void *) {
    // Upstream output is deliberately suppressed because it may include private text.
}

bool should_abort(void * user_data) {
    return static_cast<Engine *>(user_data)->abort_requested.load(std::memory_order_relaxed);
}

uint16_t read_u16(const unsigned char * data) {
    return static_cast<uint16_t>(data[0]) |
           (static_cast<uint16_t>(data[1]) << 8U);
}

uint32_t read_u32(const unsigned char * data) {
    return static_cast<uint32_t>(data[0]) |
           (static_cast<uint32_t>(data[1]) << 8U) |
           (static_cast<uint32_t>(data[2]) << 16U) |
           (static_cast<uint32_t>(data[3]) << 24U);
}

bool matches(const unsigned char * data, const char * value) {
    return data[0] == value[0] && data[1] == value[1] &&
           data[2] == value[2] && data[3] == value[3];
}

bool read_pcm16_wav(const char * path, std::vector<float> & samples) {
    std::ifstream input(path, std::ios::binary);
    if (!input) return false;

    unsigned char header[44]{};
    input.read(reinterpret_cast<char *>(header), sizeof(header));
    if (input.gcount() != sizeof(header) ||
        !matches(header, "RIFF") || !matches(header + 8, "WAVE") ||
        !matches(header + 12, "fmt ") || read_u32(header + 16) != 16 ||
        read_u16(header + 20) != 1 || read_u16(header + 22) != 1 ||
        read_u32(header + 24) != 16000 || read_u16(header + 34) != 16 ||
        !matches(header + 36, "data")) {
        return false;
    }

    const uint32_t data_size = read_u32(header + 40);
    if (data_size == 0 || data_size % 2 != 0 ||
        data_size / 2 > static_cast<uint32_t>(std::numeric_limits<int>::max())) {
        return false;
    }

    std::vector<unsigned char> pcm(data_size);
    input.read(reinterpret_cast<char *>(pcm.data()), data_size);
    if (static_cast<uint32_t>(input.gcount()) != data_size || input.peek() != EOF) return false;

    samples.resize(data_size / 2);
    for (size_t index = 0; index < samples.size(); ++index) {
        const uint16_t raw = read_u16(pcm.data() + index * 2);
        const int16_t sample = static_cast<int16_t>(raw);
        samples[index] = static_cast<float>(sample) / 32768.0F;
    }
    return true;
}

std::string jstring_to_utf8(JNIEnv * env, jstring value) {
    if (value == nullptr) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_kanwarai_voiceprofessionalmessage_ai_speech_WhisperNativeBridge_create(
    JNIEnv * env,
    jobject,
    jstring model_path
) {
    whisper_log_set(quiet_log_callback, nullptr);
    const std::string path = jstring_to_utf8(env, model_path);
    if (path.empty()) return 0;

    whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    params.flash_attn = false;
    whisper_context * context = whisper_init_from_file_with_params(path.c_str(), params);
    if (context == nullptr) return 0;

    auto engine = std::make_unique<Engine>();
    engine->context = context;
    return reinterpret_cast<jlong>(engine.release());
}

extern "C" JNIEXPORT void JNICALL
Java_com_kanwarai_voiceprofessionalmessage_ai_speech_WhisperNativeBridge_resetAbort(
    JNIEnv *,
    jobject,
    jlong handle
) {
    auto * engine = reinterpret_cast<Engine *>(handle);
    if (engine != nullptr) engine->abort_requested.store(false, std::memory_order_relaxed);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_kanwarai_voiceprofessionalmessage_ai_speech_WhisperNativeBridge_transcribe(
    JNIEnv * env,
    jobject,
    jlong handle,
    jstring wav_path,
    jint thread_count
) {
    auto * engine = reinterpret_cast<Engine *>(handle);
    if (engine == nullptr || engine->context == nullptr) return nullptr;

    const std::string path = jstring_to_utf8(env, wav_path);
    std::vector<float> samples;
    if (path.empty() || !read_pcm16_wav(path.c_str(), samples)) return nullptr;

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = thread_count > 0 ? thread_count : 1;
    params.language = "en";
    params.translate = false;
    params.detect_language = false;
    params.no_context = true;
    params.single_segment = false;
    params.token_timestamps = false;
    params.print_special = false;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.suppress_blank = true;
    params.suppress_nst = true;
    params.abort_callback = should_abort;
    params.abort_callback_user_data = engine;

    const int result = whisper_full(
        engine->context,
        params,
        samples.data(),
        static_cast<int>(samples.size())
    );
    if (result != 0 || engine->abort_requested.load(std::memory_order_relaxed)) return nullptr;

    std::string transcript;
    const int segment_count = whisper_full_n_segments(engine->context);
    for (int index = 0; index < segment_count; ++index) {
        const char * text = whisper_full_get_segment_text(engine->context, index);
        if (text != nullptr) transcript.append(text);
    }
    return env->NewStringUTF(transcript.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_kanwarai_voiceprofessionalmessage_ai_speech_WhisperNativeBridge_requestAbort(
    JNIEnv *,
    jobject,
    jlong handle
) {
    auto * engine = reinterpret_cast<Engine *>(handle);
    if (engine != nullptr) engine->abort_requested.store(true, std::memory_order_relaxed);
}

extern "C" JNIEXPORT void JNICALL
Java_com_kanwarai_voiceprofessionalmessage_ai_speech_WhisperNativeBridge_destroy(
    JNIEnv *,
    jobject,
    jlong handle
) {
    std::unique_ptr<Engine> engine(reinterpret_cast<Engine *>(handle));
    if (engine != nullptr && engine->context != nullptr) {
        whisper_free(engine->context);
        engine->context = nullptr;
    }
}
