# Play Store Release Runbook

Use this when preparing a Play Console release for Tabuada do 10.

## 1. Build locally

```sh
./gradlew test
./gradlew bundleRelease
```

Expected bundle:

```text
app/build/outputs/bundle/release/app-release.aab
```

Alternatively, run the `Android Closed Testing Release` workflow in GitHub Actions. Manual runs ask for `versionCode` and `versionName`. A pushed tag like `v1.0.0` runs automatically, uses `1.0.0` as `versionName`, uses the GitHub run number as `versionCode`, stores the `tabuada-release-aab` artifact for 14 days, and uploads it to the Play Console closed testing `alpha` track.

## 2. Store listing

- App name: `Tabuada do 10`
- Short description: `play-store/short-description.txt`
- Full description: `play-store/full-description.txt`
- App icon: `play-store/graphics/app-icon-512.png`
- Feature graphic: `play-store/graphics/feature-graphic-1024x500.png`
- Phone screenshots: all files in `play-store/screenshots/`

Do not reuse screenshots from another app. Add Tabuada-specific images before running the listing workflow for graphics.

## 3. App content

- App access: all features are available without special access.
- Ads: the app does not contain ads.
- Advertising ID: the app does not use an advertising ID.
- Government apps: not a government app.
- Financial features: no financial features.
- Health apps: no health features.
- Privacy policy: `https://geisonmcd.github.io/tabuada/privacy-policy.html`
- Data safety: use `play-store/data-safety.md`.
- Content rating: educational app for multiplication practice.

## 4. Closed testing track

The GitHub Actions workflow can create the release in `Test and release > Testing > Closed testing > Alpha` automatically.

For a manual fallback:

1. Select countries/regions.
2. Select testers.
3. Create a new release.
4. Add the app bundle from the library or upload `app-release.aab`.
5. Paste release notes from `play-store/release-notes-en-US.txt`.
6. Review and save the release.
