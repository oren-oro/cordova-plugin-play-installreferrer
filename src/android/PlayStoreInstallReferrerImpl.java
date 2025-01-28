package com.swayangjit.installreferrer;

import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by swayangjit on 16/8/20.
 */

public class PlayStoreInstallReferrerImpl implements InstallReferrerStateListener {

    private static final String TAG = "CP-ir-PlayStoreInstallReferrer";
    private InstallReferrerClient mReferrerClient;
    private InstallReferrerListener mInstallReferrerListener;

    public void start(Context context, InstallReferrerListener installReferrerListener) {
        this.mInstallReferrerListener = installReferrerListener;
        this.mReferrerClient = InstallReferrerClient.newBuilder(context).build();

        try {
            this.mReferrerClient.startConnection(this);
        } catch (Exception exception) {
            exception.printStackTrace();
            Log.e("startConnection error: ", exception.getMessage());
        }
    }

    @Override
    public void onInstallReferrerSetupFinished(int responseCode) {
        ReferrerDetails response = null;
        switch (responseCode) {
            case InstallReferrerClient.InstallReferrerResponse.OK:
                // Connection established.
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        ReferrerDetails response = mReferrerClient.getInstallReferrer();
                        String referrerUrl = response.getInstallReferrer();
                
                        Log.e(TAG, referrerUrl);
                
                        // Handle the referrer details on the main thread if needed.
                        this.handleReferrer(response, responseCode);
                
                        mReferrerClient.endConnection();
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error retrieving install referrer", e);
                    }
                });
                executor.shutdown();
                break;
            case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                // API not available on the current Play Store app.
                this.handleReferrer(null, responseCode);
                break;
            case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                // Connection couldn't be established.
                this.handleReferrer(null, responseCode);
                break;
        }
    }

    @Override
    public void onInstallReferrerServiceDisconnected() {

    }

    private void handleReferrer(@Nullable ReferrerDetails response, int responseCode) {
        Map<String, String> referrerMap = new HashMap();
        referrerMap.put("responseCode", String.valueOf(responseCode));
        if (response != null && response.getInstallReferrer() != null) {
            referrerMap.putAll(this.getQueryKeyValueMap(Uri.parse("https://mock.com?" + response.getInstallReferrer())));
            referrerMap.put("clickTs", Long.toString(response.getReferrerClickTimestampSeconds()));
            referrerMap.put("installTs", Long.toString(response.getInstallBeginTimestampSeconds()));
            referrerMap.put("isInstantExperienceLaunched", Boolean.toString(response.getGooglePlayInstantParam()));
        }
        if (this.mInstallReferrerListener != null) {
            this.mInstallReferrerListener.onHandleReferrer(referrerMap);
        }
    }

    private Map<String, String> getQueryKeyValueMap(Uri uri) {
        HashMap<String, String> keyValueMap = new HashMap();
        Set<String> keyNamesList = uri.getQueryParameterNames();
        Iterator iterator = keyNamesList.iterator();

        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            keyValueMap.put(key, uri.getQueryParameter(key));
        }
        return keyValueMap;
    }
}
