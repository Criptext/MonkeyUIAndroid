package com.criptext.uisample;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Adapter;
import android.widget.LinearLayout;

import com.criptext.monkeykitui.recycler.ChatActivity;
import com.criptext.monkeykitui.recycler.MonkeyAdapter;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.recycler.audio.AudioPlaybackHandler;
import com.criptext.monkeykitui.recycler.listeners.AudioListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements ChatActivity {

    final static String[] messages = { "Hello", "'sup", "How are you doing", "Is everything OK?", "I'm at work", "I'm at school",
    "The weather is terrible", "I'm not feeling very well", "Today is my lucky day", "I hate when that happens",
    "I'm fine", "What are you doing this weekend?", "Sorry, I have plans", "I'm free", "Everything is going according to plan",
    "Here's my credit card number: 1111 2222 3333 4444"};
    final static int MAX_MESSAGES = 150;
    MonkeyAdapter adapter;
    RecyclerView recycler;
    AudioPlaybackHandler audioHandler;

    final static class SlowMessageLoader extends AsyncTask<WeakReference<MainActivity>, Void, ArrayList<MonkeyItem>>{
        WeakReference<MainActivity> activityWeakReference;
        @Override
        protected void onPostExecute(ArrayList<MonkeyItem> newData) {
            MainActivity act = activityWeakReference.get();
            if(act != null && newData != null){
                act.adapter.smoothlyAddNewData(newData, act.recycler, act.adapter.getItemCount() + newData.size() > MAX_MESSAGES);
            }
        }


        @Override
        protected ArrayList<MonkeyItem> doInBackground(WeakReference<MainActivity>... params) {
            activityWeakReference = params[0];
            try {
                Thread.sleep(500);
            } catch(InterruptedException ex){

            }
            return generateRandomMessages(activityWeakReference.get());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createAudioFile();
        createImageFile();
        ArrayList<MonkeyItem> messages =generateRandomMessages(this);
        adapter = new MonkeyAdapter(this, messages);
        adapter.setHasReachedEnd(false);

        recycler = (RecyclerView) findViewById(R.id.recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        layoutManager.setStackFromEnd(true);
        recycler.setLayoutManager(layoutManager);

        recycler.setAdapter(adapter);
        audioHandler = new AudioPlaybackHandler(adapter, recycler);
    }

    @Override
    protected void onStop() {
        audioHandler.releasePlayer();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }



    private static ArrayList<MonkeyItem> generateRandomMessages(Context ctx){
        if(ctx == null)
            return null;

        ArrayList<MonkeyItem> arrayList = new ArrayList<MonkeyItem>();
        long timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 48;
        for(int i = 0; i < 26; i++){
            Random r = new Random();
            boolean incoming = r.nextBoolean();
            MessageItem item;

            if(i%6 == 1){
                //audio
                item = new MessageItem(incoming ? "1":"0", "" + timestamp,
                        ctx.getCacheDir() + "/barney.aac", timestamp, incoming,
                        MonkeyItem.MonkeyItemType.audio);
                item.setDuration("00:10");
            }
            else if(i%8 == 1){
                //photo
                item = new MessageItem(incoming ? "1":"0", "" + timestamp,
                        ctx.getCacheDir() + "/mrbean.jpg", timestamp, incoming,
                        MonkeyItem.MonkeyItemType.photo);
                item.setCoverBitmap(BitmapFactory.decodeResource(ctx.getResources(),R.raw.mrbean_blur));

            }
            else {
                //text
                String message = messages[r.nextInt(messages.length)];
                item = new MessageItem(incoming ? "1":"0", "" + timestamp, message, timestamp, incoming,
                        MonkeyItem.MonkeyItemType.text);
            }
            timestamp += r.nextInt(1000 * 60 * 10);
            arrayList.add(item);
        }

        return arrayList;
    }

    /**
     * Si no tengo archivos creo uno nuevo.
     */
    private void createAudioFile(){
        File file = new File(getCacheDir() + "/barney.aac");
        if(!file.exists()){
            try {
            InputStream ins = getResources().openRawResource(R.raw.barney);
            FileOutputStream outputStream = new FileOutputStream(file.getPath());

            byte buf[] = new byte[1024];
            int len;

                while ((len = ins.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                ins.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Si no tengo archivos creo uno nuevo.
     */
    private void createImageFile(){
        File file = new File(getCacheDir() + "/mrbean.jpg");
        if(!file.exists()){
            try {
                InputStream ins = getResources().openRawResource(R.raw.mrbean);
                FileOutputStream outputStream = new FileOutputStream(file.getPath());

                byte buf[] = new byte[1024];
                int len;

                while ((len = ins.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                ins.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getMemberColor(@NotNull String sessionId) {
        return Color.WHITE;
    }

    @NotNull
    @Override
    public String getMenberName(@NotNull String sessionId) {
        return "Unknown";
    }

    @Override
    public boolean isGroupChat() {
        return false;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public void onFileDownloadRequested(int position, @NotNull MonkeyItem item) {

    }

    @Override
    public void onLoadMoreData(int loadedItems) {
        SlowMessageLoader loader = new SlowMessageLoader();
        loader.execute(new WeakReference<>(this));
    }
}
