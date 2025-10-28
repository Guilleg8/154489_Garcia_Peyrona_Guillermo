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

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    private final MutableLiveData<List<Float>> temperatures = new MutableLiveData<>();
    private final MutableLiveData<Float> dailyAverage = new MutableLiveData<>();

    public LiveData<List<Float>> getTemperatures() {
        return temperatures;
    }

    public LiveData<Float> getDailyAverage() {
        return dailyAverage;
    }

    public void startTemperatureGeneration() {
        dailyAverage.setValue(null);

        executorService.execute(() -> {
            List<Float> tempList = new ArrayList<>();
            float sum = 0;

            int dayType = random.nextInt(3);
            float baseTemp;

            if (dayType == 0) {
                baseTemp = 16.0f;
            } else if (dayType == 1) {
                baseTemp = 21.0f;
            } else {
                baseTemp = 27.0f;
            }

            for (int i = 0; i < 24; i++) {
                float variation = (random.nextFloat() * 6.0f) - 3.0f;
                float temp = baseTemp + variation;

                if (temp < 10.0f) temp = 10.0f;
                if (temp > 40.0f) temp = 40.0f;

                sum += temp;
                tempList.add(temp);

                temperatures.postValue(new ArrayList<>(tempList));

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            float average = sum / 24;
            dailyAverage.postValue(average);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdownNow();
    }
}