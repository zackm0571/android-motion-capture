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

/**
 * Responsible for interacting with Android hardware to report sensor data.
 * Sensor data is encapsulated in @see {@link SensorDataPacket}
 * To instantiate use @see {@link Builder}.
 *
 * @see RawSensorCapture#beginCapture() returns a @see {@link Subject} that consumers can subscribe to.
 * Make sure you call @see {@link RawSensorCapture#stopCapture()} to release resources.
 */
public class RawSensorCapture implements SensorEventListener {
    static class SensorDataPacket {
        public String sensorType;
        public Float[] values;
        public Date date;
    }

    private Subject<SensorDataPacket> packetObservable;
    private Context context;
    private SensorManager sensorManager;
    private HashMap<Integer, Sensor> sensorMap = new HashMap<>();

    /**
     * Registers any sensors enabled in the Builder and begins reporting sensor data.
     * Make sure you call @see {@link RawSensorCapture#stopCapture()} to free allocated resources.
     *
     * @return @see {@link Subject} to subscribe to sensor data encapsulated in @see {@link SensorDataPacket}.
     */
    public Subject<SensorDataPacket> beginCapture() {
        for (HashMap.Entry<Integer, Sensor> entry : sensorMap.entrySet()) {
            Sensor s = entry.getValue();
            sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI, SensorManager.SENSOR_DELAY_UI);
        }
        packetObservable = BehaviorSubject.create();
        return packetObservable;
    }

    /**
     * Unregisters sensors and frees memory from the Subject observable.
     */
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
        for (int i = 0; i < event.values.length; i++) {
            packet.values[i] = event.values[i];
        }
        packet.sensorType = event.sensor.getName();
        packetObservable.onNext(packet);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Builder for creating an instance of @see {@link RawSensorCapture}.
     * A nonnull @see {@link Context} is required or else an Exception will be thrown.
     */
    public static class Builder {
        private Context context;
        private boolean enableRotation, enableAccel, enableGyroscope;
        private HashMap<Integer, Sensor> sensorMap = new HashMap<>();
        private SensorManager sensorManager;

        /**
         * Provides the builder with a context necessary to gain access to sensor data.
         *
         * @param context
         * @return
         */
        @NonNull
        public Builder with(@NonNull Context context) {
            this.context = context;
            return this;
        }

        /**
         * Enables use of @see {@link Sensor#TYPE_ROTATION_VECTOR}
         *
         * @return this builder
         */
        public Builder enableRotationSensor() {
            enableRotation = true;
            return this;
        }

        /**
         * Enables use of @see {@link Sensor#TYPE_ACCELEROMETER}
         *
         * @return this builder
         */
        public Builder enableAccelerometerSensor() {
            enableAccel = true;
            return this;
        }

        /**
         * Enables use of @see {@link Sensor#TYPE_GYROSCOPE}
         *
         * @return this builder
         */
        public Builder enableGyroscopeSensor() {
            enableGyroscope = true;
            return this;
        }

        /**
         * Builds an instance of @see {@link RawSensorCapture}
         * using the given context and flags for which sensors
         * to enable using the given context and flags for which sensors to enable.
         * Throws an exception if Context is invalid or cannot successfully gain access to a requested sensor.
         *
         * @return
         */
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
