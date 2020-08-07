package com.zackmathews.unifyidchallenge;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                        sensorDataPacket.sensorType.getValueDescriptor().getFullName(),
                        Arrays.toString(sensorDataPacket.values), sensorDataPacket.date));
            }

            @Override
            public void onError(@NonNull Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public void stopSensorCapture(){
        rawSensorCapture.stopCapture();
        sensorPacketObservable = null;
    }

}
