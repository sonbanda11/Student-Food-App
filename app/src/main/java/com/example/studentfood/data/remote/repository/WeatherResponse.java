package com.example.studentfood.data.remote.repository;

import java.io.Serializable;

public class WeatherResponse implements Serializable {

    public Main main;
    public Weather[] weather;
    public String name;
    public Coord coord;

    // ⭐ THÊM: thời gian hiện tại (OpenWeather trả về)
    public long dt;

    // ⭐ THÊM: hệ thống mặt trời (QUAN TRỌNG CHO isNight)
    public Sys sys;

    public static class Coord implements Serializable {
        public double lat;
        public double lon;
    }

    public static class Main implements Serializable {
        public float temp;
        public float humidity;
    }

    public static class Weather implements Serializable {
        public String main;
        public String description;
        public String icon;
    }

    // ⭐ NEW: sunrise / sunset
    public static class Sys implements Serializable {
        public long sunrise;
        public long sunset;
    }
}