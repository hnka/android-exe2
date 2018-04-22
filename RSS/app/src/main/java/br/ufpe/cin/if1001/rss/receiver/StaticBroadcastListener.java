package br.ufpe.cin.if1001.rss.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import br.ufpe.cin.if1001.rss.R;
import br.ufpe.cin.if1001.rss.service.DownloadService;
import br.ufpe.cin.if1001.rss.ui.MainActivity;

public class StaticBroadcastListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("STATIC ACTION BROADCAST", "ACTION");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String linkfeed = preferences.getString("rssfeedlink", context.getResources().getString(R.string.rssfeed));

        Intent download = new Intent(context, DownloadService.class);
        download.putExtra("linkFeed", linkfeed);
        download.putExtra("sendNotification", true);
        context.startService(download);
    }
}
