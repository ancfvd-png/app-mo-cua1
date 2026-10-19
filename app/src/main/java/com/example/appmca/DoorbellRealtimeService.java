package com.example.appmca;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.appmca.api.RetrofitClient;
import com.example.appmca.api.SupabaseApi;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoorbellRealtimeService extends Service {

    private static final String CHANNEL_ID = "DOORBELL_CHANNEL";
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private SupabaseApi api;
    private int lastHistoryCount = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        api = RetrofitClient.getApi();
        createNotificationChannel();

        // Android yêu cầu Foreground Service phải hiển thị một Notification liên tục
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Smart Doorbell")
                .setContentText("Đang chạy ngầm để nhận cuộc gọi...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        startForeground(1, builder.build());

        // Lắng nghe dữ liệu bấm chuông ngầm
        startListeningToDoorbell();
    }

    private void startListeningToDoorbell() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkNewDoorbellEvents();
                handler.postDelayed(this, 3000); // Lắng nghe mỗi 3 giây
            }
        };
        handler.post(runnable);
    }

    private void checkNewDoorbellEvents() {
        api.getHistory(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER, "created_at.desc").enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<JsonObject> history = response.body();
                    int currentCount = history.size();

                    if (lastHistoryCount == -1) {
                        lastHistoryCount = currentCount; // Lần đầu tiên chạy service, lưu số lượng hiện tại
                    } else if (currentCount > lastHistoryCount) {
                        // CÓ KHÁCH BẤM CHUÔNG MỚI!
                        lastHistoryCount = currentCount;

                        String imageUrl = "";
                        if (!history.isEmpty() && history.get(0).has("image_url") && !history.get(0).get("image_url").isJsonNull()) {
                            imageUrl = history.get(0).get("image_url").getAsString();
                        }

                        // Mở màn hình IncomingCallActivity đè lên
                        Intent callIntent = new Intent(getApplicationContext(), IncomingCallActivity.class);
                        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        callIntent.putExtra("IMAGE_URL", imageUrl);
                        startActivity(callIntent);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                Log.e("DoorbellService", "Lỗi service: " + t.getMessage());
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Doorbell Realtime Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}