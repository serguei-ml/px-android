package com.mercadopago.android.px.internal.features;

import com.mercadopago.android.px.internal.base.MvpPresenter;
import com.mercadopago.android.px.internal.callbacks.TaggedCallback;
import com.mercadopago.android.px.internal.features.providers.DiscountsProvider;
import com.mercadopago.android.px.internal.util.ApiUtil;
import com.mercadopago.android.px.model.Discount;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import java.math.BigDecimal;

public class DiscountsPresenter extends MvpPresenter<DiscountsActivityView, DiscountsProvider> {

    private DiscountsActivityView mDiscountsView;

    //Activity parameters
    private String mPublicKey;
    private String mPayerEmail;
    private BigDecimal mTransactionAmount;
    private Discount mDiscount;

    @Override
    public void attachView(DiscountsActivityView discountsView) {
        mDiscountsView = discountsView;
    }

    public void initialize() {
        if (mDiscount == null) {
            initDiscountFlow();
        } else {
            mDiscountsView.drawSummary();
        }
    }

    private void initDiscountFlow() {
        mDiscountsView.requestDiscountCode();
    }

    private Boolean isTransactionAmountValid() {
        return mTransactionAmount != null && mTransactionAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    private void getCodeDiscount(final String discountCode) {
        mDiscountsView.showProgressBar();

        getResourcesProvider().getCodeDiscount(mTransactionAmount.toString(), mPayerEmail, discountCode,
            new TaggedCallback<Discount>(ApiUtil.RequestOrigin.GET_CODE_DISCOUNT) {
                @Override
                public void onSuccess(Discount discount) {
                    mDiscountsView.setSoftInputModeSummary();
                    mDiscountsView.hideKeyboard();
                    mDiscountsView.hideProgressBar();

                    mDiscount = discount;
                    mDiscountsView.drawSummary();
                }

                @Override
                public void onFailure(MercadoPagoError error) {
                    mDiscountsView.hideProgressBar();
                    if (error.isApiException()) {
                        String errorMessage =
                            getResourcesProvider().getApiErrorMessage(error.getApiException().getError());
                        mDiscountsView.showCodeInputError(errorMessage);
                    } else {
                        mDiscountsView.showCodeInputError(getResourcesProvider().getStandardErrorMessage());
                    }
                }
            });
    }

    public void validateDiscountCodeInput(String discountCode) {
        if (isTransactionAmountValid()) {
            if (isEmpty(discountCode)) {
                mDiscountsView.showEmptyDiscountCodeError();
            } else {
                getCodeDiscount(discountCode);
            }
        } else {
            mDiscountsView.finishWithCancelResult();
        }
    }

    public Discount getDiscount() {
        return mDiscount;
    }

    public void setMerchantPublicKey(String publicKey) {
        mPublicKey = publicKey;
    }

    public void setPayerEmail(String payerEmail) {
        mPayerEmail = payerEmail;
    }

    public void setDiscount(Discount discount) {
        mDiscount = discount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        mTransactionAmount = transactionAmount;
    }

    public String getCurrencyId() {
        return mDiscount.getCurrencyId();
    }

    public BigDecimal getTransactionAmount() {
        return mTransactionAmount;
    }

    public BigDecimal getCouponAmount() {
        return mDiscount.getCouponAmount();
    }

    public String getPublicKey() {
        return mPublicKey;
    }

    private boolean isEmpty(String discountCode) {
        return discountCode == null || discountCode.isEmpty();
    }
}
