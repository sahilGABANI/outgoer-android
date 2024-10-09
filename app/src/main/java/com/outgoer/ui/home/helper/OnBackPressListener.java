package com.outgoer.ui.home.helper;

/**
 * Listener that's called when a back press is registered in the parent activity.
 */
public interface OnBackPressListener {

    /**
     * Should be called when the back key is pressed within an activity.
     * <p>
     * <p/>Specific behavior will check if the press should be consumed or not.
     *
     * @return true if the event was consumed, false otherwise
     */
    boolean onBackPressed();
}