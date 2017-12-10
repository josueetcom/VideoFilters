package edu.uw.cs.videofilters;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;

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
        return 25;
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
            editText.setLayoutParams(new GridView.LayoutParams(85, 85));
            editText.setPadding(8, 8, 8, 8);
            editText.setEnabled(enabled);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setText("0");
        } else {
            editText = (EditText) convertView;
        }
        return editText;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
