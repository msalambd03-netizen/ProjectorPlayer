# My Projector Player (Android 11)

1. Upload project files to a GitHub repository (preserve `.github/workflows/build-apk.yml`).
2. Actions > Build Android APK > Run workflow, or push to main.
3. Open successful run > Artifacts > ProjectorPlayer-debug-APK > unzip > app-debug.apk.
4. Install APK on Android projector. Launch it and set a publicly reachable **raw JSON URL** (e.g. `https://raw.githubusercontent.com/USERNAME/REPO/main/video.json`).
5. Edit `video.json` on GitHub when changing the video, then press PLAY LATEST on projector.

Note: a public repository exposes the URLs in video.json. Do not store secrets, tokens or private links there. HTTP streams require the projector to reach the video server. This is an untested sample project, not a pre-built APK. Media3 codec support depends on Android device.
