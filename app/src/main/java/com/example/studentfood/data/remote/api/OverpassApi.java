package com.example.studentfood.data.remote.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Retrofit API cho OpenStreetMap Overpass (POST form field {@code data}).
 */
public interface OverpassApi {

    @FormUrlEncoded
    @POST("api/interpreter")
    Call<ResponseBody> interpret(@Field("data") String overpassQuery);
}
