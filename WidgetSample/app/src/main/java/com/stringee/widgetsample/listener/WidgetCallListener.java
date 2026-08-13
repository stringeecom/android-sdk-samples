package com.stringee.widgetsample.listener;

import androidx.annotation.NonNull;

import com.stringee.widgetsample.manager.WidgetCallCoordinator;

/** Receives host-facing connection state and integration messages from the Widget facade. */
public interface WidgetCallListener {
    /** Called whenever the Widget connection state changes. */
    void onConnectionChanged(@NonNull WidgetCallCoordinator.ConnectionState state,
                             @NonNull String userId);

    /** Called for recoverable integration errors and informational call events. */
    void onMessage(@NonNull String message);
}
