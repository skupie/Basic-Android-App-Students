# 🎓 Student Portal – Android App

A fully-featured Android application for the **Student Portal** system at `basic.bd-d.online`.

Built with **Kotlin**, **MVVM**, **Hilt**, **Retrofit**, **Jetpack Navigation**, and **Material Design 3**.

---

## 📱 Features

| Screen | Description |
|--------|-------------|
| **Login** | Email + password auth via Laravel Sanctum |
| **Dashboard** | Due alert, today's schedule, exam stats, pending notice |
| **Attendance** | Monthly records with present/absent/late summary |
| **Fees** | Invoice list with due summary + full payment history |
| **Exams** | Weekly marks, upcoming assignments, model test results |
| **Schedule** | Class timetable / routines |
| **Notices** | Active notices with one-tap acknowledgement |
| **Study Materials** | Teacher-uploaded notes & files with open/download |

---

## 🏗️ Architecture

```
app/
├── data/
│   ├── api/          # Retrofit service + Auth interceptor
│   ├── local/        # DataStore token storage
│   ├── model/        # All data classes (Gson)
│   └── repository/   # All repositories
├── di/               # Hilt modules (Network, App)
├── ui/
│   ├── auth/         # Login
│   ├── main/         # MainActivity + BottomNav
│   ├── dashboard/
│   ├── attendance/
│   ├── fees/
│   ├── exams/
│   ├── routines/
│   ├── notices/
│   └── materials/
└── utils/            # Resource, Extensions
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/YOUR_USERNAME/StudentPortal.git
   cd StudentPortal
   ```

2. Open in Android Studio.

3. The base URL is already set to `https://basic.bd-d.online/api/v1/` in `app/build.gradle.kts`.

4. Sync Gradle → Run on device/emulator.

> **Note:** The backend API endpoints must be implemented in the Laravel app. See `student_portal_api.yaml` for the full OpenAPI 3.0 spec.

---

## 🔑 Authentication Flow

1. `POST /api/v1/auth/login` → receives Bearer token
2. Token stored in **Android DataStore** (encrypted)
3. `AuthInterceptor` attaches `Authorization: Bearer {token}` to every request
4. On `401` → token cleared → redirect to LoginActivity

---

## 📦 Tech Stack

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | 1.9.24 | Language |
| Hilt | 2.51.1 | Dependency Injection |
| Retrofit | 2.11.0 | HTTP Client |
| OkHttp | 4.12.0 | Logging + Interceptors |
| Gson | 2.10.1 | JSON Serialization |
| DataStore | 1.1.1 | Secure Token Storage |
| Navigation | 2.7.7 | Fragment Navigation |
| Material3 | 1.12.0 | UI Components |
| Glide | 4.16.0 | Image Loading |
| Coroutines | 1.8.1 | Async Operations |

---

## 📋 API Reference

See `student_portal_api.yaml` (OpenAPI 3.0) for the complete API documentation.

**Base URL:** `https://basic.bd-d.online/api/v1`

---

## 🌐 Business Rules (from backend analysis)

- **Due Alert**: Shows after 6th of month; dismissible for 3 days
- **Routine date**: After 19:00 BDT → shows tomorrow's schedule
- **Fee calculation**: ≤5 days = free, ≤12 days = half fee, >12 days = full fee
- **Performance labels**: ≥80% Excellent, ≥65% Good, ≥50% Needs Focus, <50% Needs Improvement
- **Notice flow**: One pending notice at a time; must acknowledge before next appears

---

## 📄 License

MIT License — free to use, modify, and distribute.
# Basic-Android-App-Students
