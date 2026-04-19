package com.example.studentfood.data.remote.datasource;

import com.example.studentfood.data.remote.api.OverpassPlacesApiService;
import com.example.studentfood.data.remote.client.RetrofitClient;
import com.example.studentfood.data.remote.dto.OverpassResponse;
import retrofit2.Callback;

/**
 * RemoteDataSource: Lớp bao bọc API Overpass.
 */
public class RemoteDataSource {
    private final OverpassPlacesApiService apiService;

    public RemoteDataSource() {
        this.apiService = RetrofitClient.getOverpassInstance().create(OverpassPlacesApiService.class);
    }

    public void fetchPois(String query, Callback<OverpassResponse> callback) {
        apiService.getPoisByQuery(query).enqueue(callback);
    }
}
