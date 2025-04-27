package com.example.thefinalapp;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface BackendApiService {
    @POST("/process")
    Call<String> sendFirebaseUrl(@Body String firebaseUrl);
}