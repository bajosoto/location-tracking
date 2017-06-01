package com.example.administrator.smartphonesensing;

import android.net.wifi.ScanResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import static android.net.wifi.WifiManager.calculateSignalLevel;

/**
 * Created by Sergio on 6/1/17.
 */

public class ProbMassFuncs {
    private Map<String, TableRss> tablesRss;
    private StoredPMF pmf;
    private int numCells;
    private int numRssLevels;

    public ProbMassFuncs(int numCells, int numRssLevels) {
        this.tablesRss = new HashMap<String, TableRss>();
        this.pmf = new StoredPMF();
        this.numCells = numCells;
        this.numRssLevels = numRssLevels;
    }

    public void storePMF() {
        // Serialize the pmf
    }

    public void loadPMF() {
        // Load serialized pmf
    }

    // Write PMF contents into a readable .txt log file
    public void logPMF(){

    }

    // Store the data acquired during the current data training scan
    public void addScanResults(List<ScanResult> scanResults, int cell) {
        // For every AP present in the scan
        for (ScanResult s : scanResults) {
            TableRss targetTable;
            if (this.tablesRss.containsKey(s.BSSID)){
                // Not the first time we scan this SSID. There is a table already.
                targetTable = tablesRss.get(s.BSSID);
            } else {
                // First time this SSID is found. Create a new table.
                targetTable = new TableRss(this.numCells, this.numRssLevels);
                // Then put it in the tables hash map
                tablesRss.put(s.BSSID, targetTable);
            }
            // Get a RSS in a value from range 0...255
            int newRss = calculateSignalLevel(s.level, 255);
            // Add new entry to the right table
            targetTable.addEntry(cell, newRss);
        }
    }

    // Compute the gaussian distribution for all the data once we are done acquiring samples. Storing
    // it this way reduces space in memory and adds granularity to our data for RSS values we didn't
    // capture during data acquisition.
    public void calcGauss(){
        // For each RSS table, corresponding to each AP found during scans
        for (Map.Entry<String, TableRss> e : this.tablesRss.entrySet()){
            TableRss rTable = e.getValue();
            String key = e.getKey();
            TableGaussian gTable = new TableGaussian(this.numCells);
            // For each cell
            for(int i = 0; i < numCells; i++) {
                // Calculate gaussian pair from RSS table
                GaussianPair pair = rTable.getGaussian(i);
                // Then set it in the new gaussian table
                gTable.setGaussianPair(i, pair);
            }
            // Add the gaussian table to the gaussian tables hashmap
            this.pmf.tablesGauss.put(key, gTable);
        }
    }

    /**
     * StoredPMF is the stored trained data.
     */
    private class StoredPMF {
        private Map<String, TableGaussian> tablesGauss;
        private Vector<String> ap_keys;

        public StoredPMF() {
            this.tablesGauss = new HashMap<String, TableGaussian>();
            this.ap_keys = new Vector<String>();
        }
    }

    /**
     * TableGaussian holds the gaussian pairs representing the normal distribution curves for each cell.
     * This object is instantiated for each sensed SSID during training.
     */
    class TableGaussian {
        // Gaussian pairs for each cell
        private GaussianPair values[];
        // Number of cells
        private int numCells;

        // Constructor
        public TableGaussian(int numCells){
            this.values = new GaussianPair[numCells];
            this.numCells = numCells;
        }

        // Increment the histogram level for the indicated cell and rss level
        public void setGaussianPair(int cell, GaussianPair pair){
            if(cell < this.numCells) {
                if (pair != null) {
                    this.values[cell] = pair;
                } else {
                    // Invalid Gaussian pair
                }
            } else {
                // Invalid cell number
            }
        }

        // Calculate the probability density
        public double getProb(int cell, int measuredRss){
            if(cell < this.numCells) {
                double mean = this.values[cell].getMean();
                double variance = this.values[cell].getVariance();
                double numerator = Math.exp((-1.0) * (Math.pow(measuredRss - mean, 2)) / (2 * variance));
                double denominator = Math.sqrt(2 * Math.PI * variance);
                return (numerator) / (denominator);
            } else {
                // Invalid cell number
                return 0.0;
            }
        }
    }

    /**
     * TableRss holds the histogram with the frequencies of RSS levels measured in each cell. One of
     * these table objects is instantiated for each SSID sensed by the application during training.
     */
    public class TableRss {
        // Histogram of cell vs rss frequencies
        private int values[][];
        // Number of cells
        private int numCells;
        // Number of RSS level values
        private int numRssLevels;

        // Constructor
        public TableRss(int numCells, int numRssLevels){
            this.values = new int [numCells][numRssLevels];
            this.numCells = numCells;
            this.numRssLevels = numRssLevels;
        }

        // Increment the histogram level for the indicated cell and rss level
        public void addEntry(int cell, int rss){
            if(cell < this.numCells) {
                if (rss < this.numRssLevels) {
                    this.values[cell][rss] += 1;
                } else {
                    // This shouldn't be possible if using calculateSignalLevel() to convert levels
                }
            } else {
                // Invalid cell number
            }
        }

        // Calculate the gaussian pair for a certain cell
        public GaussianPair getGaussian(int cell){
            double mean = 0;
            double variance = 0;

            if(cell < this.numCells) {
                // Calculate mean
                for (int i = 0; i < numCells; i++) {
                    mean += (double) this.values[cell][i];
                }
                mean /= (double) this.numCells;

                // Calculate variance
                for (int i = 0; i < numCells; i++) {
                    double tmp = (double)(this.values[cell][i]) - mean;
                    variance += Math.pow(tmp, 2);
                }
                variance /= (double) this.numCells;

                return new GaussianPair(mean, variance);
            } else {
                // Invalid cell number
                return new GaussianPair(0, 1);
            }
        }
    }

    /**
     * GaussianPair holds the mean and variance values of a normal distribution
     */
    public class GaussianPair {
        private double mean;
        private double variance;

        public GaussianPair(double mean, double variance){
            this.mean = mean;
            this.variance = variance;
        }

        public double getMean(){
            return this.mean;
        }

        public double getVariance(){
            return this.variance;
        }
    }
}
