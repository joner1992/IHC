package com.example.guilherme.myapplication;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
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

    private MediaCursorAdapter mediaAdapter = null;
    private TextView selectedFile = null;
    private SeekBar seekbar = null;
    private MediaPlayer player = null;
    private ImageButton voiceButton = null;
    private ImageButton playButton = null;
    private ImageButton prevButton = null;
    private ImageButton nextButton = null;
    private ImageButton helpButton = null;
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
    private ArrayList<String> tags = new ArrayList<>();
    private int counter_tags = 0;


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
        helpButton = (ImageButton) findViewById(R.id.help);
        player = new MediaPlayer();
        player.setOnCompletionListener(onCompletion);
        player.setOnErrorListener(onError);
        seekbar.setOnSeekBarChangeListener(seekBarChanged);
        MediaButtonIntentReceiver mMediaButtonReceiver = new MediaButtonIntentReceiver();
        IntentFilter mediaFilter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        mediaFilter.setPriority(1000);
        registerReceiver(mMediaButtonReceiver, mediaFilter);
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (null != cursor) {
            cursor.moveToFirst();
            mediaAdapter = new MediaCursorAdapter(this, R.layout.listitem, cursor);
            setListAdapter(mediaAdapter);
            voiceButton.setOnClickListener(onButtonClick);
            playButton.setOnClickListener(onButtonClick);
            prevButton.setOnClickListener(onButtonClick);
            nextButton.setOnClickListener(onButtonClick);
            helpButton.setOnClickListener(onButtonClick);

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
            tags.add(counter_tags, cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));
            counter_tags += 1;
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

    public class MediaButtonIntentReceiver extends BroadcastReceiver {

        public MediaButtonIntentReceiver() {
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
                return;
            }
            KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }
            int action = event.getAction();
            if (action == KeyEvent.ACTION_DOWN) {
                // do something
                startVoiceRecognition();
            }
            abortBroadcast();
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
                        Toast.makeText(v.getContext(), "Reconhecimento de voz ativado",Toast.LENGTH_LONG).show();
                        if (player.isPlaying()) {
                            handler.removeCallbacks(updatePositionRunnable);
                            player.pause();
                            playButton.setImageResource(android.R.drawable.ic_media_play);
                        }
                        startVoiceRecognition();
                    }
                    else {
                        isVoiceAllowed = false;
                        Toast.makeText(v.getContext(), "Reconhecimento de voz desativado",Toast.LENGTH_LONG).show();
                    }
                    break;

                }
                case R.id.help: {
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("Helper")
                            .setMessage("\tAqui você encontra a lista de comandos de voz diponíveis para você utilizar o aplicativo:\n " +
                                    "-> Para dar PLAY: Play! | Go! | OK, go! | Continue | Start\n" +
                                    "-> Para dar PAUSE: Pause! | Stop! | Hold! | Hold The Door!!! | Wait! | Stop\n " +
                                    "-> Para passar à próxima música: Next\n" +
                                    "-> Para voltar à música anterior: Back | Previous\n" +
                                    "-> Para aumentar o volume: On! | Up! | Increase|\n" +
                                    "-> Para diminuir o volume: Low! | Down! | Decrease")
                            .setNegativeButton("Voltar", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                    break;
                }
            }
        }
    };

    private void startVoiceRecognition(){

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale seu comando...");
        startActivityForResult(intent, REQUEST_CODE);

    }



    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra( RecognizerIntent.EXTRA_RESULTS);

            for(int i = 0; i < matches.size();i++) {
                if(matches.get(i).equalsIgnoreCase("play")) {
                    if (isStarted) {
                        player.start();
                        playButton.setImageResource(android.R.drawable.ic_media_pause);
                        updatePosition();

                    }
                }
                if(matches.get(i).equalsIgnoreCase("start")) {
                    if (isStarted) {
                        player.start();
                        playButton.setImageResource(android.R.drawable.ic_media_pause);
                        updatePosition();

                    }
                }
                if(matches.get(i).equalsIgnoreCase("go")){
                    if (isStarted) {
                        player.start();
                        playButton.setImageResource(android.R.drawable.ic_media_pause);
                        updatePosition();

                    }
                }
                if(matches.get(i).equalsIgnoreCase("ok go")){
                    if (isStarted) {
                        player.start();
                        playButton.setImageResource(android.R.drawable.ic_media_pause);
                        updatePosition();

                    }
                }
                if(matches.get(i).equalsIgnoreCase("continue")){
                    if (isStarted) {
                        player.start();
                        playButton.setImageResource(android.R.drawable.ic_media_pause);
                        updatePosition();

                    }
                }

                if(matches.get(i).equalsIgnoreCase("pause")) {
                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                }
                if(matches.get(i).equalsIgnoreCase("wait")) {
                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                }
                if(matches.get(i).equalsIgnoreCase("hold")) {
                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                }
                if(matches.get(i).equalsIgnoreCase("hold the door")) {
                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                }
                if(matches.get(i).equalsIgnoreCase("stop")) {
                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                }

                if(matches.get(i).equalsIgnoreCase("next")) {
                    int current = tags.lastIndexOf(currentFile);
                    if(current == tags.size()-1){

                        currentFile = tags.get(0);
                        startPlay(currentFile);
                    }
                    else{
                        currentFile = tags.get(current+1);
                        startPlay(currentFile);
                    }
                }

                if(matches.get(i).equalsIgnoreCase("previous")) {
                    int current = tags.lastIndexOf(currentFile);
                    if(current == 0){
                        currentFile = tags.get(0);
                        startPlay(currentFile);
                    }
                    else{
                        currentFile = tags.get(current-1);
                        startPlay(currentFile);
                    }
                }
                if(matches.get(i).equalsIgnoreCase("back")) {
                    int current = tags.lastIndexOf(currentFile);
                    if(current == 0){
                        currentFile = tags.get(0);
                        startPlay(currentFile);
                    }
                    else{
                        currentFile = tags.get(current-1);
                        startPlay(currentFile);
                    }
                }



                if(matches.get(i).equalsIgnoreCase("increase")) {
                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_VIBRATE );
                    Toast.makeText(MainActivity.this, "Volume aumentou",Toast.LENGTH_LONG).show();

                }
                if(matches.get(i).equalsIgnoreCase("on")) {
                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_VIBRATE );
                    Toast.makeText(MainActivity.this, "Volume aumentou",Toast.LENGTH_LONG).show();
                }
                if(matches.get(i).equalsIgnoreCase("up")) {
                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE, AudioManager.FLAG_VIBRATE );
                    Toast.makeText(MainActivity.this, "Volume aumentou",Toast.LENGTH_LONG).show();
                }
                if(matches.get(i).equalsIgnoreCase("decrease")) {
                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER, AudioManager.FLAG_VIBRATE );
                    Toast.makeText(MainActivity.this, "Volume diminuiu",Toast.LENGTH_LONG).show();

                }
                if(matches.get(i).equalsIgnoreCase("down")) {
                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER, AudioManager.FLAG_VIBRATE );
                    Toast.makeText(MainActivity.this, "Volume diminuiu",Toast.LENGTH_LONG).show();
                }
                if(matches.get(i).equalsIgnoreCase("low")) {
                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER, AudioManager.FLAG_VIBRATE );
                    Toast.makeText(MainActivity.this, "Volume diminuiu",Toast.LENGTH_LONG).show();
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

