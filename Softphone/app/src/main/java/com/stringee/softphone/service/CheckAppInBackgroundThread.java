package com.stringee.softphone.service;

import com.stringee.softphone.common.Common;

public class CheckAppInBackgroundThread extends Thread {

    private boolean isRunning = false;

    public CheckAppInBackgroundThread() {
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            checkInBackground();
        }
    }

    private void checkInBackground() {
        if (!Common.isVisible && Common.client != null && Common.client.isConnected() && Common.lastTime > 0 && !Common.isInCall) {
            long delta = System.currentTimeMillis() - Common.lastTime;
            if (delta > 5000) {
                Common.client.disconnect();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

}
