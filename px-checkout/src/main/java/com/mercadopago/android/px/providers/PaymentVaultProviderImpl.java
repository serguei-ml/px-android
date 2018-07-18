package com.mercadopago.android.px.providers;

import android.content.Context;
import android.support.annotation.NonNull;
import com.mercadopago.android.px.BuildConfig;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.core.MercadoPagoServicesAdapter;
import com.mercadopago.android.px.model.PaymentMethodSearch;
import com.mercadopago.android.px.model.PaymentMethodSearchItem;
import com.mercadopago.android.px.tracker.Tracker;
import com.mercadopago.android.px.tracking.MPTracker;
import com.mercadopago.android.px.util.MercadoPagoESC;
import com.mercadopago.android.px.util.MercadoPagoESCImpl;
import com.mercadopago.android.px.util.TextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PaymentVaultProviderImpl implements PaymentVaultProvider {

    private final Context context;
    private final MercadoPagoServicesAdapter mercadoPago;
    private final String merchantBaseUrl;
    private final String merchantGetCustomerUri;
    private final Map<String, String> merchantGetCustomerAdditionalInfo;
    private final MercadoPagoESC mercadoPagoESC;
    private final String merchantPublicKey;

    public PaymentVaultProviderImpl(final Context context, final String publicKey, final String privateKey,
        final String merchantBaseUrl,
        final String merchantGetCustomerUri, final Map<String, String> merchantGetCustomerAdditionalInfo,
        final boolean escEnabled) {
        this.context = context;
        this.merchantBaseUrl = merchantBaseUrl;
        this.merchantGetCustomerUri = merchantGetCustomerUri;
        this.merchantGetCustomerAdditionalInfo = merchantGetCustomerAdditionalInfo;
        mercadoPagoESC = new MercadoPagoESCImpl(context, escEnabled);
        merchantPublicKey = publicKey;
        mercadoPago = new MercadoPagoServicesAdapter(context, publicKey, privateKey);
    }

    @Override
    public String getTitle() {
        return context.getString(R.string.px_title_activity_payment_vault);
    }

    @Override
    public String getAllPaymentTypesExcludedErrorMessage() {
        return context.getString(R.string.px_error_message_excluded_all_payment_type);
    }

    @Override
    public String getInvalidDefaultInstallmentsErrorMessage() {
        return context.getString(R.string.px_error_message_invalid_default_installments);
    }

    @Override
    public String getInvalidMaxInstallmentsErrorMessage() {
        return context.getString(R.string.px_error_message_invalid_max_installments);
    }

    @Override
    public String getStandardErrorMessage() {
        return context.getString(R.string.px_standard_error_message);
    }

    @Override
    public String getEmptyPaymentMethodsErrorMessage() {
        return context.getString(R.string.px_no_payment_methods_found);
    }

    private boolean isMerchantServerCustomerAvailable() {
        return !TextUtils.isEmpty(merchantBaseUrl) && !TextUtils.isEmpty(merchantGetCustomerUri);
    }

    public void initializeMPTracker(String siteId) {
        MPTracker.getInstance().initTracker(merchantPublicKey, siteId, BuildConfig.VERSION_NAME, context);
    }

    public void trackInitialScreen(PaymentMethodSearch paymentMethodSearch, String siteId) {
        initializeMPTracker(siteId);
        Tracker
            .trackPaymentVaultScreen(context, merchantPublicKey, paymentMethodSearch, mercadoPagoESC.getESCCardIds());
    }

    public void trackChildrenScreen(@NonNull final PaymentMethodSearchItem paymentMethodSearchItem,
        final @NonNull String siteId) {
        initializeMPTracker(siteId);
        Tracker.trackPaymentVaultChildrenScreen(context, merchantPublicKey, paymentMethodSearchItem);
    }

    @Override
    public List<String> getCardsWithEsc() {
        return new ArrayList<>(mercadoPagoESC.getESCCardIds());
    }
}
