#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <chrono>
#include <mutex>
#include <string>
#include <vector>
#include "llama.h"

namespace {
constexpr int CONTEXT_TOKENS = 2048;
constexpr int MAX_OUTPUT_TOKENS = 384;
struct Engine { llama_model * model{}; llama_context * context{}; std::atomic<bool> cancel{false}; std::mutex mutex; };

std::string utf8(JNIEnv * env, jstring value) {
    if (!value) return {};
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string result(chars); env->ReleaseStringUTFChars(value, chars); return result;
}
void quiet_log(ggml_log_level, const char *, void *) {}
long long millis(std::chrono::steady_clock::time_point start) {
    return std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - start).count();
}
jobjectArray response(JNIEnv * env, const char * status, const std::string & text = {}, long long prompt_ms = 0, long long gen_ms = 0, int tokens = 0) {
    auto cls = env->FindClass("java/lang/String"); auto result = env->NewObjectArray(5, cls, nullptr);
    const std::string values[] = {status, text, std::to_string(prompt_ms), std::to_string(gen_ms), std::to_string(tokens)};
    for (int i = 0; i < 5; ++i) { auto s = env->NewStringUTF(values[i].c_str()); env->SetObjectArrayElement(result, i, s); env->DeleteLocalRef(s); }
    return result;
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_kanwarai_voiceprofessionalmessage_ai_rewrite_QwenNativeBridge_create(JNIEnv * env, jobject, jstring path_value) {
    llama_log_set(quiet_log, nullptr); llama_backend_init();
    auto engine = new (std::nothrow) Engine(); if (!engine) return 0;
    auto params = llama_model_default_params(); params.n_gpu_layers = 0;
    const auto path = utf8(env, path_value); engine->model = llama_model_load_from_file(path.c_str(), params);
    if (!engine->model) { delete engine; return 0; }
    auto cp = llama_context_default_params(); cp.n_ctx = CONTEXT_TOKENS; cp.n_batch = CONTEXT_TOKENS; cp.n_ubatch = 256; cp.n_threads = 4; cp.n_threads_batch = 4; cp.no_perf = true;
    engine->context = llama_init_from_model(engine->model, cp);
    if (!engine->context) { llama_model_free(engine->model); delete engine; return 0; }
    return reinterpret_cast<jlong>(engine);
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_kanwarai_voiceprofessionalmessage_ai_rewrite_QwenNativeBridge_rewrite(JNIEnv * env, jobject, jlong ptr, jstring system_value, jstring user_value) {
    auto * e = reinterpret_cast<Engine *>(ptr); if (!e) return nullptr; std::lock_guard<std::mutex> lock(e->mutex); e->cancel = false;
    const auto system = utf8(env, system_value), user = utf8(env, user_value);
    llama_chat_message messages[] = {{"system", system.c_str()}, {"user", user.c_str()}};
    const char * tmpl = llama_model_chat_template(e->model, nullptr);
    int required = llama_chat_apply_template(tmpl, messages, 2, true, nullptr, 0); if (required <= 0) return nullptr;
    std::vector<char> formatted(required + 1); required = llama_chat_apply_template(tmpl, messages, 2, true, formatted.data(), formatted.size()); if (required <= 0) return nullptr;
    const llama_vocab * vocab = llama_model_get_vocab(e->model);
    int count = -llama_tokenize(vocab, formatted.data(), required, nullptr, 0, true, true); if (count <= 0) return nullptr;
    if (count + MAX_OUTPUT_TOKENS > CONTEXT_TOKENS) return response(env, "TOO_LONG");
    std::vector<llama_token> prompt(count); if (llama_tokenize(vocab, formatted.data(), required, prompt.data(), count, true, true) < 0) return nullptr;
    llama_memory_clear(llama_get_memory(e->context), true);
    auto prompt_start = std::chrono::steady_clock::now();
    if (llama_decode(e->context, llama_batch_get_one(prompt.data(), prompt.size())) != 0) return nullptr;
    const auto prompt_ms = millis(prompt_start);
    auto chain = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(chain, llama_sampler_init_penalties(llama_vocab_n_tokens(vocab), 64, 1.0f, 0.0f, 1.5f));
    llama_sampler_chain_add(chain, llama_sampler_init_top_k(20));
    llama_sampler_chain_add(chain, llama_sampler_init_top_p(0.8f, 1));
    llama_sampler_chain_add(chain, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(chain, llama_sampler_init_dist(0x51A7E));
    std::string output; int generated = 0; bool ended = false; auto gen_start = std::chrono::steady_clock::now();
    for (; generated < MAX_OUTPUT_TOKENS; ++generated) {
        if (e->cancel.load()) { llama_sampler_free(chain); return response(env, "CANCELLED"); }
        llama_token token = llama_sampler_sample(chain, e->context, -1);
        if (llama_vocab_is_eog(vocab, token)) { ended = true; break; }
        char piece[512]; int bytes = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, false); if (bytes < 0) { llama_sampler_free(chain); return nullptr; }
        output.append(piece, bytes);
        if (llama_decode(e->context, llama_batch_get_one(&token, 1)) != 0) { llama_sampler_free(chain); return nullptr; }
    }
    const auto gen_ms = millis(gen_start); llama_sampler_free(chain);
    return response(env, ended ? "OK" : "LIMIT", output, prompt_ms, gen_ms, generated);
}

extern "C" JNIEXPORT void JNICALL Java_com_kanwarai_voiceprofessionalmessage_ai_rewrite_QwenNativeBridge_cancel(JNIEnv *, jobject, jlong ptr) {
    auto * e = reinterpret_cast<Engine *>(ptr); if (e) e->cancel = true;
}
extern "C" JNIEXPORT void JNICALL Java_com_kanwarai_voiceprofessionalmessage_ai_rewrite_QwenNativeBridge_destroy(JNIEnv *, jobject, jlong ptr) {
    auto * e = reinterpret_cast<Engine *>(ptr); if (!e) return;
    { std::lock_guard<std::mutex> lock(e->mutex); llama_free(e->context); llama_model_free(e->model); }
    delete e;
}
