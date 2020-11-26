package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.AemExtension
import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.internal.DefaultJavaToolchainService
import org.gradle.jvm.toolchain.internal.JavaToolchainQueryService

class JavaSupport(private val aem: AemExtension) {

    val version = aem.obj.string {
        convention("11")
        aem.prop.string("javaSupport.version")?.let { set(it) }
    }

    val compatibilityVersion = aem.obj.typed<JavaVersion> {
        convention(version.map { JavaVersion.toVersion(it) })
    }

    val languageVersion = aem.obj.typed<JavaLanguageVersion> {
        convention(version.map { JavaLanguageVersion.of(it) })
    }

    val toolchainQuery by lazy { aem.common.services.get<JavaToolchainQueryService>() }

    val toolchains by lazy {
        aem.project.extensions.create(
                JavaToolchainService::class.java, TOOLCHAINS_EXTENSION, DefaultJavaToolchainService::class.java, toolchainQuery
        )
    }

    val launcher get() = toolchains.launcherFor { it.languageVersion.set(languageVersion) }

    val compiler get() = toolchains.compilerFor { it.languageVersion.set(languageVersion) }

    companion object {
        const val TOOLCHAINS_EXTENSION = "aemJavaToolchains"
    }
}
