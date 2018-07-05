package ca.bc.gov.fw.wildlifetracker;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ListView;

import com.google.android.gms.maps.GoogleMap;

/**
 * Dialog to choose map type
 */
public class MapOptionsDialogFragment extends DialogFragment {
    public interface MapOptionsDialogListener {
        void setMapType(int mapType);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.map_options);

        String[] list = getResources().getStringArray(R.array.map_types);

        builder.setItems(list, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int googleMapType = GoogleMap.MAP_TYPE_NONE;
                switch (which) {
                    case 0:
                        googleMapType = GoogleMap.MAP_TYPE_NORMAL;
                        break;
                    case 1:
                        googleMapType = GoogleMap.MAP_TYPE_SATELLITE;
                        break;
                    case 2:
                        googleMapType = GoogleMap.MAP_TYPE_HYBRID;
                        break;
                    case 3:
                        googleMapType = GoogleMap.MAP_TYPE_TERRAIN;
                        break;
                    default:
                        Log.e(MainActivity.LOG_TAG, "Unknown index for map type!");
                }
                MapOptionsDialogListener listener = null;
                if (getTargetFragment() instanceof MapOptionsDialogListener) {
                    listener = (MapOptionsDialogListener) getTargetFragment();
                } else if (getParentFragment() instanceof MapOptionsDialogListener) {
                    listener = (MapOptionsDialogListener) getParentFragment();
                } else if (getActivity() instanceof MapOptionsDialogListener) {
                    listener = (MapOptionsDialogListener) getActivity();
                }
                if (listener != null) {
                    listener.setMapType(googleMapType);
                } else {
                    Log.e(MainActivity.LOG_TAG, "Failed to find listener for MapOptionsDialogFragment");
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        // Create the AlertDialog object and return it
        AlertDialog dialog = builder.create();
        ListView listView = dialog.getListView();
        listView.setDivider(new ColorDrawable(Color.LTGRAY));
        listView.setDividerHeight(1);
        return dialog;
    }

}
