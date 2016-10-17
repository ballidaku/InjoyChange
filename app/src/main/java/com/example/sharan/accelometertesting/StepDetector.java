/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.example.sharan.accelometertesting;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.example.sharan.accelometertesting.MainScreen.Activities.Pedometer;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Detects steps and notifies all listeners (that implement StepListener).
 *
 * @author Levente Bagi
 * @todo REFACTOR: SensorListener is deprecated
 */
public class StepDetector implements SensorEventListener
{
    private final static String TAG = "StepDetector";
    private float mLimit = 10;
    private float mLastValues[] = new float[3 * 2];
    private float mScale[] = new float[2];
    private float mYOffset;

    private float mLastDirections[] = new float[3 * 2];
    private float mLastExtremes[][] = {new float[3 * 2], new float[3 * 2]};
    private float mLastDiff[] = new float[3 * 2];
    private int mLastMatch = -1;

    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();

    public StepDetector()
    {
        int h = 480; // TODO: remove this constant
        mYOffset = h * 0.5f;
        mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
    }


    long INTERVAL_LIMIT = 60000;
    long lastSleepTime = 0;

    public void setSensitivity(float sensitivity)
    {
        mLimit = sensitivity; // 1.97  2.96  4.44  6.66  10.00  15.00  22.50  33.75  50.62
    }

    public void addStepListener(StepListener sl)
    {
        mStepListeners.add(sl);
    }

    //public void onSensorChanged(int sensor, float[] values) {
    public void onSensorChanged(SensorEvent event)
    {
        Sensor sensor = event.sensor;
        synchronized (this)
        {
           /* if (sensor.getType() == Sensor.TYPE_ORIENTATION)
            {
            }
            else
            {*/
//                int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;
//                if (j == 1)


            if (sensor.getType() == Sensor.TYPE_STEP_COUNTER)
            {
//                    Toast.makeText(Pedometer.context,"Step Detector",Toast.LENGTH_SHORT).show();

                Pedometer.show_Toast("Step Detector");
                for (StepListener stepListener : mStepListeners)
                {
                    stepListener.onStep();
                }
                checkSleepTime();
            }

            else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && Pedometer.isStepCounterAvailable == false)
            {
                float vSum = 0;
                for (int i = 0; i < 3; i++)
                {
//                        final float v = mYOffset + event.values[i] * mScale[j];
                    final float v = mYOffset + event.values[i] * mScale[1];
                    vSum += v;
                }
                int k = 0;
                float v = vSum / 3;

                float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
                if (direction == -mLastDirections[k])
                {
                    // Direction changed
                    int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                    mLastExtremes[extType][k] = mLastValues[k];
                    float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                    if (diff > mLimit)
                    {

                        boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
                        boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
                        boolean isNotContra = (mLastMatch != 1 - extType);

                        if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra)
                        {
                            Log.i(TAG, "step");

                            checkSleepTime();

//                                Log.e("mStepListeners",""+mStepListeners.size());
                            for (StepListener stepListener : mStepListeners)
                            {
                                stepListener.onStep();
                            }
                            Pedometer.show_Toast("Accelerometer");
//                                Toast.makeText(Pedometer.context,"Accelerometer",Toast.LENGTH_SHORT).show();

                            mLastMatch = extType;
                        }
                        else
                        {
                            mLastMatch = -1;
                        }
                    }
                    mLastDiff[k] = diff;
//                        Log.e(TAG, "diff : " + diff);
                }
                mLastDirections[k] = direction;
                mLastValues[k] = v;
//                    Log.e(TAG, "direction : " + direction);
//                    Log.e(TAG, "v : " + v);
            }
        }
//        }
    }


    private void checkSleepTime()
    {

        long stepNow = System.currentTimeMillis();

        if (Pedometer.stepLast != 0)
        {
            long diff = stepNow - Pedometer.stepLast;

            Log.e(TAG, "diff " + diff + " Interval " + INTERVAL_LIMIT);
            if (diff > INTERVAL_LIMIT)
            {

                Log.e(TAG, "User is in sleep mode " + lastSleepTime);

                diff += lastSleepTime;

                lastSleepTime = diff;

                String t = String.format("%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(diff),
                        TimeUnit.MILLISECONDS.toMinutes(diff) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(diff)), // The change is in this line
                        TimeUnit.MILLISECONDS.toSeconds(diff) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(diff)));


                /*for (StepListener stepListener : mStepListeners)
                {
                    stepListener.OnSleepTimeChanged(t);
                }*/
                Pedometer.txtv_sleepTime.setText(t);
            }
        }

        Pedometer.stepLast = stepNow;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // TODO Auto-generated method stub
    }

}