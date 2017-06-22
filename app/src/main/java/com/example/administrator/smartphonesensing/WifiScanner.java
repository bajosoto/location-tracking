package com.example.administrator.smartphonesensing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import static android.net.wifi.WifiManager.calculateSignalLevel;

/**
 * Created by Sergio on 6/21/17.
 */

public class WifiScanner {

    public enum WifiScanAction {
        TRAINING,
        LOCATION_KNN,
        LOCATION_BAYES_NEW,
        LOCATION_BAYES_ITER,
        NONE
    }

    private int numSSIDs;
    private int numRSSLvl;
    private int numScans;
    private int numRooms;

    /* The room we're currently training */
    private int trainRoom = 0;
    // Stores the number of samples acquired so far for a given room
    private int sampleCount = 0;

    private WifiManager wifiManager;
    private List<ScanResult> scanResults;
    private WifiScanAction scanAction;
    public ProbMassFuncs pmf;
    private FloorMap floorMap3D;
    private Vector<RSSPoint> refPoints = new Vector();
    private TextView textTraining;
    private TextView textKNN;
    private TextView textBayes;
    LogWriter logRss;

    Context context;

    public WifiScanner(Context _context, WifiManager _wifiManager, int _numSSIDs, int _numRSSLvl, int _numScans, int _numRooms,
                       ProbMassFuncs _pmf, FloorMap _floorMap3D, TextView _textTraining, TextView _textKNN,
                       TextView _textBayes) {

        wifiManager = _wifiManager;
        numSSIDs = _numSSIDs;
        numRSSLvl = _numRSSLvl;
        numScans = _numScans;
        pmf = _pmf;
        floorMap3D = _floorMap3D;
        context = _context;
        scanAction = WifiScanAction.NONE;
        textTraining = _textTraining;
        textKNN = _textKNN;
        textBayes = _textBayes;
        numRooms = _numRooms;
        trainRoom = 0;
        logRss = new LogWriter("logRss.txt");
        logRss.clearFile();
    }

    public void init() {
        context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                scanResults = wifiManager.getScanResults();
                int foundAPs = scanResults.size();
                String line = "";

                Collections.sort(scanResults, new Comparator<ScanResult>() {
                    @Override
                    public int compare(ScanResult lhs, ScanResult rhs) {
                        // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                        return lhs.level > rhs.level ? -1 : (lhs.level < rhs.level ) ? 1 : 0;
                    }
                });

                // Remove extra scan results with weaker RSS. We only need numSSIDs amount of them
                scanResults = scanResults.subList(0, foundAPs >= numSSIDs ? numSSIDs : foundAPs);

                RSSPoint point = new RSSPoint(Integer.toString(trainRoom));

                for (ScanResult s : scanResults) {
                    line += "Room " + (trainRoom + 1) + "\t" + s.BSSID + "\t" + calculateSignalLevel(s.level, numRSSLvl) + "\n";
                    point.addAP(s.BSSID, s.level);
                }
                line += "\n";

                switch (scanAction) {
                    case TRAINING:
                        // PMF
                        pmf.addScanResults(scanResults, trainRoom);
                        // KNN
                        refPoints.addElement(point);
                        textTraining.setText("Acquired sample (" + (sampleCount + 1) + " / " + numScans + ")");
                        // Check if we still have more scans in this room to do
                        if (sampleCount < numScans) {
                            sampleCount++;
                            wifiManager.startScan();
                        } else {
                            textTraining.setText("Acquired " + (sampleCount) + " samples from room " + (trainRoom + 1));
                            sampleCount = 0;
                            scanAction = WifiScanAction.NONE;
                        }
                        break;

                    case LOCATION_KNN:
                        scanAction = WifiScanAction.NONE;
                        // KNN
                        textKNN.setText("You are in " + KNN.knn(point, refPoints));
                        break;

                    case LOCATION_BAYES_NEW:
                        pmf.resetLocation();        // Intentional missing break; statement

                    case LOCATION_BAYES_ITER:
                        scanAction = WifiScanAction.NONE;
                        int estimatedLocationCell = pmf.findLocation(scanResults);
                        String estimatedProb = String.format("%.2f", pmf.getPxPrePost(estimatedLocationCell) * 100);
                        textBayes.setText("I'm " + estimatedProb + "% sure you are in room " + (estimatedLocationCell + 1));
                        floorMap3D.updateRooms(estimatedLocationCell);
                        break;

                    case NONE:
                        break;
                }

                logRss.writeToFile(line, true);

            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public void startScan(WifiScanAction action) {
        scanAction = action;
        wifiManager.startScan();
    }

    public int incTrainRoom() {
        if (trainRoom < numRooms - 1)
            trainRoom += 1;
        return trainRoom;
    }

    public int decTrainRoom() {
        if (trainRoom > 0)
            trainRoom -= 1;
        return trainRoom;
    }

    // TODO: create Train Room button actions inc dec
}
