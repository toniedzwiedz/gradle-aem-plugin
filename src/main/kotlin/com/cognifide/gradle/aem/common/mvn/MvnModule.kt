package com.cognifide.gradle.aem.common.mvn

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

class MvnModule(val build: MvnBuild, val name: String, val project: Project) {

    val aem = build.aem

    val dir = aem.obj.dir()

    val artifactFiles = aem.obj.map<String, Provider<RegularFile>> { set(mapOf()) }

    val artifactTasks = aem.obj.map<String, TaskProvider<Exec>> { set(mapOf()) }

    val pom = aem.obj.file { set(dir.file("pom.xml")) }

    val gav = aem.obj.typed<MvnGav> {
        set(pom.map { MvnGav.readFile(it.asFile) })
    }

    val repositoryDir = aem.obj.dir {
        set(build.repositoryDir.map { it.dir("${gav.get().groupId}/${gav.get().artifactId}") })
    }

    val inputFiles get() = project.fileTree(dir).matching { pf ->
        pf.excludeTypicalOutputs()
        pf.exclude("**/package-lock.json")
    }

    val outputFiles get() = project.fileTree(targetDir)

    val targetDir = aem.obj.dir {
        set(dir.dir("target"))
    }

    fun targetFile(extension: String) = targetDir.map { it.file("${gav.get().artifactId}-${gav.get().version ?: build.rootModule.get().gav.get().version!!}.$extension") }

    fun installPom() = exec("pom") {
        moreArgs(listOf("-N"))
        inputs.file(pom)
        outputs.dir(repositoryDir)
    }

    val frontendIndicator = aem.obj.boolean {
        set(dir.map { it.file("clientlib.config.js").asFile.exists() })
    }

    val frontendProfiles = aem.obj.strings {
        set(project.provider {
            mutableListOf<String>().apply {
                if (aem.prop.boolean("mvn.frontend.dev") == true) {
                    add("fedDev")
                }
            }
        })
    }

    fun buildFrontend(extension: String = "zip", options: Task.() -> Unit = {}) = exec("frontend") {
        moreArgs(frontendProfiles.get().map { "-P$it" })
        inputs.property("profiles", frontendProfiles.get())
        inputs.files(inputFiles)
        outputArtifact(extension)
        options()
    }.also { artifactTasks.put(extension, it) }

    fun buildJar(options: Task.() -> Unit = {}) = buildArtifact("jar", options)

    fun buildZip(options: Task.() -> Unit = {}) = buildArtifact("zip", options)

    fun buildArtifact(extension: String, options: Task.() -> Unit = {}) = exec(extension) {
        inputs.files(inputFiles)
        outputArtifact(extension)
        options()
    }.also { artifactTasks.put(extension, it) }

    fun exec(name: String, options: Exec.() -> Unit) = build.tasks.register<Exec>(name) {
        executable("mvn")
        moreArgs(listOf())
        workingDir(dir)
        options()
    }.also { task ->
        build.tasks.named<Delete>(LifecycleBasePlugin.CLEAN_TASK_NAME).configure { it.delete(task) }
        build.tasks.named<Task>(LifecycleBasePlugin.BUILD_TASK_NAME).configure { it.dependsOn(task) }
    }

    val commonArgs = aem.obj.strings {
        convention(listOf("-B", "-T", "2C"))
        aem.prop.string("mvn.commonArgs")?.let { set(it.split(" ")) }
    }

    fun Exec.outputArtifact(extension: String) {
        val file = targetFile(extension)
        artifactFiles.put(extension, file)
        outputs.file(file)
    }

    fun Exec.moreArgs(args: Iterable<String>) {
        args(commonArgs.get() + listOf("clean", "install") + args)
    }

    companion object {
        const val NAME_ROOT = "root"
    }
}
