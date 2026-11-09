# ==============================================================================
# Texidor Makefile
# ==============================================================================
# Usage:
#   make                     Build default: Universal Release APK
#   make ARCH=arm            Build for ARM
#   make ARCH=x86            Build for x86-64
#   make TYPE=debug          Build Debug variant
#   make all                 Build all 3 architectures in Release mode
#   make clean               Clean build directory
# ==============================================================================

ARCH ?= uni
TYPE ?= release

.PHONY: all default release debug arm x86 uni clean test help

default:
	@bash ./build.sh --$(ARCH) --$(TYPE)

all:
	@echo "Building all architectures in Release mode..."
	@bash ./build.sh --uni --release
	@bash ./build.sh --arm --release
	@bash ./build.sh --x86 --release
	@echo "All 3 APKs built in build-outputs/"

uni:
	@bash ./build.sh --uni --$(TYPE)

arm:
	@bash ./build.sh --arm --$(TYPE)

x86:
	@bash ./build.sh --x86 --$(TYPE)

release:
	@bash ./build.sh --$(ARCH) --release

debug:
	@bash ./build.sh --$(ARCH) --debug

test:
	@gradle :app:testDebugUnitTest

clean:
	@gradle clean
	@rm -rf build-outputs app/build

help:
	@echo "Texidor Makefile targets:"
	@echo "  make               - Build default (Universal Release APK)"
	@echo "  make uni           - Build Universal APK"
	@echo "  make arm           - Build ARM APK"
	@echo "  make x86           - Build x86-64 APK"
	@echo "  make release       - Build Release APK"
	@echo "  make debug         - Build Debug APK"
	@echo "  make all           - Build all 3 architectures (uni, arm, x86) in Release mode"
	@echo "  make test          - Run unit tests"
	@echo "  make clean         - Clean build caches"
