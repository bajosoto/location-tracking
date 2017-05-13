package com.example.administrator.smartphonesensing;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v4.content.ContextCompat;
import android.Manifest;
import android.util.Log;

import com.example.administrator.smartphonesensing.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

/**
 * Smart Phone Sensing Data Acquisition Code
 */
public class MainActivity extends Activity implements SensorEventListener {

    /* The number of access points we are taking into consideration */
    private static final int numSSIDs = 5;
    private static Vector<ReferencePoint> refPoints = new Vector();
    private static String scanReqSender = "";

    private static final String DIR_NAME = "SmartPhoneSensing";
    private static final int REQUEST_CODE_WRITE_PERMISSION = 0;
    private static final int REQUEST_CODE_WIFI_PERMISSION = 0;
    private static final String LOG_TAG = "MainActivity.LOG";
    private static final boolean LOG_INFO = true;
    private static final boolean LOG_ERR = true;
    /**
     * The sensor manager object.
     */
    private SensorManager sensorManager;
    /**
     * The accelerometer.
     */
    private Sensor accelerometer;
    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    /**
     * The wifi info.
     */
    private WifiInfo wifiInfo;
    /**
     * Accelerometer x value
     */
    private float aX = 0;
    /**
     * Accelerometer y value
     */
    private float aY = 0;
    /**
     * Accelerometer z value
     */
    private float aZ = 0;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
    String timestamp = simpleDateFormat.format(new Date());


    /**
     * Text fields to show the sensor values.
     */
    private TextView currentX, currentY, currentZ, titleAcc, textRssi;
    private EditText tbRoomName;

    Button buttonRssi, buttonLocation;

    private File root, dir;
    private String logFileNameAcc, logFileNameRSSI;
    private List<ScanResult> scanResults;

    //AppCompatActivity appCompatActivity;

    public String knn(ReferencePoint refPoint, Vector<ReferencePoint> referencePoints) {
        int smallestDistance = 0;
        String label = "some place, somewhere...";

        for (ReferencePoint rp : referencePoints){
            textRssi.setText("Comparing" + refPoint.toString() + "/n With: " + rp.toString());
            int distance = 0;
            for (String key : refPoint.accessPoints.keySet()){
                distance += Math.pow((refPoint.getApDistance(key) - rp.getApDistance(key)), 2);
            }
            distance = (int) Math.sqrt(distance);
            if(smallestDistance == 0 || smallestDistance > distance) {
                smallestDistance = distance;
                label = rp.label;
            }
        }
        return label;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Remove hardcoded values maybe
        logFileNameAcc = "logAcc.txt";
        logFileNameRSSI = "logRSSI.txt";

        // Check for writing permission to external memory of the device
        if (isExternalStorageWritable())
            checkWritingPermission();

        checkWifiPermission();

        root = android.os.Environment.getExternalStorageDirectory();
        dir = new File(root.getAbsolutePath() + "/" + DIR_NAME);
        if (!dir.mkdirs())
        {
            // file not created
            if(LOG_ERR) Log.e(LOG_TAG,"Directory Creation Failed!!");
        }

        // Create the text views.
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);
        titleAcc = (TextView) findViewById(R.id.titleAcc);
        textRssi = (TextView) findViewById(R.id.textRSSI);

        // Create the button
        buttonRssi = (Button) findViewById(R.id.buttonRSSI);
        buttonLocation = (Button) findViewById(R.id.buttonLocation);
        tbRoomName = (EditText) findViewById(R.id.tbRoomName);

        clearFile(logFileNameRSSI, timestamp);


        // Set the sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // if the default accelerometer exists
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // set accelerometer
            accelerometer = sensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // register 'this' as a listener that updates values. Each time a sensor value changes,
            // the method 'onSensorChanged()' is called.
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // No accelerometer!
        }

        // Set the wifi manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //wifiManager.startScan();

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                scanResults = wifiManager.getScanResults();
                Collections.sort(scanResults, new Comparator<ScanResult>() {
                    @Override
                    public int compare(ScanResult lhs, ScanResult rhs) {
                        // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                        return lhs.level > rhs.level ? -1 : (lhs.level < rhs.level ) ? 1 : 0;
                    }
                });

                int foundAPs = scanResults.size();
                scanResults = scanResults.subList(0, foundAPs >= numSSIDs ? numSSIDs : foundAPs);
                //String line = "";

                ReferencePoint point = new ReferencePoint(tbRoomName.getText().toString());

                for (ScanResult s : scanResults)
                {
                    //line += tbRoomName.getText() + "\t" + s.BSSID + "\t" + s.level + "\n";
                    point.addAP(s.BSSID, s.level);
                }

                //textRssi.setText(point.toString());
                if (scanReqSender == "rssi"){
                    refPoints.addElement(point);
                    scanReqSender = "";
                    textRssi.setText("Acquired!");
                } else if (scanReqSender == "location") {
                    textRssi.setText("You are in " + knn(point, refPoints));
                    scanReqSender = "";
                }


                //writeToFile(logFileNameRSSI, line, true);
                //writeToFile(logFileNameRSSI, "--------------------------------------------\n", true);
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // Create a click listener for our button.
        buttonRssi.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scanReqSender = "rssi";
                textRssi.setText("Acquiring...");
                wifiManager.startScan();
            }
        });

        // Create a click listener for our button.
        buttonLocation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Add some way to identify which button is requesting the scan
                scanReqSender = "location";
                textRssi.setText("Finding your location...");
                wifiManager.startScan();
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_WRITE_PERMISSION) {
            if (grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
            } else {
                // permission wasn't granted
                if(LOG_INFO) Log.i(LOG_TAG,"No Write Permission!!");
            }
        }
        if (requestCode == REQUEST_CODE_WIFI_PERMISSION) {
            if (grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
            } else {
                // permission wasn't granted
                if(LOG_INFO) Log.i(LOG_TAG,"No WiFi Permission!!");
            }
        }
    }

    private void checkWritingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // permission wasn't granted
                if(LOG_INFO) Log.i(LOG_TAG,"No Write Permission!!");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_PERMISSION);
            }
        }
    }

    private void checkWifiPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // permission wasn't granted
                if(LOG_INFO) Log.i(LOG_TAG,"No WiFi Permission!!");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_WIFI_PERMISSION);
            }
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");

        // get the the x,y,z values of the accelerometer
        aX = event.values[0];
        aY = event.values[1];
        aZ = event.values[2];

        // display the current x,y,z accelerometer values
        currentX.setText(Float.toString(aX));
        currentY.setText(Float.toString(aY));
        currentZ.setText(Float.toString(aZ));

        if ((Math.abs(aX) > Math.abs(aY)) && (Math.abs(aX) > Math.abs(aZ))) {
            titleAcc.setTextColor(Color.RED);
        }
        if ((Math.abs(aY) > Math.abs(aX)) && (Math.abs(aY) > Math.abs(aZ))) {
            titleAcc.setTextColor(Color.BLUE);
        }
        if ((Math.abs(aZ) > Math.abs(aY)) && (Math.abs(aZ) > Math.abs(aX))) {
            titleAcc.setTextColor(Color.GREEN);
        }

        String line = String.format(Locale.getDefault(), "%f\t%f\t%f\n", aX, aY, aZ);
        writeToFile(logFileNameAcc, line, true);
    }

    private File writeToFile(String fileName, String line, boolean append)
    {
        if (!isExternalStorageWritable())
            return null;
        File file = new File(dir, fileName);
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(file, append);
            fileWriter.write(line);
            fileWriter.flush();
            fileWriter.close();
            return file;
        } catch (IOException e) {
            if(LOG_ERR) Log.e(LOG_TAG,"Can't Write To File!!");
            e.printStackTrace();
            return null;
        }
    }

    private File clearFile(String fileName, String date)
    {
        if (!isExternalStorageWritable())
            return null;
        File file = new File(dir, fileName);
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(timestamp);
            fileWriter.write("\n\n");
            fileWriter.flush();
            fileWriter.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

