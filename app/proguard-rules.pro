# Keep default AGP/R8 behaviour. Minification is disabled for the first release,
# so no custom keep rules are required yet.
# If you enable minifyEnabled in app/build.gradle.kts, verify Room + OkHttp remain intact.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
