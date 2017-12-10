package edu.uw.cs.videofilters;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;

/**
 * Created by Josue Rios on 12/9/2017.
 */

public class ValueAdapter extends BaseAdapter {
    private Context mContext;
    private boolean enabled;

    public ValueAdapter(Context c, boolean enabled) {
        mContext = c;
        this.enabled = enabled;
    }

    public int getCount() {
        return 9;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        EditText editText;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            editText = new EditText(mContext);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            editText.setText("0");
            float dp = (parent.getResources().getDisplayMetrics().density);
            int dp24 = (int) (16 * dp);
            editText.setPadding(dp24, dp24, dp24, dp24);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            editText.setLayoutParams(params);
            editText.setGravity(Gravity.CENTER);
            editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } else {
            editText = (EditText) convertView;
        }
        editText.setEnabled(enabled);
        return editText;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
