package com.robdich.mapsapi;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.robdich.mapsapi.model.Place;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapClickListener,
        SlidingUpPanelLayout.PanelSlideListener {

    private static final float ANCHORED_PANEL_RATIO = 2f/3f;

    private GoogleMap googleMap;
    private List<Place> places = new ArrayList<Place>();
    private List<Marker> markers = new ArrayList<Marker>();

    private SlidingUpPanelLayout slidingPanel;
    private View anchorView;
    private View headerImage;
    private TextView title;
    private TextView description;

    private int startPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        anchorView = findViewById(R.id.anchorView);

        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        headerImage = findViewById(R.id.headerImage);
        title = (TextView) findViewById(R.id.title);
        description = (TextView) findViewById(R.id.description);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        this.places = getPlaces();

        headerImage.setVisibility(View.INVISIBLE);

        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        slidingPanel.setAnchorPoint(ANCHORED_PANEL_RATIO);
        slidingPanel.setPanelSlideListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        startPosition = anchorView.getTop();
        SlidingUpPanelLayout.PanelState panelState = slidingPanel.getPanelState();
        int translation = panelState == SlidingUpPanelLayout.PanelState.COLLAPSED ?
                startPosition : 0;
        headerImage.setTranslationY(translation);

        if(panelState != SlidingUpPanelLayout.PanelState.HIDDEN) {
            headerImage.setVisibility(View.VISIBLE);
        } else {
            headerImage.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onPanelSlide(View panel, float slideOffset) {
        float percent = slideOffset / ANCHORED_PANEL_RATIO;
        percent = percent >= .99f ? 1.0f : percent;
        int translation = startPosition - (int) (startPosition * percent);
        headerImage.setTranslationY(translation >= 0 ? translation : 0);
        description.setText("" + percent);
    }

    @Override
    public void onPanelCollapsed(View panel) {
        headerImage.setTranslationY(startPosition);
        headerImage.setVisibility(View.VISIBLE);
//        description.setText("Collapsed");
    }

    @Override
    public void onPanelExpanded(View panel) {
//        description.setText("Expanded");
    }

    @Override
    public void onPanelAnchored(View panel) {
//        description.setText("Anchored");
    }

    @Override
    public void onPanelHidden(View panel) {
        headerImage.setVisibility(View.INVISIBLE);
//        description.setText("Hidden");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;

        addMarkers();

        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnInfoWindowClickListener(this);
        googleMap.setOnMapClickListener(this);

        // Pan to see all markers in view.
        // Cannot zoom to bounds until the map has a size.
        final View mapView = getSupportFragmentManager().findFragmentById(R.id.map).getView();
        if (mapView.getViewTreeObserver().isAlive()) {
            mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation") // We use the new method when supported
                @SuppressLint("NewApi") // We check which build version we are using.
                @Override
                public void onGlobalLayout() {

                    LatLngBounds bounds;
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for(int i = 0; i < markers.size(); i++) {
                        LatLng latLng = markers.get(i).getPosition();
                        builder.include(latLng);
                    }

                    bounds = builder.build();

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
                }
            });
        }

    }

    private void addMarkers() {
        for(int i = 0; i < places.size(); i++) {
            Place place = places.get(i);
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(place.latitude, place.longitude))
                    .title(place.title));
            markers.add(marker);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        showDetails(marker, false);
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        showDetails(marker, true);
    }

    private void showDetails(Marker marker, boolean isFullScreen) {
        for(int i = 0; i < markers.size(); i++) {
            if(markers.get(i).equals(marker)) {
                Place place = places.get(i);
                title.setText(place.title);
                description.setText(place.description);
                slidingPanel.setPanelState(isFullScreen ?
                        SlidingUpPanelLayout.PanelState.EXPANDED :
                        SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    private List<Place> getPlaces() {
        List<Place> places = new ArrayList<Place>();

        Place place = new Place();
        place.title = "Brisbane";
        place.description = "Description of this wonderful city";
        place.latitude = -27.47093;
        place.longitude = 153.0235;
        places.add(place);

        place = new Place();
        place.title = "Melbourne";
        place.description = "Description of this wonderful city";
        place.latitude = -37.81319;
        place.longitude = 144.96298;
        places.add(place);

        place = new Place();
        place.title = "Sydney";
        place.description = "Description of this wonderful city";
        place.latitude = -33.87365;
        place.longitude = 151.20689;
        places.add(place);

        place = new Place();
        place.title = "Adelaide";
        place.description = "Description of this wonderful city";
        place.latitude = -34.92873;
        place.longitude = 138.59995;
        places.add(place);

        place = new Place();
        place.title = "Perth";
        place.description = "Description of this wonderful city";
        place.latitude = -31.952854;
        place.longitude = 115.857342;
        places.add(place);

        return places;
    }

}
