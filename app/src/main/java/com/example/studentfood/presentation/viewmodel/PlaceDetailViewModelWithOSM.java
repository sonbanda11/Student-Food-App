package com.example.studentfood.presentation.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.repository.PlaceRepository;
import com.example.studentfood.data.repository.PlaceRepositoryImpl;
import com.example.studentfood.data.mapper.OSMMapper;

import android.app.Application;
import java.util.Map;

/**
 * PlaceDetailViewModelWithOSM - Enhanced ViewModel with OSM support
 */
public class PlaceDetailViewModelWithOSM extends ViewModel {
    
    private final PlaceRepository repository;
    private final Application application;
    
    // LiveData for place data
    private final MutableLiveData<Place> place = new MutableLiveData<>();
    private final MutableLiveData<OSMMapper.OSMData> osmData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    public PlaceDetailViewModelWithOSM(Application application) {
        this.application = application;
        this.repository = new PlaceRepositoryImpl(application);
    }
    
    /**
     * Initialize with place ID
     */
    public void init(String placeId) {
        isLoading.setValue(true);
        
        try {
            // Get place data
            Place placeData = repository.getPlaceById(placeId);
            this.place.setValue(placeData);
            
            // Get OSM data
            OSMMapper.OSMData mappedData = repository.getMappedOsmData(placeId);
            this.osmData.setValue(mappedData);
            
            isLoading.setValue(false);
        } catch (Exception e) {
            error.setValue("Error loading place: " + e.getMessage());
            isLoading.setValue(false);
        }
    }
    
    /**
     * Refresh place data
     */
    public void refresh(String placeId) {
        isLoading.setValue(true);
        
        try {
            // Refresh from repository
            Place placeData = repository.getPlaceById(placeId);
            this.place.setValue(placeData);
            
            // Get fresh OSM data
            OSMMapper.OSMData mappedData = repository.getMappedOsmData(placeId);
            this.osmData.setValue(mappedData);
            
            isLoading.setValue(false);
        } catch (Exception e) {
            error.setValue("Error refreshing place: " + e.getMessage());
            isLoading.setValue(false);
        }
    }
    
    /**
     * Get raw OSM tags
     */
    public Map<String, String> getOsmTags(String placeId) {
        return repository.getOsmTags(placeId);
    }
    
    // ==================== GETTERS ====================
    
    public LiveData<Place> getPlace() {
        return place;
    }
    
    public LiveData<OSMMapper.OSMData> getOsmData() {
        return osmData;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    /**
     * Factory for creating ViewModel
     */
    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private final Application application;
        
        public Factory(Application application) {
            this.application = application;
        }
        
        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(PlaceDetailViewModelWithOSM.class)) {
                return (T) new PlaceDetailViewModelWithOSM(application);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
