package com.example.appmca.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://pimpkssvjjqdpwjpzwwq.supabase.co/rest/v1/";
    public static final String API_KEY = "sb_publishable_NW5gsx6APJA5rKfrqFBWFQ_9QrI17Qo"; // Supabase anon_key của bạn
    public static final String AUTH_HEADER = "Bearer " + API_KEY;

    // Mã UUID thực tế của Chuông cửa trong bảng doorbells trên Supabase
    public static String DOORBELL_UUID = "72d579e4-0933-4a54-96bc-1dcd80c74105";

    private static Retrofit retrofit = null;

    public static SupabaseApi getApi() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SupabaseApi.class);
    }
}