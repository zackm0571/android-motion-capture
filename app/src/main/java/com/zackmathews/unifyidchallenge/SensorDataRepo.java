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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import io.reactivex.rxjava3.internal.observers.BlockingBaseObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.Subject;

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

    public void stopSensorCapture() {
        Log.d(getClass().getSimpleName(), "Stopping sensor capture");
        rawSensorCapture.stopCapture();
        sensorPacketObservable = null;
    }

    public void writeSessionToDisk() throws IOException {
        if (packets != null && packets.size() > 0) {
            List<UnifyChallengeProto.SensorData> sensorData = new ArrayList<>();
            for (RawSensorCapture.SensorDataPacket packet : packets) {
                List<Float> values = Arrays.asList(packet.values);
                UnifyChallengeProto.SensorData data = UnifyChallengeProto.SensorData.newBuilder().addAllSensorValues(values).setSensorType(packet.sensorType).build();
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
        }
    }

}
