package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecretKeyListFragment extends Fragment {

    private int _currentRequestCode;
    private Map<Integer, List<File>> _fileMap;

    private ListView _listView;
    private SecretKeyListAdapter _listAdapter;
    private TextView _noSecretKeys;

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
        _currentRequestCode = 1;
        _fileMap = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View result = inflater.inflate(R.layout.fragment_secret_key_list, container, false);

        _noSecretKeys = (TextView) result.findViewById(R.id.text_view_no_secret_keys);

        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.layout_coordinator);

        _listView = (ListView) result.findViewById(R.id.list_view_secret_keys);
        _listAdapter = new SecretKeyListAdapter(getContext());
        _listView.setAdapter(_listAdapter);
        SecretKeyListChoiceListener choiceListener = new SecretKeyListChoiceListener(coordinatorLayout, _listView) {
            @Override
            protected void startShareActivity(List<File> files) {

                // Put the list of files in the file map so we can delete them.
                _fileMap.put(_currentRequestCode, files);

                // Start the activity for result.
                Intent shareIntent = ShareUtil.buildShareIntent(getActivity(), files);
                startActivityForResult(shareIntent, _currentRequestCode++);
            }
        };
        _listView.setMultiChoiceModeListener(choiceListener);
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SecretKey selectedKey = (SecretKey) _listView.getItemAtPosition(position);
                SecretKey keyWithData = SecretKeyDatabase.getInstance(getContext()).fetch(selectedKey.getId(), true);

                Intent intent = new Intent(getActivity(), WriteMessageActivity.class);
                intent.putExtra(WriteMessageActivity.EXTRA_KEY_SECRET_KEY, (Serializable) keyWithData);
                startActivity(intent);
            }
        });
        _listView.setEmptyView(_noSecretKeys);

        return result;
    }

    @Override
    public void onResume() {

        // Ensure the list view has the most recent data.
        _listAdapter.refresh();

        super.onResume();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (!_fileMap.containsKey(requestCode)) {
            return;
        }

        // Delete the files that were shared.
        List<File> files = _fileMap.remove(requestCode);
        for (File file : files) {
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
