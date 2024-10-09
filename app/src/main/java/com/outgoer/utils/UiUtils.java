package com.outgoer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class UiUtils {
    private static final String TAG = UiUtils.class.getSimpleName();


    public static void hideKeyboard(final Context context) {
        if (context == null) {
            return;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (context instanceof ContextWrapper) {
            Context parentContext = ((ContextWrapper) context).getBaseContext();
            if (parentContext instanceof Activity) {
                activity = (Activity) parentContext;
            }
        }

        if (activity == null) {
            Log.w(TAG, "Try to hide keyboard but context type is incorrect " + context.getClass().getSimpleName());
            return;
        }

        if (activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } else {
            Log.w(TAG, "Try to hide keyboard but there is no current focus view");
        }
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static boolean hideKeyboard(@NonNull Window window) {
        View view = window.getCurrentFocus();
        return hideKeyboard(window, view);
    }

    private static boolean hideKeyboard(@NonNull Window window, @Nullable View view) {
        if (view == null) {
            return false;
        }
        InputMethodManager inputMethodManager = (InputMethodManager) window.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            return inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        return false;
    }

    /**
     * Find the top fragment in FragmentManager.
     *
     * @param fragmentManager
     * @param fragmentContainerId this has to be the fragment container id in xml
     * @return
     */
    @Nullable
    public static final Fragment findTopFragment(FragmentManager fragmentManager, @IdRes int fragmentContainerId) {
        return fragmentManager.findFragmentById(fragmentContainerId);
    }
}
