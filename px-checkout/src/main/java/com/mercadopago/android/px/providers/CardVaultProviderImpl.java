package com.mercadopago.android.px.providers;

import android.content.Context;
import android.support.annotation.Nullable;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.core.MercadoPagoServicesAdapter;
import com.mercadopago.android.px.model.Installment;
import com.mercadopago.android.px.model.SavedESCCardToken;
import com.mercadopago.android.px.model.Token;
import com.mercadopago.android.px.mvp.TaggedCallback;
import com.mercadopago.android.px.util.MercadoPagoESC;
import com.mercadopago.android.px.util.MercadoPagoESCImpl;
import java.math.BigDecimal;
import java.util.List;

public class CardVaultProviderImpl implements CardVaultProvider {

    private final Context context;
    private final MercadoPagoServicesAdapter mercadoPago;
    private final MercadoPagoESC mercadoPagoESC;

    public CardVaultProviderImpl(Context context, String publicKey, String privateKey, boolean escEnabled) {
        this.context = context;

        mercadoPago = new MercadoPagoServicesAdapter(context, publicKey, privateKey);

        mercadoPagoESC = new MercadoPagoESCImpl(context, escEnabled);
    }

    @Override
    public String getMultipleInstallmentsForIssuerErrorMessage() {
        return context.getString(R.string.px_error_message_multiple_installments_for_issuer);
    }

    @Override
    public String getMissingInstallmentsForIssuerErrorMessage() {
        return context.getString(R.string.px_error_message_missing_installment_for_issuer);
    }

    @Override
    public String getMissingPayerCostsErrorMessage() {
        return context.getString(R.string.px_error_message_missing_payer_cost);
    }

    @Override
    public String getMissingAmountErrorMessage() {
        return context.getString(R.string.px_error_message_missing_amount);
    }

    @Override
    public String getMissingPublicKeyErrorMessage() {
        return context.getString(R.string.px_error_message_missing_public_key);
    }

    @Override
    public String getMissingSiteErrorMessage() {
        return context.getString(R.string.px_error_message_missing_site);
    }

    @Override
    public void getInstallmentsAsync(final String bin,
        final Long issuerId,
        final String paymentMethodId,
        final BigDecimal amount,
        @Nullable final Integer differentialPricingId,
        final TaggedCallback<List<Installment>> taggedCallback) {
        mercadoPago.getInstallments(bin, amount, issuerId, paymentMethodId, differentialPricingId, taggedCallback);
    }

    @Override
    public void createESCTokenAsync(SavedESCCardToken escCardToken,
        final TaggedCallback<Token> taggedCallback) {
        mercadoPago.createToken(escCardToken, taggedCallback);
    }

    @Override
    public String findESCSaved(String cardId) {
        return mercadoPagoESC.getESC(cardId);
    }

    @Override
    public void deleteESC(String cardId) {
        mercadoPagoESC.deleteESC(cardId);
    }
}
