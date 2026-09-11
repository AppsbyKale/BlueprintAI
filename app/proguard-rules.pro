# Room entities and DAOs
-keep class com.example.blueprintai.data.** { *; }
-keep interface com.example.blueprintai.data.** { *; }

# Models and Serialization
-keep class com.example.blueprintai.model.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep kotlinx.serialization models
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep LiteRT and MediaPipe GenAI native bindings
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class com.google.mediapipe.** { *; }

# Optional transitive dependencies
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.google.mediapipe.proto.**
-dontwarn com.tom_roush.pdfbox.**
