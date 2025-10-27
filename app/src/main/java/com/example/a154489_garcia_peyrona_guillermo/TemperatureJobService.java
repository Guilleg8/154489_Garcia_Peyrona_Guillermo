package com.example.a154489_garcia_peyrona_guillermo;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.PersistableBundle;

public class TemperatureJobService extends JobService {

    public static final String KEY_AVERAGE_TEMP = "averageTemp";

    // ...
    @Override
    public boolean onStartJob(JobParameters params) {
        PersistableBundle extras = params.getExtras();
        if (extras != null) {

            // --- CAMBIO AQUÍ ---
            // float average = extras.getFloat(KEY_AVERAGE_TEMP, 0.0f); // <- LÍNEA ANTIGUA
            float average = (float) extras.getDouble(KEY_AVERAGE_TEMP, 0.0); // <- LÍNEA NUEVA
            // --- FIN DEL CAMBIO ---

            // Delegar la creación de la notificación
            NotificationsUtils.sendTemperatureNotification(getApplicationContext(), average);
        }

        jobFinished(params, false);
        return true;
    }
    // ...
    @Override
    public boolean onStopJob(JobParameters params) {
        // Si el sistema cancela el job, no queremos re-planificarlo
        return false;
    }
}