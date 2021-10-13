# Picasso
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# Public access of constructor was revoked in an update, reflectively calling constructor for now
-keep class okhttp3.internal.cache.DiskLruCache {
    <init>(...);
}

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform

-dontwarn com.squareup.okhttp.**

-keep class com.shockwave.**

-keepclassmembers class com.shockwave.** { *; }

# Moshi
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}

-keep @com.squareup.moshi.JsonQualifier interface *

# Enum field names are used by the integrated EnumJsonAdapter.
# Annotate enums with @JsonClass(generateAdapter = false) to use them with Moshi.
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
}

# The name of @JsonClass types is used to look up the generated adapter.
-keepnames @com.squareup.moshi.JsonClass class *

# Microsoft Azure
# Keep ADAL classes
-keep class com.microsoft.aad.adal.** { *; }
-keep class com.microsoft.identity.common.** { *; }

# Keep Gson for ADAL https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

-dontwarn org.bouncycastle.**
-dontwarn com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity
-dontwarn org.apache.log4j.**
-dontwarn com.microsoft.azure.storage.**
-dontwarn org.slf4j.*

# Kotlin Coroutines
# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Kubota App
# Keep Models
#-keep class com.kubota.network.model.** { *; }
#-keep,allowobfuscation class com.android.kubota.** { *; }
-keep class com.kubota.service.** { *; }
-keep class com.inmotionsoftware.foundation.** { *; }
-keep class com.inmotionsoftware.flowkit.** { *; }

# CouchbaseLite
# https://docs.couchbase.com/couchbase-lite/2.7/java-android.html

# OkHttp3
-dontwarn okhttp3.**
-dontwarn okio.**
#-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
#-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# CBL2.x
-keep class com.couchbase.litecore.**{ *; }
-keep class com.couchbase.lite.**{ *; }

# Shared rules
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes Exceptions

# Preserve the special static methods that are required in all enumeration classes.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

############################ Moshi ######################################
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}

-keep @com.squareup.moshi.JsonQualifier interface *

# Enum field names are used by the integrated EnumJsonAdapter.
# Annotate enums with @JsonClass(generateAdapter = false) to use them with Moshi.
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
}

# The name of @JsonClass types is used to look up the generated adapter.
-keepnames @com.squareup.moshi.JsonClass class *

# Retain generated JsonAdapters if annotated type is retained.
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*
-keep class <1>_<2>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*$*
-keep class <1>_<2>_<3>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*$*$*
-keep class <1>_<2>_<3>_<4>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*$*$*$*
-keep class <1>_<2>_<3>_<4>_<5>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class **$*$*$*$*$*
-keep class <1>_<2>_<3>_<4>_<5>_<6>JsonAdapter {
    <init>(...);
    <fields>;
}

-keep class kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoaderImpl

# bug in moshi in data models https://github.com/square/moshi/issues/345
-keep class kotlin.Metadata { *; }

# bug in moshi refer to link https://github.com/square/moshi/issues/402
-keep class kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.BuiltInsLoaderImpl
-keep interface kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader

-keepclassmembers class * implements android.os.Parcelable {
  static ** CREATOR;
}
