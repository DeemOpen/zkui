NAME = zkui
VERSION=2.0-SNAPSHOT
REGISTRY = depot.fra.hybris.com:5000

.PHONY: all build publish

all: build publish

build:
	# mvn clean install
	cp config.cfg docker
	cp target/$(NAME)-*-jar-with-dependencies.jar docker
	docker build -t $(NAME):$(VERSION) --no-cache --rm docker
	rm docker/$(NAME)-*.jar
	rm docker/config.cfg

publish:
	docker tag -f $(NAME):$(VERSION) $(REGISTRY)/$(NAME):$(VERSION)
	docker tag -f $(NAME):$(VERSION) $(REGISTRY)/$(NAME):latest
	docker push $(REGISTRY)/$(NAME)