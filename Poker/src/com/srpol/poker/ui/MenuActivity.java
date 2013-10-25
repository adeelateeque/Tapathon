/*
 ********************************************************************************
 * Copyright (c) 2013 Samsung Electronics, Inc.
 * All rights reserved.
 *
 * This software is a confidential and proprietary information of Samsung
 * Electronics, Inc. ("Confidential Information"). You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with Samsung Electronics.
 ********************************************************************************
 */
package com.srpol.poker.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.srpol.poker.R;
import com.srpol.poker.ui.ChooseServerDialog.OnServerChosenListener;

public class MenuActivity extends Activity implements OnServerChosenListener {

	public static final String TAG = "Poker";
	public static final String POKER_PREFERENCES = "POKER_PREFERENCES";
	public static final String USER_NAME_KEY = "USER_NAME_KEY";

	private View mClientButton;
	private View mServerButton;
	private TextView mNameView;
	private SharedPreferences mSharedPreferences;

	private final BroadcastReceiver mWiFiBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
			if (info == null) {
				enableButtons(false);
			} else {
				enableButtons(true);
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu_activity);
		mClientButton = findViewById(R.id.button_client);
		mServerButton = findViewById(R.id.button_server);
		mNameView = (TextView) findViewById(R.id.name_view);

		mSharedPreferences = getSharedPreferences(POKER_PREFERENCES, MODE_PRIVATE);
		if (isNewUser()) {
			onNameViewClick(null);
		} else {
			final String name = mSharedPreferences.getString(USER_NAME_KEY, "");
			setNameTextView(name);
		}

		registerWifiStateReceiver();

		if (!isWifiConnected()) {
			enableButtons(false);
			Toast.makeText(this, getString(R.string.wifi_off), Toast.LENGTH_LONG).show();
		}
	}

	private boolean isNewUser() {
		return !mSharedPreferences.contains(USER_NAME_KEY);
	}

	public void onNameViewClick(View v) {
		final UserNameDialog dialog = new UserNameDialog();
		dialog.show(getFragmentManager(), null);
	}

	void setNameTextView(String name) {
		final StringBuilder builder = new StringBuilder(getString(R.string.hello)).append(' ').append(name);
		mNameView.setText(builder.toString());
	}

	@Override
	public void onServerChosen(String serverName) {
		final Intent intent = new Intent(this, GameActivity.class);
		intent.putExtra(GameActivity.CLIENT, true);
		intent.putExtra(GameActivity.SERVER_NAME, serverName);
		startActivity(intent);
	}

	private void registerWifiStateReceiver() {
		final IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(mWiFiBroadcastReceiver, filter);
	}

	public void serverClick(View v) {
		startActivity(new Intent(this, GameActivity.class));
	}

	public void clientClick(View v) {
		mClientButton.setEnabled(false);
		final ChooseServerDialog chooseServerDialog = new ChooseServerDialog();
		final FragmentManager fragmentManager = getFragmentManager();
		chooseServerDialog.show(fragmentManager, "");
	}

	public void settingsClick(View v) {
		startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
	}

	public void aboutClick(View v) {
		final AboutDialog aboutDialog = new AboutDialog();
		final FragmentManager fragmentManager = getFragmentManager();
		aboutDialog.show(fragmentManager, "");
	}

	public void enableButtons(boolean enabled) {
		mClientButton.setEnabled(enabled);
		mServerButton.setEnabled(enabled);
	}

	public void enableClientButton() {
		mClientButton.setEnabled(true);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mWiFiBroadcastReceiver);
		super.onDestroy();
	}

	public boolean isWifiConnected() {
		final ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return networkInfo != null && networkInfo.isConnected();
	}

}
