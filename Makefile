SHELL := bash# we want bash behaviour in all shell invocations
PLATFORM := $(shell uname)

VERSION ?= latest



### TARGETS ###
#
.DEFAULT_GOAL := help

.PHONY: build
build: ## b  | Build JAR
	@docker run \
	  --rm \
	  --interactive --tty \
	  --workdir /workspace \
	  --volume $(CURDIR)/benchmark:/workspace \
	  --volume rabbittesttool-maven-cache:/root/.m2 \
	  maven:3.6-jdk-8 \
	  mvn clean package
.PHONY: b
b: build

.PHONY: docker-build
docker-build:
	@docker build \
	  --tag pivotalrabbitmq/rabbittesttool:$(VERSION) \
	  benchmark

.PHONY: test-docker-image
test-docker-image: docker-build
	@docker run -it --rm pivotalrabbitmq/rabbittesttool:$(VERSION) help

.PHONY: docker-push
docker-push: test-docker-image
	@docker push pivotalrabbitmq/rabbittesttool:$(VERSION)

.PHONY: help
help:
	@awk -F"[:#]" '/^[^\.][a-zA-Z\._\-]+:+.+##.+$$/ { printf "\033[36m%-24s\033[0m %s\n", $$1, $$4 }' $(MAKEFILE_LIST) \
	| sort
