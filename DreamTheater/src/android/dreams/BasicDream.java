package android.dreams;

import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.util.AttributeSet;
import android.graphics.Canvas;

public class BasicDream extends Activity {
    public class BasicDreamView extends View {
        public BasicDreamView(Context c) {
            super(c);
        }

        public BasicDreamView(Context c, AttributeSet at) {
            super(c, at);
        }

        @Override
        public void onAttachedToWindow() {
            setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
        }

        @Override
        public void onDraw(Canvas c) {
            BasicDream.this.onDraw(c);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setContentView(new BasicDreamView(this));
    }

    public void onDraw(Canvas c) {
    }

    public void onUserInteraction() {
        finish();
    }
}

