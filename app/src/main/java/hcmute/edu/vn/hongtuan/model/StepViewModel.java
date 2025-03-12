package hcmute.edu.vn.hongtuan.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class StepViewModel extends ViewModel {
    private final MutableLiveData<Integer> stepCount = new MutableLiveData<>(0);
    private final MutableLiveData<Float> distance = new MutableLiveData<>(0f);
    private final MutableLiveData<Float> calories = new MutableLiveData<>(0f);

    public LiveData<Integer> getStepCount() {
        return stepCount;
    }

    public LiveData<Float> getDistance() {
        return distance;
    }

    public LiveData<Float> getCalories() {
        return calories;
    }

    public void setStepCount(int steps) {
        stepCount.postValue(steps); // Use postValue to update LiveData from background threads
    }

    public void setDistance(float distanceValue) {
        distance.postValue(distanceValue);
    }

    public void setCalories(float caloriesValue) {
        calories.postValue(caloriesValue);
    }

}