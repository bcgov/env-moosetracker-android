package ca.bc.gov.fw.wildlifetracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class RegulationsIndexDialogFragment extends DialogFragment {
	public interface RegulationsIndexDialogListener {
		void indexPageSelected(int page);
	}
	
	private int[] pageNumbers_;
	
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.regs_section_index);
		
		String[] list = getResources().getStringArray(R.array.regs_index);
		String[] sections = new String[list.length];
		pageNumbers_ = new int[list.length];
		for (int i = 0; i < list.length; i++) {
			String[] temp = list[i].split(":");
			sections[i] = temp[0];
			pageNumbers_[i] = Integer.parseInt(temp[1]);
		}

		builder.setItems(sections, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				RegulationsIndexDialogListener listener = null;
				if (getTargetFragment() instanceof RegulationsIndexDialogListener) {
                    listener = (RegulationsIndexDialogListener) getTargetFragment();
				} else if (getParentFragment() instanceof RegulationsIndexDialogListener) {
                    listener = (RegulationsIndexDialogListener) getParentFragment();
                } else if (getActivity() instanceof RegulationsIndexDialogListener) {
                    listener = (RegulationsIndexDialogListener) getActivity();
                }
                if (listener != null) {
                    listener.indexPageSelected(pageNumbers_[which]);
				} else {
		    		if (MainActivity.DEBUG)
		    			Log.e(MainActivity.LOG_TAG, "Failed to find listener for RegulationsIndexDialogFragment");
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
		return builder.create();
	}

}
