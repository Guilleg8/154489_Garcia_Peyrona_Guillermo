package com.example.a154489_garcia_peyrona_guillermo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemperatureViewModel extends ViewModel {

    // Executor para el hilo secundario
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    // LiveData para la lista de temperaturas (para el gráfico)
    private final MutableLiveData<List<Float>> temperatures = new MutableLiveData<>();

    // LiveData para la media final del día (para disparar el JobScheduler)
    private final MutableLiveData<Float> dailyAverage = new MutableLiveData<>();

    public LiveData<List<Float>> getTemperatures() {
        return temperatures;
    }

    public LiveData<Float> getDailyAverage() {
        return dailyAverage;
    }

    /**
     * Inicia la generación de 24 temperaturas en un hilo secundario.
     */
    public void startTemperatureGeneration() {
        // Resetea la media para que el observador no se dispare por un valor antiguo
        dailyAverage.setValue(null);

        executorService.execute(() -> {
            List<Float> tempList = new ArrayList<>();
            float sum = 0;

            for (int i = 0; i < 24; i++) {
                // Genera temperatura entre 10.0 y 40.0
                float temp = 10.0f + random.nextFloat() * 30.0f;
                sum += temp;
                tempList.add(temp);

                // Publica el valor en el LiveData (para el hilo principal)
                // Se postea una copia para asegurar que el observador detecte el cambio
                temperatures.postValue(new ArrayList<>(tempList));

                // Simula el paso de una hora
                try {
                    Thread.sleep(500); // 0.5 segundos por "hora" para demo
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // Cálculo final y publicación de la media
            float average = sum / 24;
            dailyAverage.postValue(average);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Detener el hilo si el ViewModel se destruye
        executorService.shutdownNow();
    }
}