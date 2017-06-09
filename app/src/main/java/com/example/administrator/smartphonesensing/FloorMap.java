package com.example.administrator.smartphonesensing;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;
import android.widget.TextView;

import static android.content.res.Resources.getSystem;

/**
 * Created by Sergio on 6/9/17.
 */

public class FloorMap {
   //private Context context;
    // Layers with the map images
    private Drawable[] layers;
    private LayerDrawable layerDrawable;
    private ImageView theMap;

    public FloorMap(Context context, Activity activity) {
        this.theMap = (ImageView) activity.findViewById(R.id.the3dMap);
        this.layers = new Drawable[21];
        this.layers[0] = ContextCompat.getDrawable(context, R.drawable.base);
        this.layers[1] = ContextCompat.getDrawable(context, R.drawable.c01);
        this.layers[2] = ContextCompat.getDrawable(context, R.drawable.c02);
        this.layers[3] = ContextCompat.getDrawable(context, R.drawable.c03);
        this.layers[4] = ContextCompat.getDrawable(context, R.drawable.c04);
        this.layers[5] = ContextCompat.getDrawable(context, R.drawable.c05);
        this.layers[6] = ContextCompat.getDrawable(context, R.drawable.c06);
        this.layers[7] = ContextCompat.getDrawable(context, R.drawable.c07);
        this.layers[8] = ContextCompat.getDrawable(context, R.drawable.c08);
        this.layers[9] = ContextCompat.getDrawable(context, R.drawable.c09);
        this.layers[10] = ContextCompat.getDrawable(context, R.drawable.c10);
        this.layers[11] = ContextCompat.getDrawable(context, R.drawable.c11);
        this.layers[12] = ContextCompat.getDrawable(context, R.drawable.c12);
        this.layers[13] = ContextCompat.getDrawable(context, R.drawable.c13);
        this.layers[14] = ContextCompat.getDrawable(context, R.drawable.c14);
        this.layers[15] = ContextCompat.getDrawable(context, R.drawable.c15);
        this.layers[16] = ContextCompat.getDrawable(context, R.drawable.c16);
        this.layers[17] = ContextCompat.getDrawable(context, R.drawable.c17);
        this.layers[18] = ContextCompat.getDrawable(context, R.drawable.c18);
        this.layers[19] = ContextCompat.getDrawable(context, R.drawable.c19);
        this.layers[20] = ContextCompat.getDrawable(context, R.drawable.c20);

        this.layerDrawable = new LayerDrawable(layers);

        // "Turn off" all rooms
        for(int i = 1; i < 21; i++) {
            setRoomProb(i, 0.0);
        }

        this.theMap.setImageDrawable(layerDrawable);
    }

    public void setRoomProb(int room, double alpha){
        int newAlpha = (int)(255.0 * alpha);
        this.layerDrawable.getDrawable(room).setAlpha(newAlpha);
        this.theMap.invalidate();
    }
}
