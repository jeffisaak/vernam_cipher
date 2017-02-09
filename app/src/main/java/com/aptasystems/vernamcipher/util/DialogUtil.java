package com.aptasystems.vernamcipher.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.aptasystems.vernamcipher.R;

/**
 * Created by jisaak on 2016-03-04.
 */
public class DialogUtil {

    private DialogUtil() {
        // Prevents unwanted instantiation.
    }

    /**
     * Potentially show a warning dialog without the "do not show this warning again". If the dialog
     * has already been shown once to the user it will not be shown again.
     *
     * @param context
     * @param titleResId
     * @param warningTextResId
     * @param preferenceName
     * @param callback
     */
    public static void showWarningDialogOnce(Context context, int titleResId, int warningTextResId, final String preferenceName, @NonNull final WarningDialogCallback callback) {
        showWarningDialog(context, titleResId, warningTextResId, preferenceName, callback, false, true);
    }

    /**
     * Potentially show a warning dialog, including the "do not show this warning again" checkbox.
     *
     * @param context
     * @param titleResId
     * @param warningTextResId
     * @param preferenceName
     * @param callback
     */
    public static void showWarningDialog(Context context, int titleResId, int warningTextResId, final String preferenceName, @NonNull final WarningDialogCallback callback) {
        showWarningDialog(context, titleResId, warningTextResId, preferenceName, callback, true, false);
    }

    /**
     * Potentially show a warning dialog that may or may not be shown again.
     *
     * @param context
     * @param titleResId
     * @param warningTextResId
     * @param preferenceName
     * @param callback
     * @param includeDontShowAgain Whether or not to show the "do not show this warning again"
     *                             checkbox.
     * @param autoDontShowAgain    Whether or not this dialog is shown once then never again.
     */
    public static void showWarningDialog(Context context, int titleResId, int warningTextResId, final String preferenceName, @NonNull
    final WarningDialogCallback callback, boolean includeDontShowAgain, final boolean autoDontShowAgain) {

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
                            prefs.edit().putBoolean(preferenceName, checkBox.isChecked() || autoDontShowAgain).commit();
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
            CheckBox checkBox = (CheckBox) dialog.getCustomView().findViewById(R.id.do_not_show_warning);
            if (!includeDontShowAgain) {
                checkBox.setVisibility(View.GONE);
            } else {
                checkBox.setVisibility(View.VISIBLE);
            }
            ((TextView) dialog.findViewById(R.id.warning_dialog_text)).setText(warningTextResId);
            dialog.show();
        } else {

            // Either the user has turned off showing the dialog again or it has been automatically
            // done. Just proceed with the callback.
            callback.onProceed();
        }
    }

    public static interface WarningDialogCallback {
        public void onProceed();

        public void onCancel();
    }
}
