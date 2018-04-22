package br.ufpe.cin.if1001.rss.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import br.ufpe.cin.if1001.rss.R;
import br.ufpe.cin.if1001.rss.db.SQLiteRSSHelper;
import br.ufpe.cin.if1001.rss.domain.ItemRSS;
import br.ufpe.cin.if1001.rss.service.DownloadService;
import br.ufpe.cin.if1001.rss.util.ParserRSS;

public class MainActivity extends Activity {

    private ListView conteudoRSS;
    private final String RSS_FEED = "http://rss.cnn.com/rss/edition.rss";
    private SQLiteRSSHelper db;

    public static final String APP_ON_FOREGROUND = "br.ufpe.cin.if1001.rss.service.APP_ON_FOREGROUND";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = SQLiteRSSHelper.getInstance(this);

        conteudoRSS = findViewById(R.id.conteudoRSS);

        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(
                        //contexto, como estamos acostumados
                        this,
                        //Layout XML de como se parecem os itens da lista
                        R.layout.item,
                        //Objeto do tipo Cursor, com os dados retornados do banco.
                        //Como ainda não fizemos nenhuma consulta, está nulo.
                        null,
                        //Mapeamento das colunas nos IDs do XML.
                        // Os dois arrays a seguir devem ter o mesmo tamanho
                        new String[]{SQLiteRSSHelper.ITEM_TITLE, SQLiteRSSHelper.ITEM_DATE},
                        new int[]{R.id.itemTitulo, R.id.itemData},
                        //Flags para determinar comportamento do adapter, pode deixar 0.
                        0
                );
        //Seta o adapter. Como o Cursor é null, ainda não aparece nada na tela.
        conteudoRSS.setAdapter(adapter);

        // permite filtrar conteudo pelo teclado virtual
        conteudoRSS.setTextFilterEnabled(true);

        //Complete a implementação deste método de forma que ao clicar, o link seja aberto no navegador e
        // a notícia seja marcada como lida no banco
        conteudoRSS.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SimpleCursorAdapter adapter = (SimpleCursorAdapter) parent.getAdapter();
                Cursor mCursor = ((Cursor) adapter.getItem(position));
                mCursor.moveToPosition(position);
                String link = mCursor.getString(mCursor.getColumnIndex(SQLiteRSSHelper.ITEM_LINK));
                db.markAsRead(link);

                Uri uri = Uri.parse(link);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

                mCursor.moveToFirst();
            }
        });


        // registering broadcast listener for download
        BroadcastListener listener = new BroadcastListener();
        IntentFilter intent = new IntentFilter(DownloadService.DOWNLOAD_COMPLETE);
        this.registerReceiver(listener, intent);

        // registering broadcast listener for app on foreground
        IntentFilter foregroundIntent = new IntentFilter(APP_ON_FOREGROUND);
        this.registerReceiver(listener, foregroundIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.startDownloadService();
    }

    protected void onResume() {
        super.onResume();
        Log.d("CALLING ON FOREGROUND", "OK");

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(APP_ON_FOREGROUND);
        sendBroadcast(broadcastIntent);
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_Config:
                startActivity(new Intent(this, ConfigActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startDownloadService() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String linkfeed = preferences.getString("rssfeedlink", getResources().getString(R.string.rssfeed));

        Intent download = new Intent(this, DownloadService.class);
        download.putExtra("linkFeed", linkfeed);
        startService(download);
    }

    class BroadcastListener extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("ACTION BROADCAST", action);

            // if donwload finished action, process feed
            if (action.equalsIgnoreCase(DownloadService.DOWNLOAD_COMPLETE)) {
                new ExibirFeed().execute();
            } else if (action.equalsIgnoreCase(APP_ON_FOREGROUND)) {
                Context c = getApplicationContext();
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(c);
                String linkfeed = preferences.getString("rssfeedlink", getResources().getString(R.string.rssfeed));

                Intent download = new Intent(c, DownloadService.class);
                download.putExtra("linkFeed", linkfeed);
                startService(download);
            }
        }
    }

    class ExibirFeed extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... voids) {
            Cursor c = db.getItems();
            c.getCount();
            return c;
        }

        @Override
        protected void onPostExecute(Cursor c) {
            if (c != null) {
                ((CursorAdapter) conteudoRSS.getAdapter()).changeCursor(c);
            }
        }
    }
}
