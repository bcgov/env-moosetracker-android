package ca.bc.gov.fw.wildlifetracker;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlayOptions;


/**
 * Displays the map with MU overlays.
 */
public class MapFragment extends SupportMapFragment implements OnMapReadyCallback, MapOptionsDialogFragment.MapOptionsDialogListener {

    private GoogleMap map;
    private Bundle savedState;

    public MapFragment() {
        // Required empty public constructor
    }

    public static MapFragment newInstance() {
        GoogleMapOptions options = new GoogleMapOptions();
        options.compassEnabled(false);
        options.rotateGesturesEnabled(false);
        options.tiltGesturesEnabled(false);
        options.camera(new CameraPosition(new LatLng(54.5, -126.5), 8, 0, 0));
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putParcelable("MapOptions", options);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedState = savedInstanceState;
        getMapAsync(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (map != null) {
            outState.putInt("map_type", map.getMapType());
            outState.putParcelable("camera_position", map.getCameraPosition());
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getActivity() == null)
            return;
        ImageButton mapTypeButton = (ImageButton) getActivity().findViewById(R.id.btnMapLayers);
        if (mapTypeButton == null)
            return;
        if (isVisibleToUser) {
            mapTypeButton.setVisibility(View.VISIBLE);
            mapTypeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("Show map type dialog *********");
                    MapOptionsDialogFragment frag = new MapOptionsDialogFragment();
                    frag.setStyle(DialogFragment.STYLE_NORMAL, R.style.MyListViewStyle);
                    frag.show(MapFragment.this.getChildFragmentManager(), null);
                }
            });
        } else {
            mapTypeButton.setVisibility(View.GONE);
            mapTypeButton.setOnClickListener(null);
        }
    }

    private static final int LOCATIONS_PERMISSION_REQUEST_CODE = 42;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.addTileOverlay(new TileOverlayOptions().tileProvider(RegionManager.getInstance()));

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        } else {
            requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, LOCATIONS_PERMISSION_REQUEST_CODE);
        }

        if (savedState != null) {
            map.setMapType(savedState.getInt("map_type", GoogleMap.MAP_TYPE_NORMAL));
            CameraPosition cp = savedState.getParcelable("camera_position");
            if (cp != null) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATIONS_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //noinspection ResourceType
            map.setMyLocationEnabled(true);
        }
    }

    @Override
    public void setMapType(int mapType) {
        if (map != null) {
            map.setMapType(mapType);
        }
    }
}
