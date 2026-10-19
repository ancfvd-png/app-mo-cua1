package com.example.appmca;

public class HistoryItem {
    private long id;
    private String dateTime;
    private int clickCount;
    private String imageUrl;

    public HistoryItem() {
    }

    public HistoryItem(long id, String dateTime, int clickCount, String imageUrl) {
        this.id = id;
        this.dateTime = dateTime;
        this.clickCount = clickCount;
        this.imageUrl = imageUrl;
    }

    public long getId() {
        return id;
    }

    public String getDateTime() {
        return dateTime;
    }

    public int getClickCount() {
        return clickCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}