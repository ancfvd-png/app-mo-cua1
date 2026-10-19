package com.example.appmca;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.appmca.api.RetrofitClient;
import com.example.appmca.api.SupabaseApi;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ImageView imgCameraView;
    private TextView tvLastDate, tvLastTime, tvTotalBells;
    private SupabaseApi api;
    
    // Handler để fetch dữ liệu từ Supabase định kỳ (vì Supabase REST API không hỗ trợ Realtime socket mặc định)
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Khởi tạo Supabase API
        api = RetrofitClient.getApi();

        imgCameraView = findViewById(R.id.imgCameraView);
        tvLastDate = findViewById(R.id.tvLastDate);
        tvLastTime = findViewById(R.id.tvLastTime);
        tvTotalBells = findViewById(R.id.tvTotalBells);

        // Chuyển sang HistoryActivity khi click vào "Lịch sử"
        LinearLayout btnHistory = findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // Chuyển sang SettingsActivity khi click vào "Cài đặt"
        LinearLayout btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Xử lý nút Mở cửa
        MaterialButton btnOpenDoor = findViewById(R.id.btnOpenDoor);
        btnOpenDoor.setOnClickListener(v -> sendCommandToSupabase("door_open", 1));

        // Xử lý nút Chụp ảnh lại
        MaterialButton btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(v -> sendCommandToSupabase("force_capture", 1));

        // Bắt đầu Service chạy ngầm để nhận cuộc gọi bấm chuông
        Intent serviceIntent = new Intent(this, DoorbellRealtimeService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Bắt đầu vòng lặp lấy dữ liệu (Polling) mỗi 2 giây
        startPollingSupabase();
    }
    
    private void startPollingSupabase() {
        runnable = new Runnable() {
            @Override
            public void run() {
                fetchControlData();
                fetchHistoryData();
                // Lặp lại sau 2 giây (2000ms)
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(runnable);
    }

    private void fetchControlData() {
        api.getControlState(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER).enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    JsonObject data = response.body().get(0); // Bảng doorbell_state chỉ có 1 dòng dữ liệu chính

                    // 1. Cập nhật ảnh từ cột latest_image
                    if (data.has("latest_image") && !data.get("latest_image").isJsonNull()) {
                        String imageUrl = data.get("latest_image").getAsString();
                        Glide.with(MainActivity.this)
                                .load(imageUrl)
                                .placeholder(android.R.drawable.ic_menu_camera)
                                .into(imgCameraView);
                    }
                    // Thời gian sẽ được lấy ở bảng doorbell_history bên dưới
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                Log.e("SupabaseError", "Lỗi fetch Control: " + t.getMessage());
            }
        });
    }

    private void fetchHistoryData() {
        // Lấy lịch sử từ bảng doorbell_history, sắp xếp mới nhất lên đầu để lấy thời gian
        api.getHistory(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER, "created_at.desc").enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<JsonObject> history = response.body();
                    
                    // Cập nhật tổng số lần bấm
                    int totalBells = history.size();
                    tvTotalBells.setText(String.valueOf(totalBells));

                    // Lấy thời gian của bản ghi mới nhất để hiển thị ra màn hình chính
                    if (!history.isEmpty()) {
                        JsonObject latest = history.get(0);
                        if (latest.has("created_at") && !latest.get("created_at").isJsonNull()) {
                            String dateTime = latest.get("created_at").getAsString();
                            // Chuyển đổi giờ UTC từ Supabase sang giờ địa phương (Việt Nam UTC+7)
                            try {
                                String cleanTime = dateTime;
                                if (cleanTime.contains(".")) {
                                    cleanTime = cleanTime.substring(0, cleanTime.indexOf("."));
                                } else if (cleanTime.contains("+")) {
                                    cleanTime = cleanTime.substring(0, cleanTime.indexOf("+"));
                                } else if (cleanTime.contains("Z")) {
                                    cleanTime = cleanTime.substring(0, cleanTime.indexOf("Z"));
                                }

                                java.text.SimpleDateFormat sdfUtc = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                                sdfUtc.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                                java.util.Date date = sdfUtc.parse(cleanTime);

                                java.text.SimpleDateFormat sdfLocalDate = new java.text.SimpleDateFormat("dd/MM/yyyy");
                                java.text.SimpleDateFormat sdfLocalTime = new java.text.SimpleDateFormat("HH:mm:ss");
                                sdfLocalDate.setTimeZone(java.util.TimeZone.getDefault());
                                sdfLocalTime.setTimeZone(java.util.TimeZone.getDefault());

                                tvLastDate.setText(sdfLocalDate.format(date));
                                tvLastTime.setText(sdfLocalTime.format(date));
                            } catch (Exception e) {
                                tvLastDate.setText(dateTime);
                                tvLastTime.setText("");
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
            }
        });
    }

    private void sendCommandToSupabase(String column, int value) {
        JsonObject updateData = new JsonObject();
        updateData.addProperty(column, value);

        // Update vào dòng có id=1 trong bảng doorbell_state
        api.updateControl(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER, "eq.1", updateData).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Đã gửi lệnh thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng vòng lặp khi tắt app để tránh tốn pin
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
