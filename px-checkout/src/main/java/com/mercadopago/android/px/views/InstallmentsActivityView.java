package com.mercadopago.android.px.views;

import android.support.annotation.NonNull;
import com.mercadopago.android.px.callbacks.OnSelectedCallback;
import com.mercadopago.android.px.exceptions.MercadoPagoError;
import com.mercadopago.android.px.internal.repository.DiscountRepository;
import com.mercadopago.android.px.model.Campaign;
import com.mercadopago.android.px.model.Discount;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.model.Site;
import com.mercadopago.android.px.mvp.MvpView;
import java.math.BigDecimal;
import java.util.List;

public interface InstallmentsActivityView extends MvpView {
    void showInstallments(List<PayerCost> payerCostList, OnSelectedCallback<Integer> onSelectedCallback);

    void finishWithResult(PayerCost payerCost);

    void showLoadingView();

    void hideLoadingView();

    void showError(MercadoPagoError error, String requestOrigin);

    void showHeader();

    void initInstallmentsReviewView(PayerCost payerCost);

    void hideInstallmentsRecyclerView();

    void showInstallmentsRecyclerView();

    void hideInstallmentsReviewView();

    void showInstallmentsReviewView();

    void warnAboutBankInterests();

    void showDetailDialog(@NonNull final Discount discount, @NonNull final Campaign campaign,
        final boolean notAvailableDiscount);

    void showDiscountInputDialog();

    void showAmount(@NonNull final DiscountRepository discountRepository,
        @NonNull final BigDecimal itemsPlusCharges,
        @NonNull final Site site);

    void onSuccessCodeDiscountCallback(Discount discount);

    void onFailureCodeDiscountCallback();
}
