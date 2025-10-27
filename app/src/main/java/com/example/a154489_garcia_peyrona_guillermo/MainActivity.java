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

import com.example.a154489_garcia_peyrona_guillermo.TemperatureJobService;
import com.example.a154489_garcia_peyrona_guillermo.NotificationsUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Acción para el Intent de la notificación
    public static final String ACTION_START_NEW_DAY = "com.example.tempcontrol.START_NEW_DAY";

    private static final int JOB_ID = 1;
    private static final long JOB_DELAY_MS = 60 * 1000; // 1 minuto

    private TemperatureViewModel viewModel;
    private LineChart lineChart;

    // Launcher para el permiso de notificaciones (API 33+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permiso concedido
                    startSimulation();
                } else {
                    // Permiso denegado. Manejar (ej. mostrar mensaje)
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Crear canal de notificación
        NotificationsUtils.createNotificationChannel(this);

        // 2. Configurar Vista (Gráfico)
        lineChart = findViewById(R.id.lineChart);
        setupChart();

        // 3. Configurar ViewModel
        viewModel = new ViewModelProvider(this).get(TemperatureViewModel.class);

        // 4. Observar cambios en los datos
        viewModel.getTemperatures().observe(this, this::updateChart);

        // 5. Observar la media final para programar el Job
        viewModel.getDailyAverage().observe(this, average -> {
            if (average != null) {
                scheduleNotificationJob(average);
            }
        });

        // 6. Manejar la acción de la notificación (si la app se abre desde ella)
        handleIntent(getIntent());

        // 7. Iniciar simulación (solo si no es por recreación de config)
        if (savedInstanceState == null && !isNotificationClick(getIntent())) {
            checkPermissionsAndStart();
        }
    }

    /**
     * Se llama cuando la Activity está abierta y recibe un nuevo Intent
     * (Ej. al pulsar la notificación)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Actualiza el intent de la activity
        handleIntent(intent); // Maneja la acción
    }

    /**
     * Comprueba si el Intent es el de la notificación y actúa en consecuencia
     */
    private void handleIntent(Intent intent) {
        if (intent != null && ACTION_START_NEW_DAY.equals(intent.getAction())) {
            // Viene de la notificación: Iniciar nuevo día
            startSimulation();
        }
    }

    private boolean isNotificationClick(Intent intent) {
        return intent != null && ACTION_START_NEW_DAY.equals(intent.getAction());
    }

    /**
     * Pide permiso de notificación (API 33+) o inicia la simulación directamente
     */
    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Pedir permiso
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Permiso ya concedido
                startSimulation();
            }
        } else {
            // Versiones anteriores a Android 13 no necesitan permiso en runtime
            startSimulation();
        }
    }

    private void startSimulation() {
        viewModel.startTemperatureGeneration();
    }

    /**
     * Configuración inicial del gráfico
     */
    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisLeft().setAxisMaximum(50f);
        lineChart.getXAxis().setAxisMinimum(0f);
        lineChart.getXAxis().setAxisMaximum(23f); // 24 puntos (0 a 23)
        lineChart.setNoDataText("Generando datos...");
        lineChart.invalidate();
    }

    /**
     * Actualiza el gráfico con la nueva lista de temperaturas
     */
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
        lineChart.invalidate(); // Refrescar el gráfico
    }

    /**
     * Planifica el JobScheduler para mostrar la notificación en 1 minuto
     */
    // ...
    private void scheduleNotificationJob(float average) {
        ComponentName componentName = new ComponentName(this, TemperatureJobService.class);

        // Pasar la media al JobService
        PersistableBundle bundle = new PersistableBundle();

        // --- CAMBIO AQUÍ ---
        // bundle.putFloat(TemperatureJobService.KEY_AVERAGE_TEMP, average); // <- LÍNEA ANTIGUA
        bundle.putDouble(TemperatureJobService.KEY_AVERAGE_TEMP, (double) average); // <- LÍNEA NUEVA
        // --- FIN DEL CAMBIO ---

        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, componentName)
                .setMinimumLatency(JOB_DELAY_MS) // 1 minuto
                .setExtras(bundle)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE) // No necesita red
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(jobInfo);
    }
}