APP_NAME=kubernetes-custom-hpa
CHART_PATH=$(shell pwd)/helm/custom-hpa
VERSION=$(shell cat $(CHART_PATH)/Chart.yaml | grep version | grep -o '[0-9\.]\+')
APP_VERSION=$(shell cat $(CHART_PATH)/Chart.yaml | grep appVersion | grep -o 'v[0-9\.]\+')
IMAGE_NAME=nanit/$(APP_NAME):$(APP_VERSION)
CHART_PACKAGE_FILE=$(CHART_PATH)/custom-hpa-$(VERSION).tgz
RELEASER_IMAGE=quay.io/helmpack/chart-releaser:v1.0.0
GIT_REPO=helm-charts
OWNER=nanit
TOKEN=$(GITHUB_TOKEN)

dev:
	source ./envfile.dev && export $(shell cut -d= -f1 envfile.dev) && cd app && lein with-profile +test repl

package:
	helm package $(CHART_PATH) -d $(CHART_PATH)/pack

upload:
	docker run --rm \
        -v $(CHART_PATH)/pack:/charts \
        $(RELEASER_IMAGE) cr upload \
        -r $(GIT_REPO) \
        -o $(OWNER) \
        -p /charts \
        -t $(TOKEN)
	@echo "Done uploading release to $(OWNER)/$(GIT_REPO) github repo"

cleanup:
	mkdir -p $(CHART_PATH)/pack
	rm -f $(CHART_PATH)/pack/*

release: ci cleanup package upload

lein-test:
	@echo "Running tests..."
	source ./envfile.dev && export $(shell cut -d= -f1 envfile.dev) && cd app && lein with-profile +test test

helm-test:
	@echo "Validating helm chart"
	helm lint $(CHART_PATH) -f $(CHART_PATH)/ci/values.yaml

docker:
	@echo "Building Dockerfile"
	sudo docker pull $(IMAGE_NAME) || (sudo docker build -t $(IMAGE_NAME) app && sudo docker push $(IMAGE_NAME))

ci: lein-test helm-test docker