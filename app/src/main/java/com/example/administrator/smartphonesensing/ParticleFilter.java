package com.example.administrator.smartphonesensing;

import java.util.Vector;

/**
 * Created by Sergio on 6/22/17.
 */

public class ParticleFilter {

    private final int NUM_COLS = 13;
    private final int NUM_ROWS = 3;
    private int numParticles;
    private int numCells;
    private FloorMap floorMap;
    private Block[][] blockGrid;
    private int[][] cellGridIndex;
    private Vector<Particle> particles = new Vector();

    public ParticleFilter (int _numParticles, int _numCells, FloorMap _floorMap) {
        numParticles = _numParticles;
        numCells = _numCells;
        floorMap = _floorMap;
        blockGrid = new Block[NUM_COLS][NUM_ROWS];
        /* For each cell, (col, row) indexes in grid */
        cellGridIndex = new int[2][numCells];
        initBlocks();
        initGridIndexes();
        initParticles();
    }

    private void initBlocks() {
        int maxWidth = floorMap.getMapWidth();
        int maxHeight = floorMap.getMapHeigth();

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
                blockGrid[x][y] = new Block(newX1, newY1, newX2, newY2, newIsCell);
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
    }

    public int getCellGridX(int cell) {
        return cellGridIndex[0][cell];
    }

    public int getCellGridY(int cell) {
        return cellGridIndex[1][cell];
    }

    public void initParticles() {
        for(int cell = 0; cell < numCells; cell++) {
            int pPerBlock = numParticles / numCells; // TODO: Ensure somehow that numCells is multiple of numParticles
            for (int p = 0; p < pPerBlock; p++) {
                int cellIndexR = getCellGridX(cell);
                int cellIndexC = getCellGridY(cell);
                int cellX1 = blockGrid[cellIndexR][cellIndexC].x1;
                int cellY1 = blockGrid[cellIndexR][cellIndexC].y1;
                int cellX2 = blockGrid[cellIndexR][cellIndexC].x2;
                int cellY2 = blockGrid[cellIndexR][cellIndexC].y2;
                int randX = (int) (Math.random() * (cellX2 - cellX1) + cellX1);
                int randY = (int) (Math.random() * (cellY2 - cellY1) + cellY1);
                particles.addElement(new Particle(randX, randY, 1)); // TODO: How to assign initial weight?
            }
        }
        redrawParticles();
    }

    public void updateParticles() {
        // TODO
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

        public double getWeigth() {
            return weight;
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
        private boolean isCell;

        public Block (int _x1, int _y1, int _x2, int _y2, boolean _isCell) {
            x1 = _x1;
            y1 = _y1;
            x2 = _x2;
            y2 = _y2;
            isCell = _isCell;
        }

        // TODO: WE won't need this one once we have a block grid. It might be removable, saving computations by storing as bool
        public boolean isValidCell (int x, int y) {
            return ((x >= x1) && (x <= x2)) && ((y >= y1) && (y <= y2));
        }
    }
}
