import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import java.io.FileInputStream
import java.util.Base64
import java.util.Properties

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("application")
  id("com.apollographql.apollo")
  id("com.bmuschko.docker-java-application")
}

group = "org.jraf"
version = "1.0.0"

repositories {
  mavenLocal()
  mavenCentral()
}

kotlin {
  jvmToolchain(11)
}

application {
  mainClass.set("org.jraf.confluencetobookmarks.main.MainKt")
}

// Build properties
ext["buildProperties"] = loadPropertiesFromFile("build.properties")
fun Project.loadPropertiesFromFile(fileName: String): Properties {
  val file = file(fileName)
  if (!file.exists()) {
    logger.warn("$fileName file does not exist: creating it now - please check its values")
    copy {
      from("${fileName}.SAMPLE")
      into(project.projectDir)
      rename { fileName }
    }
  }
  val res = Properties()
  val fileInputStream = FileInputStream(file)
  fileInputStream.use {
    res.load(it)
  }
  return res
}

apollo {
  val buildProperties = rootProject.ext["buildProperties"] as Properties

  service("atlassian") {
    packageName.set("org.jraf.confluencetobookmarks")

    introspection {
      endpointUrl.set("https://${buildProperties["domain"]}.atlassian.net/cgraphql")
      schemaFile.set(file("src/main/graphql/schema.graphqls"))
      val atlassianEmail = buildProperties["atlassianEmail"]
      val atlassianApiToken = buildProperties["atlassianApiToken"]
      headers.put("Authorization", "Basic ${Base64.getEncoder().encodeToString("$atlassianEmail:$atlassianApiToken".toByteArray())}")
    }
  }
}

dependencies {
  // Ktor
  implementation(Ktor.server.core)
  implementation(Ktor.server.netty)
  implementation(Ktor.server.defaultHeaders)
  implementation(Ktor.server.statusPages)
  implementation(Ktor.server.contentNegotiation)
  implementation(Ktor.plugins.serialization.kotlinx.json)

  // Logging
  implementation("org.slf4j:slf4j-simple:_")

  // JSON
  implementation(KotlinX.serialization.json)

  // Apollo
  // implementation(ApolloGraphQL.runtime) // <- points to v3, see https://github.com/Splitties/refreshVersions/issues/722
  implementation("com.apollographql.apollo:apollo-runtime:_")
}

docker {
  javaApplication {
    // Use OpenJ9 instead of the default one
    baseImage.set("adoptopenjdk/openjdk11-openj9:x86_64-ubuntu-jre-11.0.24_8_openj9-0.46.1")
    maintainer.set("BoD <BoD@JRAF.org>")
    ports.set(listOf(8080))
    images.add("bodlulu/${rootProject.name}:latest")
    jvmArgs.set(listOf("-Xms16m", "-Xmx128m"))
  }
  registryCredentials {
    username.set(System.getenv("DOCKER_USERNAME"))
    password.set(System.getenv("DOCKER_PASSWORD"))
  }
}

tasks.withType<DockerBuildImage> {
  platform.set("linux/amd64")
}

tasks.withType<com.bmuschko.gradle.docker.tasks.image.Dockerfile> {
  environmentVariable("MALLOC_ARENA_MAX", "4")
}

// `./gradlew downloadAtlassianApolloSchemaFromIntrospection` to download the schema
// `./gradlew distZip` to create a zip distribution
// `./gradlew refreshVersions` to update dependencies
// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
