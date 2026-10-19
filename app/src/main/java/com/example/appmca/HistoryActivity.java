package com.example.appmca;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appmca.api.RetrofitClient;
import com.example.appmca.api.SupabaseApi;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;
    private SupabaseApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish()); // Đóng trang khi bấm quay lại

        ImageView btnClearAll = findViewById(R.id.btnClearAll);
        btnClearAll.setOnClickListener(v -> showClearAllDialog());

        rvHistory = findViewById(R.id.rvHistory);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvHistory.setLayoutManager(layoutManager);

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList, (item, position) -> deleteSingleItem(item, position));
        rvHistory.setAdapter(adapter);

        // Khởi tạo API
        api = RetrofitClient.getApi();

        // Tải dữ liệu từ Supabase
        loadHistoryFromSupabase();
    }

    private void loadHistoryFromSupabase() {
        api.getHistory(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER, "created_at.desc").enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historyList.clear();
                    
                    int count = response.body().size();

                    for (JsonObject data : response.body()) {
                        try {
                            long id = 0;
                            if (data.has("id") && !data.get("id").isJsonNull()) {
                                id = data.get("id").getAsLong();
                            }

                            String dateTime = "";
                            int clickCount = count--; // Giảm dần: 3, 2, 1...
                            String imageUrl = "";

                            if (data.has("created_at") && !data.get("created_at").isJsonNull()) {
                                String rawTime = data.get("created_at").getAsString();
                                try {
                                    String cleanTime = rawTime;
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

                                    java.text.SimpleDateFormat sdfLocal = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                                    sdfLocal.setTimeZone(java.util.TimeZone.getDefault());
                                    dateTime = sdfLocal.format(date);
                                } catch (Exception e) {
                                    dateTime = rawTime;
                                }
                            }
                            
                            if (data.has("image_url") && !data.get("image_url").isJsonNull()) {
                                imageUrl = data.get("image_url").getAsString();
                            }

                            if (dateTime != null && !dateTime.isEmpty()) {
                                HistoryItem item = new HistoryItem(id, dateTime, clickCount, imageUrl);
                                historyList.add(item);
                            }
                        } catch (Exception e) {
                            Log.e("SupabaseError", "Lỗi parse History: " + e.getMessage());
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                Toast.makeText(HistoryActivity.this, "Lỗi khi tải lịch sử: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSingleItem(HistoryItem item, int position) {
        api.deleteHistoryItem(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER, "eq." + item.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(HistoryActivity.this, "Đã xóa mục lịch sử!", Toast.LENGTH_SHORT).show();
                    loadHistoryFromSupabase(); // Tải lại danh sách
                } else {
                    Toast.makeText(HistoryActivity.this, "Không thể xóa: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(HistoryActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showClearAllDialog() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, "Lịch sử đang trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xóa tất cả lịch sử")
                .setMessage("Bạn có chắc chắn muốn xóa TOÀN BỘ lịch sử bấm chuông không?")
                .setPositiveButton("Xóa tất cả", (dialog, which) -> clearAllHistory())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void clearAllHistory() {
        api.clearAllHistory(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER, "gt.0").enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(HistoryActivity.this, "Đã xóa toàn bộ lịch sử!", Toast.LENGTH_SHORT).show();
                    loadHistoryFromSupabase(); // Tải lại danh sách (trống)
                } else {
                    Toast.makeText(HistoryActivity.this, "Không thể xóa: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(HistoryActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}