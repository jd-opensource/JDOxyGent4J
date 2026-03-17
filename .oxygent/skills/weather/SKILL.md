---
name: weather
description: Retrieve real-time weather data from public weather services without requiring API authentication.
---

# Weather Data Retrieval

## Purpose

This capability enables fetching current weather conditions and forecasts through publicly accessible weather endpoints.

When handling weather-related inquiries, this skill offers multiple query approaches to obtain meteorological information.

## Available Query Methods

### Method 1: Text-Based Weather Query

A straightforward approach using console commands:

```bash
curl -s "wttr.in/Beijing?format=3"
```

For detailed output with humidity and wind information:

```bash
curl -s "wttr.in/Shanghai?format=%l:+%c+%t+%h+%w"
```

### Method 2: Structured Data Query

For programmatic processing, use the JSON-based endpoint:

```bash
curl -s "https://api.open-meteo.com/v1/forecast?latitude=31.2&longitude=121.5&current_weather=true"
```

This returns structured weather metrics including temperature, wind speed, and weather condition codes.

## Usage Notes

- Latitude/longitude coordinates are required for JSON queries
- Location names should be URL-encoded for text queries
- Data availability depends on the respective service providers
- Users should verify compliance with each provider's terms of use


