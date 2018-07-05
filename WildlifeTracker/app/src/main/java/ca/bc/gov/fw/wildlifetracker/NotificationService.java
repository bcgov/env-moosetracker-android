package ca.bc.gov.fw.wildlifetracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

/**
 * This class is a service that will send a notification whenever it is
 * called
 *
 * @author dchui
 *
 */
public class NotificationService extends Service {

    private boolean notificationsOn = true;
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void sendNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        Context context = getApplicationContext();

        String contentText = "Please report your moose sightings.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle("BC Moose");
        builder.setContentText(contentText);
        builder.setSmallIcon(R.drawable.ic_notification);

        Resources res = context.getResources();
        Bitmap iconBitmap = BitmapFactory.decodeResource(res, R.mipmap.ic_launcher);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
            int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
            iconBitmap = Bitmap.createScaledBitmap(iconBitmap, width, height, false);
        }
        builder.setLargeIcon(iconBitmap);
        builder.setAutoCancel(true);

        Intent notificationIntent = new Intent(this, SightingsFragment.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        builder.setContentIntent(contentIntent);

        Uri path = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.moosecall);
        builder.setSound(path);

        NotificationCompat.BigTextStyle bts = new NotificationCompat.BigTextStyle();
        bts.bigText(contentText);
        builder.setStyle(bts);

        Notification notification = builder.build();
        mNotificationManager.notify(1, notification);

    }

    @Override
    public void onCreate() {
        if (!notificationsOn) {
            return;
        }
        System.out.println("In notification service");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("OnStartcmd");
        sendNotification();
        return super.onStartCommand(intent, flags, 10);
    }
}

