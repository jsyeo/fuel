import com.google.protobuf.gradle.*

plugins {
    id(Protobuf.plugin)
}

sourceSets {
    getByName("test").java.srcDirs("${project.buildDir}/generated/source/proto/main/javalite")
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.6.1"
    }

    plugins {
        id("javalite") {
            artifact = "com.google.protobuf:protoc-gen-javalite:3.0.0"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.builtins {
                remove("java")
            }
            it.plugins {
                id("javalite")
            }
        }
    }
}

dependencies {
    api(project(":fuel"))

    implementation("com.google.protobuf:protobuf-lite:3.0.1")
    protobuf("com.google.protobuf:protobuf-java:3.6.1")
}
