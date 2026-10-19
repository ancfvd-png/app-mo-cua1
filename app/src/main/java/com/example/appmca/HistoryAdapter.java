package com.example.appmca;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnItemDeleteListener {
        void onItemDelete(HistoryItem item, int position);
    }

    private List<HistoryItem> historyList;
    private OnItemDeleteListener deleteListener;

    public HistoryAdapter(List<HistoryItem> historyList, OnItemDeleteListener deleteListener) {
        this.historyList = historyList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        
        // Hiển thị thời gian và số lần bấm
        holder.tvDateTime.setText(item.getDateTime());
        holder.tvClickCount.setText("Lần bấm: " + item.getClickCount());

        // Hiển thị ảnh thu nhỏ (thumbnail) bằng Glide
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .into(holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_camera);
        }

        // Bắt sự kiện click vào mục lịch sử -> Xem chi tiết
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), LatestImageActivity.class);
            intent.putExtra("dateTime", item.getDateTime());
            intent.putExtra("clickCount", item.getClickCount());
            intent.putExtra("imageUrl", item.getImageUrl());
            v.getContext().startActivity(intent);
        });

        // Bắt sự kiện giữ lâu (Long Click) -> Xóa mục này
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Xóa lịch sử")
                    .setMessage("Bạn có muốn xóa mục lịch sử này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        if (deleteListener != null) {
                            deleteListener.onItemDelete(item, holder.getAdapterPosition());
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime;
        TextView tvClickCount;
        ImageView ivThumbnail;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvClickCount = itemView.findViewById(R.id.tvClickCount);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
        }
    }
}