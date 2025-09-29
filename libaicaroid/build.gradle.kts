plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // Maven Central に公開する際に利用
    `maven-publish`
    signing
}

// ライブラリ公開は Android でも言及するようになったので目を通すといいかも
// https://developer.android.com/build/publish-library/upload-library
// そのほか役に立ちそうなドキュメント
// https://docs.gradle.org/current/dsl/org.gradle.api.publish.maven.MavenPublication.html
// https://github.com/gradle-nexus/publish-plugin

// OSSRH にアップロードせずに成果物を確認する方法があります。ローカルに吐き出せばいい
// gradle :libaicaroid:publishToMavenLocal

android {
    namespace = "io.github.takusan23.libaicaroid"
    compileSdk = 36

    defaultConfig {
        minSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        externalNativeBuild {
            cmake {
                cppFlags += ""
                arguments += listOf(
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON", // 16KB ページサイズ
                    "-DANDROID_STL=c++_shared" // java.lang.UnsatisfiedLinkError: dlopen failed: library "libc++_shared.so" not found: needed 対策
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ライブラリのメタデータ
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "io.github.takusan23"
            artifactId = "libaicaroid"
            version = "1.0.0" // バージョンアップの際は CORE_RELEASE_NOTE.md も更新 + release_akari_core ブランチの更新

            // afterEvaluate しないとエラーなる
            afterEvaluate {
                from(components["release"])
            }

            pom {
                // ライブラリ情報
                name.set("libaicaroid")
                description.set("From RGBA_1010102 To UltraHDR Library")
                url.set("https://github.com/takusan23/andAicaroid/")
                // ライセンス
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://github.com/takusan23/andAicaroid/blob/master/LICENSE")
                    }
                }
                // 開発者
                developers {
                    developer {
                        id.set("takusan_23")
                        name.set("takusan_23")
                        url.set("https://takusan.negitoro.dev/")
                    }
                }
                // git
                scm {
                    connection.set("scm:git:github.com/takusan23/andAicaroid")
                    developerConnection.set("scm:git:ssh://github.com/takusan23/andAicaroid")
                    url.set("https://github.com/takusan23/andAicaroid")
                }
            }
        }
    }
}

// 署名
signing {
    // ルート build.gradle.kts の extra を見に行く
    useInMemoryPgpKeys(
        rootProject.extra["signing.keyId"] as String,
        rootProject.extra["signing.key"] as String,
        rootProject.extra["signing.password"] as String,
    )
    sign(publishing.publications["release"])
}