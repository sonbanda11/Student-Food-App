package com.example.studentfood.presentation.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.example.studentfood.R;
import com.example.studentfood.domain.model.Place;
import com.example.studentfood.presentation.ui.adapter.PlaceAdapter;
import com.example.studentfood.presentation.viewmodel.PlaceViewModel;

public class NearbyPlacesTabFragment_Fixed extends Fragment {

    public static final int TYPE_MARKET  = 0; // Ch? & Siêu thi?
    public static final int TYPE_VENDING = 1; // Máy bán n??c
    public static final int TYPE_CAFE    = 2; // Cafe
    public static final int TYPE_FASTFOOD = 3; // Fast Food
    public static final int TYPE_RESTAURANT = 4; // Restaurant

    private static final String ARG_TYPE = "type";

    private int tabType;
    private PlaceViewModel placeViewModel;
    private PlaceAdapter adapter;
    private double userLat = 0, userLng = 0;

    public interface OnPlaceSelectedListener {
        void onPlaceSelected(Place place);
    }
    private OnPlaceSelectedListener routeListener;

    public static NearbyPlacesTabFragment_Fixed newInstance(int type) {
        NearbyPlacesTabFragment_Fixed f = new NearbyPlacesTabFragment_Fixed();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        f.setArguments(args);
        return f;
    }

    public void setOnPlaceSelectedListener(OnPlaceSelectedListener l) { this.routeListener = l; }

    public void setUserLocation(double lat, double lng) {
        this.userLat = lat;
        this.userLng = lng;
        if (adapter != null) adapter.setUserLocation(lat, lng);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tabType = getArguments() != null ? getArguments().getInt(ARG_TYPE, TYPE_MARKET) : TYPE_MARKET;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby_places, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        placeViewModel = new ViewModelProvider(requireActivity()).get(PlaceViewModel.class);

        TextView txtTitle = view.findViewById(R.id.txtPlaceTitle);
        ProgressBar progress = view.findViewById(R.id.progressPlaces);
        TextView txtEmpty = view.findViewById(R.id.txtEmptyPlaces);
        RecyclerView rv = view.findViewById(R.id.rvPlaces);

        String title;
        switch (tabType) {
            case TYPE_VENDING: title = "Máy bán n??c g?n dây"; break;
            case TYPE_CAFE: title = "Quán cà phê g?n dây"; break;
            case TYPE_FASTFOOD: title = "?? ?n nhanh g?n dây"; break;
            case TYPE_RESTAURANT: title = "Nhà hàng g?n dây"; break;
            default: title = "Ch? & Siêu thi? g?n dây"; break;
        }
        txtTitle.setText(title);

        adapter = new PlaceAdapter(requireContext(), PlaceAdapter.TYPE_VERTICAL);
        adapter.setUserLocation(userLat, userLng);
        adapter.setOnItemClickListener(place -> {
            if (routeListener != null) {
                routeListener.onPlaceSelected(place);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // Ch? load d? li?u n?u ch?a có ho?c c?n refresh
        if (userLat != 0 && userLng != 0) {
            if (!placeViewModel.hasValidData(userLat, userLng)) {
                placeViewModel.loadPlaces(userLat, userLng);
            }
        }

        if (tabType == TYPE_VENDING) {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getVendingLiveData().getValue(), txtEmpty, rv);
            });
            placeViewModel.getVendingLiveData().observe(getViewLifecycleOwner(), places -> {
                android.util.Log.d("VendingTab", "=== VENDING DATA RECEIVED ===");
                android.util.Log.d("VendingTab", "Places count: " + (places != null ? places.size() : 0));
                android.util.Log.d("VendingTab", "Loading state: " + placeViewModel.getIsLoading().getValue());
                android.util.Log.d("VendingTab", "Adapter null: " + (adapter == null));
                
                if (adapter != null) {
                    adapter.updateData(places);
                    android.util.Log.d("VendingTab", "Adapter updated with data");
                }
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty, rv);
            });
        } else if (tabType == TYPE_CAFE) {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getCafeLiveData().getValue(), txtEmpty, rv);
            });
            placeViewModel.getCafeLiveData().observe(getViewLifecycleOwner(), places -> {
                adapter.updateData(places);
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty, rv);
            });
        } else if (tabType == TYPE_FASTFOOD) {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getFastFoodLiveData().getValue(), txtEmpty, rv);
            });
            placeViewModel.getFastFoodLiveData().observe(getViewLifecycleOwner(), places -> {
                adapter.updateData(places);
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty, rv);
            });
        } else if (tabType == TYPE_RESTAURANT) {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getRestaurantLiveData().getValue(), txtEmpty, rv);
            });
            placeViewModel.getRestaurantLiveData().observe(getViewLifecycleOwner(), places -> {
                adapter.updateData(places);
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty, rv);
            });
        } else {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getMarketsLiveData().getValue(), txtEmpty, rv);
            });
            placeViewModel.getMarketsLiveData().observe(getViewLifecycleOwner(), places -> {
                adapter.updateData(places);
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty, rv);
            });
        }
    }

    private void updateEmptyState(Boolean isLoading, List<Place> places, TextView txtEmpty, RecyclerView rv) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        boolean empty = (places == null || places.isEmpty());
        
        android.util.Log.d("VendingTab", "=== UPDATE EMPTY STATE ===");
        android.util.Log.d("VendingTab", "Loading: " + loading + ", Empty: " + empty);
        android.util.Log.d("VendingTab", "RV null: " + (rv == null) + ", Empty null: " + (txtEmpty == null));
        
        // Fix: Add RecyclerView visibility management
        if (!loading) {
            if (rv != null) {
                rv.setVisibility(empty ? View.GONE : View.VISIBLE);
                android.util.Log.d("VendingTab", "RV visibility set to: " + (empty ? "GONE" : "VISIBLE"));
            }
            if (txtEmpty != null) {
                txtEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                android.util.Log.d("VendingTab", "Empty visibility set to: " + (empty ? "VISIBLE" : "GONE"));
            }
        } else {
            if (rv != null) {
                rv.setVisibility(View.GONE);
                android.util.Log.d("VendingTab", "RV set to GONE (loading)");
            }
            if (txtEmpty != null) {
                txtEmpty.setVisibility(View.GONE);
                android.util.Log.d("VendingTab", "Empty set to GONE (loading)");
            }
        }
    }
}
