package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.SecretKey;
import com.aptasystems.vernamcipher.util.ShareUtil;

import java.io.File;
import java.util.List;

public class SecretKeyListFragment extends Fragment {

    private ListView _listView;
    private SecretKeyListAdapter _listAdapter;

    public SecretKeyListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment SecretKeyListFragment.
     */
    public static SecretKeyListFragment newInstance() {
        SecretKeyListFragment fragment = new SecretKeyListFragment();
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
        View result = inflater.inflate(R.layout.fragment_secret_key_list, container, false);

        TextView noSecretKeys = (TextView) result.findViewById(R.id.text_view_no_secret_keys);

        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.layout_coordinator);

        _listView = (ListView) result.findViewById(R.id.list_view_secret_keys);
        _listAdapter = new SecretKeyListAdapter(getContext());
        _listView.setAdapter(_listAdapter);
        SecretKeyListChoiceListener choiceListener = new SecretKeyListChoiceListener(coordinatorLayout, _listView) {
            @Override
            protected void startShareActivity(List<File> files) {

                Intent shareIntent = ShareUtil.buildShareIntent(getActivity(), files);
                Intent chooserIntent = Intent.createChooser(shareIntent, "Share secret key");
                startActivity(chooserIntent);
            }

            @Override
            protected void startViewKeyActivity(SecretKey selectedKey) {
                SecretKey keyWithData = SecretKeyDatabase.getInstance(getContext()).fetch(selectedKey.getId(), true);

                Intent intent = new Intent(getActivity(), ViewSecretKeyActivity.class);
                intent.putExtra(WriteMessageActivity.EXTRA_KEY_SECRET_KEY, keyWithData);
                startActivity(intent);
            }
        };
        _listView.setMultiChoiceModeListener(choiceListener);
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SecretKey selectedKey = (SecretKey) _listView.getItemAtPosition(position);
                SecretKey keyWithData = SecretKeyDatabase.getInstance(getContext()).fetch(selectedKey.getId(), true);

                Intent intent = new Intent(getActivity(), WriteMessageActivity.class);
                intent.putExtra(WriteMessageActivity.EXTRA_KEY_SECRET_KEY, keyWithData);
                startActivity(intent);
            }
        });
        _listView.setEmptyView(noSecretKeys);

        return result;
    }

    @Override
    public void onResume() {

        // Ensure the list view has the most recent data.
        _listAdapter.refresh();

        super.onResume();
    }
}
