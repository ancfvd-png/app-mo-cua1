package com.example.appmca;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.appmca.api.RetrofitClient;
import com.example.appmca.api.SupabaseApi;
import com.example.appmca.helper.FaceDetectionHelper;
import com.example.appmca.helper.FaceRecognitionHelper;
import com.example.appmca.helper.OwnerFaceManager;
import com.example.appmca.helper.SessionManager;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private SupabaseApi api;
    private FaceDetectionHelper faceDetector;
    private FaceRecognitionHelper faceRecognizer;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        api = RetrofitClient.getApi();
        faceDetector = new FaceDetectionHelper();
        faceRecognizer = new FaceRecognitionHelper(this, "mobilefacenet.tflite");

        // Đăng ký bộ chọn ảnh từ thư viện điện thoại
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri imageUri = result.getData().getData();
                        enrollFaceFromUri(imageUri);
                    }
                }
        );

        // Nút quay lại (Back)
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Nút Cài đặt thiết bị & Chia sẻ quyền
        View btnDeviceSettings = findViewById(R.id.btnDeviceSettings);
        btnDeviceSettings.setOnClickListener(v -> showShareAccessDialog());

        // Nút Quản lý khuôn mặt
        View btnFaceManagement = findViewById(R.id.btnFaceManagement);
        btnFaceManagement.setOnClickListener(v -> showFaceManagementDialog());

        // Nút Đăng xuất
        View btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất tài khoản")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    SessionManager.clearSession(this);
                    Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showShareAccessDialog() {
        String[] options = {
                "➕ Chia sẻ quyền cho người thân mới",
                "📋 Xem & Thu hồi quyền đã chia sẻ"
        };

        new AlertDialog.Builder(this)
                .setTitle("Cài đặt chia sẻ thiết bị")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showAddMemberDialog();
                    } else if (which == 1) {
                        showMemberListAndRevokeDialog();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showAddMemberDialog() {
        android.widget.EditText inputEmail = new android.widget.EditText(this);
        inputEmail.setHint("Ví dụ: nguoi_than@gmail.com");
        inputEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        inputEmail.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        new AlertDialog.Builder(this)
                .setTitle("Chia sẻ quyền truy cập")
                .setMessage("Nhập Email của người thân để cấp quyền sử dụng chuông cửa:")
                .setView(inputEmail)
                .setPositiveButton("Xác nhận chia sẻ", (dialog, which) -> {
                    String email = inputEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập Email!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    shareDoorbellAccess(email);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showMemberListAndRevokeDialog() {
        Toast.makeText(this, "Đang tải danh sách thành viên...", Toast.LENGTH_SHORT).show();

        api.getMemberList(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER).enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<JsonObject> members = response.body();
                    String[] emailList = new String[members.size()];

                    for (int i = 0; i < members.size(); i++) {
                        JsonObject obj = members.get(i);
                        if (obj.has("member_email") && !obj.get("member_email").isJsonNull()) {
                            emailList[i] = obj.get("member_email").getAsString();
                        } else {
                            emailList[i] = "Thành viên #" + (i + 1);
                        }
                    }

                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle("Danh sách quyền (Bấm để Thu hồi)")
                            .setItems(emailList, (dialog, which) -> {
                                String selectedEmail = emailList[which];
                                confirmRevokeMember(selectedEmail);
                            })
                            .setNegativeButton("Đóng", null)
                            .show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Chưa có thành viên nào được chia sẻ!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                Toast.makeText(SettingsActivity.this, "Lỗi tải danh sách: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmRevokeMember(String targetEmail) {
        new AlertDialog.Builder(this)
                .setTitle("Thu hồi quyền truy cập")
                .setMessage("Bạn có chắc chắn muốn thu hồi quyền truy cập của " + targetEmail + " không?")
                .setPositiveButton("Thu hồi", (dialog, which) -> thuHoiQuyen(targetEmail))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void thuHoiQuyen(String targetEmail) {
        api.revokeMemberAccess(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER, "eq." + targetEmail).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SettingsActivity.this, "✅ Đã thu hồi quyền truy cập của " + targetEmail, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "❌ Lỗi: Bạn không có quyền Thu hồi (Chỉ Admin mới được phép)!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(SettingsActivity.this, "❌ Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareDoorbellAccess(String email) {
        JsonObject body = new JsonObject();
        body.addProperty("doorbell_uuid", RetrofitClient.DOORBELL_UUID); // ID chuông cửa thật từ Supabase
        body.addProperty("member_email", email);

        Log.d("LoiChiaSe", "Gửi RPC add_member_by_email: " + body.toString());

        api.addMemberByEmail(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER, body).enqueue(new Callback<com.google.gson.JsonElement>() {
            @Override
            public void onResponse(@NonNull Call<com.google.gson.JsonElement> call, @NonNull Response<com.google.gson.JsonElement> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String result = response.body().getAsString();
                    Log.d("LoiChiaSe", "Kết quả trả về từ RPC: " + result);

                    if ("SUCCESS".equals(result)) {
                        Toast.makeText(SettingsActivity.this, "✅ Đã chia sẻ quyền cho " + email, Toast.LENGTH_LONG).show();
                    } else if ("USER_NOT_FOUND".equals(result)) {
                        Toast.makeText(SettingsActivity.this, "❌ Email này chưa đăng ký tài khoản App!", Toast.LENGTH_LONG).show();
                    } else if ("UNAUTHORIZED".equals(result)) {
                        Toast.makeText(SettingsActivity.this, "❌ Bạn không phải Admin của chuông cửa này!", Toast.LENGTH_LONG).show();
                    } else if ("ALREADY_MEMBER".equals(result)) {
                        Toast.makeText(SettingsActivity.this, "⚠️ Người này đã có quyền truy cập rồi!", Toast.LENGTH_LONG).show();
                    } else if (result.startsWith("ERROR:")) {
                        Toast.makeText(SettingsActivity.this, "❌ Lỗi: " + result, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SettingsActivity.this, "❌ Lỗi Server: " + result, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errBody = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Log.d("LoiChiaSe", "Lỗi RPC HTTP " + response.code() + ": " + errBody);
                    Toast.makeText(SettingsActivity.this, "❌ Lỗi Supabase (" + response.code() + "): " + errBody, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.google.gson.JsonElement> call, @NonNull Throwable t) {
                Log.d("LoiChiaSe", "Lỗi onFailure: " + t.getMessage());
                Toast.makeText(SettingsActivity.this, "❌ Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showFaceManagementDialog() {
        boolean hasFace = OwnerFaceManager.hasOwnerEmbedding(this);
        String title = hasFace ? "Quản lý khuôn mặt (Đã có dữ liệu)" : "Quản lý khuôn mặt (Chưa có dữ liệu)";

        String[] options = {
                "🖼️ Chọn ảnh chân dung từ Thư viện điện thoại",
                "📷 Đăng ký từ ảnh mới nhất của Camera ESP32",
                "🗑️ Xóa dữ liệu khuôn mặt Chủ nhà"
        };

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Chọn ảnh từ gallery
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(intent);
                    } else if (which == 1) {
                        // Lấy ảnh từ Supabase camera
                        registerOwnerFaceFromLatestImage();
                    } else if (which == 2) {
                        // Xóa dữ liệu
                        OwnerFaceManager.clearOwnerEmbedding(this);
                        Toast.makeText(this, "Đã xóa dữ liệu khuôn mặt Chủ nhà!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void enrollFaceFromUri(Uri imageUri) {
        Toast.makeText(this, "Đang phân tích ảnh từ thư viện...", Toast.LENGTH_SHORT).show();
        Glide.with(this)
                .asBitmap()
                .load(imageUri)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        processAndEnrollBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void registerOwnerFaceFromLatestImage() {
        Toast.makeText(this, "Đang tải ảnh từ camera để đăng ký...", Toast.LENGTH_SHORT).show();

        api.getControlState(RetrofitClient.API_KEY, RetrofitClient.AUTH_HEADER).enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    JsonObject data = response.body().get(0);
                    if (data.has("latest_image") && !data.get("latest_image").isJsonNull()) {
                        String imageUrl = data.get("latest_image").getAsString();
                        downloadAndEnrollFace(imageUrl);
                    } else {
                        Toast.makeText(SettingsActivity.this, "Không có ảnh mới nhất từ camera!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                Toast.makeText(SettingsActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadAndEnrollFace(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap originalBitmap, @Nullable Transition<? super Bitmap> transition) {
                        processAndEnrollBitmap(originalBitmap);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void processAndEnrollBitmap(Bitmap bitmap) {
        // 1. Dùng Google ML Kit tìm và cắt khuôn mặt
        faceDetector.detectAndCropFace(bitmap, new FaceDetectionHelper.FaceCropCallback() {
            @Override
            public void onFaceCropped(Bitmap croppedFaceBitmap, com.google.mlkit.vision.face.Face face) {
                // 2. Dùng TFLite (MobileFaceNet) rút trích 192 số đặc trưng
                float[] embedding = faceRecognizer.getFaceEmbedding(croppedFaceBitmap);

                if (embedding != null) {
                    // 3. Lưu vĩnh viễn vào SharedPreferences bằng Gson
                    OwnerFaceManager.saveOwnerEmbedding(SettingsActivity.this, embedding);
                    Toast.makeText(SettingsActivity.this, "✅ ĐĂNG KÝ KHUÔN MẶT CHỦ NHÀ THÀNH CÔNG!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Lỗi trích xuất đặc trưng AI!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNoFaceDetected() {
                Toast.makeText(SettingsActivity.this, "Không tìm thấy khuôn mặt trong bức ảnh!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(SettingsActivity.this, "Lỗi ML Kit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (faceRecognizer != null) {
            faceRecognizer.close();
        }
    }
}