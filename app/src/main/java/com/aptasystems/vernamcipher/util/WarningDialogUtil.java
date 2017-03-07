package com.aptasystems.vernamcipher.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.widget.CheckBox;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aptasystems.vernamcipher.R;

/**
 * Created by jisaak on 2016-03-04.
 */
public class WarningDialogUtil {

    public static void showDialog(Context context, int titleResId, int warningTextResId, final String preferenceName, final WarningDialogCallback callback) {

        // Show a warning dialog if the user hasn't turned it off yet.
        final SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Activity.MODE_PRIVATE);
        boolean hideClipboardWarning = prefs.getBoolean(preferenceName, false);
        if (!hideClipboardWarning) {
            MaterialDialog dialog = new MaterialDialog.Builder(context)
                    .title(titleResId)
                    .customView(R.layout.dialog_warning, true)
                    .positiveText(android.R.string.ok)
                    .negativeText(android.R.string.cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            CheckBox checkBox = (CheckBox) dialog.getCustomView().findViewById(R.id.do_not_show_warning);
                            prefs.edit().putBoolean(preferenceName, checkBox.isChecked()).commit();
                            callback.onProceed();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            callback.onCancel();
                        }
                    })
                    .build();
            ((TextView) dialog.findViewById(R.id.warning_dialog_text)).setText(warningTextResId);
            dialog.show();
        } else {
            callback.onProceed();
        }
    }

    public interface WarningDialogCallback {
        void onProceed();

        void onCancel();
    }
}
