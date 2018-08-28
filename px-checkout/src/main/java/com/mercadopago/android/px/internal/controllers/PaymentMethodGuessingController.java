package com.mercadopago.android.px.internal.controllers;

import android.support.annotation.Nullable;
import com.mercadopago.android.px.internal.util.MercadoPagoUtil;
import com.mercadopago.android.px.model.CardInformation;
import com.mercadopago.android.px.model.PaymentMethod;
import com.mercadopago.android.px.model.PaymentTypes;
import com.mercadopago.android.px.model.Setting;
import java.util.ArrayList;
import java.util.List;

public class PaymentMethodGuessingController {

    private final List<PaymentMethod> mAllPaymentMethods;
    private final List<String> mExcludedPaymentTypes;
    private final String mPaymentTypeId;
    private String mSavedBin;
    private List<PaymentMethod> mGuessedPaymentMethods;

    public PaymentMethodGuessingController(final List<PaymentMethod> paymentMethods,
        final String paymentTypeId, final List<String> excludedPaymentTypes) {
        mAllPaymentMethods = paymentMethods;
        mExcludedPaymentTypes = excludedPaymentTypes;
        mPaymentTypeId = paymentTypeId;
        mSavedBin = "";
    }

    @Nullable
    public static Setting getSettingByPaymentMethodAndBin(final PaymentMethod paymentMethod, final String bin) {
        Setting setting = null;
        if (bin == null) {
            if (paymentMethod.getSettings() != null && !paymentMethod.getSettings().isEmpty()) {
                setting = paymentMethod.getSettings().get(0);
            }
        } else {
            final List<Setting> settings = paymentMethod.getSettings();
            setting = Setting.getSettingByBin(settings, bin);
        }
        return setting;
    }

    public static Integer getCardNumberLength(@Nullable final PaymentMethod paymentMethod, final String bin) {

        if (paymentMethod == null || bin == null) {
            return CardInformation.CARD_NUMBER_MAX_LENGTH;
        }
        final Setting setting = PaymentMethodGuessingController.getSettingByPaymentMethodAndBin(paymentMethod, bin);
        int cardNumberLength = CardInformation.CARD_NUMBER_MAX_LENGTH;
        if (setting != null) {
            cardNumberLength = setting.getCardNumber().getLength();
        }
        return cardNumberLength;
    }

    public String getPaymentTypeId() {
        return mPaymentTypeId;
    }

    public List<PaymentMethod> getGuessedPaymentMethods() {
        return mGuessedPaymentMethods;
    }

    public List<PaymentMethod> getAllSupportedPaymentMethods() {
        final List<PaymentMethod> supportedPaymentMethods = new ArrayList<>();
        for (final PaymentMethod paymentMethod : mAllPaymentMethods) {
            if (isCardPaymentType(paymentMethod) &&
                ((mPaymentTypeId == null) || (mPaymentTypeId.equals(paymentMethod.getPaymentTypeId())))) {
                supportedPaymentMethods.add(paymentMethod);
            }
        }
        return supportedPaymentMethods;
    }

    private boolean isCardPaymentType(final PaymentMethod paymentMethod) {
        final String paymentTypeId = paymentMethod.getPaymentTypeId();
        return paymentTypeId.equals(PaymentTypes.CREDIT_CARD) ||
            paymentTypeId.equals(PaymentTypes.DEBIT_CARD) ||
            paymentTypeId.equals(PaymentTypes.PREPAID_CARD);
    }

    public List<PaymentMethod> guessPaymentMethodsByBin(final String bin) {
        if (mSavedBin.equals(bin) && mGuessedPaymentMethods != null) {
            return mGuessedPaymentMethods;
        }
        saveBin(bin);
        mGuessedPaymentMethods = MercadoPagoUtil
            .getValidPaymentMethodsForBin(mSavedBin, mAllPaymentMethods);
        mGuessedPaymentMethods = getValidPaymentMethodForType(mPaymentTypeId, mGuessedPaymentMethods);
        if (mGuessedPaymentMethods.size() > 1) {
            mGuessedPaymentMethods = filterByPaymentType(mExcludedPaymentTypes, mGuessedPaymentMethods);
        }
        return mGuessedPaymentMethods;
    }

    public void saveBin(@Nullable final String bin) {
        mSavedBin = bin;
    }

    private List<PaymentMethod> getValidPaymentMethodForType(final String paymentTypeId,
        final List<PaymentMethod> paymentMethods) {
        if (paymentTypeId == null) {
            return paymentMethods;
        } else {
            final List<PaymentMethod> validPaymentMethodsForType = new ArrayList<>();
            for (final PaymentMethod pm : paymentMethods) {
                if (pm.getPaymentTypeId().equals(paymentTypeId)) {
                    validPaymentMethodsForType.add(pm);
                }
            }
            return validPaymentMethodsForType;
        }
    }

    private List<PaymentMethod> filterByPaymentType(final Iterable<String> excludedPaymentTypes,
        final List<PaymentMethod> guessingPaymentMethods) {
        if (excludedPaymentTypes == null) {
            return guessingPaymentMethods;
        }

        final List<PaymentMethod> paymentMethods = new ArrayList<>();
        for (final PaymentMethod p : guessingPaymentMethods) {
            for (final String paymentType : excludedPaymentTypes) {
                if (!paymentType.equals(p.getPaymentTypeId())) {
                    paymentMethods.add(p);
                }
            }
        }
        return paymentMethods;
    }
}
