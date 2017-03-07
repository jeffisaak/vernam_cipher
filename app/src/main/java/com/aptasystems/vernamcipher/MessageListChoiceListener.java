package com.aptasystems.vernamcipher;

import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ListView;

import com.aptasystems.vernamcipher.database.MessageDatabase;
import com.aptasystems.vernamcipher.model.Message;

public class MessageListChoiceListener implements AbsListView.MultiChoiceModeListener {

    private ListView _listView;

    public MessageListChoiceListener(ListView listView) {
        _listView = listView;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
                                          long id, boolean checked) {
        // Just update the title text.
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

            case R.id.action_delete: {
                int len = _listView.getCount();
                SparseBooleanArray checked = _listView.getCheckedItemPositions();
                for (int ii = 0; ii < len; ii++) {
                    if (checked.get(ii)) {
                        Message message = (Message) _listView.getItemAtPosition(ii);
                        MessageDatabase.getInstance(_listView.getContext()).delete(message.getId());
                    }
                }
                ((MessageListAdapter) _listView.getAdapter()).refresh();
                mode.finish();
                return true;
            }

            default:
                return false;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu
            menu) {
        // Inflate the menu for the CAB
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_selected_messages, menu);

        updateTitleText(mode);

        return true;
    }

    private void updateTitleText(ActionMode mode) {
        String titleText = String.format(_listView.getContext().getString(R.string.messages_selected),
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

}
