# ProGuard rules for RoPay
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class ru.martynov.clientbank.data.models.** { *; }
