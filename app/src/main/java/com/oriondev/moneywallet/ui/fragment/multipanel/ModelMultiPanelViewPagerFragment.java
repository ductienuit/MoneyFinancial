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

package com.oriondev.Viti.ui.fragment.multipanel;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.view.View;

import com.oriondev.Viti.R;
import com.oriondev.Viti.broadcast.LocalAction;
import com.oriondev.Viti.broadcast.Message;
import com.oriondev.Viti.ui.activity.NewEditItemActivity;
import com.oriondev.Viti.ui.activity.NewEditTransactionModelActivity;
import com.oriondev.Viti.ui.activity.NewEditTransferModelActivity;
import com.oriondev.Viti.ui.adapter.pager.ModelViewPagerAdapter;
import com.oriondev.Viti.ui.fragment.base.MultiPanelViewPagerMultiItemFragment;
import com.oriondev.Viti.ui.fragment.base.SecondaryPanelFragment;
import com.oriondev.Viti.ui.fragment.secondary.TransactionModelItemFragment;
import com.oriondev.Viti.ui.fragment.secondary.TransferModelItemFragment;
import com.oriondev.Viti.ui.view.theme.ThemeEngine;

/**
 * Created by DucTien on 13/10/2019.
 */
public class ModelMultiPanelViewPagerFragment extends MultiPanelViewPagerMultiItemFragment {

    private static final String SECONDARY_FRAGMENT_TAG = "ModelMultiPanelViewPagerFragment::Tag::SecondaryPanelFragment";

    private static final int TYPE_TRANSACTION_MODEL = 0;
    private static final int TYPE_TRANSFER_MODEL = 1;

    @NonNull
    @Override
    protected PagerAdapter onCreatePagerAdapter(FragmentManager fragmentManager) {
        return new ModelViewPagerAdapter(fragmentManager, getActivity());
    }

    @Override
    protected int getTitleRes() {
        return R.string.menu_models;
    }

    @Override
    protected SecondaryPanelFragment onCreateSecondaryPanel(int type) {
        switch (type) {
            case TYPE_TRANSACTION_MODEL:
                return new TransactionModelItemFragment();
            case TYPE_TRANSFER_MODEL:
                return new TransferModelItemFragment();
            default:
                throw new IllegalArgumentException("Unknown item type: " + type);
        }
    }

    @Override
    protected String getSecondaryFragmentTag(int type) {
        return SECONDARY_FRAGMENT_TAG + "::Type" + type;
    }

    @Override
    protected void onFloatingActionButtonClick() {
        Intent intent;
        switch (getViewPagerPosition()) {
            case 0:
                intent = new Intent(getActivity(), NewEditTransactionModelActivity.class);
                break;
            case 1:
                intent = new Intent(getActivity(), NewEditTransferModelActivity.class);
                break;
            default:
                return;
        }
        intent.putExtra(NewEditItemActivity.MODE, NewEditItemActivity.Mode.NEW_ITEM);
        startActivity(intent);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        IntentFilter filter = new IntentFilter(LocalAction.ACTION_ITEM_CLICK);
        LocalBroadcastManager.getInstance(context).registerReceiver(mItemClickReceiver, filter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Activity activity = getActivity();
        if (activity != null) {
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(mItemClickReceiver);
        }
    }

    private BroadcastReceiver mItemClickReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                long id = intent.getLongExtra(Message.ITEM_ID, 0L);
                switch (intent.getIntExtra(Message.ITEM_TYPE, 0)) {
                    case Message.TYPE_TRANSACTION_MODEL:
                        showItemId(TYPE_TRANSACTION_MODEL, id);
                        break;
                    case Message.TYPE_TRANSFER_MODEL:
                        showItemId(TYPE_TRANSFER_MODEL, id);
                        break;
                    default:
                        return;
                }
                showSecondaryPanel();
            }
        }

    };

    public void onTransactionAdded(Uri uri) {
        showItemAddedSnackBar(R.string.snackbar_message_model_transaction_added,
                R.string.snackbar_message_model_transaction_removed, uri);
    }

    public void onTransferAdded(Uri uri) {
        showItemAddedSnackBar(R.string.snackbar_message_model_transfer_added,
                R.string.snackbar_message_model_transfer_removed, uri);
    }

    private void showItemAddedSnackBar(int additionText, int undoText, Uri uri) {
        View view = getPrimaryPanel();
        Snackbar snackbar = Snackbar.make(view, additionText, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(ThemeEngine.getTheme().getColorAccent());
        snackbar.setAction(R.string.action_undo, new UndoModelItemInsert(view, undoText, uri));
        snackbar.show();
    }

    private static class UndoModelItemInsert implements View.OnClickListener {

        private final View mView;
        private final int mStringRes;
        private final Uri mUri;

        private UndoModelItemInsert(View view, int stringRes, Uri uri) {
            mView = view;
            mStringRes = stringRes;
            mUri = uri;
        }

        @Override
        public void onClick(View v) {
            Context context = mView.getContext();
            ContentResolver contentResolver = context.getContentResolver();
            contentResolver.delete(mUri, null, null);
            Snackbar snackbar = Snackbar.make(mView, mStringRes, Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
    }
}