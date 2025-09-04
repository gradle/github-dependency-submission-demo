// Constrain 'com.squareup.okio:okio' to avoid https://github.com/advisories/GHSA-w33c-445m-f8w7
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        constraints {
            classpath(libs.okio)
        }
    }
}

plugins {
    alias(libs.plugins.versions)
}
