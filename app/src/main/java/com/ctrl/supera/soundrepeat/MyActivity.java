package com.ctrl.supera.soundrepeat;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;


public class MyActivity extends ActionBarActivity {
    private static final String TAG = "MyActivity";
    private static final String RECORD_DIR = "//sdcard//Record";
    private static final String RECORD_REPEAT_FILE = "repeat.wav";

    ExtAudioRecorder recorder;
    Button bt_record_ctrl;
    Button bt_play_ctrl;

    /**
     * Last Record filename (include the total path)
     */
    public String last_record_file;

    MediaPlayer mediaPlayer;

    AutoRecordPlay auto_record_play;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        bt_record_ctrl = (Button) findViewById(R.id.ButtonRecordCtrl);
        bt_record_ctrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recordMedia() == true) {
                    bt_record_ctrl.setText("Stop Record");
                } else {
                    bt_record_ctrl.setText("Record");
                }
            }
        });

        bt_play_ctrl = (Button) findViewById(R.id.ButtonPlayCtrl);
        bt_play_ctrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (playLastRecordFile() == true) {
                    bt_play_ctrl.setText("Stop Play");
                } else {
                    bt_play_ctrl.setText("Play");
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        auto_record_play = new AutoRecordPlay();
        auto_record_play.execute();
    }

    @Override
    public void onPause() {
        super.onPause();

        auto_record_play.cancel(true);
    }

    private class AutoRecordPlay extends AsyncTask<String, Void, String> {
        /**
         * The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute()
         */
        protected String doInBackground(String... urls) {
            boolean status;
            int duration;

            while (true) {
            /* First record media */
                status = startRecord();
                if (status == false) {
                    Log.d(TAG, "Start record file fail");
                    return "Fail";
                }

                /* Check if record is over */
                while (recorder.getRecordFinish() == true) {
                    if (isCancelled() == true) {
                        Log.d(TAG, "User cancel on record process");
                        stopRecord();
                        return "Cancel";
                    }
                }

                status = stopRecord();
                if (status == false) {
                    Log.d(TAG, "Stop record file fail");
                    return "Fail";
                }

                if (isCancelled() == true) {
                    Log.d(TAG, "User cancel on record finish");
                    return "Cancel";
                }

                status = startPlayRecord();
                if (status == false) {
                    Log.d(TAG, "Start play file fail");
                    return "Fail";
                }

                duration = mediaPlayer.getDuration();

                while (true) {
                    Log.d(TAG, "duration:" + duration + "\tcurrent:" + mediaPlayer.getCurrentPosition());
                    if ((mediaPlayer.getCurrentPosition() >= duration) || (mediaPlayer.isPlaying() == false)) {
                        break;
                    }
                    if (isCancelled() == true) {
                        Log.d(TAG, "User cancel on play process");
                        stopPlayRecord();
                        return "Cancel";
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.d(TAG, e.getMessage());
                        e.printStackTrace();
                    }
                }

                status = stopPlayRecord();
                if (status == false) {
                    Log.d(TAG, "Stop play file fail");
                    return "Fail";
                }

                if (isCancelled() == true) {
                    Log.d(TAG, "User cancel on play finish");
                    return "Cancel";
                }
            }
        }

        /**
         * The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground()
         */
        protected void onPostExecute(String result) {

        }
    }

    /**
     * Start Record File to an fix file
     *
     * @return true if sucessfully, false if fail
     */
    public boolean startRecord() {
        recorder = ExtAudioRecorder.getInstanse(false);
        recorder.recordChat(RECORD_DIR + File.separator, RECORD_REPEAT_FILE);
        if (recorder.getState() != ExtAudioRecorder.State.ERROR) {
            return true;
        }
        return false;
    }

    /**
     * Stop Record File to an fix file
     *
     * @return true if sucessfully, false if fail
     */
    public boolean stopRecord() {
        recorder.stopRecord();
        recorder = null;
        return true;
    }

    public boolean startPlayRecord() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(RECORD_DIR + File.separator + RECORD_REPEAT_FILE);
            mediaPlayer.prepare();
            mediaPlayer.start();
            return true;
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
        mediaPlayer = null;
        return false;
    }

    public boolean stopPlayRecord() {
        mediaPlayer.release();
        mediaPlayer = null;
        return true;
    }

    public boolean recordMedia() {
        if (recorder == null) {
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss");
            String filename = "Record " + sDateFormat.format(new java.util.Date()) + ".wav";
            last_record_file = RECORD_DIR + File.separator + filename;
            recorder = ExtAudioRecorder.getInstanse(false); // 未压缩的录音（WAV）
            recorder.recordChat(RECORD_DIR + File.separator,
                    filename);
            if (recorder.getState() != ExtAudioRecorder.State.ERROR) {
                return true;
            } else {
                recorder = null;
                return false;
            }
        } else {
            recorder.stopRecord();
            recorder = null;
            return false;
        }
    }

    public boolean playLastRecordFile() {
        if (last_record_file != null) {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(last_record_file);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    return true;
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                    e.printStackTrace();
                }
                mediaPlayer = null;
                return false;
            } else {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
        return false;
    }
}
