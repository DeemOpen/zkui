NAME = zkui
VERSION=2.0-SNAPSHOT

.PHONY: all build publish

all: build publish

build:
	docker build -t $(NAME):$(VERSION) .

publish:
	docker tag $(NAME):$(VERSION) $(NAME):$(VERSION)
	docker tag $(NAME):$(VERSION) $(NAME):latest
	docker push $(NAME)
