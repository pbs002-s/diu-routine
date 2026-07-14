<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=500&size=20&duration=3000&pause=800&color=8B5CF6&center=true&vCenter=true&width=600&lines=Scan+a+PDF.+Get+a+Routine.+Done.;Pinch.+Zoom.+Parse.+Track.;Built+for+DIU+Students+%F0%9F%8E%93" alt="Typing SVG" />

<br/>

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose-M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Gemini](https://img.shields.io/badge/AI-Gemini%203.1%20Pro-F9AB00?style=for-the-badge&logo=googlegemini&logoColor=white)

</div>

<br/>

## ⚡ What it does

A fully **offline** academic schedule manager for **Daffodil International University**. Upload your official routine PDF/DOCX, tap **Scan This Page**, and Gemini Vision reads dates, times, rooms, and codes straight into a clean, trackable timetable.

<br/>

## 🧩 How it works

```mermaid
%%{init: {'theme':'dark', 'themeVariables': {'primaryColor':'#8B5CF6','primaryTextColor':'#fff','primaryBorderColor':'#8B5CF6','lineColor':'#8B5CF6','secondaryColor':'#1e1e2e','tertiaryColor':'#27273f'}}}%%
flowchart LR
    A[📄 Upload PDF / DOCX] --> B[🔍 Pinch • Zoom • Pan Viewer]
    B --> C["✨ Scan This Page"]
    C --> D[🤖 Gemini 3.1 Pro Vision]
    D --> E[🧠 Parse dates · times · rooms · codes]
    E --> F[(🗄️ Room Database)]
    F --> G[📅 Class Routine]
    F --> H[📝 Exam Routine]
    G --> I[⏰ Smart Reminders]
    H --> I
    I --> J[📊 Study Stats & Streaks]

    style A fill:#27273f,stroke:#8B5CF6,color:#fff
    style D fill:#F9AB00,stroke:#F9AB00,color:#000
    style F fill:#27273f,stroke:#8B5CF6,color:#fff
    style J fill:#3DDC84,stroke:#3DDC84,color:#000
```

<br/>

## ✨ Features

| | |
|---|---|
| 📄 **Gemini Scanner** | Auto-parses PDFs → dates, times (24h), rooms, teacher initials, subject codes |
| 📝 **Routine Management** | Color-coded classes, exam seating plans, live "active class" indicator |
| ⏰ **Smart Reminders** | Customizable pre-class alerts, never miss a lecture |
| 📊 **Study Stats** | Attendance charts, streaks, syllabus progress — 100% on-device |
| 🎨 **M3 Design** | Adaptive dark/light, edge-to-edge, minimalist calendar icon |

<br/>

## 🛠️ Stack

<div align="center">

`Kotlin` · `Jetpack Compose` · `Room` · `Retrofit/OkHttp` · `Moshi` · `Gemini API`

</div>

<br/>

## 📲 Install

```bash
# Grab the debug APK
APK_DOWNLOAD/app-debug.apk
```
Enable **Install from Unknown Sources** → open the APK → done.

<br/>

## 🔑 Setup

```bash
cp .env.example .env
# then add inside .env
GEMINI_API_KEY=your_actual_api_key_here
```

<br/>

## ⚙️ Build

```bash
git clone <this-repo>
gradle assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

<br/>

<div align="center">

*Crafted elegantly to simplify campus schedules and empower academic success* 🎓

<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=6,11,20&height=100&section=footer" width="100%"/>

</div>
