package com.mercadopago.android.px.views;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;

import com.mercadolibre.android.ui.widgets.MeliDialog;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.components.DiscountDetailContainer;
import com.mercadopago.android.px.components.DiscountDetailContainer.Props.DialogTitleType;
import com.mercadopago.android.px.model.Campaign;
import com.mercadopago.android.px.model.CampaignError;
import com.mercadopago.android.px.model.Discount;

public class DiscountDetailDialog extends MeliDialog {

    private static final String TAG = DiscountDetailDialog.class.getName();
    private static final String ARG_DISCOUNT = "arg_discount";
    private static final String ARG_CAMPAIGN = "arg_campaign";
    private static final String ARG_CAMPAIGN_ERROR = "arg_campaign_error";

    public static void showDialog(@NonNull final Discount discount,
        @NonNull final Campaign campaign,
        @Nullable final CampaignError campaignError,
        final FragmentManager supportFragmentManager) {
        DiscountDetailDialog discountDetailDialog = new DiscountDetailDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_DISCOUNT, discount);
        bundle.putParcelable(ARG_CAMPAIGN, campaign);
        bundle.putParcelable(ARG_CAMPAIGN_ERROR, campaignError);
        discountDetailDialog.setArguments(bundle);
        discountDetailDialog.show(supportFragmentManager, TAG);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();

        if (args != null) {
            final Discount discount = args.getParcelable(ARG_DISCOUNT);
            final Campaign campaign = args.getParcelable(ARG_CAMPAIGN);
            final CampaignError campaignError = args.getParcelable(ARG_CAMPAIGN_ERROR);
            final ViewGroup container = view.findViewById(R.id.main_container);
            final DiscountDetailContainer discountDetailContainer = new DiscountDetailContainer(
                new DiscountDetailContainer.Props(DialogTitleType.BIG, discount, campaign, campaignError));
            discountDetailContainer.render(container);
        } else {
            dismiss();
        }
    }

    @Override
    public int getContentView() {
        return R.layout.px_dialog_detail_discount;
    }

    @Nullable
    @Override
    public String getSecondaryExitString() {
        return getString(R.string.px_terms_and_conditions);
    }
}
