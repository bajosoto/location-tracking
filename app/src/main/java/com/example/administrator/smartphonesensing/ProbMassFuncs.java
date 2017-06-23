package com.example.administrator.smartphonesensing;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import static android.net.wifi.WifiManager.calculateSignalLevel;

/**
 * Created by Sergio on 6/1/17.
 */

public class ProbMassFuncs implements Serializable{
    private Map<String, TableRss> tablesRss;
    private StoredPMF pmf;
    private PerceptionModel pm;
    private int numCells;
    private int numRssLevels;
    LogWriter logPmf = new LogWriter("logPmf.txt");
    LogWriter csvPmf = new LogWriter("pmfcurves.txt");
    private File root, dir;
    private static final String DIR_NAME = "SmartPhoneSensing";
    private Context context;


    public ProbMassFuncs(int numCells, int numRssLevels) {
        this.tablesRss = new HashMap<String, TableRss>();
        this.pm = new PerceptionModel(numCells, numRssLevels);
        this.numCells = numCells;
        this.numRssLevels = numRssLevels;
        this.root = android.os.Environment.getExternalStorageDirectory();
        this.dir = new File(root.getAbsolutePath() + "/" + DIR_NAME);
        //this.loadPMF();
    }

    public void storePMF() {
        // Serialize the pmf
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(this.dir, "/storedPMF.bin"))); //Select where you wish to save the file...
            oos.writeObject(this.pmf); // write the class as an 'object'
            oos.flush(); // flush the stream to insure all of the information was written to 'storedPMF.bin'
            oos.close();// close the stream
            logPmf.writeToFile("File saved successfully. \n", true);
        }
        catch(Exception ex) {
            logPmf.writeToFile("Error writing to file. \n" + ex.toString(), true);
            ex.printStackTrace();
        }
    }

    // Get the post (or prev) probability calculated for a given cell. This accesses the perception model
    // and will be used to update the graphic map from the floor map. Also to update the label from
    // the main activity
    public double getPxPrePost(int cell) {
        return this.pm.getPxPrePost(cell);
    }

    public boolean loadPMF() {
        // Load serialized pmf
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(this.dir, "/storedPMF.bin")));
            Object o = ois.readObject();
            this.pmf = (StoredPMF) o;
            logPmf.writeToFile("File loaded successfully. \n", true);
        }
        catch(Exception ex) {
            logPmf.writeToFile("Error loading file. Generating new pmf... \n" + ex.toString(), true);
            this.pmf = new StoredPMF();
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    // Write PMF contents into a readable .txt log file
    public void logPMF() {

        // Header for csv
        csvPmf.writeToFile(",", false);
        for (int lvl = 0; lvl < this.numRssLevels; lvl++) {
            csvPmf.writeToFile(lvl + ",", true);
        }
        csvPmf.writeToFile("\n", true);

        // Header for log
        logPmf.writeToFile("========================: \n", true);
        logPmf.writeToFile("Raw RSS DATA: \n", true);
        logPmf.writeToFile("========================: \n", true);

        for(Map.Entry<String, TableRss> t : this.tablesRss.entrySet()) {
            logPmf.writeToFile("AP: " + t.getKey(), true);
            logPmf.writeToFile("\n", true);
            for (int cell = 0; cell < t.getValue().numCells; cell++) {
                csvPmf.writeToFile(t.getKey() + " - " + (cell + 1) + ",", true);
                for (int rss = 0; rss < t.getValue().numRssLevels; rss++) {
                    String line = String.valueOf(t.getValue().values[cell][rss]) + ",";
                    logPmf.writeToFile(line, true);
                    csvPmf.writeToFile(line, true);
                }
                logPmf.writeToFile("\n", true);
                csvPmf.writeToFile("\n", true);
            }
            logPmf.writeToFile("\n\n", true);
            csvPmf.writeToFile("\n", true);
        }

        logPmf.writeToFile("========================: \n", true);
        logPmf.writeToFile("Gaussian DATA: \n", true);
        logPmf.writeToFile("========================: \n", true);
        for(Map.Entry<String, TableGaussian> t : this.pmf.tablesGauss.entrySet()) {
            logPmf.writeToFile("AP: " + t.getKey(), true);
            logPmf.writeToFile("\n", true);
            for (int cell = 0; cell < t.getValue().numCells; cell++) {
                String line = String.valueOf(t.getValue().values[cell].getMean()) + "," +
                        String.valueOf(t.getValue().values[cell].getVariance());
                logPmf.writeToFile(line, true);
                logPmf.writeToFile("\n", true);
            }
            logPmf.writeToFile("\n\n", true);
        }
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
            int newRss = calculateSignalLevel(s.level, this.numRssLevels);
            // Add new entry to the right table
            targetTable.addEntry(cell, newRss);
        }
    }

    // Compute the gaussian distribution for all the data once we are done acquiring samples. Storing
    // it this way reduces space in memory and adds granularity to our data for RSS values we didn't
    // capture during data acquisition.
    public void calcGauss(){

//        int entries = tablesRss.size();
//        entries *= numCells;
//        int counter = 0;
        // For each RSS table, corresponding to each AP found during scans
        for (Map.Entry<String, TableRss> e : this.tablesRss.entrySet()){
            TableRss rTable = e.getValue();
            String key = e.getKey();
            TableGaussian gTable = new TableGaussian(this.numCells);
            // For each cell
            for(int i = 0; i < this.numCells; i++) {
                // Calculate gaussian pair from RSS table
                GaussianPair pair = rTable.getGaussian(i);
                // Then set it in the new gaussian table
                gTable.setGaussianPair(i, pair);
//                counter++;
//                textTraining.setText("Calculating Gaussians... (" + counter + " / " + entries + ")");
            }
            // Add the gaussian table to the gaussian tables hashmap
            this.pmf.tablesGauss.put(key, gTable);
        }
        this.logPmf.clearFile();
        this.csvPmf.clearFile();
        this.logPMF();
        this.storePMF();
    }

    // Resets the prev probabilities in order to init iterating anew
    public void resetLocation() {
        pm.resetPriorBelief();
        pm.printNewBelief();
    }

    // Attempt to find the current location. It performs a single measurement iteration with n
    // iterations corresponding to APs considered
    // scanResults is already sorted descending
    // scanResults is already cropped to either numSSIDs, or the max number of SSIDs found if
    // this was less than numSSIDs
    public int findLocation(List<ScanResult> scanResults) {

        // Guessed location
        int loc = 0;
        // Print a new iteration to log
        pm.printNewIter();
        // One iteration per scan result
        for (ScanResult s : scanResults) {
            loc = pm.updateBelief(s, numRssLevels);
            pm.adjustZeroes();
        }

        return loc;
    }

    /**
     * TableGaussian holds the gaussian pairs representing the normal distribution curves for each cell.
     * This object is instantiated for each sensed SSID during training.
     */
    class TableGaussian implements Serializable{
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
                if(variance == 0) {
                    if (mean == measuredRss) {
                        return 1.0;
                    } else {
                        return 0.0;
                    }
                }
                double numerator = Math.exp((-1.0) * (Math.pow(measuredRss - mean, 2.0)) / (2.0 * variance));
                double denominator = Math.sqrt(2.0 * Math.PI * variance);
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
    public class TableRss implements Serializable{
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
                int sum = 0;
                for (int i = 0; i < this.numRssLevels; i++) {
                    mean += ((double) this.values[cell][i]) * i;
                    sum += this.values[cell][i];
                }

                // No data at any RSS level for this SSID found in this cell, so var = mean = 0.
                // This will make the probability zero
                if(sum == 0) {
                    mean = 0.0;
                    variance = 0.0;
                    return new GaussianPair(mean, variance);
                }
                // Otherwise, calculate the mean
                mean /= (double) sum;

                // Calculate variance
                for (int i = 0; i < this.numRssLevels; i++) {
                    double tmp = (double)i - mean;
                    variance += (double)(this.values[cell][i]) * Math.pow(tmp, 2.0);
                }
                // Variance can still be zero, in case all samples were identical
                variance /= (double) sum;

                return new GaussianPair(mean, variance);
            } else {
                // Invalid cell number
                return new GaussianPair(0.0, 0.0);
            }
        }
    }

    /**
     * GaussianPair holds the mean and variance values of a normal distribution
     */
    public class GaussianPair implements Serializable{
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

        public String toString() {
            return "" + mean + " - " + variance;
        }
    }

    /**
     * StoredPMF is the stored trained data.
     */
    public class StoredPMF implements Serializable {
        public Map<String, ProbMassFuncs.TableGaussian> tablesGauss;
        public Vector<String> ap_keys;
        private static final long serialVersionUID = 46543445;

        public StoredPMF() {
            this.tablesGauss = new HashMap<String, ProbMassFuncs.TableGaussian>();
            this.ap_keys = new Vector<String>();
        }
    }

    /**
     * Perception Model uses Bayesian filtering to update the believed current location
     */
    public class PerceptionModel implements Serializable {
        // Current room we believe we are in
        private int current_belief;
        // Probability of being at each room x from previous iteration
        private double p_x_prior[];
        // Probability of reading AP at certain RSS (z) given we are in room x
        private double p_z_x[];
        // Probability of reading AP at certain RSS (z) AND being in room x
        private double p_wifi[];
        // Perception model log
        LogWriter logBayes = new LogWriter("logBayes.txt");
        // Number of cells
        private int numCells;
        // Number of RSS level values
        private int numRssLevels;

        // Constructor. Starts at believing room 0, equal probability for all rooms
        public PerceptionModel(int numCells, int numRssLevels) {
            this.numCells = numCells;
            this.numRssLevels = numRssLevels;
            this.p_x_prior = new double[numCells];
            this.p_z_x = new double[numCells];
            this.p_wifi = new double[numCells];
            this.resetPriorBelief();
            this.logBayes.clearFile();
        }

        // Reset the current belief to room 0 and all prior to equal probability
        public void resetPriorBelief() {
            for (int i = 0; i < this.numCells; i++) {
                this.p_x_prior[i] = 1.0 / this.numCells;
                this.current_belief = 0;
            }
        }

        public double getPxPrePost(int cell) {
            return this.p_x_prior[cell];
        }

        public void printNewIter() {
            logBayes.writeToFile("============================================================\n", true);
            logBayes.writeToFile("New iteration\n", true);
            logBayes.writeToFile("============================================================\n", true);
        }

        public void printNewBelief() {
            logBayes.writeToFile("============================================================\n", true);
            logBayes.writeToFile("New belief\n", true);
            logBayes.writeToFile("============================================================\n", true);
        }

        // Update current belief using bayesian filtering
        public int updateBelief(ScanResult s, int numRssLevels) {
            logBayes.writeToFile("------------------------------------------------------------\n", true);
            logBayes.writeToFile("Generating new belief. SSID: " + s.BSSID + "  |  RSS: "
                    + calculateSignalLevel(s.level, numRssLevels) + "\n", true);
            // Check if a table exists for the current AP scanned
            if (pmf.tablesGauss.containsKey(s.BSSID)) {
                // Absolute probability of reading this wifi at this signal level (used for normalization)
                double tot_p_wifi = 0;

                // Print prior belief
                printArray(p_x_prior, "Prior\t\t\t", this.numCells);

                for (int i = 0; i < this.numCells; i++) {
                    // Update p(z|x)
                    TableGaussian t = pmf.tablesGauss.get(s.BSSID);
                    this.p_z_x[i] = t.getProb(i, calculateSignalLevel(s.level, numRssLevels));

                    // Update p_wifi = p(z|x) * p(x)
                    this.p_wifi[i] = this.p_z_x[i] * this.p_x_prior[i];

                    // Calculate total p_wifi to normalize
                    tot_p_wifi += this.p_wifi[i];
                }

                for(int i = 0; i < this.numCells; i++) {
                    // Calculate posterior p(x) and store it as prior for next iteration
                    if(tot_p_wifi != 0) {
                        this.p_x_prior[i] = this.p_wifi[i] / tot_p_wifi;
                    }
                }

                printArray(this.p_z_x, "p(z|x)\t\t", this.numCells);
                printArray(this.p_wifi, "p(z|x)*p(x)", this.numCells);
                if(tot_p_wifi == 0){
                    logBayes.writeToFile("\t~This SSID was ignored since p(z|x) * p(x) was 0~\n", true);
                }
                printArray(this.p_x_prior, "Post\t\t\t", this.numCells);

                // Find highest probability
                double max = 0;
                for(int i = 0; i < this.numCells; i++) {
                    if (this.p_x_prior[i] > max) {
                        max = this.p_x_prior[i];
                        this.current_belief = i;
                    }
                }

                logBayes.writeToFile("\nHighest prob: " + this.current_belief + "\n\n", true);

            } else {
                logBayes.writeToFile(s.BSSID + " was not found in the training data. Skipping...\n", true);
            }
            return this.current_belief;
        }


        // This function sets cells whose probability was 0% to 1%, in order to avoid false negatives
        public void adjustZeroes() {
            int counter = 0;
            for (int i = 0; i < numCells; i++) {
                if (this.p_x_prior[i] == 0) {
                    counter++;
                }
            }
            for (int i = 0; i < numCells; i++) {
                if (p_x_prior[i] == 0) {
                    p_x_prior[i] = 0.02;
                } else {
                    p_x_prior[i] = p_x_prior[i] * (1 - counter * 0.02);
                }
            }
        }

        public void printArray(double[] a, String title, int numCells) {
            String line = title + "\t";
            for(int i = 0; i < numCells; i++) {
                line += String.format("%.6f", a[i]) + "\t";
            }
            line += "\n";
            logBayes.writeToFile(line, true);
        }
    }
}
