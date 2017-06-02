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
import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v4.content.ContextCompat;
import android.Manifest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import static android.net.wifi.WifiManager.calculateSignalLevel;
import static com.example.administrator.smartphonesensing.LogWriter.isExternalStorageWritable;

/**
 * Smart Phone Sensing Data Acquisition Code
 */
public class MainActivity extends Activity implements SensorEventListener {

    /* The number of access points we are taking into consideration */
    private static final int numSSIDs = 5;
    private static String scanReqSender = "";
    private static Vector<RSSPoint> refPoints = new Vector();
    private List<ScanResult> scanResults;

    private static final int numACCSamples = 80;
    private static int currAccSample = 0;
    private static String accReqSender = "";
    private static Vector<ACCPoint> accPoints = new Vector();
    private List<Float> xResults = new Vector();
    private List<Float> yResults = new Vector();
    private List<Float> zResults = new Vector();

    LogWriter logAcc = new LogWriter("logAcc.txt");
    LogWriter logRss = new LogWriter("logRss.txt");

    ProbMassFuncs pmf = new ProbMassFuncs(3, 5);


    private static final int REQUEST_CODE_WRITE_PERMISSION = 0;
    private static final int REQUEST_CODE_WIFI_PERMISSION = 0;
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

    /**
     * Text fields to show the sensor values.
     */
    private TextView currentX, currentY, currentZ, titleAcc, textRssi, textAcc;
    private EditText tbRoomName;

    Button buttonRssi, buttonLocation, buttonWalk, buttonStand, buttonWalkOrStand;

    //AppCompatActivity appCompatActivity;

    public String knn(RSSPoint refPoint, Vector<RSSPoint> RSSPoints) {
        int smallestDistance = 0;
        String label = "some place, somewhere...";

        for (RSSPoint rp : RSSPoints){
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

    public String knn(ACCPoint refPoint, Vector<ACCPoint> ACCPoints) {
        int smallestDistance = 0;
        String label = "some place, somewhere...";

        for (ACCPoint ap : ACCPoints){

            int distance = 0;
            distance += Math.pow((refPoint.getX() - ap.getX()), 2);
            distance += Math.pow((refPoint.getY() - ap.getY()), 2);
            distance += Math.pow((refPoint.getZ() - ap.getZ()), 2);
            distance = (int) Math.sqrt(distance);

            if(smallestDistance == 0 || smallestDistance > distance) {
                smallestDistance = distance;
                label = ap.label;
            }
        }
        return label;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for writing permission to external memory of the device
        if (isExternalStorageWritable())
            checkWritingPermission();

        checkWifiPermission();


        // Create the text views.
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);
        titleAcc = (TextView) findViewById(R.id.titleAcc);
        textRssi = (TextView) findViewById(R.id.textRSSI);
        textAcc = (TextView) findViewById(R.id.textAcc);

        // Create the buttons
        buttonRssi = (Button) findViewById(R.id.buttonRSSI);
        buttonLocation = (Button) findViewById(R.id.buttonLocation);
        buttonWalk = (Button) findViewById(R.id.buttonWalk);
        buttonStand = (Button) findViewById(R.id.buttonStand);
        buttonWalkOrStand = (Button) findViewById(R.id.buttonWalkOrStand);

        tbRoomName = (EditText) findViewById(R.id.tbRoomName);

        logAcc.clearFile();
        logRss.clearFile();

        // Set the sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // if the default accelerometer exists
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // set accelerometer
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // register 'this' as a listener that updates values. Each time a sensor value changes,
            // the method 'onSensorChanged()' is called.
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_GAME);
        } else {
            // No accelerometer!
        }

        // Set the wifi manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

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
                String line = "";

                RSSPoint point = new RSSPoint(tbRoomName.getText().toString());

                for (ScanResult s : scanResults)
                {
                    line += tbRoomName.getText() + "\t" + s.BSSID + "\t" + calculateSignalLevel(s.level, 255) + "\n";
                    point.addAP(s.BSSID, s.level);
                }


                if (scanReqSender == "rssi"){
                    // New stuff pmf
                    pmf.addScanResults(scanResults, Integer.parseInt(tbRoomName.getText().toString()));

                    refPoints.addElement(point);
                    scanReqSender = "";
                    textRssi.setText("Acquired!");
                } else if (scanReqSender == "location") {
                    textRssi.setText("You are in " + knn(point, refPoints));
                    scanReqSender = "";

                    // New stuff
                    pmf.calcGauss();
                    pmf.logPMF();
                }


                logRss.writeToFile(line, true);
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
                scanReqSender = "location";
                textRssi.setText("Finding your location...");
                wifiManager.startScan();
            }
        });

        // Create a click listener for our button.
        buttonWalk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                accReqSender = "walking";
                textAcc.setText("Start walking...");
            }
        });

        // Create a click listener for our button.
        buttonStand.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                accReqSender = "standing";
                textAcc.setText("Stand still...");
            }
        });

        // Create a click listener for our button.
        buttonWalkOrStand.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                accReqSender = "walkorstand";
                textAcc.setText("Tracking your movement...");
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
            }
        }
        if (requestCode == REQUEST_CODE_WIFI_PERMISSION) {
            if (grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
            } else {
                // permission wasn't granted
            }
        }
    }

    private void checkWritingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // permission wasn't granted
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_PERMISSION);
            }
        }
    }

    private void checkWifiPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // permission wasn't granted
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_WIFI_PERMISSION);
            }
        }
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

        //String line = String.format(Locale.getDefault(), "%f\t%f\t%f\n", aX, aY, aZ);
        //writeToFile(logFileNameAcc, line, true);

        if (!accReqSender.equals("")) {
            if (currAccSample == 0) {        // First element is stored unfiltered
                xResults.add(aX);
                yResults.add(aY);
                zResults.add(aZ);
            } else {                        // Remaining elements are smoothed
                xResults.add(0.5f * aX + 0.5f * xResults.get(currAccSample - 1));
                yResults.add(0.5f * aY + 0.5f * yResults.get(currAccSample - 1));
                zResults.add(0.5f * aZ + 0.5f * zResults.get(currAccSample - 1));
            }
            currAccSample++;
            if (currAccSample == numACCSamples) {   // If this is the last sample in the window
                // Sort Lists
                Collections.sort(xResults, new Comparator<Float>() {
                    @Override
                    public int compare(Float lhs, Float rhs) {
                        return lhs > rhs ? -1 : (lhs < rhs) ? 1 : 0;
                    }
                });
                Collections.sort(yResults, new Comparator<Float>() {
                    @Override
                    public int compare(Float lhs, Float rhs) {
                        return lhs > rhs ? -1 : (lhs < rhs) ? 1 : 0;
                    }
                });
                Collections.sort(zResults, new Comparator<Float>() {
                    @Override
                    public int compare(Float lhs, Float rhs) {
                        return lhs > rhs ? -1 : (lhs < rhs) ? 1 : 0;
                    }
                });

                // Calculate peak to peak values
                Float newX = Math.abs(xResults.get(0) - xResults.get(xResults.size() - 1));
                Float newY = Math.abs(yResults.get(0) - yResults.get(yResults.size() - 1));
                Float newZ = Math.abs(zResults.get(0) - zResults.get(zResults.size() - 1));

                // Create new ACCPoint instance
                ACCPoint newAccPoint = new ACCPoint(accReqSender, newX, newY, newZ);

                if (accReqSender.equals("walkorstand")) {
                    // Do KNN and update label
                    textAcc.setText("You are " + knn(newAccPoint, accPoints));
                } else {
                    // Add new trained data to Vector
                    textAcc.setText("Done!");
                    accPoints.add(newAccPoint);
                }

                currAccSample = 0;
                xResults.clear();
                yResults.clear();
                zResults.clear();
                accReqSender = "";
            }
        }
    }
}

