package com.aptasystems.vernamcipher;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.SecretKey;

import java.text.NumberFormat;
import java.util.List;

public class SecretKeyListAdapter extends ArrayAdapter<SecretKey> {

    private static int layoutId = R.layout.row_secret_key;

    public SecretKeyListAdapter(Context context) {
        super(context, layoutId);
        refresh();
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        SecretKey secretKey = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(layoutId, parent, false);
        }

        // Set the filename text view.
        TextView textView = (TextView) convertView.findViewById(R.id.text_view_filename);
        textView.setText(secretKey.getName());
        textView.setTextColor(secretKey.getColour());

        // Set the description if there is one -- don't show the description text view if there isn't one.
        TextView descriptionTextView = (TextView) convertView.findViewById(R.id.text_view_description);
        if (TextUtils.getTrimmedLength(secretKey.getDescription()) == 0) {
            descriptionTextView.setVisibility(View.GONE);
        } else {
            descriptionTextView.setVisibility(View.VISIBLE);
            descriptionTextView.setText(secretKey.getDescription());
        }

        // Set the bytes remaining.
        TextView bytesRemaniningTextView = (TextView) convertView.findViewById(R.id.text_view_bytes_remaining);
        String bytesRemainingString =
                String.format(getContext().getResources().getString(R.string.bytes_remaining),
                        NumberFormat.getIntegerInstance().format(secretKey.getBytesRemaining()));
        bytesRemaniningTextView.setText(bytesRemainingString);

        // Return the completed view to render on screen
        return convertView;
    }

    public void refresh() {
        // Go off to the database and get our list of secret keys.
        List<SecretKey> secretKeys = SecretKeyDatabase.getInstance(getContext()).list(false);
        clear();
        addAll(secretKeys);
    }
}
