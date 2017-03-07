package com.aptasystems.vernamcipher;

import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class SecretKeyListChoiceListener implements AbsListView.MultiChoiceModeListener {

    private CoordinatorLayout _coordinatorLayout;
    private ListView _listView;

    public SecretKeyListChoiceListener(CoordinatorLayout coordinatorLayout, ListView listView) {
        _coordinatorLayout = coordinatorLayout;
        _listView = listView;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
        updateTitleText(mode);
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

                // We need to ensure that external storage is available and writable before we continue.
                FileManager.StorageState storageState = FileManager.getInstance(_listView.getContext()).getExternalStorageState();
                if (storageState == FileManager.StorageState.READ_ONLY) {
                    Snackbar.make(_coordinatorLayout, R.string.toast_external_storage_read_only_share_secret_key, Snackbar.LENGTH_LONG).show();
                } else if (storageState == FileManager.StorageState.NOT_AVAILABLE) {
                    Snackbar.make(_coordinatorLayout, R.string.toast_external_storage_not_available_share_secret_key, Snackbar.LENGTH_LONG).show();
                } else {

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
                    List<File> files = null;
                    try {
                        files = buildShareFileList(selectedItems);
                    } catch (IOException e) {
                        Snackbar.make(_coordinatorLayout,
                                R.string.error_exception_build_share_list,
                                Snackbar.LENGTH_LONG).show();
                        return true;
                    }

                    startShareActivity(files);
                    mode.finish();

                }
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

    protected List<File> buildShareFileList(List<SecretKey> selectedItems) throws IOException {
        List<File> files = new ArrayList<>();
        for (SecretKey selectedItem : selectedItems) {

            // Get the secret key with the key data.
            SecretKey secretKey = SecretKeyDatabase.getInstance(_listView.getContext()).fetch(selectedItem.getId(), true);

            File file = FileManager.getInstance(_listView.getContext()).newTempFile(secretKey.getId() + ";" + secretKey.getName());
            FileOutputStream outStream = new FileOutputStream(file);
            outStream.write(secretKey.getKey());
            outStream.close();
            files.add(file);
        }
        return files;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu
            menu) {
        // Inflate the menu for the CAB
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_selected_secret_keys, menu);

        updateTitleText(mode);

        return true;
    }

    private void updateTitleText(ActionMode mode) {
        String titleText = String.format(_listView.getContext().getString(R.string.secret_keys_selected),
                _listView.getCheckedItemCount());
        mode.setTitle(titleText);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Noop.
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Here you can perform updates to the CAB due to
        // an invalidate() request
        return false;
    }

    protected abstract void startShareActivity(List<File> files);
}
