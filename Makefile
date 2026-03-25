SHELL := /bin/sh

MVNW ?= ./mvnw
JAVA ?= java
DOCKER ?= docker
APP_NAME ?= prodops-control-tower-mcp
DOCKER_IMAGE ?= $(APP_NAME):local
JAR ?= target/prodops-control-tower-mcp-0.1.0-SNAPSHOT.jar

.PHONY: help clean format test verify package run-fixture-http run-fixture-stdio run-live-http docker-build

help:
	@printf '%s\n' \
		'Targets:' \
		'  make format            Apply Spotless formatting' \
		'  make test              Run unit and integration tests' \
		'  make verify            Run the full Maven verification lifecycle' \
		'  make package           Build the runnable jar' \
		'  make run-fixture-http  Run fixture mode over HTTP on localhost' \
		'  make run-fixture-stdio Run fixture mode over stdio' \
		'  make run-live-http     Run live mode over HTTP using externalized config' \
		'  make docker-build      Build the production container image' \
		'  make clean             Remove build outputs'

clean:
	$(MVNW) -B -ntp clean

format:
	$(MVNW) -B -ntp spotless:apply

test:
	$(MVNW) -B -ntp test

verify:
	$(MVNW) -B -ntp clean verify

package:
	$(MVNW) -B -ntp -DskipTests package

run-fixture-http:
	SPRING_PROFILES_ACTIVE=fixture,http PRODOPS_BIND_ADDRESS=127.0.0.1 PORT=8080 MANAGEMENT_PORT=8081 $(MVNW) -B -ntp spring-boot:run

run-fixture-stdio: package
	SPRING_PROFILES_ACTIVE=fixture,stdio $(JAVA) -jar $(JAR)

run-live-http:
	SPRING_PROFILES_ACTIVE=live,http PRODOPS_BIND_ADDRESS=127.0.0.1 PORT=8080 MANAGEMENT_PORT=8081 $(MVNW) -B -ntp spring-boot:run

docker-build:
	$(DOCKER) build -t $(DOCKER_IMAGE) .
