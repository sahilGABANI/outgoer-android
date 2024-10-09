package com.outgoer.utils;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.outgoer.BuildConfig;
import com.outgoer.R;

import timber.log.Timber;

public class ShareHelper {

    private final static String TAG = "ShareHelper";

    private final static String baseUrl = "https://share.outgoerapp.com?";
    private final static String fallBackUrl = "https://play.google.com/store/apps/details?id=com.outgoer.app";
    private final static String domain = "https://share.outgoerapp.com";
    private final static String iosBundleId = "com.outgoer.app";

    public static void shareText(Context context, String text) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        context.startActivity(sendIntent);
    }

    public static void shareDeepLink(Context context, boolean isPost, int postOrReelId, SetOnShareUrl setOnShareUrl) {
        Toast.makeText(context, context.getResources().getString(R.string.msg_preparing_link_wait_for_moment), Toast.LENGTH_LONG).show();

        String urlParameters = "";
        if (isPost) {
            urlParameters = "post_id=".concat(String.valueOf(postOrReelId));
        } else {
            urlParameters = "reels_id=".concat(String.valueOf(postOrReelId));
        }
        Timber.tag("DynamicLink").i(Uri.parse(baseUrl.concat(urlParameters)).toString());
        Uri encryptedUrl = Uri.parse(fallBackUrl);
        DynamicLink link = FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(Uri.parse(baseUrl.concat(urlParameters)))
                .setDomainUriPrefix(domain)
                .setIosParameters(new DynamicLink.IosParameters.Builder(iosBundleId)
                        .setFallbackUrl(encryptedUrl)
                        .setIpadFallbackUrl(encryptedUrl)
                        .build())
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID)
                        .setFallbackUrl(encryptedUrl)
                        .build())
                .buildDynamicLink();

        shortenLongLink(link.getUri(), setOnShareUrl);
    }

    public static void shortenLongLink(Uri longLink, SetOnShareUrl setOnShareUrl) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLongLink(longLink)
                .buildShortDynamicLink()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Uri shortLink = task.getResult().getShortLink();
                        Uri flowchartLink = task.getResult().getPreviewLink();
                        if (shortLink != null)
                            setOnShareUrl.loadShareUrl(shortLink.toString());
                        if (flowchartLink != null)
                            Timber.tag(TAG).i("flowchartLink: %s", flowchartLink.toString());
                    }
                });
    }

    public interface SetOnShareUrl {
        void loadShareUrl(String shareUrl);
    }
}