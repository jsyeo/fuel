import com.google.protobuf.gradle.ExecutableLocator
import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    id("com.google.protobuf")
}

sourceSets["test"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("${project.buildDir}/gen/main/javalite")
}

protobuf.protobuf.run {

    generatedFilesBaseDir = "${project.buildDir}/gen"

    protoc(delegateClosureOf<ExecutableLocator> {
        artifact = "com.google.protobuf:protoc:3.5.1-1"
    })

    plugins(delegateClosureOf<NamedDomainObjectContainer<ExecutableLocator>> {
        this {
            "javalite" {
                artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0"
            }
        }
    })

    generateProtoTasks(delegateClosureOf<ProtobufConfigurator.GenerateProtoTaskCollection> {
        all().forEach {
            it.plugins(delegateClosureOf<NamedDomainObjectContainer<GenerateProtoTask.PluginOptions>> {
                this {
                    "javalite"()
                }
            })

            it.builtins(delegateClosureOf<NamedDomainObjectContainer<GenerateProtoTask.PluginOptions>> {
                this {
                    remove("java"())
                }
            })
        }
    })
}

dependencies {
    api(project(":fuel"))

    implementation("com.google.protobuf:protobuf-lite:3.0.1")
    protobuf("com.google.protobuf:protobuf-java:3.5.1")
}
