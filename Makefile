APP_NAME=kubernetes-custom-hpa
VERSION=$(shell cat ./helm/Chart.yaml | grep appVersion | grep -o '[0-9\.]\+')
IMAGE_NAME=nanit/$(APP_NAME):$(VERSION)

dev:
	source ./envfile.dev && export $(shell cut -d= -f1 envfile.dev) && cd app && lein with-profile +test repl

package:
	helm package helm/custom-hpa/

ci:
	@echo "Running tests..."
	source ./envfile.dev && export $(shell cut -d= -f1 envfile.dev) && lein with-profile +test test
	@echo "Done. Starting release version $(VERSION)..."
	sudo docker build -t $(DEMUXER_IMAGE_NAME) demuxer && sudo docker push $(DEMUXER_IMAGE_NAME)