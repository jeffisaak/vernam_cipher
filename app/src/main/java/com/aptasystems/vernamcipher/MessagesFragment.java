package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.aptasystems.vernamcipher.model.Message;

public class MessagesFragment extends Fragment {

    private ListView _listView;
    private MessageListAdapter _listAdapter;

    public MessagesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment MessagesFragment.
     */
    public static MessagesFragment newInstance() {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View result = inflater.inflate(R.layout.fragment_messages, container, false);

        TextView noMessages = (TextView) result.findViewById(R.id.text_view_no_messages);

        _listView = (ListView) result.findViewById(R.id.list_view_messages);
        _listAdapter = new MessageListAdapter(getContext());
        _listView.setAdapter(_listAdapter);
        MessageListChoiceListener choiceListener = new MessageListChoiceListener(_listView);
        _listView.setMultiChoiceModeListener(choiceListener);
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Message selectedMessage = (Message) _listView.getItemAtPosition(position);

                // Start the read message activity.
                Intent readIntent = new Intent(getActivity(), ReadMessageActivity.class);
                readIntent.putExtra(ReadMessageActivity.EXTRA_KEY_MESSAGE_ID, selectedMessage.getId());
                startActivity(readIntent);
            }
        });
        _listView.setEmptyView(noMessages);

        return result;
    }

    @Override
    public void onResume() {
        // Ensure the list view has the most recent data.
        _listAdapter.refresh();
        super.onResume();
    }

}
