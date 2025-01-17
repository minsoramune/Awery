import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.compose)
	alias(libs.plugins.compose.compiler)
}

group = "com.mrboomdev.awery.desktop"
version = "1.0.0"

dependencies {
	// Core
	implementation(project(":ext"))
	implementation(kotlin("stdlib-jdk8"))
    implementation(libs.moshi)

	// Ui
	implementation(compose.desktop.currentOs)
	implementation(project(":ui"))

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.okhttp.brotli)
}

compose.desktop {
	application {
		mainClass = "Main"

		nativeDistributions {
			targetFormats(TargetFormat.Exe, TargetFormat.Deb)
			packageName = "com.mrboomdev.awery"
			packageVersion = "1.0.0"
		}
	}
}