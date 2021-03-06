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
package com.zhideel.tapathon.chord;

import android.content.Context;
import com.zhideel.tapathon.chord.ChordMessage.MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Used on the client side for game server discovery. Handles only messages sent over public Chord channel.
 */
public class ConnectionChord extends AbstractChord {

    private final OnServerListChangedListener mServerListChangedListener;
    private final List<String> mAvailableServers;

    public ConnectionChord(Context context, String gameName, OnServerListChangedListener serverListChangedListener) {
        super(context, gameName);
        mServerListChangedListener = serverListChangedListener;
        mAvailableServers = new ArrayList<String>();
    }

    @Override
    void handlePublicMessage(ChordMessage message) {
        switch (message.getType()) {
            case SERVER_NAME_BROADCAST:
                final String serverName = message.getString(ChordMessage.PRIVATE_CHANNEL_NAME);
                if (!mAvailableServers.contains(serverName)) {
                    mAvailableServers.add(serverName);
                }
                mServerListChangedListener.onChanged(mAvailableServers);
                break;
            case GET_SERVERS_LIST:
                // Does nothing intentionally.
                break;
            default:
                throw new IllegalArgumentException(message.getType().name());
        }
    }

    @Override
    void onChordStarted(String userNodeName, int reason) {
        super.onChordStarted(userNodeName, reason);
        joinPublicChannel();
    }

    /**
     * Triggers game server discovery.
     */
    public void findServers() {
        mAvailableServers.clear();
        mServerListChangedListener.onChanged(mAvailableServers);
        final ChordMessage message = ChordMessage.obtainMessage(MessageType.GET_SERVERS_LIST);
        sendPublicMessage(message);
    }

    /**
     * Listener responsible for passing {@link List} of discovered game server names.
     */
    public interface OnServerListChangedListener {

        void onChanged(List<String> availableServers);

    }

}
