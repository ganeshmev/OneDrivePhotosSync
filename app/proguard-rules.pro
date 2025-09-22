# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

## Keep MSAL (Microsoft Authentication Library)
-keep class com.microsoft.identity.** { *; }
-dontwarn com.microsoft.identity.**

## Keep AppAuth for Google OAuth
-keep class net.openid.appauth.** { *; }
-dontwarn net.openid.appauth.**

## Keep JSON org classes used
-dontwarn org.json.**

## Silence optional annotation/telemetry libraries referenced transitively
# AutoValue (annotations-only; not needed at runtime)
-dontwarn com.google.auto.value.**
# OpenTelemetry (not used by app runtime)
-dontwarn io.opentelemetry.**
# FindBugs annotations (provided-only)
-dontwarn edu.umd.cs.findbugs.annotations.**
# Yubico YubiKit (transitive reference from dependencies; not used)
-dontwarn com.yubico.yubikit.**