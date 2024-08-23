plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.dokka") version "1.9.20"
    alias(libs.plugins.sonatype.publish)
}

android {
    namespace = "io.github.jeadyx.gitversionmanager"
    compileSdk = 34

    defaultConfig {
        minSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

group = "io.github.jeadyx.compose"
version = "1.7"
val tokenUsername:String by project
val tokenPassword:String by project
sonatypeUploader{
    bundleName = "GitVersionManager-$version"
    tokenName = tokenUsername
    tokenPasswd = tokenPassword
    pom = Action<MavenPom>{
        name.set("GitVersionManager")
        description.set("A library for version manager for your publish, eg: apk")
        url.set("https://github.com/jeadyx/GitVersionManager")
        scm {
            connection.set("scm:git:git://github.com/jeadyx/GitVersionManager.git")
            developerConnection.set("scm:git:ssh://github.com/jeadyx/GitVersionManager.git")
            url.set("https://github.com/jeadyx/GitVersionManager")
        }
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id.set("jeadyx")
                name.set("Jeady")
            }
        }
        withXml {
            val dependenciesNode = asNode().appendNode("dependencies")
            val dependencyNetManager = dependenciesNode.appendNode("dependency")
            dependencyNetManager.appendNode("groupId", "io.github.jeadyx.compose")
            dependencyNetManager.appendNode("artifactId", "SimpleNetManager")
            dependencyNetManager.appendNode("version", "1.4")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.simplenetmanager)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}