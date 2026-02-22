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

# Jika kamu menggunakan refleksi untuk membaca BuildConfig (seperti di AuthViewModel),
# aturan ini wajib ada agar field seperti FIREBASE_DATABASE_URL tidak hilang/berubah nama.
-keep class com.example.fitme.BuildConfig { *; }

# Tambahan: Jika kamu menggunakan Firebase Database, biasanya ProGuard sudah ditangani otomatis oleh SDK,
# tapi jika ada error terkait parsing data, kamu bisa menambahkan:
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.fitme.viewModel.UserProfile { *; }
