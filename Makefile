BUILD_DIR ?= build
CONFIG ?= Release
JOBS ?= 2
CMAKE ?= cmake
NATIVE_MAKE ?= $(MAKE)
.DEFAULT_GOAL := build
.PHONY: configure build test clean rebuild help
configure:
	$(CMAKE) -S . -B "$(BUILD_DIR)" -G "MinGW Makefiles" "-DCMAKE_MAKE_PROGRAM=$(NATIVE_MAKE)" "-DCMAKE_BUILD_TYPE=$(CONFIG)"
build: configure
	$(CMAKE) --build "$(BUILD_DIR)" --parallel $(JOBS)
test: build
	ctest --test-dir "$(BUILD_DIR)" --output-on-failure
clean:
	$(CMAKE) "-DNEBULA_ROOT=$(CURDIR)" "-DNEBULA_BUILD=$(BUILD_DIR)" -P tools/clean.cmake
rebuild:
	$(MAKE) clean
	$(MAKE) build
help:
	@echo make build    - compile Java, verify dependencies, package payload and build DLL
	@echo make test     - build and execute Java/native checks
	@echo make clean    - remove verified generated outputs only
	@echo make rebuild  - clean then build
	@echo Variables: BUILD_DIR CONFIG JOBS CMAKE NATIVE_MAKE
