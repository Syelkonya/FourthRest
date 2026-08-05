plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "su.syel"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	compileOnly("org.projectlombok:lombok:1.18.46")
	annotationProcessor("org.projectlombok:lombok:1.18.46")

	implementation("org.mapstruct:mapstruct:1.5.5.Final")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
	implementation("com.opencsv:opencsv:5.12.0")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")

// ===== MongoDB =====
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

// ===== ClickHouse =====
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("com.clickhouse:clickhouse-jdbc:0.9.8")

// ===== Testcontainers =====
	testImplementation("org.testcontainers:testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mongodb")
	testImplementation("org.testcontainers:clickhouse")

// ===== Awaitility (ожидание async в тестах) =====
	testImplementation("org.awaitility:awaitility")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

extra["springCloudVersion"] = "2025.1.2"

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
		mavenBom("org.testcontainers:testcontainers-bom:1.21.4")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
