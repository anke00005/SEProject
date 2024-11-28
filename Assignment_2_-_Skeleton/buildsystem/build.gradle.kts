plugins {
  kotlin("jvm") version "2.0.21"
}

kotlin {
  jvmToolchain(22)
}

group = "de.uni_saarland.cs.se"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(kotlin("test"))
}

val featureAuthentication =
  providers.gradleProperty("authentication").getOrElse("false").toBoolean()
val featureColor =
  providers.gradleProperty("color").getOrElse("false").toBoolean()
val featureLogging =
  providers.gradleProperty("logging").getOrElse("false").toBoolean()
val featureEncryption =
  providers.gradleProperty("encryption").getOrNull()
val encEnabled = listOf("reverse", "rot13").contains(featureEncryption)

sourceSets {
  main {
    kotlin {
      setSrcDirs(listOf("src/main/kotlin/base"))

      // TODO: Add additional sources based on configuration.
      // For example:
      when {
        !featureAuthentication && !featureColor && !featureLogging && !encEnabled ->
          srcDir("src/main/kotlin/default")
      }
    }
  }

  test {
    kotlin {
      setSrcDirs(emptyList<String>())

      when {
        !featureAuthentication && !featureColor && !featureLogging && !encEnabled ->
          srcDir("src/test/kotlin/default")
        featureAuthentication && !featureColor && !featureLogging && !encEnabled ->
          srcDir("src/test/kotlin/auth")
        !featureAuthentication && featureColor && !featureLogging && !encEnabled ->
          srcDir("src/test/kotlin/color")
        !featureAuthentication && !featureColor && !featureLogging && featureEncryption == "reverse" ->
          srcDir("src/test/kotlin/encreverse")
        !featureAuthentication && !featureColor && !featureLogging && featureEncryption == "rot13" ->
          srcDir("src/test/kotlin/encrot13")
        !featureAuthentication && !featureColor && featureLogging && !encEnabled ->
          srcDir("src/test/kotlin/log")
      }
    }
  }
}

tasks.test {
  useJUnitPlatform()
}
