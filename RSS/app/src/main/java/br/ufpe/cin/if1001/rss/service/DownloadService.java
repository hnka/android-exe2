package br.ufpe.cin.if1001.rss.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

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
