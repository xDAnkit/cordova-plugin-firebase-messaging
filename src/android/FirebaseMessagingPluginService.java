package by.chemerisuk.cordova.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nuvolo.mobius.R;

import java.util.Map;
import java.util.Random;

import static android.content.ContentResolver.SCHEME_ANDROID_RESOURCE;


public class FirebaseMessagingPluginService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseMessaging";

    public static final String ACTION_FCM_MESSAGE = "by.chemerisuk.cordova.firebase.ACTION_FCM_MESSAGE";
    public static final String EXTRA_FCM_MESSAGE = "by.chemerisuk.cordova.firebase.EXTRA_FCM_MESSAGE";
    public static final String ACTION_FCM_TOKEN = "by.chemerisuk.cordova.firebase.ACTION_FCM_TOKEN";
    public static final String EXTRA_FCM_TOKEN = "by.chemerisuk.cordova.firebase.EXTRA_FCM_TOKEN";
    public final static String NOTIFICATION_ICON_KEY = "com.google.firebase.messaging.default_notification_icon";
    public final static String NOTIFICATION_COLOR_KEY = "com.google.firebase.messaging.default_notification_color";

    private LocalBroadcastManager broadcastManager;
    private NotificationManager notificationManager;
    private int defaultNotificationIcon;
    private int defaultNotificationColor;

    @Override
    public void onCreate() {
        this.broadcastManager = LocalBroadcastManager.getInstance(this);
        this.notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
            this.defaultNotificationIcon = ai.metaData.getInt(NOTIFICATION_ICON_KEY, ai.icon);
            this.defaultNotificationColor = ContextCompat.getColor(this, ai.metaData.getInt(NOTIFICATION_COLOR_KEY));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to load meta-data", e);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Failed to load notification color", e);
        }
    }

    @Override
    public void onNewToken(String token) {
        FirebaseMessagingPlugin.sendInstanceId(token);

        Intent intent = new Intent(ACTION_FCM_TOKEN);
        intent.putExtra(EXTRA_FCM_TOKEN, token);
        this.broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        //FirebaseMessagingPlugin.sendNotification(remoteMessage);

        Intent intent = new Intent(ACTION_FCM_MESSAGE);
        intent.putExtra(EXTRA_FCM_MESSAGE, remoteMessage);
        this.broadcastManager.sendBroadcast(intent);

        if (FirebaseMessagingPlugin.isForceShow()) {
            if (remoteMessage.getData().size() > 0) {
                Log.d("MESS", "Message data payload: " + remoteMessage.getData());
                sendNotificationWithPopup(remoteMessage);
            }
        } else {
            sendNotificationWithOutPopup(remoteMessage);
        }
    }


    private void sendNotificationWithOutPopup(RemoteMessage remoteMessage) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notification.getChannelId());
        
        Map remoteMessageData = remoteMessage.getData();
        String aTitle = (String) remoteMessageData.get("title");
        String aMessage = (String) remoteMessageData.get("body");
        builder.setContentTitle(aTitle);
        builder.setContentText(aMessage);
        builder.setGroup(notification.getTag());
        builder.setSmallIcon(this.defaultNotificationIcon);
        builder.setColor(this.defaultNotificationColor);
        // must set sound and priority in order to display alert
        builder.setSound(getNotificationSound(notification.getSound()));
        builder.setPriority(1);

        this.notificationManager.notify(0, builder.build());
        // dismiss notification to hide icon from status bar automatically
        new Handler(getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                notificationManager.cancel(0);
            }
        }, 3000);
    }

    private void sendNotificationWithPopup(RemoteMessage remoteMessage) {

        Map remoteMessageData = remoteMessage.getData();
        String aTitle = (String) remoteMessageData.get("title");
        String aMessage = (String) remoteMessageData.get("body");


        String name = "user_channel"; // They are hardcoded only for show it's just strings
        String id = "user_channel_1"; // The user-visible name of the channel.
        String description = "user_first_channel"; // The user-visible description of the channel.

        PendingIntent pendingIntent;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.journaldev.com/"));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, id);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = this.notificationManager.getNotificationChannel(id);

            if (mChannel == null) {
                mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
                mChannel.setDescription(description);
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                this.notificationManager.createNotificationChannel(mChannel);
            }
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        builder.setContentTitle(aTitle)
                .setContentText(aMessage)
                .setSmallIcon(this.defaultNotificationIcon)
                .setColor(this.defaultNotificationColor)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setTicker(aTitle)
                .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true);


        //To handle Multiple Notification in system tray
        Random random = new Random();
        int m = random.nextInt(9999 - 1000) + 1000;
        Notification notification = builder.build();
        this.notificationManager.notify(m, notification);

    }

    private Uri getNotificationSound(String soundName) {
        if (soundName != null && !soundName.equals("default") && !soundName.equals("enabled")) {
            return Uri.parse(SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/raw/" + soundName);
        } else {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
    }
}
