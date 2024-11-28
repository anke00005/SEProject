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

tasks.test {
  useJUnitPlatform()
}