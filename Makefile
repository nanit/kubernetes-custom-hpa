APP_NAME=kubernetes-custom-hpa
CHART_PATH=$(shell pwd)/helm/custom-hpa
VERSION=$(shell cat $(CHART_PATH)/Chart.yaml | grep version | grep -o '[0-9\.]\+')
IMAGE_NAME=nanit/$(APP_NAME):$(VERSION)
CHART_PACKAGE_FILE=$(CHART_PATH)/custom-hpa-$(VERSION).tgz
INDEX_PATH=$(shell pwd)/../helm-charts/
RELEASER_IMAGE=quay.io/helmpack/chart-releaser:v1.0.0
GIT_REPO=helm-charts
OWNER=nanit
TOKEN=$(GITHUB_TOKEN)

package:
	helm package helm/custom-hpa/ -d $(CHART_PATH)/pack

upload:
	docker run --rm \
        -v $(CHART_PATH)/pack:/charts $(RELEASER_IMAGE) cr upload \
        -r $(GIT_REPO) \
        -o $(OWNER) \
        -p /charts \
        -t $(TOKEN)
	@echo "Done uploading release to $(OWNER)/$(GIT_REPO) github repo"

index:
	docker run --rm \
		-v $(CHART_PATH)/pack:/charts \
		-v $(INDEX_PATH):/index \
		$(RELEASER_IMAGE) cr index \
		-c https://github.com/$(OWNER)/$(GIT_REPO) \
		-r $(GIT_REPO) \
		-i /index \
		-o $(OWNER) \
		-p /charts \
		-t $(TOKEN)
	@echo "Done updating index file"

ci:
	@echo "Running tests..."
	source ./envfile.dev && export $(shell cut -d= -f1 envfile.dev) && cd app && lein with-profile +test test
	@echo "Building Dockerfile"
	sudo docker pull $(IMAGE_NAME) || sudo docker build -t $(IMAGE_NAME) app && sudo docker push $(IMAGE_NAME)

release: ci package upload index
	@echo "Updated index with new release. Do not forget to push updated index file."

dev:
	source ./envfile.dev && export $(shell cut -d= -f1 envfile.dev) && cd app && lein with-profile +test repl