package com.example.appmca.helper;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class FaceDetectionHelper {

    public interface FaceAnalysisCallback {
        void onSuccess(boolean faceDetected, float smileProb, float leftEyeOpenProb, float rightEyeOpenProb, String statusSummary);
        void onError(Exception e);
    }

    public interface FaceCropCallback {
        void onFaceCropped(Bitmap croppedFaceBitmap, Face face);
        void onNoFaceDetected();
        void onError(Exception e);
    }

    private final FaceDetector detector;

    public FaceDetectionHelper() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();

        detector = FaceDetection.getClient(options);
    }

    public void analyzeImage(Bitmap bitmap, FaceAnalysisCallback callback) {
        if (bitmap == null) {
            callback.onSuccess(false, 0, 0, 0, "Không có hình ảnh");
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces == null || faces.isEmpty()) {
                        callback.onSuccess(false, 0, 0, 0, "Không phát hiện khuôn mặt");
                        return;
                    }

                    Face face = faces.get(0);

                    float smileProb = face.getSmilingProbability() != null ? face.getSmilingProbability() : 0.0f;
                    float leftEye = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0.0f;
                    float rightEye = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0.0f;

                    StringBuilder summary = new StringBuilder();
                    summary.append("Phát hiện 1 khuôn mặt!\n");
                    summary.append("• Tỉ lệ mỉm cười: ").append(Math.round(smileProb * 100)).append("%\n");
                    summary.append("• Mắt trái mở: ").append(Math.round(leftEye * 100)).append("%\n");
                    summary.append("• Mắt phải mở: ").append(Math.round(rightEye * 100)).append("%");

                    callback.onSuccess(true, smileProb, leftEye, rightEye, summary.toString());
                })
                .addOnFailureListener(callback::onError);
    }

    // Cắt sát khuôn mặt bằng Google ML Kit Bounding Box
    public void detectAndCropFace(Bitmap originalBitmap, FaceCropCallback callback) {
        if (originalBitmap == null) {
            callback.onNoFaceDetected();
            return;
        }

        InputImage image = InputImage.fromBitmap(originalBitmap, 0);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces == null || faces.isEmpty()) {
                        callback.onNoFaceDetected();
                        return;
                    }

                    Face face = faces.get(0);
                    Rect bounds = face.getBoundingBox();

                    int left = Math.max(0, bounds.left);
                    int top = Math.max(0, bounds.top);
                    int width = Math.min(originalBitmap.getWidth() - left, bounds.width());
                    int height = Math.min(originalBitmap.getHeight() - top, bounds.height());

                    if (width > 0 && height > 0) {
                        Bitmap cropped = Bitmap.createBitmap(originalBitmap, left, top, width, height);
                        callback.onFaceCropped(cropped, face);
                    } else {
                        callback.onNoFaceDetected();
                    }
                })
                .addOnFailureListener(callback::onError);
    }
}