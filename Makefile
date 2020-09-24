VERSION=$(shell cat ./helm/Chart.yaml | grep appVersion | grep -o '[0-9\.]\+')
APP_NAME=custom-hpa
IMAGE_NAME=

dev:
	source ./envfile.dev && export $(shell cut -d= -f1 envfile.dev) && cd app && lein with-profile +test repl

docker:
	sudo docker pull $(DEMUXER_IMAGE_NAME) || (sudo docker build -t $(DEMUXER_IMAGE_NAME) demuxer && sudo docker push $(DEMUXER_IMAGE_NAME))