# ProGuard rules for RossetiAlert
# Keep all data classes (used with JSON)
-keep class com.rosseti.alert.data.** { *; }

# OkHttp — не обфусцировать
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# org.json — часть Android SDK, не трогать
-keep class org.json.** { *; }