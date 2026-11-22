# NS Player – Cross-Platform IPTV Media Player (Android TV + Mobile)

**NS Player** is a production-grade, native Android IPTV streaming media player engineered for Android Mobile (smartphones, tablets) and Android TV / Google TV.

Inspired by and substantially improving upon the reference *Network Stream Player* (`com.genuine.leone`), this project is written from the ground up using **Kotlin**, **Jetpack Compose / Material 3**, and **AndroidX Media3 ExoPlayer**. It delivers high-performance streaming, robust M3U/M3U8 playlist handling, XMLTV EPG scheduling, adaptive bitrate streaming, custom headers, and a true 10-foot TV remote-control experience.

---

## 1. Key Architectural Improvements Over Reference APK

| Capability | Reference APK (`com.genuine.leone`) | Improved NS Player (`com.genuine.nsplayer`) |
| :--- | :--- | :--- |
| **Android TV Navigation** | Mobile UI stretched onto TV; no focus rings; touch-first. | **True 10-foot TV experience:** Navigation Rail, focus scale animations, glow borders, and full D-pad remote support. |
| **Player Controls** | Basic controls with confusing vertical swipe gestures and invasive ads. | **Ad-free modern OSD:** Explicit manual buttons and interactive sliders for Volume and Brightness (NO drag gestures required), Prev/Next channel zapping, and in-player quick channel drawer. |
| **Device Mode Detection** | Static manifest flags without UI differentiation. | **Automatic runtime detection:** Dynamically switches between Mobile Scaffold (bottom nav) and TV Navigation Rail. |
| **M3U / M3U8 Engine** | Flat synchronous list parsing; risk of UI freezing. | **Asynchronous reactive Flow parser:** Stream-based batch loading capable of parsing 10,000+ entries without blocking the main thread. |
| **Content Organization** | Single flat channel list per playlist. | **Automated categorization:** Automatically groups items into Live TV, Movies (VOD), and TV Series. |
| **EPG Support** | **None.** | **Full XMLTV guide engine:** Asynchronously parses schedules and displays current and upcoming programs. |
| **Player UX & Formats** | Basic player with fixed scaling. | **Cinematic streaming player:** Aspect ratio scaling (Fit, 16:9, Zoom, Stretch, 4:3), multi-audio track switcher, embedded subtitle selector, sleep timer, and live specs badge. |
| **Stream Recovery** | Playback halts on timeout. | **Auto-reconnection:** Exponential backoff reconnection algorithm handles network blips. |
| **Security & Privacy** | Plaintext preferences; telemetry SDKs. | **Private local storage:** Zero telemetry, PIN-protected parental controls. |

---

## 2. Redesigned UI & Player Controls

### Manual Volume & Brightness Controls (No Gestures)
- **Elimination of Gestures:** Vertical drag swipes for volume and brightness have been completely removed, preventing accidental changes and ensuring 100% remote D-pad compatibility.
- **Manual Volume Control Bar:**
  - Dedicated Mute / Unmute icon button.
  - Step Down `[-]` button (decreases volume level by 1 step).
  - Interactive Volume Slider (drag or tap to set exact level).
  - Step Up `[+]` button (increases volume level by 1 step).
  - Real-time percentage badge (`Vol: 75%`).
  - Integrated with Android `AudioManager.STREAM_MUSIC`.
- **Manual Brightness Control Bar:**
  - Brightness icon indicator (`Low`, `Medium`, `High`).
  - Step Down `[-]` button (decreases window brightness by 10%).
  - Interactive Brightness Slider (drag or tap to set exact level).
  - Step Up `[+]` button (increases window brightness by 10%).
  - Real-time percentage badge (`Bri: 80%`).
  - Integrated with Android `window.attributes.screenBrightness`.
- **Center Floating HUD:** Whenever volume, brightness, or aspect ratio is modified, a sleek floating glass pill displays the new value with an animated progress bar and auto-fades after 1.5 seconds.
- **D-Pad Focusable:** All manual buttons, sliders, and controls are focusable with Android TV D-pad remote navigation.

### In-Player Quick Channels Drawer
- Fast zapping: Users can browse the playlist channels and zap to any stream without exiting the full-screen player.
- Channel previous (`SkipPrevious`) and channel next (`SkipNext`) buttons for rapid channel surfing.

### Premium Streaming Aesthetic Across Screens
- **Deep Obsidian & Slate Dark Theme:** Glassmorphic surfaces with subtle borders (`#1B202E`), glowing focus highlights (`#3D7BFF`), and customizable theme accents (Obsidian, Frosty Cyan, Ruby Glow, Emerald Stream, Amber Glow, Cyber Purple).
- **Home Dashboard:**
  - Spotlight Hero Card with active playlist status, total channels, and "Watch Now" action.
  - Quick Hub Rail (Live TV, Movies, TV Series, Playlists).
  - Continue Watching carousel with progress bars and quick resume.
  - Favorites carousel with glowing star indicators.
  - Direct Stream dialog with one-click test presets.
- **Channel List Screen:**
  - Modern capsule search bar with instant query filtering.
  - Segmented content type chips (All, Live TV, Movies, Series).
  - Horizontally scrollable category pills with channel counts.
  - Adaptive channel grid with high-res logos, live indicators, and favorite toggles.
- **Playlist Management Screen:**
  - Active playlist badge, channel count, and last refreshed timestamps.
  - Add playlist dialog with remote URL and local file import options.
- **Settings Screen:**
  - Categorized preferences for video decoding (GPU hardware acceleration), stream buffer profiles (Low Latency, Normal, Deep Buffer), default aspect ratio, parental controls PIN, and platform diagnostics.

---

## 3. Technology Stack

- **Language:** Kotlin 2.0.21
- **UI Toolkit:** Jetpack Compose & Material 3 (BOM 2024.10.00)
- **Media Framework:** AndroidX Media3 1.4.1 (ExoPlayer, HLS, DASH, RTSP, UI, Session)
- **Database:** AndroidX Room 2.6.1 (SQLite) with KSP
- **Data Persistence:** Jetpack DataStore Preferences 1.1.1
- **Image Loading:** Coil Compose 2.7.0 with 100MB disk logo cache
- **Networking:** OkHttp 4.12.0
- **Asynchronous Execution:** Kotlin Coroutines & Flow

---

## 4. Project Structure

```
NS Player/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/genuine/nsplayer/
│   │   │   │   ├── NSPlayerApplication.kt
│   │   │   │   ├── core/
│   │   │   │   │   ├── DeviceType.kt          # Automatic Mobile vs. TV detection
│   │   │   │   │   └── PreferencesManager.kt  # DataStore settings & parental PIN
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/Models.kt        # Channel, Playlist, History, EPG models
│   │   │   │   │   ├── local/Entities.kt      # Room database entities
│   │   │   │   │   ├── local/Daos.kt          # Room DAOs
│   │   │   │   │   ├── local/AppDatabase.kt   # Room database singleton
│   │   │   │   │   ├── parser/M3UParser.kt    # High-speed streaming M3U parser
│   │   │   │   │   ├── parser/EPGParser.kt    # XMLTV guide parser
│   │   │   │   │   └── repository/Repositories.kt # Repository layer
│   │   │   │   ├── player/
│   │   │   │   │   └── MediaEngine.kt         # Media3 ExoPlayer wrapper & reconnector
│   │   │   │   └── ui/
│   │   │   │       ├── MainActivity.kt        # Adaptive entry point (Mobile + TV)
│   │   │   │       ├── theme/Theme.kt         # Glassmorphic color palette & theme engine
│   │   │   │       ├── common/FocusableCard.kt# TV focus-scaled card
│   │   │   │       ├── common/Components.kt   # HeroBanner, QuickNavCard, ChannelCard
│   │   │   │       ├── home/HomeScreen.kt     # Dashboard, favorites, continue watching
│   │   │   │       ├── channel/ChannelListScreen.kt # Category filtering & channel grid
│   │   │   │       ├── playlist/PlaylistScreen.kt   # Playlist management & add dialog
│   │   │   │       ├── settings/SettingsScreen.kt   # App settings & parental PIN
│   │   │   │       ├── player/PlayerActivity.kt     # Fullscreen Media3 player & manual controls
│   │   │   │       └── viewmodel/ViewModels.kt      # MainViewModel & PlayerViewModel
│   │   │   └── res/                           # Icons, TV banners, strings, themes
│   │   └── test/
│   │       └── java/com/genuine/nsplayer/
│   │           └── M3UParserTest.kt           # Automated unit tests for playlist parsing
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── NS-Player-debug.apk                        # Generated working debug APK (23.2 MB)
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 5. Verification & Testing

The project has been tested and verified:
1. **Unit Tests:** `.\gradlew.bat testDebugUnitTest` passed all test suites including complex M3U parsing, tags, and DRM headers.
2. **Compilation:** `.\gradlew.bat assembleDebug` built cleanly with zero errors.
3. **APK Verification:** `aapt dump badging NS-Player-debug.apk` verified:
   - Application ID: `com.genuine.nsplayer.debug`
   - Target SDK: 34 (Android 14) / Compile SDK: 35 (Android 15)
   - Dual Launchers: Standard mobile activity + Android TV Leanback activity banner.
