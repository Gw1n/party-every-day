import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.2.20"
}

tasks.register("buildCss", Exec::class) {
    inputs.files("tailwind.config.js", "input.css")
    inputs.dir("src/jsMain/kotlin")
    outputs.file("src/jsMain/resources/output.css")
    
    workingDir(projectDir)
    commandLine(
        "npx",
        "--yes",
        "tailwindcss@3.4.1",
        "-i", "./input.css",
        "-o", "./src/jsMain/resources/output.css",
        "--minify"
    )
}

kotlin {
    jvmToolchain(11)
    
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
                devServer?.let {
                    it.static = (it.static ?: mutableListOf()).apply {
                        add(project.projectDir.resolve("src/jsMain/resources").absolutePath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val jsMain by getting {
            dependencies {
                // Tailwind CSS
                implementation(npm("tailwindcss", "3.4.1"))
                implementation(npm("postcss", "8.4.35"))
                implementation(npm("autoprefixer", "10.4.17"))

                // React
                implementation(npm("react", "18.2.0"))
                implementation(npm("react-dom", "18.2.0"))
                implementation(kotlinWrappers.react)
                implementation(kotlinWrappers.reactDom)
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }
    }
}

// This block ensures that Gradle finds the tasks at the right time
afterEvaluate {
    tasks.findByName("buildCss")?.let { buildCssTask ->
        rootProject.tasks.findByName("kotlinNpmInstall")?.let { npmTask ->
            buildCssTask.dependsOn(npmTask)
        }
    }
    tasks.findByName("jsProcessResources")?.let { processTask ->
        tasks.findByName("buildCss")?.let { buildCssTask ->
            processTask.dependsOn(buildCssTask)
        }
    }
}


android {
    namespace = "com.example.hackatum"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.hackatum"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
