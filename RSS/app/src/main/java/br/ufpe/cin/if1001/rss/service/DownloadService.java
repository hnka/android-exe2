package br.ufpe.cin.if1001.rss.service;

import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import br.ufpe.cin.if1001.rss.R;
import br.ufpe.cin.if1001.rss.db.SQLiteRSSHelper;
import br.ufpe.cin.if1001.rss.domain.ItemRSS;
import br.ufpe.cin.if1001.rss.util.ParserRSS;

public class DownloadService extends IntentService {

    public static final String DOWNLOAD_COMPLETE = "br.ufpe.cin.if1001.rss.service.DOWNLOAD_COMPLETE";

    public DownloadService() {
        super("DownloadService");
    }

    public void onHandleIntent(Intent intent) {
        boolean flagProblema = false;
        List<ItemRSS> items = null;
        String linkFeed = intent.getStringExtra("linkFeed");
        Boolean sendNotification = intent.getBooleanExtra("sendNotification", false);

        Context c = getApplicationContext();
        SQLiteRSSHelper db = SQLiteRSSHelper.getInstance(c);

        Log.d("FEED LINK", linkFeed);

        try {
            String feed = getRssFeed(linkFeed);
            items = ParserRSS.parse(feed);
            for (ItemRSS i : items) {
                Log.d("DB", "Buscando no Banco por link: " + i.getLink());
                ItemRSS item = db.getItemRSS(i.getLink());
                if (item == null) {
                    Log.d("DB", "Encontrado pela primeira vez: " + i.getTitle());

                    if (sendNotification) {
                        this.generateNotification(i);
                    }

                    db.insertItem(i);
                }
            }

        } catch (Exception e) {
            Log.e(getClass().getName(), "Exception durante download", e);
            flagProblema = true;
        }

        if (flagProblema) {
            Toast.makeText(c, "Houve algum problema ao carregar o feed.", Toast.LENGTH_SHORT).show();
        }

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(DOWNLOAD_COMPLETE);

        sendBroadcast(broadcastIntent);
        Log.d("FINISH DOWNLOADING", DOWNLOAD_COMPLETE);
    }

    public NotificationManager initNotificationManager(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT < 26) {
            return notificationManager;
        }

        NotificationChannel channel = new NotificationChannel("default","Notification Channel", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("New Item Notification Channel");
        notificationManager.createNotificationChannel(channel);

        return notificationManager;
    }

    private void generateNotification(ItemRSS item) {
        Log.d("DOWNLOAD SERVICE", "GENERATE NOTIFICATION");
        Context c = getApplicationContext();

        NotificationManager manager = this.initNotificationManager(c);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c, "default");
        notificationBuilder.setContentTitle("Nova Notícia");
        notificationBuilder.setContentText(item.getDescription());
        notificationBuilder.setSmallIcon(R.mipmap.ic_notification);

        manager.notify(0, notificationBuilder.build());
    }

    private String getRssFeed(String feed) throws IOException {
        InputStream in = null;
        String rssFeed = "";
        try {
            URL url = new URL(feed);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int count; (count = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, count);
            }
            byte[] response = out.toByteArray();
            rssFeed = new String(response, "UTF-8");
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return rssFeed;
    }

}