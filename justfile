set quiet
set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

default:
    just --list

assets-generate:
    ./assets/generate-assets.sh

build-debug:
    ./gradlew :app:assembleDebug

build-release:
    ./gradlew :app:assembleRelease

check:
    ./gradlew :app:assembleDebug :app:ktlintCheck :app:lint :app:testDebugUnitTest

format:
    ./gradlew :app:ktlintFormat

hooks-install:
    ./gradlew lefthookInstall

hooks-version:
    ./gradlew lefthookVersion

install-debug:
    adb install -r app/build/outputs/apk/debug/app-debug.apk

launch:
    adb shell am start -n dev.beeman.glint/.MainActivity

release-signed: setup-signer
    ./gradlew :app:assembleRelease
    test -f app/build/outputs/apk/release/app-release.apk
    printf "Signed release APK: app/build/outputs/apk/release/app-release.apk\n"

setup-signer:
    mkdir -p .signing
    if [ -f signing.properties ]; then printf "signing.properties already exists\n"; exit 0; fi; \
    RELEASE_PASSWORD="$(openssl rand -base64 32)"; \
    keytool -genkeypair \
        -alias glint-release \
        -dname "CN=Glint, OU=Glint, O=Glint, L=Local, ST=Local, C=US" \
        -keyalg RSA \
        -keypass "$RELEASE_PASSWORD" \
        -keysize 4096 \
        -keystore .signing/glint-release.jks \
        -storepass "$RELEASE_PASSWORD" \
        -validity 10000; \
    { \
        printf "keyAlias=glint-release\n"; \
        printf "keyPassword=%s\n" "$RELEASE_PASSWORD"; \
        printf "storeFile=.signing/glint-release.jks\n"; \
        printf "storePassword=%s\n" "$RELEASE_PASSWORD"; \
    } > signing.properties; \
    printf "Created .signing/glint-release.jks and signing.properties\n"

test:
    ./gradlew :app:testDebugUnitTest

wallpaper-activities:
    adb shell cmd package query-activities -a android.intent.action.SET_WALLPAPER
