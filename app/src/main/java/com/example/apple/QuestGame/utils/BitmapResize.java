package com.example.apple.QuestGame.utils;

import android.graphics.Bitmap;


public class BitmapResize {
    public static Bitmap getResizedBitmap(Bitmap bm) {
        float aspectRatio = bm.getWidth() /
                (float) bm.getHeight();
        int width = 140;
        int height = Math.round(width / aspectRatio);

        bm = Bitmap.createScaledBitmap(
                bm, width, height, false);
        return bm;
    }
}
