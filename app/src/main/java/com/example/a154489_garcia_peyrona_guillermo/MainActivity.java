package com.example.a154489_garcia_peyrona_guillermo;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_START_NEW_DAY = "com.example.tempcontrol.START_NEW_DAY";

    private static final int JOB_ID = 1;

    private static final long JOB_DELAY_MS = 60 * 1000;

    private TemperatureViewModel viewModel;

    private LineChart lineChart;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startSimulation();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationsUtils.createNotificationChannel(this);

        lineChart = findViewById(R.id.lineChart);
        setupChart();

        viewModel = new ViewModelProvider(this).get(TemperatureViewModel.class);

        viewModel.getTemperatures().observe(this, this::updateChart);

        viewModel.getDailyAverage().observe(this, average -> {
            if (average != null) {
                scheduleNotificationJob(average);
            }
        });

        handleIntent(getIntent());

        if (savedInstanceState == null && !isNotificationClick(getIntent())) {
            checkPermissionsAndStart();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && ACTION_START_NEW_DAY.equals(intent.getAction())) {
            startSimulation();
        }
    }

    private boolean isNotificationClick(Intent intent) {
        return intent != null && ACTION_START_NEW_DAY.equals(intent.getAction());
    }

    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startSimulation();
            }
        } else {
            startSimulation();
        }
    }

    private void startSimulation() {
        viewModel.startTemperatureGeneration();
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisLeft().setAxisMaximum(50f);
        lineChart.getXAxis().setAxisMinimum(0f);
        lineChart.getXAxis().setAxisMaximum(23f);
        lineChart.setNoDataText("Generando datos...");
        lineChart.invalidate();
    }

    private void updateChart(List<Float> temps) {
        if (temps == null || temps.isEmpty()) {
            lineChart.clear();
            lineChart.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < temps.size(); i++) {
            entries.add(new Entry(i, temps.get(i)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Temperatura (°C)");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setCircleColor(Color.BLUE);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private void scheduleNotificationJob(float average) {
        ComponentName componentName = new ComponentName(this, TemperatureJobService.class);

        PersistableBundle bundle = new PersistableBundle();

        bundle.putDouble(TemperatureJobService.KEY_AVERAGE_TEMP, (double) average);

        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, componentName)
                .setMinimumLatency(JOB_DELAY_MS)
                .setExtras(bundle)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(jobInfo);
    }
}