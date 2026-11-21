import java.io.FileInputStream
import java.security.KeyStore
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release version. Increment versionCode for every published release.
val quipuVersionCode = 7
val quipuVersionName = "2.1.3"

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
val requiredSigningProperties = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val hasCompleteSigningConfig = keystorePropertiesFile.isFile &&
    requiredSigningProperties.all { !keystoreProperties.getProperty(it).isNullOrBlank() }

fun releaseStoreFile(): File {
    val configuredPath = keystoreProperties.getProperty("storeFile")
        ?: throw GradleException("Falta storeFile en keystore.properties")
    val configuredFile = File(configuredPath)
    return if (configuredFile.isAbsolute) configuredFile else rootProject.file(configuredPath)
}

val releaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("Release", ignoreCase = true) ||
        taskName == "build" ||
        taskName == "assemble" ||
        taskName == "publish"
}

if (releaseTaskRequested) {
    if (!keystorePropertiesFile.isFile) {
        throw GradleException(
            "No se puede generar un Release firmado: falta keystore.properties en la raíz.",
        )
    }
    if (!hasCompleteSigningConfig) {
        throw GradleException(
            "No se puede generar un Release firmado: faltan propiedades de firma.",
        )
    }
    val storeFile = releaseStoreFile()
    if (!storeFile.isFile) {
        throw GradleException("No se puede generar un Release firmado: el keystore no existe.")
    }
    val keyAlias = keystoreProperties.getProperty("keyAlias")
    if (keyAlias != "quipu-release") {
        throw GradleException("El alias de firma Release debe ser quipu-release.")
    }
    try {
        val keyStore = KeyStore.getInstance("JKS")
        val storePassword = keystoreProperties.getProperty("storePassword")
        val keyPassword = keystoreProperties.getProperty("keyPassword")
        storeFile.inputStream().use { keyStore.load(it, storePassword.toCharArray()) }
        if (!keyStore.containsAlias(keyAlias)) {
            throw GradleException("El alias de firma no existe en el keystore.")
        }
        val entry = keyStore.getEntry(
            keyAlias,
            KeyStore.PasswordProtection(keyPassword.toCharArray()),
        )
        if (entry !is KeyStore.PrivateKeyEntry) {
            throw GradleException("El alias no contiene una clave privada.")
        }
    } catch (exception: Exception) {
        throw GradleException(
            "La configuración de firma Release no es válida.",
            exception,
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

android {
    namespace = "io.github.dijksys.quipu"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.dijksys.quipu"
        minSdk = 26
        targetSdk = 37
        versionCode = quipuVersionCode
        versionName = quipuVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasCompleteSigningConfig) {
            create("release") {
                storeFile = releaseStoreFile()
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            matchingFallbacks += listOf("debug")
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isDebuggable = false
            if (hasCompleteSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("QUIPU-$quipuVersionName.apk")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
