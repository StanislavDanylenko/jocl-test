plugins {
    id("java")
}

group = "stanislav.danylenko"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jocl:jocl:2.0.5")
    implementation("org.jogamp.jocl:jocl:2.3.2")
    implementation("org.jogamp.jocl:jocl-main:2.3.2")
    implementation("org.jogamp.gluegen:gluegen-rt:2.3.2")
    implementation("org.jogamp.gluegen:gluegen-rt-main:2.3.2")


    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}