// dummy notifications for demos
// for anandx@google.com by dsandler@google.com

package com.android.example.notificationshowcase;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class NotificationShowcaseActivity extends Activity {
    public static class ToastFeedbackActivity extends Activity {
       @Override
       public void onStart() {
           Intent i = getIntent();
           if (i.hasExtra("text")) {
               final String text = i.getStringExtra("text");
               Toast.makeText(this, text, Toast.LENGTH_LONG).show();
           }
           finish();
       }
    }
    
    private static final int NOTIFICATION_ID = 31338;

    private static final boolean FIRE_AND_FORGET = true;
    
    private ArrayList<Notification> mNotifications = new ArrayList<Notification>();
    
    NotificationManager mNoMa;
    int mLargeIconWidth, mLargeIconHeight;
    
    private Bitmap getBitmap(int resId) {
        Drawable d = getResources().getDrawable(resId);
        Bitmap b = Bitmap.createBitmap(mLargeIconWidth, mLargeIconHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, mLargeIconWidth, mLargeIconHeight);
        d.draw(c);
        return b;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mLargeIconWidth = (int) getResources().getDimension(android.R.dimen.notification_large_icon_width);
        mLargeIconHeight = (int) getResources().getDimension(android.R.dimen.notification_large_icon_height);
        
        mNoMa = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // none of them does anything; if you want them to auto-destruct when tapped, add a 
        //   .setAutoCancel(true)
        // if you want to launch an app, you need to do more work, but then again it won't launch the 
        // right thing anyway because these notifications are just dummies. :) 
        
//        mNotifications.add(new Notification.Builder(this)
//            .setContentTitle("Larry Page")
//            .setContentText("hey, free nachos at MoMA!")
//            .setLargeIcon(getBitmap(R.drawable.page_hed))
//            .setSmallIcon(android.R.drawable.stat_notify_chat)
//            .setPriority(Notification.PRIORITY_HIGH)
//            .setNumber(2)
//            .getNotification());

//        mNotifications.add(new Notification.Builder(this)
//        .setContentTitle("Andy Rubin")
//        .setContentText("Drinks tonight?")
//        .setTicker("Andy Rubin: Drinks tonight?")
//        .setLargeIcon(getBitmap(R.drawable.arubin_hed))
//        .setSmallIcon(R.drawable.stat_notify_sms)
//        .setPriority(Notification.PRIORITY_MAX)
//        .getNotification());

        String longSmsText = "Hey, looks like I'm getting kicked out of this conference room, so stay in the hangout and I'll rejoin in about 5-10 minutes. If you don't see me, assume I got pulled into another meeting. And now \u2026 I have to find my shoes.";
        mNotifications.add(new Notification.BigTextStyle(
                new Notification.Builder(this)
                    .setContentTitle("Mike Cleron")
                    .setContentText(longSmsText)
                    .setTicker("Mike Cleron: " + longSmsText)
                    .setLargeIcon(getBitmap(R.drawable.bucket))
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.stat_notify_talk_text))
                .bigText(longSmsText)
                .build());

        Intent toastIntent = new Intent(this, ToastFeedbackActivity.class);
        toastIntent.putExtra("text", "Clicked on Matias");
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, toastIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        
        mNotifications.add(new Notification.Builder(this)
        .setContentTitle("Incoming call")
        .setContentText("Matias Duarte")
        .setLargeIcon(getBitmap(R.drawable.matias_hed))
        .setSmallIcon(R.drawable.stat_sys_phone_call)
        .setPriority(Notification.PRIORITY_MAX)
        .setContentIntent(contentIntent)
        .addAction(R.drawable.ic_dial_action_call, "Answer", null)
        .addAction(R.drawable.ic_end_call, "Ignore", null)
        .setUsesIntruderAlert(true)
        .setIntruderActionsShowText(true)
        .setAutoCancel(true)
        .getNotification());


        mNotifications.add(new Notification.Builder(this)
        .setContentTitle("J Planning")
        .setContentText("The Botcave")
        .setSmallIcon(R.drawable.stat_notify_calendar)
        .setContentInfo("7PM")
        .getNotification());

        BitmapDrawable d = (BitmapDrawable) getResources().getDrawable(R.drawable.romainguy_rockaway);
        mNotifications.add(new Notification.BigPictureStyle(
                new Notification.Builder(this)
                    .setContentTitle("Romain Guy")
                    .setContentText("I was lucky to find a Canon 5D Mk III at a local Bay Area store last "
                            + "week but I had not been able to try it in the field until tonight. After a "
                            + "few days of rain the sky finally cleared up. Rockaway Beach did not disappoint "
                            + "and I was finally able to see what my new camera feels like when shooting "
                            + "landscapes.")
                    .setSmallIcon(R.drawable.ic_stat_gplus)
                    .setLargeIcon(getBitmap(R.drawable.romainguy_hed)))
                .bigPicture(d.getBitmap())
                .build());

        // Note: this may conflict with real email notifications
        mNotifications.add(new Notification.Builder(this)
        .setContentTitle("24 new messages")
        .setContentText("test.hugo2@gmail.com")
        .setSmallIcon(R.drawable.stat_notify_email)
        .getNotification());

        // No idea what this would really look like since the app is in flux
        mNotifications.add(new Notification.Builder(this)
        .setContentTitle("Google+")
        .setContentText("Kanye West has added you to his circles")
        .setSmallIcon(R.drawable.googleplus_icon)
        .setPriority(Notification.PRIORITY_LOW)
        .getNotification());
        
        mNotifications.add(new Notification.Builder(this)
        .setContentTitle("Twitter")
        .setContentText("New mentions")
        .setSmallIcon(R.drawable.twitter_icon)
        .setNumber(15)
        .setPriority(Notification.PRIORITY_LOW)
        .getNotification());

        if (FIRE_AND_FORGET) {
            doPost(null);
            finish();
        }
    }
    
    public void doPost(View v) {
        for (int i=0; i<mNotifications.size(); i++) {
            mNoMa.notify(NOTIFICATION_ID + i, mNotifications.get(i));
        }
    }
    
    public void doRemove(View v) {
        for (int i=0; i<mNotifications.size(); i++) {
            mNoMa.cancel(NOTIFICATION_ID + i);
        }
    }
}
