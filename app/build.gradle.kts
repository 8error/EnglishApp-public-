plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.englishapp"

    // 编译SDK版本
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.englishapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 添加 Java 8 支持（用于 Room 的 TypeConverter）
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 添加数据绑定/视图绑定支持
    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // ========== 基础 UI 库 ==========
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ========== 背单词 App 核心依赖 ==========

    // Room 数据库
    val room_version = "2.6.0"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // Room 和 RxJava 集成
    implementation("androidx.room:room-rxjava3:$room_version")
    implementation("io.reactivex.rxjava3:rxjava:3.1.7")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // ViewModel 和 LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2")

    // UI 组件
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.cardview:cardview:1.0.0")

    // ========== 新增的依赖 ==========
    // Material Components 扩展
    implementation("com.google.android.material:material:1.9.0")

    // RecyclerView 选择器
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")

    // 工具库
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ========== 使用本地 TinyPinyin JAR 文件 ==========
    // 将 TinyPinyin-2.0.3.RELEASE.jar 放入 app/libs/ 目录
    implementation(files("libs/TinyPinyin-2.0.3.RELEASE.jar"))

    // 如果还有其他词典包需要添加，可以继续添加
    // implementation(files("libs/其他文件名.jar"))

    // ========== 测试依赖 ==========
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}