# Mahesh PDF Editor

Mahesh PDF Editor is a Windows desktop PDF editor project with visual PDF editing tools.

The Windows app can merge PDFs, split PDFs, preview pages, cover existing visible text, and place replacement text on top.

It is not an Adobe Acrobat clone and does not edit PDF paragraphs like Microsoft Word.

## Windows EXE

Build online with GitHub Actions:

1. Upload this project to GitHub.
2. Open `Actions`.
3. Run `Build Windows EXE`.
4. Download `mahesh-pdf-editor-windows-exe`.

Important files:

```text
package.json
electron/main.js
website/index.html
website/vendor/
.github/workflows/windows-exe.yml
```

## Android App

Mahesh PDF Editor is an Android app for PDF management.

## Features

- Merge multiple PDF files into one PDF.
- Extract selected pages, such as `1-3, 5, 8-10`.
- Split every page into separate PDFs inside a ZIP file.
- Uses Android's file picker and save dialog.
- Does not need broad storage permission.

## Build APK Online With GitHub

Upload the contents of this folder to the top level of a GitHub repository.

Your GitHub repository must show these files at the top level:

```text
.github
app
build.gradle.kts
settings.gradle.kts
gradle.properties
README.md
```

Then:

1. Open your GitHub repository.
2. Click `Actions`.
3. Click `Build Android APK`.
4. Click `Run workflow`.
5. Wait for the build to finish.
6. Download the artifact named `pdf-forge-debug-apk`.

The APK file inside that artifact is:

```text
app-debug.apk
```

## Important Upload Note

Do not upload the parent folder itself.

Upload the files and folders inside it, so `.github` and `app` are visible directly in the GitHub repository.
