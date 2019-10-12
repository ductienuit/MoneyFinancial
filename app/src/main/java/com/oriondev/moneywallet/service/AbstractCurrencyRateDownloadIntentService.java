/*
 * Copyright (c) 2019.
 *
 * This file is part of Viti.
 *
 * Viti is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Viti is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Viti.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oriondev.Viti.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.oriondev.Viti.R;
import com.oriondev.Viti.broadcast.LocalAction;
import com.oriondev.Viti.model.CurrencyUnit;
import com.oriondev.Viti.service.openexchangerates.OpenExchangeRatesCurrencyRateDownloadIntentService;
import com.oriondev.Viti.storage.cache.ExchangeRateCache;
import com.oriondev.Viti.storage.preference.PreferenceManager;
import com.oriondev.Viti.ui.notification.NotificationContract;
import com.oriondev.Viti.utils.CurrencyManager;
import com.oriondev.Viti.utils.Utils;

/**
 * Created by andre on 24/03/2019.
 */
public abstract class AbstractCurrencyRateDownloadIntentService extends IntentService {

    public static Intent buildIntent(Activity activity) {
        int service = PreferenceManager.getCurrentExchangeRateService();
        switch (service) {
            case PreferenceManager.SERVICE_OPEN_EXCHANGE_RATE:
                return new Intent(activity, OpenExchangeRatesCurrencyRateDownloadIntentService.class);
            default:
                throw new IllegalStateException("Unknown exchange rate service: " + service);
        }
    }

    private ExchangeRateCache mCache;
    private NotificationCompat.Builder mBuilder;

    public AbstractCurrencyRateDownloadIntentService(String name) {
        super(name);
        mCache = CurrencyManager.getExchangeRateCache();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        mBuilder = new NotificationCompat.Builder(getBaseContext(), NotificationContract.NOTIFICATION_CHANNEL_EXCHANGE_RATE)
                .setSmallIcon(Utils.isAtLeastLollipop() ? R.drawable.ic_notification : R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_title_download_exchange_rates))
                .setProgress(0, 0, true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);
        startForeground(NotificationContract.NOTIFICATION_ID_EXCHANGE_RATE_PROGRESS, mBuilder.build());
        Exception exception = null;
        try {
            updateExchangeRates();
            sendBroadcastMessage();
        } catch (Exception e) {
            exception = e;
        } finally {
            stopForeground(true);
        }
        if (exception != null) {
            showError(exception.getMessage());
        }
    }

    protected abstract void updateExchangeRates() throws Exception;

    private void sendBroadcastMessage() {
        PreferenceManager.setLastExchangeRateUpdateTimestamp(System.currentTimeMillis());
        Intent intent = new Intent(LocalAction.ACTION_EXCHANGE_RATES_UPDATED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    protected void storeExchangeRate(CurrencyUnit currency, double rate, long timestamp) {
        mCache.setExchangeRate(currency.getIso(), rate, timestamp);
    }

    protected void setCurrentProgress(String operation, int percentage) {
        mBuilder.setContentText(operation)
                .setProgress(100, percentage, false);
        startForeground(NotificationContract.NOTIFICATION_ID_EXCHANGE_RATE_PROGRESS, mBuilder.build());
    }

    private void showError(String error) {
        mBuilder = new NotificationCompat.Builder(getBaseContext(), NotificationContract.NOTIFICATION_CHANNEL_ERROR)
                .setSmallIcon(Utils.isAtLeastLollipop() ? R.drawable.ic_notification : R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_title_download_exchange_rates))
                .setContentText(getString(R.string.notification_content_error_message, error))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_content_error_message, error)))
                .setCategory(NotificationCompat.CATEGORY_ERROR);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NotificationContract.NOTIFICATION_ID_EXCHANGE_RATE_ERROR, mBuilder.build());
    }

    public class MissingApiKey extends Exception {

        public MissingApiKey(String message) {
            super(message);
        }
    }
}