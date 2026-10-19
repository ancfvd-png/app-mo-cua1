package com.example.appmca.helper;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceRecognitionHelper {

    private static final int INPUT_SIZE = 112; // Kích thước đầu vào chuẩn cho MobileFaceNet (112x112)
    private static final int EMBEDDING_SIZE = 192; // Đầu ra mảng 192 con số đặc trưng
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 128.0f;

    private Interpreter tflite;

    public FaceRecognitionHelper(Context context, String modelFileName) {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(context, modelFileName);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4); // Sử dụng 4 luồng CPU để xử lý nhanh hơn
            tflite = new Interpreter(modelBuffer, options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // 1. Chuyển đổi Bitmap khuôn mặt thành mảng 192 con số đặc trưng (Face Embeddings)
    public float[] getFaceEmbedding(Bitmap faceBitmap) {
        if (tflite == null || faceBitmap == null) {
            return null;
        }

        // Scale ảnh khuôn mặt về chuẩn 112x112
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, false);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(scaledBitmap);

        float[][] outputEmbeddings = new float[1][EMBEDDING_SIZE];

        // Chạy TensorFlow Lite AI Offline!
        tflite.run(inputBuffer, outputEmbeddings);

        return outputEmbeddings[0];
    }

    // 2. Chuyển đổi Bitmap thành ByteBuffer cho TensorFlow Lite
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        imgData.order(ByteOrder.nativeOrder());
        imgData.rewind();

        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixelValue : intValues) {
            // Lấy các kênh màu RGB và chuẩn hóa về dải [-1.0, 1.0]
            float r = (((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            float g = (((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            float b = ((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD;

            imgData.putFloat(r);
            imgData.putFloat(g);
            imgData.putFloat(b);
        }

        return imgData;
    }

    // 3. Hàm toán học tính độ tương đồng Cosine (Cosine Similarity) giữa 2 khuôn mặt (Trả về 0.0 -> 1.0)
    public static float calculateCosineSimilarity(float[] emb1, float[] emb2) {
        if (emb1 == null || emb2 == null || emb1.length != emb2.length) {
            return 0.0f;
        }

        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;

        for (int i = 0; i < emb1.length; i++) {
            dotProduct += emb1[i] * emb2[i];
            normA += emb1[i] * emb1[i];
            normB += emb2[i] * emb2[i];
        }

        if (normA == 0.0f || normB == 0.0f) {
            return 0.0f;
        }

        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    // 4. Kiểm tra xem 2 khuôn mặt có phải cùng 1 người hay không (Threshold mặc định 0.75 - 75%)
    public static boolean isMatch(float[] emb1, float[] emb2, float threshold) {
        float similarity = calculateCosineSimilarity(emb1, emb2);
        return similarity >= threshold;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}