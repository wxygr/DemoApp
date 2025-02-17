import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.VariantDimension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import java.io.File
import java.util.Properties
import kotlin.math.pow
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

const val appVersionName = "1.5.0"
val appVersionCode = appVersionName.versionCode
val javaVersion = JavaVersion.VERSION_11

val String.versionCode: Int
  get() = takeWhile { it.isDigit() || it == '.' }
    .split('.')
    .map { it.toInt() }
    .reversed()
    .sumByIndexed { index, unit ->
      // 1.2.3 -> 102030
      (unit * 10.0.pow(2 * index + 1)).toInt()
    }

fun VariantDimension.buildConfigField(field: BuildConfigField) {
  if (field.value is Int) {
    buildConfigField("Integer", field.key, field.value.toString())
  } else if (field.value is String) {
    buildConfigField("String", field.key, "\"${field.value}\"")
  }
}

inline fun <reified T : BaseExtension> Project.setupBase(
  module: Module, crossinline block: T.() -> Unit = {}
) {
  extensions.configure<BaseExtension> {
    resourcePrefix = "${module.tag}_"
    namespace = module.id
    compileSdkVersion(32)
    defaultConfig {
      minSdk = 21
      vectorDrawables.useSupportLibrary = true
      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets.configureEach {
      java.srcDirs("src/$name/kotlin")
    }
    buildFeatures.buildConfig = false
    compileOptions.setDefaultJavaVersion(javaVersion)
    (this as ExtensionAware).extensions.configure<KotlinJvmOptions> {
      jvmTarget = javaVersion.toString()
      // https://youtrack.jetbrains.com/issue/KT-41985
      freeCompilerArgs += listOf(
        "-progressive",
        "-opt-in=kotlin.RequiresOptIn",
        "-Xjvm-default=all"
      )
    }
    packagingOptions.resources.excludes += setOf(
      "**/*.proto",
      "**/*.bin",
      "**/*.java",
      "**/*.properties",
      "**/*.version",
      "**/*.*_module",
      "*.txt",
      "META-INF/services/**",
      "META-INF/com/**",
      "META-INF/licenses/**",
      "META-INF/AL2.0",
      "META-INF/LGPL2.1",
      "com/**",
      "kotlin/**",
      "kotlinx/**",
      "okhttp3/**",
      "google/**"
    )
    (this as? CommonExtension<*, *, *, *>)?.lint {
      abortOnError = true
    }
    (this as T).block()
  }
}

inline fun <reified T : BaseExtension> Project.setupCommon(
  module: Module, crossinline block: T.() -> Unit = {}
) = setupBase<T>(module) {
  flavorDimensions("channel")
  productFlavors {
    create("dev")
    create("prod")
  }
  block()
}

fun Project.setupLib(
  module: LibModule, block: LibraryExtension.() -> Unit = {}
) = setupCommon<LibraryExtension>(module) {
  dependencies.add("implementation", project(LibModule.Common))
  block()
}

fun Project.setupApp(
  module: AppModule, block: BaseAppModuleExtension.() -> Unit = {}
) = setupCommon<BaseAppModuleExtension>(module) {
  defaultConfig {
    applicationId = module.id
    targetSdk = 32
    versionCode = appVersionCode
    versionName = appVersionName
    resourceConfigurations += setOf("en", "zh-rCN", "xxhdpi")
  }
  signingConfigs.create("release") {
    keyAlias = getSignProperty("keyAlias")
    keyPassword = getSignProperty("keyPassword")
    storeFile = File(rootDir, getSignProperty("storeFile"))
    storePassword = getSignProperty("storePassword")
    enableV3Signing = true
    enableV4Signing = true
  }
  buildTypes {
    release {
      resValue("string", "app_name", module.appName)
      signingConfig = signingConfigs["release"]
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles("$rootDir/gradle/proguard-rules.pro")
    }
    debug {
      resValue("string", "app_name", "${module.appName}.debug")
      signingConfig = signingConfigs["release"]
      isJniDebuggable = true
      isRenderscriptDebuggable = true
      isCrunchPngs = false
    }
  }
  dependenciesInfo.includeInApk = false
  applicationVariants.all {
    outputs.all {
      (this as? ApkVariantOutputImpl)?.outputFileName = "../../../../" +
        "${module.appName}_${versionName}_${versionCode}_${flavorName}_${buildType.name}.apk"
    }
  }
  dependencies.add("implementation", project(LibModule.Common))
  block()
}

private fun DependencyHandler.config(operation: String, vararg names: Any): Array<Dependency?> =
  names.map { add(operation, it) }.toTypedArray()

private fun Project.getSignProperty(
  key: String, path: String = "gradle/keystore.properties"
): String = Properties().apply {
  rootProject.file(path).inputStream().use(::load)
}.getProperty(key)

private inline fun <T> List<T>.sumByIndexed(selector: (Int, T) -> Int): Int {
  var index = 0
  var sum = 0
  for (element in this) {
    sum += selector(index++, element)
  }
  return sum
}

private fun Project.project(module: Module): Project = project(":${module.tag}")
