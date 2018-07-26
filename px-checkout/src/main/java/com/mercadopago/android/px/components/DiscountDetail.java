package com.mercadopago.android.px.components;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mercadopago.android.px.R;
import com.mercadopago.android.px.model.Campaign;
import com.mercadopago.android.px.model.Discount;
import com.mercadopago.android.px.util.textformatter.TextFormatter;

import java.util.Locale;

import javax.annotation.Nonnull;

public class DiscountDetail extends CompactComponent<DiscountDetail.Props, Void> {

    public static class Props {

        @NonNull
        private final Discount discount;
        @NonNull
        private final Campaign campaign;

        public Props(@NonNull final Discount discount, @NonNull final Campaign campaign) {
            this.discount = discount;
            this.campaign = campaign;
        }
    }

    public DiscountDetail(final Props props) {
        super(props);
    }

    @Override
    public View render(@Nonnull final ViewGroup parent) {
        final View mainContainer = inflate(parent, R.layout.px_view_discount_detail);
        configureSubtitleMessage(mainContainer);
        configureDetailMessage(mainContainer);
        configureSubDetailsMessage(mainContainer);
        return mainContainer;
    }

    private void configureSubDetailsMessage(View mainContainer) {
        if (props.campaign.isUsedUpDiscount()) {
            mainContainer.findViewById(R.id.px_discount_detail_line).setVisibility(View.GONE);
            mainContainer.findViewById(R.id.px_discount_sub_details).setVisibility(View.GONE);
        }
    }

    private void configureSubtitleMessage(final View mainContainer) {
        final TextView subtitleMessage = mainContainer.findViewById(R.id.subtitle);
        if (isMaxCouponAmountSubtitleApplicable()) {
            TextFormatter.withCurrencyId(props.discount.getCurrencyId())
                    .withSpace()
                    .amount(props.campaign.getMaxCouponAmount())
                    .normalDecimals()
                    .into(subtitleMessage)
                    .holder(R.string.px_max_coupon_amount);
        } else {
            subtitleMessage.setVisibility(View.GONE);
        }
    }

    private void configureDetailMessage(final View mainContainer) {
        final TextView detailTextView = mainContainer.findViewById(R.id.detail);
        if (props.campaign.hasMaxCouponAmount()) {
            if (props.campaign.isUsedUpDiscount()) {
                setDetailMessage(detailTextView, R.string.px_used_up_discount_detail, mainContainer);
            } else if (props.campaign.isAlwaysOnDiscount()) {
                setDetailMessage(detailTextView, R.string.px_always_on_discount_detail, mainContainer);
            } else {
                setDetailMessage(detailTextView, R.string.px_one_shot_discount_detail, mainContainer);
            }
        } else {
            detailTextView.setVisibility(View.GONE);
        }
    }

    private void setDetailMessage(TextView detailTextView, int detailId, View view) {
        String detailMessage = view.getResources().getString(detailId);

        if (isEndDateApplicable()) {
            String endDateMessage = view.getResources().getString(R.string.px_discount_detail_end_date,
                    props.campaign.getPrettyEndDate());
            detailTextView.setText(String.format(Locale.getDefault(), "%s %s", detailMessage, endDateMessage));
        } else {
            detailTextView.setText(detailMessage);
        }
    }

    private boolean isEndDateApplicable() {
        return props.campaign.hasEndDate() && !props.campaign.isUsedUpDiscount();
    }

    private boolean isMaxCouponAmountSubtitleApplicable() {
        return props.campaign.hasMaxCouponAmount() && !props.campaign.isUsedUpDiscount();
    }
}
