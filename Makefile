SHELL := /bin/bash
.DEFAULT_GOAL := build

.PHONY: all build install run screenshot clean keystore

all: build

build:
	./build.sh build

install: build
	./build.sh install

run: install
	./build.sh run

screenshot:
	./build.sh screenshot

keystore:
	./build.sh keystore

clean:
	./build.sh clean
