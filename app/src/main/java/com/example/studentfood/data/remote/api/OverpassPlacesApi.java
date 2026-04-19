package com.example.studentfood.data.remote.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Overpass API Service cho OpenStreetMap POI data
 */
public interface OverpassPlacesApi {
    
    /**
     * Query POI data via Overpass API
     * @param query Overpass QL query string
     * @return JSON response with POI data
     */
    @POST("api/interpreter")
    Call<String> getPoisByQuery(@Body String query);
}
