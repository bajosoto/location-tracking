package com.example.administrator.smartphonesensing;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;

import static android.content.res.Resources.getSystem;

/**
 * Created by Sergio on 6/9/17.
 */

public class FloorMap {
   //private Context context;
    // Layers with the map images
    private Drawable[] layers;
    // Layers currently displayed
    private int[] layerID;
    private LayerDrawable layerDrawable;
    private ImageView theMap;
    private int numRoomsLit;
    private Context context;

    public FloorMap(Context context, Activity activity, int numRoomsLit) {
        this.context = context;
        this.numRoomsLit = numRoomsLit;
        this.theMap = (ImageView) activity.findViewById(R.id.the3dMap);

        // Save IDs of all drawables
        this.layerID = new int[21];
        this.layerID[0] = this.context.getResources().getIdentifier("base", "drawable", context.getPackageName());
        for (int i = 1; i < 21; i++) {
            this.layerID[i] = this.context.getResources().getIdentifier("c" + i, "drawable", context.getPackageName());
        }

        // Initialize layers to first rooms
        this.layers = new Drawable[this.numRoomsLit];
        for (int i = 0; i < this.numRoomsLit; i++) {
            this.layers[i] = ContextCompat.getDrawable(this.context, layerID[i]);
        }

        this.layerDrawable = new LayerDrawable(layers);

        // "Turn off" all rooms
//        for(int i = 1; i < this.numRoomsLit; i++) {
//            setRoomProb(i, 0.0);
//        }

        this.theMap.setImageDrawable(layerDrawable);
    }

    public void updateRooms(ProbMassFuncs pmf, int numRooms){
        double[] probs = new double[numRooms];
        int[] newAlpha = new int[numRoomsLit];
        newAlpha[0] = 255;

        // get all probabilities
        for (int i = 0; i < numRooms; i++) {
            probs[i] = pmf.getPxPrePost(i);
        }

        // find largest numRoomsLit rooms
        for(int candidate = 1; candidate < numRoomsLit; candidate++) {
            int maxRoom = 0;
            double maxRoomProb = 0.0;
            for (int room = 0; room < numRooms; room++) {
                if (probs[room] > maxRoomProb) {
                    maxRoom = room;
                    maxRoomProb = probs[room];
                }
            }
            probs[maxRoom] = 0.0;

            //int index = numRooms - 1 - i;
            newAlpha[candidate] = (int)(255.0 * maxRoomProb);
            this.layers[candidate] = ContextCompat.getDrawable(this.context, layerID[maxRoom]);
        }

        this.layerDrawable = new LayerDrawable(layers);
        this.theMap.setImageDrawable(layerDrawable);

        for (int candidate = 0; candidate < numRoomsLit; candidate++) {
            this.layerDrawable.getDrawable(candidate).setAlpha(newAlpha[candidate]);
        }


        this.theMap.invalidate();
    }
}
