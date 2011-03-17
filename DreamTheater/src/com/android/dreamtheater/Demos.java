package com.android.dreamtheater;

import android.app.Activity;
import android.content.Intent;
import android.dreams.BasicDream;
import android.graphics.Canvas;

public class Demos {
    public static class Demo1 extends BasicDream {
        @Override
        public void onDraw(Canvas c) {
            c.drawColor(0x806699FF);
        }
    }

    public static class Demo2 extends BasicDream {
        @Override
        public void onDraw(Canvas c) {
            c.drawColor(0x80FFCC00);
        }
    }
}
