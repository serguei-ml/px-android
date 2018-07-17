package com.mercadopago.android.px.tracking.strategies;

public interface ConnectivityChecker {
    boolean hasConnection();

    boolean hasWifiConnection();
}
