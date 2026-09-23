# AI Call Gateway

Experimental Android cellular-call gateway targeting Samsung Galaxy S24 FE.

## Milestone 1
- Native Android app
- Runtime phone permissions
- Test cellular call via Android Telecom
- Request speakerphone at call start
- Basic number validation and emergency-number blocking
- GitHub Actions APK build

This first milestone intentionally proves device/SIM call control before remote command transport and AI audio bridging are added.

## Install
Open the latest GitHub Actions run, download the `ai-call-gateway-debug` artifact, unzip it, then install `app-debug.apk` on the Android device. Android may ask you to allow installs from the browser/files app.

Only test calls to numbers you control or have permission to call.
