package com.zackmathews.unifyidchallenge;

import android.content.Context;
import android.util.Log;

import com.zackmatthews.unifyidchallenge.proto.UnifyChallengeProto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.internal.observers.BlockingBaseObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * This repo acts as the source of truth for sensor data.
 *
 * @see RawSensorCapture feeds sensor data and the repo determines
 * whether or not conditions are met for that data to be written to disk.
 * Following MVVM it would be very easy to add a ViewModel to render this data in the UI.
 */
public class SensorDataRepo {
    private Context context;
    private RawSensorCapture rawSensorCapture;
    private Subject<RawSensorCapture.SensorDataPacket> sensorPacketObservable;
    private List<RawSensorCapture.SensorDataPacket> packets = new ArrayList<>();

    public SensorDataRepo(@NonNull Context context) {
        this.context = context;
        rawSensorCapture = new RawSensorCapture.Builder().with(context)
                .enableAccelerometerSensor()
                .enableGyroscopeSensor()
                .enableRotationSensor().build();
    }

    /**
     * Starts capturing sensor data, clears the buffer of packets if it contains any.
     */
    public void startSensorCapture() {
        Log.d(getClass().getSimpleName(), "Starting sensor capture");
        packets.clear();
        sensorPacketObservable = rawSensorCapture.beginCapture();
        sensorPacketObservable.observeOn(Schedulers.io()).subscribe(new BlockingBaseObserver<RawSensorCapture.SensorDataPacket>() {
            @Override
            public void onNext(@NonNull RawSensorCapture.SensorDataPacket sensorDataPacket) {
                packets.add(sensorDataPacket);
                Log.d(getClass().getSimpleName(), String.format("Received packet { type: %s, data: %s, date: %s",
                        sensorDataPacket.sensorType,
                        Arrays.toString(sensorDataPacket.values), sensorDataPacket.date));
            }

            @Override
            public void onError(@NonNull Throwable e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Stops capturing sensor data.
     */
    public void stopSensorCapture() {
        Log.d(getClass().getSimpleName(), "Stopping sensor capture");
        rawSensorCapture.stopCapture();
        sensorPacketObservable = null;
        boolean isSimpleCallAnswerMotion = isSimpleCallAnswerMotion();
        Log.d(getClass().getSimpleName(), String.format("isSimpleCallAnswerMotion: %b", isSimpleCallAnswerMotion));
        if (isSimpleCallAnswerMotion) {
            try {
                writeSessionToDisk();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Determines whether or not the phone started
     * flat on a table and ended held up to the ear.
     * Looks at packets in the first X ms and last X ms,
     * calculates the average of the accelerometer significant axis' (Z axis for flat on table, Y for held up to ear).
     * Note: Clears the buffer of packets.
     *
     * @return true if conditions were met for a simple call answer motion.
     */
    public boolean isSimpleCallAnswerMotion() {
        if (packets != null && packets.size() > 0) {
            final long EVENT_WINDOW = 1000; // milliseconds

            final float TABLE_Z_THRESHOLD = 3.7f;
            final float TABLE_Z_DRIFT = .75f;
            final float EAR_Y_THRESHOLD = 2.5f;
            final float EAR_Y_DRIFT = .75f;

            int length = packets.size();
            Date start = packets.get(0).date;
            Date end = (length - 1 > 0) ? packets.get(length - 1).date : null;
            if (end == null) return false;

            float[] startAvgAccelerometer = new float[3];
            float[] endAvgAccelerometer = new float[3];
            int lo = 1, hi = length - 1;
            int loCount = 0, hiCount = 0;
            boolean isFinishedLo = false;
            boolean isFinishedHi = false;
            while (lo < hi || (!isFinishedLo && !isFinishedHi)) {
                RawSensorCapture.SensorDataPacket loPacket = packets.get(lo);
                RawSensorCapture.SensorDataPacket hiPacket = packets.get(hi);
                Date loTimestamp = loPacket.date;
                Date hiTimestamp = hiPacket.date;
                if (start.getTime() + EVENT_WINDOW > loTimestamp.getTime()) {
                    startAvgAccelerometer[0] += Math.abs(loPacket.values[0]);
                    startAvgAccelerometer[1] += Math.abs(loPacket.values[1]);
                    startAvgAccelerometer[2] += Math.abs(loPacket.values[2]);
                    loCount++;
                } else {
                    isFinishedLo = true;
                }

                if (end.getTime() - EVENT_WINDOW < hiTimestamp.getTime()) {
                    endAvgAccelerometer[0] += Math.abs(hiPacket.values[0]);
                    endAvgAccelerometer[1] += Math.abs(hiPacket.values[1]);
                    endAvgAccelerometer[2] += Math.abs(hiPacket.values[2]);
                    hiCount++;
                } else {
                    isFinishedHi = true;
                }
                lo++;
                hi--;
            }
            startAvgAccelerometer[0] /= loCount;
            startAvgAccelerometer[1] /= loCount;
            startAvgAccelerometer[2] /= loCount;

            endAvgAccelerometer[0] /= hiCount;
            endAvgAccelerometer[1] /= hiCount;
            endAvgAccelerometer[2] /= hiCount;

            return startAvgAccelerometer[2] > TABLE_Z_THRESHOLD - TABLE_Z_DRIFT
                    && startAvgAccelerometer[2] < TABLE_Z_THRESHOLD + TABLE_Z_DRIFT
                    && endAvgAccelerometer[1] > EAR_Y_THRESHOLD - EAR_Y_DRIFT
                    && endAvgAccelerometer[1] < EAR_Y_THRESHOLD + EAR_Y_DRIFT;
        }
        return false;
    }

    /**
     * Builds a protobuf summation of sensor data + timestamps and then writes to disk.
     *
     * @throws IOException
     */
    public void writeSessionToDisk() throws IOException {
        if (packets != null && packets.size() > 0) {
            List<UnifyChallengeProto.SensorData> sensorData = new ArrayList<>();
            for (RawSensorCapture.SensorDataPacket packet : packets) {
                List<Float> values = Arrays.asList(packet.values);
                UnifyChallengeProto.SensorData data = UnifyChallengeProto.SensorData
                        .newBuilder().addAllSensorValues(values)
                        .setSensorType(packet.sensorType)
                        .setTimestamp(packet.date.getTime()).build();
                sensorData.add(data);
            }
            UnifyChallengeProto.SensorDataCollection collection = UnifyChallengeProto.SensorDataCollection.newBuilder().addAllSensorData(sensorData).build();
            File path = context.getFilesDir();
            File file = new File(path, String.format(Locale.getDefault(), "ID_SENSOR_%d", System.currentTimeMillis()));
            try (FileOutputStream stream = new FileOutputStream(file)) {
                collection.writeTo(stream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Log.d(getClass().getSimpleName(), String.format("Wrote session to filename: %s in directory: %s", file.getName(), file.getAbsolutePath()));
            packets.clear();
        }
    }
}
