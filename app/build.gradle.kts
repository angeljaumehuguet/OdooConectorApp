plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.odooconectorapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.odooconectorapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packagingOptions {
        resources.excludes.add("META-INF/DEPENDENCIES")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation("org.apache.xmlrpc:xmlrpc-client:3.1.3")
    implementation("org.apache.xmlrpc:xmlrpc-common:3.1.3")
    implementation("org.apache.ws.commons.util:ws-commons-util:1.0.2") {
        // Esta exclusión es importante para evitar que JUnit 3.8.1 entre en el classpath de la app
        exclude(group = "junit", module = "junit")
    }

    // Dependencias para Pruebas Unitarias Locales (src/test/java)
    testImplementation("junit:junit:4.13.2") // O usa libs.junit si apunta a esto

    // Dependencias para Pruebas de Instrumentación (src/androidTest/java)
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // O usa libs.ext.junit
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // O usa libs.espresso.core

    // Añadida explícitamente para asegurar que JUnit 4.13.2 esté disponible para androidTest
    androidTestImplementation("junit:junit:4.13.2")
}

// BLOQUE AÑADIDO PARA FORZAR LA VERSIÓN DE JUNIT Y RESOLVER CONFLICTOS
configurations.all {
    resolutionStrategy {
        // Forzar la versión de JUnit a 4.13.2 para todas las configuraciones
        // Esto ayuda a resolver el conflicto con la versión estrictamente 3.8.1
        force("junit:junit:4.13.2")
    }
}