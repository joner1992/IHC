package com.example.guilherme.myapplication;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;
import android.media.AudioManager;
import android.widget.ArrayAdapter;

import android.hardware.SensorManager;



import android.widget.Toast;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Timer;


public class MainActivity extends ListActivity  {
    private static final int UPDATE_FREQUENCY = 500;
    private static final int STEP_VALUE = 4000;
    private int maxVolume = 100;

    private MediaCursorAdapter mediaAdapter = null;
    private TextView selectedFile = null;
    private SeekBar seekbar = null;
    private MediaPlayer player = null;
    private ImageButton voiceButton = null;
    private ImageButton playButton = null;
    private ImageButton prevButton = null;
    private ImageButton nextButton = null;
    private boolean isStarted = true;
    private String currentFile = "";
    private boolean isMoveingSeekBar = false;
    private boolean isVoiceAllowed = false;
    private boolean captureOver = false;
    private final Handler handler = new Handler();
    private final Runnable updatePositionRunnable = new Runnable() {
        @Override
        public void run() {
            updatePosition();
        }
    };

    private static final int REQUEST_CODE = 1234;
    private ArrayList resultList;
    private SensorManager sManager;
    private int valor = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        selectedFile = (TextView) findViewById(R.id.selectedfile);
        seekbar = (SeekBar) findViewById(R.id.seekbar);
        voiceButton = (ImageButton) findViewById(R.id.voice);
        playButton = (ImageButton) findViewById(R.id.play);
        prevButton = (ImageButton) findViewById(R.id.prev);
        nextButton = (ImageButton) findViewById(R.id.next);
        player = new MediaPlayer();
        player.setOnCompletionListener(onCompletion);
        player.setOnErrorListener(onError);
        seekbar.setOnSeekBarChangeListener(seekBarChanged);
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (null != cursor) {
            cursor.moveToFirst();
            mediaAdapter = new MediaCursorAdapter(this, R.layout.listitem, cursor);
            setListAdapter(mediaAdapter);
            voiceButton.setOnClickListener(onButtonClick);
            playButton.setOnClickListener(onButtonClick);
            prevButton.setOnClickListener(onButtonClick);
            nextButton.setOnClickListener(onButtonClick);

        }
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {
            voiceButton.setEnabled(false);
            Toast.makeText(getApplicationContext(), "Reconhecedor de voz nao encontrado", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected  void onListItemClick(ListView list, View view, int position, long id){
        super.onListItemClick(list, view, position, id);

        currentFile = (String) view.getTag();
        startPlay(currentFile);
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        handler.removeCallbacks(updatePositionRunnable);
        player.stop();
        player.reset();
        player.release();
        player = null;
    }
    private void startPlay(String file){
        Log.i("Selected:", file);
        String name = file.substring(file.lastIndexOf("/")+1);
        selectedFile.setText(name);
        seekbar.setProgress(0);
        player.stop();
        player.reset();
        try{
            player.setDataSource(file);
            player.prepare();
            player.start();
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        } catch (IllegalStateException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        seekbar.setMax(player.getDuration());
        playButton.setImageResource(android.R.drawable.ic_media_pause);
        updatePosition();
        isStarted = true;
    }
    private void stopPlay(){
        player.stop();
        player.reset();
        playButton.setImageResource(android.R.drawable.ic_media_play);
        handler.removeCallbacks(updatePositionRunnable);
        seekbar.setProgress(0);
        isStarted = false;
    }
    private void updatePosition(){
        handler.removeCallbacks(updatePositionRunnable);
        seekbar.setProgress(player.getCurrentPosition());
        handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY);
    }
    private class MediaCursorAdapter extends SimpleCursorAdapter{
        public MediaCursorAdapter(Context context, int layout, Cursor c) {
            super(context, layout, c,
                    new String[]{MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE, MediaStore.Audio.AudioColumns.DURATION},
                    new int[]{R.id.displayname, R.id.title, R.id.duration});
        }
        @Override
        public void bindView(View view, Context context, Cursor cursor){
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView name = (TextView) view.findViewById(R.id.displayname);
            TextView duration = (TextView) view.findViewById(R.id.duration);
            name.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)));
            title.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));
            long durationInMs = Long.parseLong(cursor.getString(
                    cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));
            double durationInMin = ((double) durationInMs / 1000.0 ) / 60.0;
            durationInMin = new BigDecimal(Double.toString(durationInMin)).setScale(2, BigDecimal.ROUND_UP).doubleValue();
            duration.setText("" + durationInMin);
            view.setTag(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
        }
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent){
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.listitem, parent, false);
            bindView(v, context, cursor);
            return v;
        }
    }
    private View.OnClickListener onButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.play: {
                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    } else {
                        if (isStarted) {
                            player.start();
                            playButton.setImageResource(android.R.drawable.ic_media_pause);
                            updatePosition();
                        } else {
                            startPlay(currentFile);
                        }
                    }
                    break;
                }
                case R.id.next: {
                    // MUDAR MUSICA
                    int seekto = player.getCurrentPosition() + STEP_VALUE;
                    if (seekto > player.getDuration())
                        seekto = player.getDuration();
                    player.pause();
                    player.seekTo(seekto);
                    player.start();
                    break;
                }
                case R.id.prev: {
                    //MUDAR MUSICA

                    int seekto = player.getCurrentPosition() - STEP_VALUE;
                    if (seekto < 0)
                        seekto = 0;
                    player.pause();
                    player.seekTo(seekto);
                    player.start();
                    break;
                }
                case R.id.voice: {
                    if(isVoiceAllowed == false){
                        isVoiceAllowed = true;
                        Toast.makeText(v.getContext(), "Voice commands activated",Toast.LENGTH_LONG).show();
                        startVoiceRecognition();
                    }
                    else {
                        isVoiceAllowed = false;
                        Toast.makeText(v.getContext(), "Voice commands deactivated",Toast.LENGTH_LONG).show();
                    }
                    break;

                }
            }
        }
    };

    private void startVoiceRecognition(){

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "AndroidBite Voice Recognition...");
        startActivityForResult(intent, REQUEST_CODE);

    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra( RecognizerIntent.EXTRA_RESULTS);
            ArrayList<String> resultList = new ArrayList<String>();
            resultList.add(0,"play");
            resultList.add(0,"pause");

            //resultList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, matches));


            for(int i = 0; i < matches.size();i++) {
                if(matches.get(i).equalsIgnoreCase("play") || matches.get(i).equalsIgnoreCase("toca") || matches.get(i).equalsIgnoreCase("continua")) {
                    if (isStarted) {
                        player.start();
                        playButton.setImageResource(android.R.drawable.ic_media_pause);
                        updatePosition();

                    }
                }
                else if(matches.get(i).equalsIgnoreCase("pause")) {
                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                }
                else if(matches.get(i).equalsIgnoreCase("increase")) {
                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    int currVolume = audioManager.getStreamVolume(player.getAudioSessionId());
                    float log1=(float)(Math.log(maxVolume-currVolume)/Math.log(maxVolume));
                    if(log1 < 0.0f){
                        log1 = 0.0f;
                    }
                    player.setVolume(1-log1, 1-log1);
                    Toast.makeText(MainActivity.this, "Volume increased",Toast.LENGTH_LONG).show();
                }
                else if(matches.get(i).equalsIgnoreCase("increase")) {
                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    int currVolume = audioManager.getStreamVolume(player.getAudioSessionId());
                    float log1=(float)(Math.log(maxVolume-currVolume)/Math.log(maxVolume));
                    if(log1 > 1.0f){
                        log1 = 1.0f;
                    }
                    player.setVolume(1-log1, 1-log1);
                    Toast.makeText(MainActivity.this, "Volume decreased",Toast.LENGTH_LONG).show();
                }

            }
            captureOver = false;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener(){
        @Override
        public void onCompletion (MediaPlayer mp){ stopPlay(); }};
    private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra){ return false; }};
    private SeekBar.OnSeekBarChangeListener seekBarChanged = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar){ isMoveingSeekBar = false; }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar){ isMoveingSeekBar = true; }
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
            if (isMoveingSeekBar){
                player.seekTo(progress);
                Log.i("OnSeekBarChangeListener", "onProgressChanged");
            }
        }
    };
}

