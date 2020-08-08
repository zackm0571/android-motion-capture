package com.zackmathews.unifyidchallenge;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Date;
import java.util.HashMap;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class RawSensorCapture implements SensorEventListener {
    class SensorDataPacket {
        public String sensorType;
        public Float[] values;
        public Date date;
    }

    private Subject<SensorDataPacket> packetObservable;
    private Context context;
    private SensorManager sensorManager;
    private HashMap<Integer, Sensor> sensorMap = new HashMap<>();

    public Subject<SensorDataPacket> beginCapture() {
        for (HashMap.Entry<Integer, Sensor> entry : sensorMap.entrySet()) {
            Sensor s = entry.getValue();
            sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI, SensorManager.SENSOR_DELAY_UI);
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
        packet.values = new Float[event.values.length];
        // Protobuf and Arrays.asList needs this to be Float vs float
        for(int i = 0; i < event.values.length; i++){
            packet.values[i] = event.values[i];
        }
        packet.sensorType = event.sensor.getName();
        packetObservable.onNext(packet);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public static class Builder {
        private Context context;
        private boolean enableRotation, enableAccel, enableGyroscope;
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

        public Builder enableGyroscopeSensor(){
            enableGyroscope = true;
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

            if (enableGyroscope) {
                Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                if (gyroscopeSensor == null)
                    throw new IllegalStateException("Couldn't instantiate the accelerometer sensor. Check your device.");
                sensorMap.put(Sensor.TYPE_GYROSCOPE, gyroscopeSensor);
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
