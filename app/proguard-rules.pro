# ---保持泛型签名 ---
# 这是解决 "Class cannot be cast to ParameterizedType" 的关键
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# --- 保护 Retrofit 接口 ---
# 确保你的 Service 接口不被改名或混淆
-keep interface com.hamster.toolbox.ai.AiService { *; }
-keep interface com.hamster.toolbox.ai.AiService { *; }

# --- 保护数据模型 ---
# 确保你的 Request 和 Response 数据类不被混淆
-keep class com.example.toolbox.ui.assistant.** { *; }

# --- Retrofit 和 OkHttp 的通用规则 ---
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# ---保护 Kotlin 协程接口 ---
# Retrofit 需要它来推断 suspend 函数的返回值
-keep interface kotlin.coroutines.Continuation

-keep class com.k2fsa.sherpa.onnx.** { *; }