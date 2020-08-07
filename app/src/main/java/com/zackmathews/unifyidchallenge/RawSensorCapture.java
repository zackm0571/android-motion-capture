package com.zackmathews.unifyidchallenge;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.zackmatthews.unifyidchallenge.proto.UnifyChallengeProto.SensorData.SensorType;

import java.util.Date;
import java.util.HashMap;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class RawSensorCapture implements SensorEventListener {
    class SensorDataPacket {
        public SensorType sensorType;
        public float[] values;
        public Date date;
    }

    private Subject<SensorDataPacket> packetObservable;
    private Context context;
    private SensorManager sensorManager;
    private HashMap<Integer, Sensor> sensorMap = new HashMap<>();


    public Subject<SensorDataPacket> beginCapture() {
        for (HashMap.Entry<Integer, Sensor> entry : sensorMap.entrySet()) {
            Sensor s = entry.getValue();
            sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_FASTEST);
        }
        packetObservable = BehaviorSubject.create();
        return packetObservable;
    }

    public void stopCapture() {
        sensorManager.unregisterListener(this);
        packetObservable = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorDataPacket packet = new SensorDataPacket();
        packet.date = new Date(System.currentTimeMillis());
        packet.values = new float[event.values.length];
        System.arraycopy(event.values, 0, packet.values,
                0, event.values.length);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            packet.sensorType = SensorType.ACCELEROMETER;
        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            packet.sensorType = SensorType.ROTATION_VECTOR;
        }
        packetObservable.onNext(packet);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public static class Builder {
        private Context context;
        private boolean enableRotation, enableAccel;
        private HashMap<Integer, Sensor> sensorMap = new HashMap<>();
        private SensorManager sensorManager;

        @NonNull
        public Builder with(@NonNull Context context) {
            this.context = context;
            return this;
        }

        public Builder enableRotationSensor() {
            enableRotation = true;
            return this;
        }

        public Builder enableAccelerometerSensor() {
            enableAccel = true;
            return this;
        }

        public RawSensorCapture build() {
            if (context == null)
                throw new IllegalStateException("A valid context must be provided");
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager == null)
                throw new IllegalStateException("SensorManager couldn't be instantiated");

            if (enableRotation) {
                Sensor rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                if (rotationSensor == null)
                    throw new IllegalStateException("Couldn't instantiate the rotation sensor. Check your device.");
                sensorMap.put(Sensor.TYPE_ROTATION_VECTOR, rotationSensor);
            }
            if (enableAccel) {
                Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accelerometerSensor == null)
                    throw new IllegalStateException("Couldn't instantiate the accelerometer sensor. Check your device.");
                sensorMap.put(Sensor.TYPE_ACCELEROMETER, accelerometerSensor);
            }
            RawSensorCapture rawSensorCapture = new RawSensorCapture();
            rawSensorCapture.context = context;
            rawSensorCapture.sensorManager = sensorManager;
            rawSensorCapture.sensorMap = sensorMap;
            return rawSensorCapture;
        }
    }

    private RawSensorCapture() {
    }
}
