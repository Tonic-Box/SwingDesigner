plugins {
    id("java")
}

group = "tonic.dev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:2.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation(group = "com.fifesoft", name = "rsyntaxtextarea", version = "3.1.2")
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.20")
    annotationProcessor(group = "org.pf4j", name = "pf4j", version = "3.6.0")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}