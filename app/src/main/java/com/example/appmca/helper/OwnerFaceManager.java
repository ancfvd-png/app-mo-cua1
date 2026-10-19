package com.example.appmca.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

public class OwnerFaceManager {

    private static final String PREF_NAME = "SmartDoorFacePrefs";
    private static final String KEY_OWNER_EMBEDDING = "owner_face_embedding";

    // 1. Lưu mảng 192 con số đặc trưng của Chủ nhà vĩnh viễn vào điện thoại
    public static void saveOwnerEmbedding(Context context, float[] embedding) {
        if (embedding == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(embedding);

        prefs.edit().putString(KEY_OWNER_EMBEDDING, json).apply();
    }

    // 2. Tải mảng 192 con số của Chủ nhà ra để so sánh
    public static float[] getOwnerEmbedding(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_OWNER_EMBEDDING, null);

        if (json == null) return null;

        Gson gson = new Gson();
        return gson.fromJson(json, float[].class);
    }

    // 3. Kiểm tra xem đã đăng ký khuôn mặt Chủ nhà chưa
    public static boolean hasOwnerEmbedding(Context context) {
        return getOwnerEmbedding(context) != null;
    }

    // 4. Xóa dữ liệu khuôn mặt Chủ nhà
    public static void clearOwnerEmbedding(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_OWNER_EMBEDDING).apply();
    }
}