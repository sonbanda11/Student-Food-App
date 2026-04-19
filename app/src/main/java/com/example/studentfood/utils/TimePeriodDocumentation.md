# Time Period Logic Documentation

## Overview
Refactored time period detection logic to use sunrise/sunset times instead of hardcoded hour ranges. This ensures accurate time detection for all countries regardless of seasonal variations.

## New Logic Implementation

### Core Function: `getTimePeriod(WeatherResponse weatherResponse)`

**Parameters:**
- `weatherResponse`: Weather response from OpenWeather API containing sunrise/sunset data

**Returns:** String time period: "MORNING", "DAY", "EVENING", or "NIGHT"

**Logic:**
1. **MORNING**: From sunrise to sunrise + 3 hours
2. **DAY**: From morning end to sunset - 2 hours  
3. **EVENING**: From sunset - 2 hours to sunset + 2 hours
4. **NIGHT**: From sunset + 2 hours to sunrise (next day)

### Fallback Logic
If sunrise/sunset data is unavailable, falls back to simple hour-based logic:
- **MORNING**: 5:00 - 7:59
- **DAY**: 8:00 - 16:59
- **EVENING**: 17:00 - 21:59
- **NIGHT**: 22:00 - 4:59

## Public API Methods

### `getCurrentTimePeriod(WeatherResponse weatherResponse)`
Returns the current time period as string. Use for custom UI logic based on time of day.

```java
String timePeriod = WeatherMapper.getCurrentTimePeriod(weatherResponse);
switch (timePeriod) {
    case "MORNING":
        // Show morning-specific UI
        break;
    case "DAY":
        // Show day-specific UI
        break;
    case "EVENING":
        // Show evening-specific UI
        break;
    case "NIGHT":
        // Show night-specific UI
        break;
}
```

### `isCurrentlyNight(WeatherResponse weatherResponse)`
Returns boolean indicating if it's currently nighttime (after 2 hours post-sunset). Use for theme switching.

```java
boolean isNight = WeatherMapper.isCurrentlyNight(weatherResponse);
if (isNight) {
    // Apply night theme
} else {
    // Apply day theme
}
```

### `isCurrentlyEvening(WeatherResponse weatherResponse)`
Returns boolean indicating if it's currently evening time. Use for evening-specific UI.

```java
boolean isEvening = WeatherMapper.isCurrentlyEvening(weatherResponse);
if (isEvening) {
    // Show evening-specific content
}
```

## Integration with WeatherMapper

The `mapWeatherResponse()` method now automatically uses the new time detection logic:

```java
// Old way (still works but isNight parameter is ignored)
WeatherData data = WeatherMapper.mapWeatherResponse(weatherResponse, context, isNight);

// New way (recommended - isNight calculated automatically)
WeatherData data = WeatherMapper.mapWeatherResponse(weatherResponse, context, false);
```

## WeatherResponse Requirements

Ensure your WeatherResponse includes the `sys` object with sunrise/sunset:

```java
public static class Sys implements Serializable {
    public long sunrise;  // Unix timestamp
    public long sunset;   // Unix timestamp
}
```

## Benefits

1. **Global Accuracy**: Works correctly for all countries and seasons
2. **Automatic Fallback**: Gracefully handles missing sunrise/sunset data
3. **Backward Compatible**: Existing code continues to work
4. **Flexible API**: Public methods for custom time-based logic

## Usage Examples

### Basic Usage
```java
// In your Fragment/Activity
WeatherResponse weatherData = // ... get from API
WeatherMapper.WeatherData mappedData = WeatherMapper.mapWeatherResponse(weatherData, getContext(), false);

// Apply UI theme
applyTheme(mappedData.backgroundResource, mappedData.textColor);
```

### Custom Time-Based Logic
```java
// Get current time period for custom logic
String timePeriod = WeatherMapper.getCurrentTimePeriod(weatherResponse);

switch (timePeriod) {
    case "MORNING":
        showBreakfastSuggestions();
        applyMorningTheme();
        break;
    case "DAY":
        showLunchSuggestions();
        applyDayTheme();
        break;
    case "EVENING":
        showDinnerSuggestions();
        applyEveningTheme();
        break;
    case "NIGHT":
        showLateNightSnacks();
        applyNightTheme();
        break;
}
```

### Theme Switching with 4 Periods
```java
// Switch app theme based on time
String timePeriod = WeatherMapper.getCurrentTimePeriod(weatherResponse);

switch (timePeriod) {
    case "MORNING":
        setAppTheme(AppTheme.MORNING); // Light, fresh colors
        break;
    case "DAY":
        setAppTheme(AppTheme.DAY); // Bright, energetic colors
        break;
    case "EVENING":
        setAppTheme(AppTheme.EVENING); // Warm, sunset colors
        break;
    case "NIGHT":
        setAppTheme(AppTheme.NIGHT); // Dark, easy-on-eyes colors
        break;
}

// Or use boolean methods for specific cases
if (WeatherMapper.isCurrentlyNight(weatherResponse)) {
    // Apply dark theme
} else if (WeatherMapper.isCurrentlyEvening(weatherResponse)) {
    // Apply warm sunset theme
} else {
    // Apply light theme
}
```

## Migration Guide

### From Old Logic
```java
// Old way
private boolean isNightTime() {
    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    return hour >= 18 || hour < 6;
}

// New way
boolean isNight = WeatherMapper.isCurrentlyNight(weatherResponse);
```

### WeatherComponent Update
The WeatherComponent has been updated to use the new logic automatically. No changes needed in existing code.

## Testing

Test with different locations and times:
1. **Equatorial regions**: Consistent day/night lengths
2. **Polar regions**: Extreme day/night variations
3. **Seasonal changes**: Verify accuracy across different months
4. **API failures**: Test fallback behavior

## Performance

- Minimal overhead: Simple timestamp comparisons
- No network calls: Uses existing weather data
- Memory efficient: No additional objects created
