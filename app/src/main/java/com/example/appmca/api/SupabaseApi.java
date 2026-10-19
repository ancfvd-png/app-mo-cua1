package com.example.appmca.api;

import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface SupabaseApi {
    // 1. Lấy trạng thái mới nhất từ bảng doorbell_state
    @GET("doorbell_state")
    Call<List<JsonObject>> getControlState(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
    );

    // 2. Gửi lệnh cập nhật trạng thái lên bảng doorbell_state
    @PATCH("doorbell_state")
    Call<Void> updateControl(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("id") String idFilter, // vd: "eq.1" để update dòng có id=1
            @Body JsonObject updateData
    );
    
    // 3. Lấy dữ liệu bảng doorbell_history
    @GET("doorbell_history")
    Call<List<JsonObject>> getHistory(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("order") String orderBy // vd: "timestamp.desc"
    );

    // 4. Gọi PostgreSQL Function (RPC) để chia sẻ chuông cửa qua Email
    @retrofit2.http.POST("rpc/add_member_by_email")
    Call<com.google.gson.JsonElement> addMemberByEmail(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Body JsonObject body
    );

    // 5. Xác thực đăng nhập Supabase Auth (GoTrue API)
    @retrofit2.http.POST("https://pimpkssvjjqdpwjpzwwq.supabase.co/auth/v1/token?grant_type=password")
    Call<JsonObject> loginWithEmail(
            @Header("apikey") String apiKey,
            @Body JsonObject body
    );

    // 6. Xóa 1 dòng lịch sử
    @retrofit2.http.DELETE("doorbell_history")
    Call<Void> deleteHistoryItem(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("id") String idFilter // vd: "eq.5"
    );

    // 7. Xóa tất cả dòng lịch sử
    @retrofit2.http.DELETE("doorbell_history")
    Call<Void> clearAllHistory(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("id") String idFilter // vd: "gt.0"
    );

    // 8. Thêm thành viên vào bảng doorbell_members
    @retrofit2.http.POST("doorbell_members")
    Call<Void> addDoorbellMember(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Body JsonObject memberData
    );

    // 9. Thu hồi quyền (Xóa thành viên khỏi bảng doorbell_members)
    @retrofit2.http.DELETE("doorbell_members")
    Call<Void> revokeMemberAccess(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("member_email") String emailFilter
    );

    // 10. Lấy danh sách thành viên trong bảng doorbell_members
    @GET("doorbell_members")
    Call<List<JsonObject>> getMemberList(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
    );
}