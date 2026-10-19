package com.example.appmca;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class LatestImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_latest_image);
        
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish(); // Đóng activity hiện tại để quay lại màn hình trước
        });

        // Ánh xạ View
        ImageView ivSnapshot = findViewById(R.id.ivSnapshot);
        // Lưu ý: Các ID TextView này chưa có trong file XML hiện tại của bạn,
        // Cần ánh xạ đúng theo ID trong activity_latest_image.xml
        // Ví dụ tạm: TextView tvTime = findViewById(R.id.tvTime);

        // Nhận dữ liệu truyền từ HistoryAdapter
        Intent intent = getIntent();
        if (intent != null) {
            String imageUrl = intent.getStringExtra("imageUrl");
            String dateTime = intent.getStringExtra("dateTime");
            int clickCount = intent.getIntExtra("clickCount", 0);

            // Hiển thị ảnh
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_camera)
                        .into(ivSnapshot);
            }

            // TODO: Gắn text thời gian và số lần bấm vào TextView (sau khi bạn đặt ID cho chúng trong XML)
        }
    }
}
