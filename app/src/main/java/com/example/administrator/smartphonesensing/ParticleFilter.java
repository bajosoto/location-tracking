package com.example.administrator.smartphonesensing;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Created by Sergio on 6/22/17.
 */

public class ParticleFilter {

    private final int NUM_COLS = 13;
    private final int NUM_ROWS = 3;
    private int numParticles;
    private int numCells;
    private int numTopParticles;
    private int deadParticles; //TODO: I think it should be WifiScanner who revives particles (add particlefilter to scanner)
    private FloorMap floorMap;
    ProbMassFuncs pmf;
    private Block[][] blockGrid;
    private int[][] cellGridIndex;
    private List<Particle> particles = new Vector();

    public ParticleFilter (int _numParticles, int _numCells, int _numTopParticles, FloorMap _floorMap, ProbMassFuncs _pmf) {
        numParticles = _numParticles;
        numCells = _numCells;
        numTopParticles = _numTopParticles;
        deadParticles = 0;
        floorMap = _floorMap;
        pmf = _pmf;
        blockGrid = new Block[NUM_COLS][NUM_ROWS];
        /* For each cell, (col, row) indexes in grid */
        cellGridIndex = new int[2][numCells];
        initBlocks();
        initGridIndexes();
        // initParticles(); Particles are init by new bayesianin WifiScanner
    }

    private void initBlocks() {
        int maxWidth = floorMap.getMapWidth();
        int maxHeight = floorMap.getMapHeight();

        for (int x = 0; x < NUM_COLS; x++) {
            for (int y = 0; y < NUM_ROWS; y++) {
                int newX1 = x * (maxWidth / NUM_COLS);
                int newX2 = (x + 1) * (maxWidth / NUM_COLS);  // TODO: Can be optimized once it's working
                int newY1;
                int newY2;
                boolean newIsCell = false;
                switch (y) {
                    case 0:
                        newY1 = 0;
                        newY2 = (int) (maxHeight * 6.1 / 14.3);
                        break;
                    case 1:
                        newY1 = (int) (maxHeight * 6.1 / 14.3); // TODO: Can be optimized getting Y2 from first row
                        newY2 = (int) (maxHeight * 8.2 / 14.3);
                        newIsCell = true;   // All blocks in hallway are cells
                        break;
                    case 2:
                        newY1 = (int) (maxHeight * 8.2 / 14.3); // TODO: Same here
                        newY2 = maxHeight;
                        break;
                    default:                // Just to stop the compiler from yelling at me. This is unreachable
                        newY1 = 0;
                        newY2 = 0;
                        break;
                }
                if(!newIsCell) {        // TODO: Can be optimized, but this is more understandable
                    switch (y) {
                        case 0:
                            if (x == 0 || x == 2 || x == 3)
                                newIsCell = true;
                            break;
                        case 2:
                            if (x == 0 || x == 7 || x == 9 || x == 12)
                                newIsCell = true;
                            break;
                    }
                }
                // Create the block
                blockGrid[x][y] = new Block(newX1, newY1, newX2, newY2, newIsCell, x, y);
            }
        }
    }

    // Hardcoded cell positions in grid
    private void initGridIndexes() {
        cellGridIndex[0][0] = 12;
        cellGridIndex[1][0] = 2;
        cellGridIndex[0][1] = 12;
        cellGridIndex[1][1] = 1;
        cellGridIndex[0][2] = 11;
        cellGridIndex[1][2] = 1;
        cellGridIndex[0][3] = 10;
        cellGridIndex[1][3] = 1;
        cellGridIndex[0][4] = 9;
        cellGridIndex[1][4] = 1;
        cellGridIndex[0][5] = 9;
        cellGridIndex[1][5] = 2;
        cellGridIndex[0][6] = 8;
        cellGridIndex[1][6] = 1;
        cellGridIndex[0][7] = 7;
        cellGridIndex[1][7] = 1;
        cellGridIndex[0][8] = 7;
        cellGridIndex[1][8] = 2;
        cellGridIndex[0][9] = 6;
        cellGridIndex[1][9] = 1;
        cellGridIndex[0][10] = 5;
        cellGridIndex[1][10] = 1;
        cellGridIndex[0][11] = 4;
        cellGridIndex[1][11] = 1;
        cellGridIndex[0][12] = 3;
        cellGridIndex[1][12] = 1;
        cellGridIndex[0][13] = 3;
        cellGridIndex[1][13] = 0;
        cellGridIndex[0][14] = 2;
        cellGridIndex[1][14] = 0;
        cellGridIndex[0][15] = 2;
        cellGridIndex[1][15] = 1;
        cellGridIndex[0][16] = 1;
        cellGridIndex[1][16] = 1;
        cellGridIndex[0][17] = 0;
        cellGridIndex[1][17] = 1;
        cellGridIndex[0][18] = 0;
        cellGridIndex[1][18] = 0;
        cellGridIndex[0][19] = 0;
        cellGridIndex[1][19] = 2;

        for (int i = 0; i < numCells; i++) {
            int col = cellGridIndex[0][i];
            int row = cellGridIndex[1][i];
            blockGrid[col][row].setCellNumber(i);
        }
    }

    public Block getBlockByCoord(int x, int y) {
        int maxWidth = floorMap.getMapWidth();
        int maxHeight = floorMap.getMapHeight();
        int col;
        int row;

        // Get column
        col = x * NUM_COLS / maxWidth;
        col = col == 13 ? 12 : col;
        // Get row
        if (y < (int)(maxHeight * 6.1 / 14.3))
            row = 0;
        else if(y < (int)(maxHeight * 8.2 / 14.3))
            row = 1;
        else
            row = 2;    //guaranteed, at least called from updateParticles
        return blockGrid[col][row];
    }

    public int getCellGridX(int cell) {
        return cellGridIndex[0][cell];
    }

    public int getCellGridY(int cell) {
        return cellGridIndex[1][cell];
    }

    public void initParticles() {

        double[] probs = new double[numCells];
        particles.clear();
        // Get all probabilities
        for (int i = 0; i < numCells; i++) {
            probs[i] = pmf.getPxPrePost(i);
        }

        for(int cell = 0; cell < numCells; cell++) {
            // int pPerBlock = numParticles / numCells; //
            int pPerBlock = (int)(probs[cell] * numParticles);
            for (int p = 0; p < pPerBlock; p++) {
                int cellIndexR = getCellGridX(cell);
                int cellIndexC = getCellGridY(cell);
                int cellX1 = blockGrid[cellIndexR][cellIndexC].x1;
                int cellY1 = blockGrid[cellIndexR][cellIndexC].y1;
                int cellX2 = blockGrid[cellIndexR][cellIndexC].x2;
                int cellY2 = blockGrid[cellIndexR][cellIndexC].y2;
                int randX = (int) (Math.random() * (cellX2 - cellX1) + cellX1);
                int randY = (int) (Math.random() * (cellY2 - cellY1) + cellY1);
                particles.add(new Particle(randX, randY, probs[cell]));
            }
        }
        redrawParticles(); // According to Lecture PPT, particles must start after Bayesian
    }

    public void updateParticles(int xOffset, int yOffset) {  // TODO: change lit room based on particle count
        int maxW = floorMap.getMapWidth();
        int maxH = floorMap.getMapHeight();
        Iterator<Particle> i = particles.iterator();
        while (i.hasNext()) {

            // Add noise
            int offset = Math.abs(xOffset) > Math.abs(yOffset) ? xOffset : yOffset;
            final double noiseFactor = 0.5;
            int xNoise = (int)((double)offset * (Math.random() * noiseFactor * 2.0 - noiseFactor));
            int yNoise = (int)((double)offset * (Math.random() * noiseFactor * 2.0 - noiseFactor));

            // Get particle data
            Particle p = i.next();
            int pX = p.getX();
            int pY = p.getY();
            int newX = pX + xOffset + xNoise;
            int newY = pY + yOffset + yNoise;
            boolean wasRemoved = false;

            if((newX > maxW) || (newX < 0) || (newY > maxH) || (newY < 0)){
                deadParticles += 1;
                i.remove();
                wasRemoved = true;
            } else { // Looks like both conditions are the same, but need to be separate to guarantee getBlockByCoord
                Block b = getBlockByCoord(newX, newY);
                // Kill cells landing in non-cell
                if(!b.isCell){
                    deadParticles += 1;
                    i.remove();
                    wasRemoved = true;
                }
            }
            if(!wasRemoved){
                p.setX(newX);
                p.setY(newY);
                p.age();
            }
        }
        reviveParticles();
        // reviveParticles sorted particles, so we can find best candidate room now
         findRoomByParticles();
        redrawParticles();
    }

    public void reviveParticles() {
        // Sort particles
        Collections.sort(particles, new Comparator<Particle>() {
            @Override
            public int compare(Particle lhs, Particle rhs) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                double lLvl = lhs.getWeight();
                double rLvl = rhs.getWeight();
                return lLvl > rLvl ? -1 : (lLvl < rLvl ) ? 1 : 0;
            }
        });
        while (deadParticles > 0) {
            int candidateID = (int)(Math.random() * numTopParticles);
            Particle candidate = particles.get(candidateID);

            // Calculate random offset for variance
            final double scaleFactor = 0.01;
            final double noiseFactor = 0.5;
            double xOffset = (floorMap.getMapWidth() * scaleFactor);
            double yOffset = (floorMap.getMapHeight() * scaleFactor);
            int xNoise = (int)(xOffset * (Math.random() * noiseFactor * 2.0 - noiseFactor));
            int yNoise = (int)(yOffset * (Math.random() * noiseFactor * 2.0 - noiseFactor));

            int newX = candidate.getX() + xNoise;
            int newY = candidate.getY() + yNoise;
            particles.add(new Particle(newX, newY, 0.0));
            deadParticles -= 1;
        }
    }

    // Finds the room with the largest amount of the eldest particles to light it in the map
    public void findRoomByParticles() {
        int[] roomByPartHisto = new int[numCells];
        for(int i = 0; i < numTopParticles; i++){
            Particle p = particles.get(i);

            // Get particle coordinates
            int pX = p.getX();
            int pY = p.getY();

            // Find containing block in grid
            Block b = getBlockByCoord(pX, pY);

            if(b.isCell) {
                roomByPartHisto[b.cell] += 1;
            }
        }

        int maxIndex = 0;
        for(int i = 0; i < numCells; i++) {
            if (roomByPartHisto[i] > roomByPartHisto[maxIndex])
                maxIndex = i;
        }
        floorMap.updateRooms(maxIndex);
    }

    public void redrawParticles() {
        floorMap.clear();
        for(Particle p : particles)
            floorMap.addParticle(p);
        floorMap.redraw();
    }

    /**
     * Particle class
     */
    class Particle {
        private int x;
        private int y;
        private double weight;

        public Particle (int _x, int _y, double _weight) {
            x = _x;
            y = _y;
            weight = _weight;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public void setX(int _x) {
            x = _x;
        }

        public void setY(int _y) {
            y = _y;
        }

        public double getWeight() {
            return weight;
        }

        public void age(){
            weight += 0.01;
        }
    }

    /**
     * Block class
     */
    class Block {
        private int x1;
        private int y1;
        private int x2;
        private int y2;
        private int rowInGrid;
        private int colInGrid;
        // Cell ID (if any)
        private int cell = 0;
        private boolean isCell;

        public Block (int _x1, int _y1, int _x2, int _y2, boolean _isCell, int _colInGrid, int _rowInGrid) {
            x1 = _x1;
            y1 = _y1;
            x2 = _x2;
            y2 = _y2;
            colInGrid = _colInGrid;
            rowInGrid = _rowInGrid;
            isCell = _isCell;
        }

        public void setCellNumber(int _cell) {
            cell = _cell;
        }

        public int getCellNumber() {
            if (isCell)
                return cell;
            else
                return -1;
        }

        // TODO: WE won't need this one once we have a block grid. It might be removable, saving computations by storing as bool
        public boolean isValidCell (int x, int y) {
            //return ((x >= x1) && (x <= x2)) && ((y >= y1) && (y <= y2));
            return isCell;
        }
    }
}
