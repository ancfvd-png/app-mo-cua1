package com.example.appmca;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.appmca.api.RetrofitClient;
import com.example.appmca.api.SupabaseApi;
import com.example.appmca.helper.FaceDetectionHelper;
import com.example.appmca.helper.FaceRecognitionHelper;
import com.example.appmca.helper.OwnerFaceManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingCallActivity extends AppCompatActivity {

    private Ringtone ringtone;
    private ImageView ivGuestPhoto;
    private TextView tvCallTitle, tvCallSubtitle;
    private SupabaseApi api;

    private FaceDetectionHelper faceDetector;
    private FaceRecognitionHelper faceRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bật sáng màn hình điện thoại kể cả khi đang khóa màn hình
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        setContentView(R.layout.activity_incoming_call);

        api = RetrofitClient.getApi();
        faceDetector = new FaceDetectionHelper();
        faceRecognizer = new FaceRecognitionHelper(this, "mobilefacenet.tflite");

        ivGuestPhoto = findViewById(R.id.ivGuestPhoto);
        tvCallTitle = findViewById(R.id.tvCallTitle);
        tvCallSubtitle = findViewById(R.id.tvCallSubtitle);

        MaterialButton btnAcceptOpen = findViewById(R.id.btnAcceptOpen);
        MaterialButton btnDecline = findViewById(R.id.btnDecline);

        // Phát nhạc chuông điện thoại
        startRingtone();

        // Tải ảnh mới nhất từ Intent hoặc Supabase
        String imageUrl = getIntent().getStringExtra("IMAGE_URL");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadAndRecognizeImage(imageUrl);
        } else {
            fetchLatestGuestImage();
        }

        // Nút Mở Cửa thủ công
        btnAcceptOpen.setOnClickListener(v -> {
            stopRingtone();
            sendDoorOpenCommand();
            finish();
        });

        // Nút Từ Chối
        btnDecline.setOnClickListener(v -> {
            stopRingtone();
            finish();
        });
    }

    private void startRingtone() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void fetchLatestGuestImage() {
        api.getControlState(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER).enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    JsonObject data = response.body().get(0);
                    if (data.has("latest_image") && !data.get("latest_image").isJsonNull()) {
                        String url = data.get("latest_image").getAsString();
                        loadAndRecognizeImage(url);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
            }
        });
    }

    private void loadAndRecognizeImage(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        ivGuestPhoto.setImageBitmap(resource);

                        // CHẠY AI NHẬN DIỆN KHUÔN MẶT
                        runFaceRecognition(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void runFaceRecognition(Bitmap originalBitmap) {
        float[] ownerEmbedding = OwnerFaceManager.getOwnerEmbedding(this);

        // Nếu chưa đăng ký khuôn mặt Chủ nhà, coi như Khách thường
        if (ownerEmbedding == null) {
            return;
        }

        // 1. Cắt mặt bằng ML Kit
        faceDetector.detectAndCropFace(originalBitmap, new FaceDetectionHelper.FaceCropCallback() {
            @Override
            public void onFaceCropped(Bitmap croppedFaceBitmap, com.google.mlkit.vision.face.Face face) {
                // 2. Trích xuất 192 số bằng TFLite MobileFaceNet
                float[] incomingEmbedding = faceRecognizer.getFaceEmbedding(croppedFaceBitmap);

                if (incomingEmbedding != null) {
                    // 3. So sánh với Chủ nhà (Cosine Similarity)
                    float similarity = FaceRecognitionHelper.calculateCosineSimilarity(ownerEmbedding, incomingEmbedding);

                    // Nếu độ giống >= 70% (0.70f) -> XÁC NHẬN LÀ CHỦ NHÀ!
                    if (similarity >= 0.70f) {
                        int percent = Math.round(similarity * 100);
                        tvCallTitle.setText("✨ NHẬN DIỆN CHỦ NHÀ (" + percent + "%)");
                        tvCallTitle.setTextColor(Color.parseColor("#20C75C"));
                        tvCallSubtitle.setText("Tự động mở cửa trong 2 giây...");

                        // Tự động bắn lệnh Mở cửa
                        sendDoorOpenCommand();

                        // Đóng màn hình sau 2.5s
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            stopRingtone();
                            finish();
                        }, 2500);
                    } else {
                        tvCallTitle.setText("🔔 CÓ KHÁCH BẤM CHUÔNG!");
                        tvCallTitle.setTextColor(Color.parseColor("#FF9500"));
                    }
                }
            }

            @Override
            public void onNoFaceDetected() {
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void sendDoorOpenCommand() {
        JsonObject updateData = new JsonObject();
        updateData.addProperty("door_open", 1);

        api.updateControl(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER, "eq.1", updateData).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                Toast.makeText(IncomingCallActivity.this, "✅ Đã tự động Mở cửa cho Chủ nhà!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
        if (faceRecognizer != null) {
            faceRecognizer.close();
        }
    }
}