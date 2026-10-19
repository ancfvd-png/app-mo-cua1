package com.example.appmca;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appmca.api.RetrofitClient;
import com.example.appmca.api.SupabaseApi;
import com.example.appmca.helper.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private SupabaseApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // KIỂM TRA PHIÊN ĐĂNG NHẬP DÙNG TỰ ĐỘNG LƯU
        if (SessionManager.isLoggedIn(this)) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Đóng LoginActivity, tự động vào thằng Màn hình chính!
            return;
        }

        setContentView(R.layout.activity_login);

        api = RetrofitClient.getApi();

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Email và Mật khẩu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tiến hành xác thực đăng nhập qua Supabase Auth API
            loginWithSupabase(email, password);
        });
    }

    private void loginWithSupabase(String email, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        api.loginWithEmail(RetrofitClient.API_KEY, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // ĐĂNG NHẬP THÀNH CÔNG! -> LƯU PHIÊN ĐĂNG NHẬP
                    SessionManager.saveSession(LoginActivity.this, email);

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Đóng LoginActivity
                } else {
                    // ĐĂNG NHẬP THẤT BẠI
                    Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}