# Huong Dan Su Dung Scroll Components

## Cau Trúc Component

### 1. AppBarScrollComponent
- Xû lý scroll behavior cho AppBar (location + search)
- **Chuc nang:**
  - Lên: An location bar, thu nhô search box
  - Xuông: Hiên location bar, mô rông search box

### 2. BottomNavScrollComponent  
- Xû lý scroll behavior cho Bottom Navigation
- **Chuc nang:**
  - Lên: An bottom navigation
  - Xuông: Hiên bottom navigation

### 3. ScrollComponentUnified
- Gôp hai component trên
- Dùng cho màn hình cân câ hai component

## Cách Su Dung

### Fragment Restaurant (Cân AppBar + BottomNav)
```java
public class RestaurantFragment extends Fragment {
    
    private AppBarScrollComponent appBarComponent;
    private BottomNavScrollComponent bottomNavComponent;
    // Hoac
    private ScrollComponentUnified scrollComponent;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Find views
        RecyclerView rvMainContent = view.findViewById(R.id.rvMainContent);
        View locationBar = view.findViewById(R.id.location_bar);
        View searchBox = view.findViewById(R.id.search_box);
        View bottomNav = getActivity().findViewById(R.id.bottom_navigation);
        View containerView = getActivity().findViewById(R.id.main_container);
        
        // Cách 1: Dùng component riêng (linh hoat hon)
        appBarComponent = AppBarScrollComponent.create(rvMainContent, locationBar, searchBox);
        bottomNavComponent = BottomNavScrollComponent.create(rvMainContent, bottomNav, containerView);
        
        appBarComponent.init();
        bottomNavComponent.init();
        
        // Cách 2: Dùng unified component (dôn giãn hon)
        scrollComponent = ScrollComponentUnified.create(rvMainContent, locationBar, searchBox, bottomNav, containerView);
        scrollComponent.init();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Release resources
        if (appBarComponent != null) {
            appBarComponent.release();
        }
        if (bottomNavComponent != null) {
            bottomNavComponent.release();
        }
        if (scrollComponent != null) {
            scrollComponent.release();
        }
    }
}
```

### Fragment Home (Chi AppBar)
```java
public class HomeFragment extends Fragment {
    
    private AppBarScrollComponent appBarComponent;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        RecyclerView rvContent = view.findViewById(R.id.rvContent);
        View locationBar = view.findViewById(R.id.location_bar);
        View searchBox = view.findViewById(R.id.search_box);
        
        appBarComponent = AppBarScrollComponent.create(rvContent, locationBar, searchBox,
            new AppBarScrollComponent.OnScrollStateChangeListener() {
                @Override
                public void onAppBarCollapsed() {
                    // Logic khi AppBar thu nhô
                    Log.d("HomeFragment", "AppBar collapsed");
                }

                @Override
                public void onAppBarExpanded() {
                    // Logic khi AppBar mô rông
                    Log.d("HomeFragment", "AppBar expanded");
                }

                @Override
                public void onScrollStateChanged(boolean isScrollingUp) {
                    // Logic khi scroll state thay dôi
                    Log.d("HomeFragment", "Scrolling up: " + isScrollingUp);
                }
            });
        
        appBarComponent.setScrollThreshold(15); // Tùy chinh threshold
        appBarComponent.init();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (appBarComponent != null) {
            appBarComponent.release();
        }
    }
}
```

### Activity Chi BottomNav (Không AppBar)
```java
public class SomeActivity extends AppCompatActivity {
    
    private BottomNavScrollComponent bottomNavComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_some);
        
        RecyclerView rvContent = findViewById(R.id.rvContent);
        View bottomNav = findViewById(R.id.bottom_navigation);
        View mainContainer = findViewById(R.id.main_container);
        
        bottomNavComponent = BottomNavScrollComponent.create(rvContent, bottomNav, mainContainer,
            new BottomNavScrollComponent.OnBottomNavStateChangeListener() {
                @Override
                public void onBottomNavHidden() {
                    // Logic khi bottom nav an
                    Log.d("SomeActivity", "Bottom nav hidden");
                }

                @Override
                public void onBottomNavShown() {
                    // Logic khi bottom nav hiên
                    Log.d("SomeActivity", "Bottom nav shown");
                }

                @Override
                public void onScrollStateChanged(boolean isScrollingUp) {
                    // Logic khi scroll state thay dôi
                    Log.d("SomeActivity", "Scrolling up: " + isScrollingUp);
                }
            });
        
        bottomNavComponent.init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bottomNavComponent != null) {
            bottomNavComponent.release();
        }
    }
}
```

## Tái Sù Dung

### AppBar khác nhau, BottomNav giông nhau
```java
// Fragment A có AppBar type 1
AppBarScrollComponent appBar1 = AppBarScrollComponent.create(rv1, locationBar1, searchBox1);
BottomNavScrollComponent bottomNav = BottomNavScrollComponent.create(rv1, bottomNav, container);

// Fragment B có AppBar type 2  
AppBarScrollComponent appBar2 = AppBarScrollComponent.create(rv2, locationBar2, searchBox2);
BottomNavScrollComponent bottomNav = BottomNavScrollComponent.create(rv2, bottomNav, container);
```

### Activity có BottomNav, không AppBar
```java
// Chi sù dung BottomNavScrollComponent
BottomNavScrollComponent bottomNav = BottomNavScrollComponent.create(rv, bottomNav, container);
```

## Tùy Chinh

### Scroll Threshold
```java
appBarComponent.setScrollThreshold(30); // Mâc dinh là 20
bottomNavComponent.setScrollThreshold(25);
```

### Programmatic Control
```java
// An/hiên AppBar
appBarComponent.collapse(); // An
appBarComponent.expand();   // Hiên

// An/hiên BottomNav
bottomNavComponent.hide();  // An
bottomNavComponent.show();  // Hiên

// Kiêm tra trang thái
boolean isAppBarCollapsed = appBarComponent.isCollapsed();
boolean isBottomNavHidden = bottomNavComponent.isHidden();
```

## Migration Tu ScrollComponent Cu

```java
// Trûc
ScrollComponent scrollComponent = new ScrollComponent(nestedScrollView, locationBar, searchBox, bottomNav);

// Sau (RecyclerView)
ScrollComponentUnified scrollComponent = ScrollComponentUnified.create(recyclerView, locationBar, searchBox, bottomNav, container);
scrollComponent.init();
```

## Lôi Ích

1. **Modular**: Dùng riêng hoac gôp linh hoat
2. **Reusable**: Tái sù dung qua các màn hình
3. **Flexible**: AppBar khác nhau + BottomNav giông nhau
4. **Performance**: Tôi uu cho RecyclerView
5. **Maintainable**: Dê maintain và extend
