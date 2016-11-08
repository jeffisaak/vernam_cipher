package com.aptasystems.vernamcipher;

import android.content.Intent;
import android.net.Uri;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ListView;

import com.aptasystems.vernamcipher.database.SecretKeyDatabase;
import com.aptasystems.vernamcipher.model.SecretKey;
import com.aptasystems.vernamcipher.util.FileManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class SecretKeyListChoiceListener implements AbsListView.MultiChoiceModeListener {

    private ListView _listView;

    public SecretKeyListChoiceListener(ListView listView) {
        _listView = listView;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
        // Here you can do something when items are selected/de-selected,
        // such as update the title in the CAB
        // TODO - Hardcoded text.
        mode.setTitle("" + _listView.getCheckedItemCount() + " selected");

    }

    private Intent buildShareIntent(List<File> files) {

        // Set up our share intent with the currently selected files.
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        ArrayList<Uri> uris = new ArrayList<Uri>();
        for (File file : files) {
            Uri uri = Uri.fromFile(file);
            uris.add(uri);
        }
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        // TODO - Hardcoded text.
        shareIntent.setType("application/octet-stream");
        return shareIntent;
    }

    private void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem
            item) {
        // Respond to clicks on the actions in the CAB
        switch (item.getItemId()) {
            case R.id.action_select_all:
                for (int ii = 0; ii < _listView.getCount(); ii++) {
                    _listView.setItemChecked(ii, true);
                }
                return true;
            case R.id.action_share: {

                // Build a list of selected items.
                List<SecretKey> selectedItems = new ArrayList<>();
                int len = _listView.getCount();
                SparseBooleanArray checkedArray = _listView.getCheckedItemPositions();
                for (int ii = 0; ii < len; ii++) {
                    if (checkedArray.get(ii)) {
                        SecretKey selectedItem = (SecretKey) _listView.getItemAtPosition(ii);
                        selectedItems.add(selectedItem);
                    }
                }

                // Build a list of files.
                List<File> files = new ArrayList<>();
                for (SecretKey selectedItem : selectedItems) {

                    // Get the secret key with the key data.
                    SecretKey secretKey = SecretKeyDatabase.getInstance(_listView.getContext()).fetch(selectedItem.getId(), true);

                    File file = FileManager.getInstance(_listView.getContext()).newTempFile(secretKey.getName());
                    try {
                        FileOutputStream outStream = new FileOutputStream(file);
                        outStream.write(secretKey.getKey());
                        outStream.close();
                    } catch (FileNotFoundException e) {
                        // TODO
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO
                        e.printStackTrace();
                    }
                    files.add(file);
                }

                // TODO - We need to clean up our files. Perhaps we can start the share activity for result, and on result, delete the files we created...?
                Intent shareIntent = buildShareIntent(files);
                _listView.getContext().startActivity(shareIntent);
                mode.finish();
                return true;
            }
            case R.id.action_delete: {
                int len = _listView.getCount();
                SparseBooleanArray checked = _listView.getCheckedItemPositions();
                for (int ii = 0; ii < len; ii++) {
                    if (checked.get(ii)) {
                        SecretKey secretKey = (SecretKey) _listView.getItemAtPosition(ii);
                        SecretKeyDatabase.getInstance(_listView.getContext()).delete(secretKey.getId());
                    }
                }
                ((SecretKeyListAdapter) _listView.getAdapter()).refresh();
                mode.finish();
                return true;
            }
            default:
                return false;
        }
    }

    @Override
//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean onCreateActionMode(ActionMode mode, Menu
            menu) {
        // Inflate the menu for the CAB
//        MenuInflater inflater = getActivity().getMenuInflater();
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_selected_secret_keys, menu);

        mode.setTitle("" + _listView.getCheckedItemCount() + " selected");

        //onCreateActionMode
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mStartColor = getActivity().getWindow().getStatusBarColor();
//            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
//        }

        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Here you can make any necessary updates to the activity when
        // the CAB is removed. By default, selected items are deselected/unchecked.
        //onDestroyActionMode
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getActivity().getWindow().setStatusBarColor(mStartColor);
//        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Here you can perform updates to the CAB due to
        // an invalidate() request
        return false;
    }
}
