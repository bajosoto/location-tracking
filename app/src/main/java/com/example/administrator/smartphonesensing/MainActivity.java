package com.example.administrator.smartphonesensing;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.support.v4.content.ContextCompat;
import android.Manifest;

import android.widget.Toast;

import static com.example.administrator.smartphonesensing.LogWriter.isExternalStorageWritable;

/**
 * Smart Phone Sensing Data Acquisition Code
 */
public class MainActivity extends Activity {

    /* The number of access points we are taking into consideration */
    private static int numSSIDs = 3;
    /* The number of RSS levels (e.g. 0..255) we are taking into consideration */
    private static int numRSSLvl = 100;
    /* The number of cells we are taking into consideration */
    private static int numRooms = 20;
    /* The number of samples per room we will be taking */
    private static int numScans = 60;
    /* The number of samples we take to detect movement */
    private static final int numACCSamples = 80;
    /* The number of particles we use for the particle system */
    private static final int numParticles = 1000;
    /* The number of particles selected for birthing */
    private static final int numTopParticles = 200;


    ProbMassFuncs pmf;
    FloorMap floorMap3D;
    Sensors sensors;
    Compass compass;
    Movement movement;
    WifiScanner wifiScanner;
    ParticleFilter particleFilter;
    StepCounter stepCounter;

    private static final int REQUEST_CODE_WRITE_PERMISSION = 0;
    private static final int REQUEST_CODE_WIFI_PERMISSION = 0;

    private WifiManager wifiManager;
    private WifiInfo wifiInfo;

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

        // Init the textViews
        initTextViews();

        // Init the buttons
        initButtons();

        // Init PMF
        pmf = new ProbMassFuncs(numRooms, numRSSLvl);
        if (pmf.loadPMF())
            Toast.makeText(MainActivity.this, "Loaded PMF", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(MainActivity.this, "No valid PMF found, created new", Toast.LENGTH_SHORT).show();

        // Init map
        floorMap3D = new FloorMap(this, this); //, numRoomsLit);

        // Init the particle filter
        particleFilter = new ParticleFilter(numParticles, numRooms, numTopParticles, floorMap3D, pmf);

        // Init sensors TODO: Create Accelerometer class and move all trash code there
        compass = new Compass(textCompass, titleCfgCompassNum);
        movement = new Movement(currentX, currentY, currentZ, textAcc, numACCSamples);
        stepCounter = new StepCounter(particleFilter, compass, floorMap3D);
        sensors = new Sensors(this, compass, movement); //TODO: Probably add stepCounter here when it's implemented
        sensors.start();



        // Init the wifi manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanner = new WifiScanner(this, wifiManager, particleFilter, numSSIDs, numRSSLvl, numScans, numRooms, pmf,
                floorMap3D, textTraining, textKNN, textBayes);
        wifiScanner.init();
    }


    protected void onResume() {
        super.onResume();
        sensors.start();
    }


    protected void onPause() {
        super.onPause();
        sensors.stop();
    }

    public void initTextViews() {
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
    }

    public void initButtons() {

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
                movement.setAccelAction(Movement.AccelScanAction.TRAIN_WALK);
                textAcc.setText("Start walking...");
            }
        });

        // Create a click listener for our button.
        buttonStand.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                movement.setAccelAction(Movement.AccelScanAction.TRAIN_STAND);
                textAcc.setText("Stand still...");
            }
        });

        // Create a click listener for our button.
        buttonWalkOrStand.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                movement.setAccelAction(Movement.AccelScanAction.DETECT_WALK);
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
                //floorMap3D.updateRooms(pmf, numRooms);
                stepCounter.incSteps(1);
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
}

