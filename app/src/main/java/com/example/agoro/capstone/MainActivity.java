package com.example.agoro.capstone;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {
    TextView textview;
    GestureDetector detector;

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    String fullName = "";

    private final String DEVICE_ADDRESS="00:21:13:00:E1:2D";//재현
    //private final String DEVICE_ADDRESS="00:21:13:00:F5:F4";


    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    boolean deviceConnected=false;
    Thread thread;
    byte buffer[];
    int bufferPosition;
    boolean stopThread;

    String RFID[] = {
            "e070c935",
            "0c75c149",
            "4850be49",
            "52494710",
            "42acf710"
    };
  /*
    String RFID[] = {
            "070c935",
            "c75c149",
            "850be49",
            "2494710",
            "2acf710"
    };
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //display on
        textview = new TextView(this);
        textview.setText("Gesture Demo");

        if(BTinit())
        {
            if(BTconnect())
            {
                //setUiEnabled(true);
                deviceConnected=true;
                beginListenForData();
                //textView.append("\nConnection Opened!\n");
                Toast.makeText(getApplicationContext(),"Connection Opened!",Toast.LENGTH_SHORT).show();
            }

        }
        detector = new GestureDetector(this);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        //Toast.makeText(getApplicationContext(), "OnDown Gesture", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //Toast.makeText(getApplicationContext(), "Fling Gesture", Toast.LENGTH_SHORT).show();
        try {
            //if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
            //    return false;

            // right to left swipe
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                Toast.makeText(getApplicationContext(), "Left Swipe", Toast.LENGTH_SHORT).show();
                Intent RFIDIntent = new Intent(getApplicationContext(), RFIDActivity.class);
                startActivity(RFIDIntent);
            }
            // left to right swipe
            else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                Toast.makeText(getApplicationContext(), "Right Swipe", Toast.LENGTH_SHORT).show();
                Intent RouteIntent = new Intent(getApplicationContext(), RouteActivity.class);
                startActivity(RouteIntent);
            }
            // down to up swipe
            else if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                Toast.makeText(getApplicationContext(), "Swipe up", Toast.LENGTH_SHORT).show();
                Intent InfoIntent = new Intent(getApplicationContext(), InfoActivity.class);
                startActivity(InfoIntent);
            }
            // up to down swipe
            else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                Toast.makeText(getApplicationContext(), "Swipe down", Toast.LENGTH_SHORT).show();
                Intent BusIntent = new Intent(getApplicationContext(), BusActivity.class);
                startActivity(BusIntent);
            }
        } catch (Exception e) {

        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        //Toast.makeText(getApplicationContext(), "Long Press Gesture", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //Toast.makeText(getApplicationContext(), "Scroll Gesture", Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        //Toast.makeText(getApplicationContext(), "Show Press gesture", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        //Toast.makeText(getApplicationContext(), "Single Tap Gesture", Toast.LENGTH_SHORT).show();
        return true;
    }


    public boolean BTinit()
    {
        boolean found=false;
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show();
        }
        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 2);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device=iterator;
                    found=true;
                    break;
                }
            }
        }
        return found;
    }

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return connected;
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {

                            final byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes, 0, byteCount); // 0과 byteCount 추가함
                            String string = new String(rawBytes, "UTF-8");
                            //String string = new String(rawBytes, 0, byteCount);
                            Log.d("string", string);
                            handler.post(new Runnable() {
                                public void run()
                                {
                                    //textView.append(string);
                                    if(fullName.length() > 8){
                                        Toast.makeText(getApplicationContext(), "here", Toast.LENGTH_SHORT).show();
                                        for(int i = 0; i < RFID.length; i++) {
                                            if (fullName.matches(RFID[i])) {
                                                Toast.makeText(getApplicationContext(), "found" + i+1,Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        fullName = "";
                                    }
                                }
                            });

                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }


}
