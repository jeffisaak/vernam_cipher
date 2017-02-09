package com.aptasystems.vernamcipher;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aptasystems.vernamcipher.database.MessageDatabase;
import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.Message;
import com.aptasystems.vernamcipher.model.SecretKey;

import java.text.NumberFormat;
import java.util.List;

public class MessageListAdapter extends ArrayAdapter<Message> {

    public MessageListAdapter(Context context) {
        super(context, R.layout.row_message);
        refresh();
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Message message = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_message, parent, false);
        }

        TextView textView = (TextView) convertView.findViewById(R.id.text_view_content);
        textView.setText(message.getContent());

        ImageView imageView = (ImageView) convertView.findViewById(R.id.image_view_message_type);
        if( message.isIncoming())
        {
            imageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_call_received_grey_24dp));
        } else {
            imageView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_call_made_grey_24dp));
        }

        // Return the completed view to render on screen
        return convertView;
    }

    public void refresh() {
        // Go off to the database and get our list of messages.
        List<Message> messages = MessageDatabase.getInstance(getContext()).list();
        clear();
        addAll(messages);
    }
}
