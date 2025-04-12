package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import org.json.JSONArray;
import org.json.JSONObject;

public class DataFetcher {

    // Thessaloniki coordinates
    private static final String API_URL = "https://api.open-meteo.com/v1/forecast?latitude=40.6401&longitude=22.9444&daily=temperature_2m_max,temperature_2m_min,relative_humidity_2m_max,relative_humidity_2m_min&timezone=auto";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/weather-app";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "12345";

    public void fetchAndStoreWeatherData(String city) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            reader.close();
            connection.disconnect();

            JSONObject json = new JSONObject(jsonBuilder.toString());
            JSONObject daily = json.getJSONObject("daily");

            JSONArray dates = daily.getJSONArray("time");
            JSONArray tempMax = daily.getJSONArray("temperature_2m_max");
            JSONArray tempMin = daily.getJSONArray("temperature_2m_min");
            JSONArray humidityMax = daily.getJSONArray("relative_humidity_2m_max");
            JSONArray humidityMin = daily.getJSONArray("relative_humidity_2m_min");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String query = "INSERT INTO daily_weather (city, date, avg_temperature, avg_humidity) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {

                    for (int i = 0; i < dates.length(); i++) {
                        String date = dates.getString(i);
                        double avgTemp = (tempMax.getDouble(i) + tempMin.getDouble(i)) / 2;
                        double avgHumidity = (humidityMax.getDouble(i) + humidityMin.getDouble(i)) / 2;

                        stmt.setString(1, city);
                        stmt.setString(2, date);
                        stmt.setDouble(3, avgTemp);
                        stmt.setDouble(4, avgHumidity);
                        stmt.executeUpdate();

                        System.out.println("Inserted data for " + date);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
