package com.example.administrator.smartphonesensing;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;
import android.view.View;
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
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint;

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

        // Initialize layers to first rooms  TODO: I don't think we need drawables anymore, just ID
        this.layers = new Drawable[this.numRoomsLit];
        for (int i = 0; i < this.numRoomsLit; i++) {
            this.layers[i] = ContextCompat.getDrawable(this.context, layerID[i]);
        }

        clear();
        redraw();


        /** TEEEEST
         *
         */
        //final Bitmap bitmap = ((BitmapDrawable)theMap.getDrawable()).getBitmap().copy(Bitmap.Config.ARGB_8888, true);
        //final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), layerID[3]).copy(Bitmap.Config.ARGB_8888, true);  // TODO This is how you fucking change the image! :D :D :D
//        bitmap = BitmapFactory.decodeResource(context.getResources(), layerID[0]).copy(Bitmap.Config.ARGB_8888, true);
//                int touchX = 50;
//                int touchY = 50;
//                Canvas canvas = new Canvas(bitmap);
//                Paint paint = new Paint();
//                paint.setColor(Color.RED);
                //canvas.drawRect(0,0,canvas.getWidth(),canvas.getHeight(),paint);
        //canvas.drawCircle(touchX, touchY, 20, paint);    // for circle dot
                //canvas.drawPoint(touchX, touchY, paint);  // for single point

        /**
         * ENd TEST
         */

    }

    public void clear() {
        bitmap = BitmapFactory.decodeResource(context.getResources(), layerID[0]).copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.RED);
        //canvas.drawCircle(getMapWidth()/2, getMapHeigth()/2, 20, paint);
    }

    public void addParticle(ParticleFilter.Particle particle) {
        int x = particle.getX();
        int y = particle.getY();

        canvas.drawCircle(x, y, 2, paint);  // TODO: Maybe weigth can affect color or diameter of dot
        // TODO: Test. Delete me
        //canvas.drawCircle(getMapWidth()/2, getMapHeigth()/2, 2, paint);
    }

    public void redraw() {
        theMap.setImageBitmap(bitmap);
        theMap.invalidate();
    }

    public int getMapHeigth() {
        return canvas.getHeight();
    }

    public int getMapWidth() {
        return canvas.getWidth();
    }

    // TODO: This function will only update the lit room in the future
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
