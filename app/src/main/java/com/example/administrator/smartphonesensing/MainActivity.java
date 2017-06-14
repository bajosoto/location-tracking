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
import android.support.v4.widget.SearchViewCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.support.v4.content.ContextCompat;
import android.Manifest;

import android.widget.Toast;


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
    private static int numSSIDs = 3;
    /* The number of RSS levels (e.g. 0..255) we are taking into consideration */
    private static int numRSSLvl = 100;
    /* The number of cells we are taking into consideration */
    private static int numRooms = 3;
    /* The number of samples per room we will be taking */
    private static int numScans = 100;
    /* The room we're currently training */
    private static int trainRoom = 0;

    private static String scanReqSender = "";
    private static Vector<RSSPoint> refPoints = new Vector();
    private List<ScanResult> scanResults;

    // Stores the number of samples acquired for a given room
    private int sampleCount = 0;

    private static final int numACCSamples = 80;
    private static int currAccSample = 0;
    private static String accReqSender = "";
    private static Vector<ACCPoint> accPoints = new Vector();
    private List<Float> xResults = new Vector();
    private List<Float> yResults = new Vector();
    private List<Float> zResults = new Vector();

    LogWriter logAcc = new LogWriter("logAcc.txt");
    LogWriter logRss = new LogWriter("logRss.txt");

    // pmf instantiated in OnCreate to load stored pmf
    ProbMassFuncs pmf;
    // floorMap3D cannot be initialized here since it needs the Activity to be initialized first
    // so init is in OnCreate()
    FloorMap floorMap3D;

    int testState = 0; //TODO: Delete this


    private static final int REQUEST_CODE_WRITE_PERMISSION = 0;
    private static final int REQUEST_CODE_WIFI_PERMISSION = 0;
    private static final String LOG_TAG = "MainActivity.LOG";
//    private static final boolean LOG_INFO = true;
//    private static final boolean LOG_ERR = true;
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
    private TextView currentX, currentY, currentZ, titleAcc, textRssi, textAcc, textBayes,
            titleCfgApNum, titleCfgRssLvlNum, titleCfgRoomsNum, titleCfgScansNum, titleTrainRoomNum,
            textTraining;
    // private EditText tbRoomName;

    Button buttonRssi, buttonLocation, buttonWalk, buttonStand, buttonWalkOrStand, buttonBayesIterate,
    buttonBayesNew, buttonBayesCompile, buttonTest, buttonCfgApSubst, buttonCfgApAdd, buttonCfgRssLvlSubst,
            buttonCfgRssLvlAdd, buttonCfgRoomsSubst, buttonCfgRoomsAdd, buttonCfgScansSubst,
            buttonCfgScansAdd, buttonTrainRoomSubs, buttonTrainRoomAdd;

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

        pmf = new ProbMassFuncs(numRooms, numRSSLvl);
        if (pmf.loadPMF()) {
            Toast.makeText(MainActivity.this, "Loaded PMF", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Invalid / No PMF found, created new", Toast.LENGTH_SHORT).show();
        }
        // init 3D map TODO: Enable this back
        // floorMap3D = new FloorMap(this, this);

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
        textBayes = (TextView) findViewById(R.id.textBAYES);
        titleCfgApNum = (TextView) findViewById(R.id.titleCfgApNum);
        titleCfgRssLvlNum = (TextView) findViewById(R.id.titleCfgRssLvlNum);
        titleCfgRoomsNum = (TextView) findViewById(R.id.titleCfgRoomsNum);
        titleCfgScansNum = (TextView) findViewById(R.id.titleCfgScansNum);
        titleTrainRoomNum = (TextView) findViewById(R.id.titleTrainRoomNum);
        textTraining = (TextView) findViewById(R.id.textTraining);

        titleCfgApNum.setText(" " + numSSIDs + " ");
        titleCfgRssLvlNum.setText(" " + numRSSLvl + " ");
        titleCfgRoomsNum.setText(" " + numRooms + " ");
        titleCfgScansNum.setText(" " + numScans + " ");
        titleTrainRoomNum.setText(" " + (trainRoom + 1) + " ");

        // Create the buttons
        buttonRssi = (Button) findViewById(R.id.buttonRSSI);
        buttonLocation = (Button) findViewById(R.id.buttonLocation);
        buttonWalk = (Button) findViewById(R.id.buttonWalk);
        buttonStand = (Button) findViewById(R.id.buttonStand);
        buttonWalkOrStand = (Button) findViewById(R.id.buttonWalkOrStand);
        buttonBayesNew = (Button) findViewById(R.id.buttonBayesNew);
        buttonBayesIterate = (Button) findViewById(R.id.buttonBayesIterate);
        buttonBayesCompile = (Button) findViewById(R.id.buttonBayesCompile);
        buttonTest = (Button) findViewById(R.id.buttonTest);
        buttonCfgApSubst = (Button) findViewById(R.id.buttonCfgApSubst);
        buttonCfgApAdd = (Button) findViewById(R.id.buttonCfgApAdd);
        buttonCfgRssLvlSubst = (Button) findViewById(R.id.buttonCfgRssLvlSubst);
        buttonCfgRssLvlAdd = (Button) findViewById(R.id.buttonCfgRssLvlAdd);
        buttonCfgRoomsSubst = (Button) findViewById(R.id.buttonCfgRoomsSubst);
        buttonCfgRoomsAdd = (Button) findViewById(R.id.buttonCfgRoomsAdd);
        buttonCfgScansSubst = (Button) findViewById(R.id.buttonCfgScansSubst);
        buttonCfgScansAdd = (Button) findViewById(R.id.buttonCfgScansAdd);
        buttonTrainRoomSubs = (Button) findViewById(R.id.buttonTrainRoomSubs);
        buttonTrainRoomAdd = (Button) findViewById(R.id.buttonTrainRoomAdd);


        // tbRoomName = (EditText) findViewById(R.id.tbRoomName);

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

                //RSSPoint point = new RSSPoint(tbRoomName.getText().toString());
                RSSPoint point = new RSSPoint(Integer.toString(trainRoom));

                for (ScanResult s : scanResults) {
                    line += "Room " + (trainRoom + 1) + "\t" + s.BSSID + "\t" + calculateSignalLevel(s.level, numRSSLvl) + "\n";
                    point.addAP(s.BSSID, s.level);
                }
                line += "\n";


                if (scanReqSender == "rssi"){

                    // ========================================================================
                    // PMF
                    // ========================================================================
                    // Add current scan results to pmf training
                    pmf.addScanResults(scanResults, trainRoom);
                    // textBayes.setText("Acquired sample (" + (sampleCount + 1) + " / " + numScans + ")");

                    // ========================================================================
                    // KNN
                    // ========================================================================
                    refPoints.addElement(point);
                    // textRssi.setText("Acquired sample (" + (sampleCount + 1) + " / " + numScans + ")");


                    textTraining.setText("Acquired sample (" + (sampleCount + 1) + " / " + numScans + ")");
                    // Check if we still have more scans in this room to do
                    if (sampleCount < numScans) {
                        sampleCount++;
                        wifiManager.startScan();
                    } else {
                        // textRssi.setText("Finished Aquiring!");
                        // textBayes.setText("Finished Aquiring!");
                        textTraining.setText("Finished Aquiring!");
                        Toast.makeText(MainActivity.this, "Acquired " + (sampleCount) + " samples from room "
                                + (trainRoom + 1), Toast.LENGTH_SHORT).show();
                        sampleCount = 0;
                        scanReqSender = "";
                    }

                } else if (scanReqSender == "location") {
                    scanReqSender = "";
                    // ========================================================================
                    // KNN
                    // ========================================================================
                    textRssi.setText("You are in " + knn(point, refPoints));
                } else if (scanReqSender == "locationbayesiter" || scanReqSender == "locationbayesnew") {
                    if(scanReqSender == "locationbayesnew") {
                        pmf.resetLocation();
                    }
                    scanReqSender = "";
                    int estimatedLocationCell = pmf.findLocation(scanResults);
                    String estimatedProb = String.format("%.2f", pmf.getPxPrePost(estimatedLocationCell) * 100);
                    textBayes.setText("I'm " + estimatedProb +
                            "% sure you are in room " + (estimatedLocationCell + 1));
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
                textBayes.setText("Acquiring...");
                wifiManager.startScan();
            }
        });

        // Create a click listener for our button.
        buttonLocation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // ========================================================================
                // KNN
                // ========================================================================
                scanReqSender = "location";
                textRssi.setText("Finding your location...");
                wifiManager.startScan();
            }
        });

        buttonBayesNew.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scanReqSender = "locationbayesnew";
                textBayes.setText("Starting over. Finding your location...");
                wifiManager.startScan();
            }
        });

        // Create a click listener for our button.
        buttonBayesIterate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scanReqSender = "locationbayesiter";
                textBayes.setText("Updating your location...");
                wifiManager.startScan();
            }
        });

        buttonBayesCompile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // ========================================================================
                // PMF
                // ========================================================================
                // Calculate gaussian curves for all
                // textBayes.setText("Calculating Gaussian distributions...");
                textTraining.setText("Calculating Gaussian distributions...");
                pmf.calcGauss();
                // textBayes.setText("Gaussian distributions stored.");
                textTraining.setText("Gaussian distributions stored.");
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

        // Create a click listener for our button.
        buttonCfgApSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numSSIDs > 1){
                    numSSIDs -= 1;
                    titleCfgApNum.setText(" " + numSSIDs + " ");
                }
            }
        });

        // Create a click listener for our button.
        buttonCfgApAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                numSSIDs += 1;
                titleCfgApNum.setText(" " + numSSIDs + " ");
            }
        });

        // Create a click listener for our button.
        buttonCfgRssLvlSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numRSSLvl > 10){
                    numRSSLvl -= 10;
                    titleCfgRssLvlNum.setText(" " + numRSSLvl + " ");
                }
            }
        });

        // Create a click listener for our button.
        buttonCfgRssLvlAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                numRSSLvl += 10;
                titleCfgRssLvlNum.setText(" " + numRSSLvl + " ");
            }
        });

        // Create a click listener for our button.
        buttonCfgRoomsSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numRooms > 2){
                    numRooms -= 1;
                    titleCfgRoomsNum.setText(" " + numRooms + " ");
                }
            }
        });

        // Create a click listener for our button.
        buttonCfgRoomsAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                numRooms += 1;
                titleCfgRoomsNum.setText(" " + numRooms + " ");
            }
        });

        // Create a click listener for our button.
        buttonCfgScansSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numScans > 10){
                    numScans -= 10;
                    titleCfgScansNum.setText(" " + numScans + " ");
                }
            }
        });

        // Create a click listener for our button.
        buttonCfgScansAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                numScans += 10;
                titleCfgScansNum.setText(" " + numScans + " ");
            }
        });

        // Create a click listener for our button.
        buttonTrainRoomSubs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (trainRoom > 0){
                    trainRoom -= 1;
                    titleTrainRoomNum.setText(" " + (trainRoom + 1) + " ");
                }
            }
        });

        // Create a click listener for our button.
        buttonTrainRoomAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (trainRoom < numRooms - 1) {
                    trainRoom += 1;
                    titleTrainRoomNum.setText(" " + (trainRoom + 1) + " ");
                }
            }
        });

        // Create a click listener for our button.
        buttonTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(testState++){
                    case 0:
                        floorMap3D.setRoomProb(1, 1.0);
                        floorMap3D.setRoomProb(2, 1.0);
                        floorMap3D.setRoomProb(3, 1.0);
                        floorMap3D.setRoomProb(4, 1.0);
                        floorMap3D.setRoomProb(5, 1.0);
                        floorMap3D.setRoomProb(6, 1.0);
                        floorMap3D.setRoomProb(7, 1.0);
                        floorMap3D.setRoomProb(8, 1.0);
                        floorMap3D.setRoomProb(9, 1.0);
                        floorMap3D.setRoomProb(10, 1.0);
                        floorMap3D.setRoomProb(11, 1.0);
                        floorMap3D.setRoomProb(12, 1.0);
                        floorMap3D.setRoomProb(13, 1.0);
                        floorMap3D.setRoomProb(14, 1.0);
                        floorMap3D.setRoomProb(15, 1.0);
                        floorMap3D.setRoomProb(16, 1.0);
                        floorMap3D.setRoomProb(17, 1.0);
                        floorMap3D.setRoomProb(18, 1.0);
                        floorMap3D.setRoomProb(19, 1.0);
                        floorMap3D.setRoomProb(20, 1.0);
                        break;
                    case 1:
                        floorMap3D.setRoomProb(1, 0.6);
                        floorMap3D.setRoomProb(2, 0.7);
                        floorMap3D.setRoomProb(3, 0.7);
                        floorMap3D.setRoomProb(4, 0.8);
                        floorMap3D.setRoomProb(5, 0.8);
                        floorMap3D.setRoomProb(6, 0.9);
                        floorMap3D.setRoomProb(7, 0.9);
                        floorMap3D.setRoomProb(8, 0.9);
                        floorMap3D.setRoomProb(9, 1.0);
                        floorMap3D.setRoomProb(10, 1.0);
                        floorMap3D.setRoomProb(11, 1.0);
                        floorMap3D.setRoomProb(12, 1.0);
                        floorMap3D.setRoomProb(13, 1.0);
                        floorMap3D.setRoomProb(14, 1.0);
                        floorMap3D.setRoomProb(15, 0.9);
                        floorMap3D.setRoomProb(16, 0.9);
                        floorMap3D.setRoomProb(17, 0.7);
                        floorMap3D.setRoomProb(18, 0.7);
                        floorMap3D.setRoomProb(19, 0.6);
                        floorMap3D.setRoomProb(20, 0.6);
                        break;
                    case 2:
                        floorMap3D.setRoomProb(1, 0.3);
                        floorMap3D.setRoomProb(2, 0.5);
                        floorMap3D.setRoomProb(3, 0.5);
                        floorMap3D.setRoomProb(4, 0.6);
                        floorMap3D.setRoomProb(5, 0.6);
                        floorMap3D.setRoomProb(6, 0.7);
                        floorMap3D.setRoomProb(7, 0.7);
                        floorMap3D.setRoomProb(8, 0.7);
                        floorMap3D.setRoomProb(9, 0.9);
                        floorMap3D.setRoomProb(10, 0.9);
                        floorMap3D.setRoomProb(11, 1.0);
                        floorMap3D.setRoomProb(12, 1.0);
                        floorMap3D.setRoomProb(13, 1.0);
                        floorMap3D.setRoomProb(14, 0.7);
                        floorMap3D.setRoomProb(15, 0.5);
                        floorMap3D.setRoomProb(16, 0.5);
                        floorMap3D.setRoomProb(17, 0.3);
                        floorMap3D.setRoomProb(18, 0.3);
                        floorMap3D.setRoomProb(19, 0.2);
                        floorMap3D.setRoomProb(20, 0.2);
                        break;
                    case 3:
                        floorMap3D.setRoomProb(1, 0.1);
                        floorMap3D.setRoomProb(2, 0.2);
                        floorMap3D.setRoomProb(3, 0.3);
                        floorMap3D.setRoomProb(4, 0.3);
                        floorMap3D.setRoomProb(5, 0.4);
                        floorMap3D.setRoomProb(6, 0.3);
                        floorMap3D.setRoomProb(7, 0.4);
                        floorMap3D.setRoomProb(8, 0.3);
                        floorMap3D.setRoomProb(9, 0.5);
                        floorMap3D.setRoomProb(10, 0.6);
                        floorMap3D.setRoomProb(11, 0.7);
                        floorMap3D.setRoomProb(12, 1.0);
                        floorMap3D.setRoomProb(13, 0.9);
                        floorMap3D.setRoomProb(14, 0.5);
                        floorMap3D.setRoomProb(15, 0.3);
                        floorMap3D.setRoomProb(16, 0.2);
                        floorMap3D.setRoomProb(17, 0.2);
                        floorMap3D.setRoomProb(18, 0.1);
                        floorMap3D.setRoomProb(19, 0.0);
                        floorMap3D.setRoomProb(20, 0.0);
                        break;
                    case 4:
                        floorMap3D.setRoomProb(1, 0.0);
                        floorMap3D.setRoomProb(2, 0.0);
                        floorMap3D.setRoomProb(3, 0.0);
                        floorMap3D.setRoomProb(4, 0.1);
                        floorMap3D.setRoomProb(5, 0.2);
                        floorMap3D.setRoomProb(6, 0.1);
                        floorMap3D.setRoomProb(7, 0.1);
                        floorMap3D.setRoomProb(8, 0.1);
                        floorMap3D.setRoomProb(9, 0.3);
                        floorMap3D.setRoomProb(10, 0.4);
                        floorMap3D.setRoomProb(11, 0.4);
                        floorMap3D.setRoomProb(12, 1.0);
                        floorMap3D.setRoomProb(13, 0.3);
                        floorMap3D.setRoomProb(14, 0.3);
                        floorMap3D.setRoomProb(15, 0.1);
                        floorMap3D.setRoomProb(16, 0.0);
                        floorMap3D.setRoomProb(17, 0.0);
                        floorMap3D.setRoomProb(18, 0.0);
                        floorMap3D.setRoomProb(19, 0.0);
                        floorMap3D.setRoomProb(20, 0.0);
                        break;
                    case 5:
                        floorMap3D.setRoomProb(1, 0.0);
                        floorMap3D.setRoomProb(2, 0.0);
                        floorMap3D.setRoomProb(3, 0.0);
                        floorMap3D.setRoomProb(4, 0.0);
                        floorMap3D.setRoomProb(5, 0.0);
                        floorMap3D.setRoomProb(6, 0.0);
                        floorMap3D.setRoomProb(7, 0.0);
                        floorMap3D.setRoomProb(8, 0.0);
                        floorMap3D.setRoomProb(9, 0.0);
                        floorMap3D.setRoomProb(10, 0.0);
                        floorMap3D.setRoomProb(11, 0.0);
                        floorMap3D.setRoomProb(12, 1.0);
                        floorMap3D.setRoomProb(13, 0.0);
                        floorMap3D.setRoomProb(14, 0.0);
                        floorMap3D.setRoomProb(15, 0.0);
                        floorMap3D.setRoomProb(16, 0.0);
                        floorMap3D.setRoomProb(17, 0.0);
                        floorMap3D.setRoomProb(18, 0.0);
                        floorMap3D.setRoomProb(19, 0.0);
                        floorMap3D.setRoomProb(20, 0.0);
                        break;


                }
                if(testState > 5) testState = 0;
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
//                if(LOG_INFO) Log.i(LOG_TAG,"No Write Permission!!");
            }
        }
        if (requestCode == REQUEST_CODE_WIFI_PERMISSION) {
            if (grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
            } else {
                // permission wasn't granted
//                if(LOG_INFO) Log.i(LOG_TAG,"No WiFi Permission!!");
            }
        }
    }

    private void checkWritingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // permission wasn't granted
//                if(LOG_INFO) Log.i(LOG_TAG,"No Write Permission!!");
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_PERMISSION);
            }
        }
    }

    private void checkWifiPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // permission wasn't granted
//                if(LOG_INFO) Log.i(LOG_TAG,"No WiFi Permission!!");
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

