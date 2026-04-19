package com.example.studentfood.data.remote.api;

import com.example.studentfood.data.remote.dto.OverpassResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Overpass API Service with DTO mapping
 */
public interface OverpassPlacesApiService {
    
    /**
     * Truy vấn dữ liệu POI qua Overpass API
     * @param query Chuỗi truy vấn Overpass QL
     * @return Phản hồi DTO chứa dữ liệu POI
     */
    @FormUrlEncoded
    @POST("api/interpreter")
    Call<OverpassResponse> getPoisByQuery(@Field("data") String query);
}
