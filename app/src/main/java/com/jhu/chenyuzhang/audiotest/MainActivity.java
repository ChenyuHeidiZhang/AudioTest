package com.jhu.chenyuzhang.audiotest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothTest";

    private Button btnSound;
    private Button btnSound2;
    private ImageView blackView;
    private TextView tvTime;
    private TextView tvReceived;

    private TimeDbHelper timeRecordDb;

    private Handler handler = new Handler();

    MediaPlayer mp;
    Context context = this;

    MediaPlayer mp2;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    public static OutputStream mmOutputStream;
    InputStream mmInputStream;

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    boolean sending = false;

    private Choreographer.FrameCallback frameCallback = null;
    private boolean frameCallbackPending = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSound = findViewById(R.id.button);
        btnSound2 = findViewById(R.id.button2);
        blackView = findViewById(R.id.imageView);
        tvTime = findViewById(R.id.tv_timestamp);
        tvReceived = findViewById(R.id.tv_received);

        timeRecordDb = new TimeDbHelper(this);

        //mp = MediaPlayer.create(context, R.raw.my_tone);
        //mp2 = MediaPlayer.create(context, R.raw.my_tone1);

        mp = MediaPlayer.create(context, R.raw.high_state_pulse);
        mp2 = MediaPlayer.create(context, R.raw.low_state_pulse);

        Log.d(TAG,"trying to findBT");
        findBT();

        btnSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {

                        handler.postDelayed(this, 3000);

                        armVSyncHandler();

                        String strobe = getBinaryTime();
                        //timeRecordDb.insertData(getCurrentTime(), "sound");
                        //playSound(strobe);

                        try {
                            sendData(strobe);
                            Log.d(TAG, "data sent");
                            timeRecordDb.insertData(strobe, "receive: " + tvReceived.getText());

                        } catch (IOException ex) {
                            Log.d(TAG, "data not sent");
                        }

                    }
                };

                if (sending==false) {
                    sending = true;
                    handler.post(myRunnable);
                } else {
                    //finishAndRemoveTask();
                    handler.removeCallbacks(myRunnable);
                    sending = false;
                }
            }

        });


        btnSound2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                if (blackView.getVisibility()==View.INVISIBLE) {
                    blackView.setVisibility(View.VISIBLE);
                    //timeRecordDb.insertData(getCurrentTime(), "black");
                } else {
                    blackView.setVisibility(View.INVISIBLE);
                    //timeRecordDb.insertData(getCurrentTime(), "white");
                }
                */

                armVSyncHandler();

                String strobe = getBinaryTime();
                //timeRecordDb.insertData(getCurrentTime(), "sound tap");
                //playSound(strobe);

                try {
                    sendData(strobe);
                    Log.d(TAG,"data sent");
                    timeRecordDb.insertData(strobe, "receive: "+tvReceived.getText());
                }
                catch (IOException ex) {
                    Log.d(TAG, "data not sent");
                }


            }
        });

        /*mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        }); */

    }

    public void armVSyncHandler() {
        if(!frameCallbackPending) {
            frameCallbackPending = true;
            if(frameCallback == null)
            {
                frameCallback = new Choreographer.FrameCallback() {
                    @Override
                    public void doFrame(long frameTimeNanos) {
                        frameCallbackPending = false;

                        if (blackView.getVisibility() == View.INVISIBLE) {
                            blackView.setVisibility(View.VISIBLE);
                        } else {
                            blackView.setVisibility(View.INVISIBLE);
                        }

                        armVSyncHandler();
                    }
                };
            }
            Choreographer.getInstance().postFrameCallback(frameCallback);
        }
    }

    private String getCurrentTime() {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss.SSS");
        String formattedDate= dateFormat.format(date);
        return formattedDate;
    }

    public String getBinaryTime() {
        String time = getCurrentTime();

        String binaryTime = "";
        int h = Integer.parseInt(time.substring(0,2));
        binaryTime += String.format("%"+Integer.toString(5)+"s",Integer.toBinaryString(h)).replace(" ","0");

        int m = Integer.parseInt(time.substring(3,5));
        binaryTime += String.format("%"+Integer.toString(6)+"s",Integer.toBinaryString(m)).replace(" ","0");

        int s = Integer.parseInt(time.substring(6,8));
        binaryTime += String.format("%"+Integer.toString(6)+"s",Integer.toBinaryString(s)).replace(" ","0");

        int ms = Integer.parseInt(time.substring(9,12));
        binaryTime += String.format("%"+Integer.toString(10)+"s",Integer.toBinaryString(ms)).replace(" ","0");

        tvTime.setText(binaryTime);
        return binaryTime;
    }

    private void playSound(String strobe) {
        char[] bits = strobe.toCharArray();
        for (int i = 0; i < bits.length; i++) {
            char b = bits[i];
            if (b=='1') {
                try {
                    if (mp.isPlaying()) {
                        mp.stop();
                        mp.release();
                        mp = MediaPlayer.create(context, R.raw.high_state_pulse);
                    } mp.start();
                } catch(Exception e) { e.printStackTrace(); }

            } else if (b=='0') {
                try {
                    if (mp2.isPlaying()) {
                        mp2.stop();
                        mp2.release();
                        mp2 = MediaPlayer.create(context, R.raw.low_state_pulse);
                    } mp2.start();
                } catch(Exception e) { e.printStackTrace(); }
            }
        }
    }


    public void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.d(TAG,"No bluetooth adapter available");
            return;
        } else {
            Log.d(TAG, "Bluetooth adapter is not null");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            Log.d(TAG, "pairedDevices>0");
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-06"))
                {
                    mmDevice = device;

                    ParcelUuid[] uuids = device.getUuids();
                    try {
                        openBT(uuids);
                    } catch (IOException e) {
                        Log.d(TAG, "can't openBT with "+ uuids[0].getUuid());
                    }

                    break;
                }
            }
        }
        Log.d(TAG,"Bluetooth Device Found");
    }

    public void openBT(ParcelUuid[] uuids) throws IOException
    {
        //UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
        Log.d(TAG, "createRfcommSocketToServiceRecord" + uuids[0].getUuid());
        mmSocket.connect();
        Log.d(TAG, "connect");
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        Log.d(TAG,"Bluetooth Opened");
    }


    public void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            tvReceived.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void sendData(String msg) throws IOException
    {
        try {
            mmOutputStream = mmSocket.getOutputStream();
        } catch (IOException e) {}

        msg += "\n";
        mmOutputStream.write(msg.getBytes());
    }


    public void closeBT() throws IOException
    {
        //stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Log.d(TAG,"Bluetooth Closed");
    }


}
