package com.example.studentfood.presentation.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.studentfood.data.local.repository.PlaceDetailRepository;
import com.example.studentfood.data.local.datasource.LocalDataSource;
import com.example.studentfood.data.repository.UserEventsRepository;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.domain.model.MenuItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PlaceDetailViewModel: Đã refactor sử dụng Repository mới và model MenuItem.
 */
public class PlaceDetailViewModel extends AndroidViewModel {

    private final PlaceDetailRepository repository;
    private final UserEventsRepository eventsRepository;

    private final MutableLiveData<Map<String, List<MenuItem>>> menuGrouped = new MutableLiveData<>(new LinkedHashMap<>());
    private final MutableLiveData<RatingStats> ratingStats = new MutableLiveData<>(new RatingStats(0f, 0));
    private final MutableLiveData<Boolean> favorite = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public PlaceDetailViewModel(@NonNull Application application) {
        super(application);
        LocalDataSource localDS = new LocalDataSource(application);
        this.repository = PlaceDetailRepository.getInstance(localDS);
        this.eventsRepository = UserEventsRepository.getInstance(localDS);
    }

    public void loadMenu(String placeId, Place.PlaceType type) {
        repository.getMenuGrouped(placeId, new PlaceDetailRepository.Callback<Map<String, List<com.example.studentfood.domain.model.PlaceMenuItem>>>() {
            @Override
            public void onResult(Map<String, List<com.example.studentfood.domain.model.PlaceMenuItem>> result) {
                Map<String, List<MenuItem>> transformed = new LinkedHashMap<>();
                if (result != null) {
                    for (Map.Entry<String, List<com.example.studentfood.domain.model.PlaceMenuItem>> entry : result.entrySet()) {
                        transformed.put(entry.getKey(), new java.util.ArrayList<>(entry.getValue()));
                    }
                }
                menuGrouped.setValue(transformed);
            }
        });
    }

    public void loadRatingStats(String placeId) {
        repository.getRatingStats(placeId, new PlaceDetailRepository.Callback<float[]>() {
            @Override
            public void onResult(float[] stats) {
                float avg = stats != null && stats.length > 0 ? stats[0] : 0f;
                int count = stats != null && stats.length > 1 ? (int) stats[1] : 0;
                ratingStats.setValue(new RatingStats(avg, count));
            }
        });
    }

    public void initFavoriteState(String userId, String placeId) {
        checkFavorite(userId, placeId);
    }

    public void toggleMenuLike(String itemId) {
        repository.toggleMenuItemLike(itemId);
    }

    public void checkFavorite(String userId, String placeId) {
        eventsRepository.checkIsFavorited(userId, placeId, new UserEventsRepository.EventCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                favorite.setValue(result);
            }

            @Override
            public void onError(String msg) {
                error.setValue(msg);
            }
        });
    }

    public void toggleFavorite(String userId, String placeId) {
        boolean isFav = favorite.getValue() != null && favorite.getValue();
        if (isFav) {
            // Logic unfavorite nếu cần
        } else {
            eventsRepository.trackAction(userId, placeId, 3); // 3: FAVORITE
        }
        favorite.setValue(!isFav);
    }

    public void trackView(String userId, String placeId) {
        eventsRepository.trackAction(userId, placeId, 1); // 1: VIEW
    }

    public LiveData<Place> getPlaceDetail(String placeId) {
        MutableLiveData<Place> placeData = new MutableLiveData<>();
        repository.getPlaceDetail(placeId, new PlaceDetailRepository.Callback<Place>() {
            @Override
            public void onResult(Place result) {
                placeData.setValue(result);
            }
        });
        return placeData;
    }

    public LiveData<Map<String, List<MenuItem>>> getMenuGrouped() { return menuGrouped; }
    public LiveData<RatingStats> getRatingStats() { return ratingStats; }
    public LiveData<Boolean> getFavorite() { return favorite; }
    public LiveData<String> getError() { return error; }

    public static final class RatingStats {
        public final float average;
        public final int count;
        public RatingStats(float average, int count) {
            this.average = average;
            this.count = count;
        }
    }
}
