# OpenWeather

An Android weather application built with **Jetpack Compose**, following Clean Architecture and a multi-module structure. 
It provides real-time weather, hourly/daily forecasts, an interactive map for location selection, and a home screen widget.

---

## AI Development Workflow

### 1. Gemini
- Planning & Review

### 2. Claude Code
- Feature implementation
- Refactoring and bug fixing

### 3. Claude Design
- Weather icon design 

---
## Features

| Feature                       | Description                                                                                                   |
|-------------------------------|---------------------------------------------------------------------------------------------------------------|
| **Current Weather**           | Temperature, feels-like, humidity, wind speed, weather description, sunrise/sunset, hourly and daily forecast |
| **City Management**           | Search, add, and delete cities; mark favorites for quick access                                               |
| **Interactive Map**           | Tap any location to preview weather and add it to your city list                                              |
| **Home Screen Widget**        | Jetpack Glance App Widget with automatic hourly updates via WorkManager                                       |
| **Multi-language**            | English and Traditional Chinese (zh-TW), switchable in Settings                                               |
| **Offline Cache**             | Room database caches weather data so the last result is shown when offline                                    |

---

## Project Structure

```
opennet/
├── app/                    # Application entry point
│
├── core/                   # Shared core module
│   └── src/main/
│       ├── common/         # AppError, Result, GeoUtils
│       ├── data/
│       │   ├── local/      # Room DB (AppDatabase, DAOs, Entities)
│       │   └── repository/ # WeatherRepositoryImpl, CityRepositoryImpl, Mappers
│       ├── domain/
│       │   ├── model/      # Domain models (CurrentWeather, Forecast, SavedCity…)
│       │   ├── repository/ # Repository interfaces
│       │   └── usecase/    # Use cases (GetCurrentWeather, GetForecast, SaveCity…)
│       ├── network/
│       │   ├── api/        # WeatherApi (OWM), NominatimApi
│       │   ├── dto/        # API response DTOs
│       │   └── interceptor/# ApiKeyInterceptor, LanguageInterceptor
│       └── ui/             # Shared UI components (WeatherIcon, WeatherEffectCanvas, AutoSizeText)
│
├── feature/
│   ├── weather/            # Main weather screen (WeatherScreen + WeatherViewModel)
│   ├── city/               # City list and search (CityListScreen + CityViewModel)
│   ├── map/                # Interactive map (MapScreen + MapViewModel)
│   └── widget/             # Home screen widget (WeatherWidget + WorkManager)
│
└── settings.gradle.kts
```

## Getting Started

### 1. Obtain an OpenWeatherMap API Key and Configure `local.properties`

Add the following to `local.properties` in the project root:

```properties example
WEATHER_API_KEY=your_api_key_here

# Optional: disable individual features (default: true)
feature.map.enabled=true
feature.city.enabled=true
```

### 2. Build and Run

```bash
# Or via Gradle Wrapper
./gradlew :app:assembleDebug
```

## Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Fetch weather data and map tiles |
| `ACCESS_FINE_LOCATION` | Precise GPS location |
| `ACCESS_COARSE_LOCATION` | Network-based location (fallback) |