plugins {
    id(Plugins.androidApplication)
}

setupApp("${appPackageName}.test", "test").run {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(
        Libs.fastjson,
        *Libs.gson,
        Libs.mmkv,
        *Libs.objectBox
    )
    testImplementation("junit:junit:4.13.1")
    androidTestImplementation(
        "androidx.test.ext:junit:1.1.2",
        "androidx.test.espresso:espresso-core:3.3.0"
    )
}