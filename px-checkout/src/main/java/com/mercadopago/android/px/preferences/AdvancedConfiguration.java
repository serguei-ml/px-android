package com.mercadopago.android.px.preferences;

import android.support.annotation.NonNull;
import java.io.Serializable;

/**
 * Advanced configuration provides you support for custom checkout functionality/configure special behaviour
 * when checkout is running.
 */
public class AdvancedConfiguration implements Serializable {

    private final boolean bankDealsEnabled;
    private final boolean escEnabled;
    @NonNull private final PaymentResultScreenPreference paymentResultScreenPreference;
    @NonNull private final ReviewAndConfirmPreferences reviewAndConfirmPreferences;


    /* default */ AdvancedConfiguration(final Builder builder) {
        bankDealsEnabled = builder.bankDealsEnabled;
        escEnabled = builder.escEnabled;
        paymentResultScreenPreference = builder.paymentResultScreenPreference;
        reviewAndConfirmPreferences = builder.reviewAndConfirmPreferences;
    }

    public boolean isBankDealsEnabled() {
        return bankDealsEnabled;
    }

    public boolean isEscEnabled() {
        return escEnabled;
    }

    @NonNull
    public PaymentResultScreenPreference getPaymentResultScreenPreference() {
        return paymentResultScreenPreference;
    }

    @NonNull
    public ReviewAndConfirmPreferences getReviewAndConfirmPreferences() {
        return reviewAndConfirmPreferences;
    }

    @SuppressWarnings("unused")
    public static class Builder {
        /* default */ boolean bankDealsEnabled = true;
        /* default */ boolean escEnabled = false;
        /* default */ @NonNull PaymentResultScreenPreference paymentResultScreenPreference =
            new PaymentResultScreenPreference.Builder().build();
        /* default */ @NonNull ReviewAndConfirmPreferences reviewAndConfirmPreferences =
            new ReviewAndConfirmPreferences.Builder().build();

        /**
         * Add the possibility to configure Bank's deals behaviour.
         * If set as true, then the checkout will try to retrieve bank deals.
         * If set as false, then the checkout will not try to retrieve bank deals.
         *
         * @param bankDealsEnabled bool that reflects it's behaviour
         * @return builder to keep operating
         */
        public Builder setBankDealsEnabled(final boolean bankDealsEnabled) {
            this.bankDealsEnabled = bankDealsEnabled;
            return this;
        }

        /**
         * Add the possibility to configure ESC behaviour.
         * If set as true, then saved cards will try to use ESC feature.
         * If set as false, then security code will be always asked.
         *
         * @param escEnabled bool that reflects it's behaviour
         * @return builder to keep operating
         */
        public Builder setEscEnabled(final boolean escEnabled) {
            this.escEnabled = escEnabled;
            return this;
        }

        /**
         * Enable to preset configurations to customize visualization on the
         * 'Congrats' screen / 'PaymentResult' screen.
         * see {@link PaymentResultScreenPreference.Builder}
         *
         * @param paymentResultScreenPreference your custom preferences.
         * @return builder to keep operating
         */
        public Builder setPaymentResultScreenPreference(
            @NonNull final PaymentResultScreenPreference paymentResultScreenPreference) {
            this.paymentResultScreenPreference = paymentResultScreenPreference;
            return this;
        }

        /**
         * Enable to preset configurations to customize visualization on
         * the 'Review and Confirm screen' see {@link ReviewAndConfirmPreferences.Builder}
         *
         * @param reviewAndConfirmPreferences your custom preferences.
         * @return builder to keep operating
         */
        public Builder setReviewAndConfirmPreferences(
            @NonNull final ReviewAndConfirmPreferences reviewAndConfirmPreferences) {
            this.reviewAndConfirmPreferences = reviewAndConfirmPreferences;
            return this;
        }

        public AdvancedConfiguration build() {
            return new AdvancedConfiguration(this);
        }
    }
}
