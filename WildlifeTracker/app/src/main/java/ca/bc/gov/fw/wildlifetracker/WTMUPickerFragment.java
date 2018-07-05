package ca.bc.gov.fw.wildlifetracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Management Unit picker dialog
 */
public class WTMUPickerFragment extends DialogFragment {

    // Can be an MU name (e.g. "7-42", a region (e.g. "7A") or "All"
    public static final String INITIAL_REGION = "initialRegion";

    public static final String INCLUDE_ALL_OPTION = "includeAll";

    // User visible string - may be used for initialRegion fragment argument iff includeAll is true
    public static final String ALL_REGIONS = "All Regions";


    public interface RegionPickerListener {
        void regionPicked(String region);
    }

    private RegionPickerListAdapter adapter_;
    private ExpandableListView list_;
    private boolean includeAll_;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String initialRegion = null;
        if (args != null) {
            initialRegion = args.getString(INITIAL_REGION);
            includeAll_ = args.getBoolean(INCLUDE_ALL_OPTION, false);
        }

        // Set up appropriately themed view
        ContextThemeWrapper wrapper = new ContextThemeWrapper(getActivity(), R.style.DialogBaseTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(wrapper);
        View view = LayoutInflater.from(wrapper).inflate(R.layout.region_picker, null);
        builder.setView(view);

        adapter_ = new RegionPickerListAdapter(getActivity());

        list_ = (ExpandableListView) view.findViewById(R.id.elRegionList);
        list_.setAdapter(adapter_);

        if (initialRegion != null) {
            // Expand and select initial region
            adapter_.setSelectedRegion(initialRegion);
            long packedPos = adapter_.findSelectedRegionPosition();
            int groupPos = ExpandableListView.getPackedPositionGroup(packedPos);
            list_.expandGroup(groupPos);
            int childPos = ExpandableListView.getPackedPositionChild(packedPos);
            list_.setSelectedChild(groupPos, childPos, true);
        }

        list_.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                String region = adapter_.getSelectedRegion(groupPosition, childPosition);
                notifyRegionSelected(region);
                return true;
            }
        });

        builder.setView(view);

        builder.setNegativeButton(R.string.cancel_button_title, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        list_ = null;
    }

    private void notifyRegionSelected(String region) {
        RegionPickerListener listener = null;
        if (getActivity() instanceof RegionPickerListener) {
            listener = (RegionPickerListener) getActivity();
        } else if (getParentFragment() instanceof  RegionPickerListener) {
            listener = (RegionPickerListener) getParentFragment();
        }
        if (listener != null) {
            listener.regionPicked(region);
        } else {
            System.out.println("RegionPickerFragment region selected but no listener!");
        }
        getDialog().dismiss();
    }

    private class RegionPickerListAdapter extends BaseExpandableListAdapter {
        private String[] groups_;
        private String[][] children_;

        private ContextThemeWrapper wrapper_;
        private String selectedRegion_ = null;

        public RegionPickerListAdapter(Context context) {
            wrapper_ = new ContextThemeWrapper(context, R.style.DialogBaseTheme);
            String[] regions = ManagementUnitHelper.regions__;
            ArrayList<String> regionNames = new ArrayList<>();
            for (String region: regions) {
                regionNames.add("Region " + region);
            }
            children_ = ManagementUnitHelper.musByRegion__;
            if (WTMUPickerFragment.this.includeAll_) {
                // Need a new children array with one more group for All Regions
                String[][] temp = new String[children_.length + 1][];

                // Add new Group for All Regions
                regionNames.add(0, ALL_REGIONS);
                temp[0] = new String[] { ALL_REGIONS };

                // Insert new child for each group for All MUs
                for (int i = 0; i < children_.length; i++) {
                    String[] temp2 = new String[children_[i].length + 1];
                    temp2[0] = "All MUs";
                    System.arraycopy(children_[i], 0, temp2, 1, children_[i].length);
                    temp[i + 1] = temp2;
                }
                children_ = temp;
            }
            groups_ = regionNames.toArray(new String[regionNames.size()]);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return children_[groupPosition][childPosition];
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return (groupPosition * 100) + childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                                 ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(wrapper_).inflate(R.layout.region_picker_child, parent, false);
            }
            TextView label = (TextView) convertView.findViewById(R.id.tvChildName);
            String region = children_[groupPosition][childPosition];
            label.setText(region);
            View iconView = convertView.findViewById(R.id.ivChildSelectedIcon);
            if (selectedRegionMatches(groupPosition, childPosition))
                iconView.setVisibility(View.VISIBLE);
            else
                iconView.setVisibility(View.GONE);
            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return children_[groupPosition].length;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groups_[groupPosition];
        }

        @Override
        public int getGroupCount() {
            return groups_.length;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(wrapper_).inflate(R.layout.region_picker_group, parent, false);
            }
            TextView label = (TextView) convertView.findViewById(R.id.tvGroupName);
            label.setText(groups_[groupPosition]);
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        private boolean selectedRegionMatches(int groupPosition, int childPosition) {
            if (selectedRegion_ == null) {
                Log.e(MainActivity.LOG_TAG, "selectedRegionMatches called when selectedRegion_ is null");
                return false;
            } else {
                return selectedRegion_.equals(getSelectedRegion(groupPosition, childPosition));
            }
        }

        public String getSelectedRegion(int groupPosition, int childPosition) {
            if (!WTMUPickerFragment.this.includeAll_) {
                return children_[groupPosition][childPosition];
            } else if (groupPosition == 0) {
                return "All";
            } else if (childPosition == 0) {
                return ManagementUnitHelper.regions__[groupPosition - 1];
            } else {
                return children_[groupPosition][childPosition];
            }
        }

        public void setSelectedRegion(String region) {
            selectedRegion_ = region;
            notifyDataSetChanged();
        }

        // Returns packed position
        public long findSelectedRegionPosition() {
            int groupPos = -1;
            int childPos = -1;
            if ((selectedRegion_ == null) || (selectedRegion_.length() == 0))
                return -1;
            if (selectedRegion_.equals("All"))
                return ExpandableListView.getPackedPositionForChild(0, 0);
            if (!selectedRegion_.contains("-")) {
                String[] regions = ManagementUnitHelper.regions__;
                for (int i = 0; i < regions.length; i++) {
                    if (regions[i].equals(selectedRegion_)) {
                        return ExpandableListView.getPackedPositionForChild(i + 1, 0);
                    }
                }
                Log.e(MainActivity.LOG_TAG, "Failed to find selectedRegion_ " + selectedRegion_ + " in regions list");
                return ExpandableListView.getPackedPositionForChild(0, 0);
            }
            char firstChar = selectedRegion_.charAt(0);
            boolean found = false;
            // Skip over "All" group & child if present
            int start = WTMUPickerFragment.this.includeAll_ ? 1 : 0;
            for (int i = start; i < children_.length; i++) {
                if (children_[i][start].charAt(0) == firstChar) {
                    // First char matches... check the rest of the strings
                    for (int j = start; j < children_[i].length; j++) {
                        if (children_[i][j].equals(selectedRegion_)) {
                            groupPos = i;
                            childPos = j;
                            found = true;
                            break;
                        }
                    }
                }
                if (found)
                    break;
            }
            if (!found) {
                Log.e(MainActivity.LOG_TAG, "Failed to find selectedRegion_ " + selectedRegion_ + " in regions list");
                return ExpandableListView.getPackedPositionForChild(0, 0);
            }
            return ExpandableListView.getPackedPositionForChild(groupPos, childPos);
        }
    }

}