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

public class NearbyPlacesTabFragment extends Fragment {

    public static final int TYPE_MARKET  = 0; // Chợ & Siêu thị
    public static final int TYPE_VENDING = 1; // Máy bán nước
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

    public static NearbyPlacesTabFragment newInstance(int type) {
        NearbyPlacesTabFragment f = new NearbyPlacesTabFragment();
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
            case TYPE_VENDING: title = "Máy bán nước gần đây"; break;
            case TYPE_CAFE: title = "Quán cà phê gần đây"; break;
            case TYPE_FASTFOOD: title = "Đồ ăn nhanh gần đây"; break;
            case TYPE_RESTAURANT: title = "Nhà hàng gần đây"; break;
            default: title = "Chợ & Siêu thị gần đây"; break;
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

        // Kích hoạt tải dữ liệu nếu chưa có
        if (userLat != 0 && userLng != 0) {
            placeViewModel.loadPlaces(userLat, userLng);
        }

        if (tabType == TYPE_VENDING) {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getVendingLiveData().getValue(), txtEmpty);
            });
            placeViewModel.getVendingLiveData().observe(getViewLifecycleOwner(), places -> {
                adapter.updateData(places);
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty);
            });
        } else if (tabType == TYPE_CAFE) {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getCafeLiveData().getValue(), txtEmpty);
            });
            placeViewModel.getCafeLiveData().observe(getViewLifecycleOwner(), places -> {
                adapter.updateData(places);
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty);
            });
        } else if (tabType == TYPE_FASTFOOD) {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getFastFoodLiveData().getValue(), txtEmpty);
            });
            placeViewModel.getFastFoodLiveData().observe(getViewLifecycleOwner(), places -> {
                adapter.updateData(places);
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty);
            });
        } else if (tabType == TYPE_RESTAURANT) {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getRestaurantLiveData().getValue(), txtEmpty);
            });
            placeViewModel.getRestaurantLiveData().observe(getViewLifecycleOwner(), places -> {
                adapter.updateData(places);
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty);
            });
        } else {
            placeViewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
                updateEmptyState(loading, placeViewModel.getMarketsLiveData().getValue(), txtEmpty);
            });
            placeViewModel.getMarketsLiveData().observe(getViewLifecycleOwner(), places -> {
                adapter.updateData(places);
                updateEmptyState(placeViewModel.getIsLoading().getValue(), places, txtEmpty);
            });
        }
    }

    private void updateEmptyState(Boolean isLoading, List<Place> places, TextView txtEmpty) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        boolean empty = (places == null || places.isEmpty());
        // Chỉ hiển thị "Không tìm thấy" khi đã load xong và thực sự không có data
        txtEmpty.setVisibility(!loading && empty ? View.VISIBLE : View.GONE);
    }
}
