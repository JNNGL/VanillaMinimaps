plugins {
    java
    id("io.papermc.paperweight.userdev").version("1.7.1")
    id("xyz.jpenilla.run-paper").version("2.3.0")
    id("io.github.goooler.shadow").version("8.1.7")
}

group = "com.jnngl"
version = "1.0.1"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.21.1-R0.1-SNAPSHOT")
    implementation("net.elytrium:serializer:1.1.1")
    implementation("com.jnngl:mapcolor:1.0.1")
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    compileOnly("org.projectlombok:lombok:1.18.30")
    implementation("com.j256.ormlite:ormlite-jdbc:6.1")
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("net.elytrium.serializer", "com.jnngl.vanillaminimaps.serializer")
        exclude("org/slf4j/**")
        minimize()
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }

    assemble {
        dependsOn(reobfJar)
        dependsOn(shadowJar)
    }
}