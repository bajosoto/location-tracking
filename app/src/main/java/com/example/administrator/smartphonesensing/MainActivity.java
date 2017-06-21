package com.example.administrator.smartphonesensing;

import android.app.Activity;
import android.content.pm.PackageManager;
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
    private static int numRooms = 20;
    /* The number of samples per room we will be taking */
    private static int numScans = 60;
    /* The number of rooms that can be lit at a time + 1 (base) */
    private static int numRoomsLit = 5;

    private static final int numACCSamples = 80;
    private static int currAccSample = 0;
    private static String accReqSender = "";
    private static Vector<ACCPoint> accPoints = new Vector();
    private List<Float> xResults = new Vector();
    private List<Float> yResults = new Vector();
    private List<Float> zResults = new Vector();

    LogWriter logAcc = new LogWriter("logAcc.txt");

    ProbMassFuncs pmf;
    FloorMap floorMap3D;
    Sensors sensors;
    Compass compass;
    WifiScanner wifiScanner;

    private static final int REQUEST_CODE_WRITE_PERMISSION = 0;
    private static final int REQUEST_CODE_WIFI_PERMISSION = 0;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private WifiManager wifiManager;
    private WifiInfo wifiInfo;
    private float aX = 0;
    private float aY = 0;
    private float aZ = 0;

    TextView currentX,
            currentY,
            currentZ,
            titleAcc,
            textKNN,
            textAcc,
            textBayes,
            titleCfgApNum,
            titleCfgRssLvlNum,
            titleCfgRoomsNum,
            titleCfgScansNum,
            titleTrainRoomNum,
            textTraining,
            textCompass,
            titleCfgCompassNum;

    Button buttonRssi,
            buttonLocation,
            buttonWalk,
            buttonStand,
            buttonWalkOrStand,
            buttonBayesIterate,
            buttonBayesNew,
            buttonBayesCompile,
            buttonTest,
            buttonCfgApSubst,
            buttonCfgApAdd,
            buttonCfgRssLvlSubst,
            buttonCfgRssLvlAdd,
            buttonCfgRoomsSubst,
            buttonCfgRoomsAdd,
            buttonCfgScansSubst,
            buttonCfgScansAdd,
            buttonTrainRoomSubs,
            buttonTrainRoomAdd,
            buttonCfgCompassSubst,
            buttonCfgCompassAdd;

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
        textKNN = (TextView) findViewById(R.id.textKNN);
        textAcc = (TextView) findViewById(R.id.textAcc);
        textBayes = (TextView) findViewById(R.id.textBAYES);
        titleCfgApNum = (TextView) findViewById(R.id.titleCfgApNum);
        titleCfgRssLvlNum = (TextView) findViewById(R.id.titleCfgRssLvlNum);
        titleCfgRoomsNum = (TextView) findViewById(R.id.titleCfgRoomsNum);
        titleCfgScansNum = (TextView) findViewById(R.id.titleCfgScansNum);
        titleTrainRoomNum = (TextView) findViewById(R.id.titleTrainRoomNum);
        textTraining = (TextView) findViewById(R.id.textTraining);
        textCompass = (TextView) findViewById(R.id.textCompass);
        titleCfgCompassNum = (TextView) findViewById(R.id.titleCfgCompassNum);

        // Set initial text for text views
        titleCfgApNum.setText(" " + numSSIDs + " ");
        titleCfgRssLvlNum.setText(" " + numRSSLvl + " ");
        titleCfgRoomsNum.setText(" " + numRooms + " ");
        titleCfgScansNum.setText(" " + numScans + " ");
        titleTrainRoomNum.setText(" 1 ");       // Safe. trainRoom is init to 0 in WifiScanner

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
        buttonCfgCompassSubst = (Button) findViewById(R.id.buttonCfgCompassSubst);
        buttonCfgCompassAdd = (Button) findViewById(R.id.buttonCfgCompassAdd);

        logAcc.clearFile();

        // Init PMF
        pmf = new ProbMassFuncs(numRooms, numRSSLvl);
        if (pmf.loadPMF())
            Toast.makeText(MainActivity.this, "Loaded PMF", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.this, "No valid PMF found, created new", Toast.LENGTH_SHORT).show();

        // Init map
        floorMap3D = new FloorMap(this, this, numRoomsLit);

        // Init sensors TODO: Create Accelerometer class and move all trash code there
        compass = new Compass(textCompass, titleCfgCompassNum);
        sensors = new Sensors(this, compass);
        sensors.start();

        // Init the wifi manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanner = new WifiScanner(this, wifiManager, numSSIDs, numRSSLvl, numScans, numRooms, pmf,
                floorMap3D, textTraining, textKNN, textBayes);
        wifiScanner.init();

        initButtons();
    }

    // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensors.start();
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensors.stop();
    }

    public void initButtons() {
        // Create a click listener for our button.
        buttonRssi.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiScanner.startScan(WifiScanner.WifiScanAction.TRAINING);
            }
        });

        // Create a click listener for our button.
        buttonLocation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // KNN
                textKNN.setText("Finding your location...");
                wifiScanner.startScan(WifiScanner.WifiScanAction.LOCATION_KNN);
            }
        });

        buttonBayesNew.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                textBayes.setText("Starting over. Finding your location...");
                wifiScanner.startScan(WifiScanner.WifiScanAction.LOCATION_BAYES_NEW);
            }
        });

        // Create a click listener for our button.
        buttonBayesIterate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                textBayes.setText("Updating your location...");
                wifiScanner.startScan(WifiScanner.WifiScanAction.LOCATION_BAYES_ITER);
            }
        });

        buttonBayesCompile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // Calculate gaussian curves for all
                textTraining.setText("Calculating Gaussian distributions...");
                pmf.calcGauss();
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
                int room = wifiScanner.decTrainRoom();
                titleTrainRoomNum.setText(" " + (room + 1) + " ");
            }
        });

        // Create a click listener for our button.
        buttonTrainRoomAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int room = wifiScanner.incTrainRoom();
                titleTrainRoomNum.setText(" " + (room + 1) + " ");
            }
        });

        // Create a click listener for our button.
        buttonTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                floorMap3D.updateRooms(pmf, numRooms);
            }
        });

        buttonCfgCompassAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                compass.incCalibration();
            }
        });

        buttonCfgCompassSubst.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                compass.decCalibration();
            }
        });
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        if (requestCode == REQUEST_CODE_WRITE_PERMISSION) {
//            if (grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // permission was granted
//            } else {
//                // permission wasn't granted
////                if(LOG_INFO) Log.i(LOG_TAG,"No Write Permission!!");
//            }
//        }
//        if (requestCode == REQUEST_CODE_WIFI_PERMISSION) {
//            if (grantResults.length >= 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // permission was granted
//            } else {
//                // permission wasn't granted
////                if(LOG_INFO) Log.i(LOG_TAG,"No WiFi Permission!!");
//            }
//        }
//    }

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
                    textAcc.setText("You are " + KNN.knn(newAccPoint, accPoints));
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

