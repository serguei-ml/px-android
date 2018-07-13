package com.mercadopago.android.px.uicontrollers.card;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.mercadopago.android.px.R;
import com.mercadopago.android.px.uicontrollers.identification.IdentificationView;
import com.mercadopago.android.px.util.MPCardMaskUtil;

public class IdentificationCardView extends IdentificationView {

    public IdentificationCardView(Context context) {
        super(context);
    }

    @Override
    public View inflateInParent(ViewGroup parent, boolean attachToRoot) {
        mView = LayoutInflater.from(mContext).inflate(R.layout.px_card_identification, parent, attachToRoot);
        return mView;
    }

    @Override
    public void draw() {
        //TODO se puede subir
        if (mIdentificationNumber == null || mIdentificationNumber.length() == 0) {
            mIdentificationNumberTextView.setVisibility(View.INVISIBLE);
            mBaseIdNumberView.setVisibility(View.VISIBLE);
        } else {
            mBaseIdNumberView.setVisibility(View.INVISIBLE);
            mIdentificationNumberTextView.setVisibility(View.VISIBLE);

            String number =
                MPCardMaskUtil.buildIdentificationNumberWithMask(mIdentificationNumber, mIdentificationType);
            mIdentificationNumberTextView.setTextColor(ContextCompat.getColor(mContext, NORMAL_TEXT_VIEW_COLOR));
            mIdentificationNumberTextView.setText(number);
        }
    }
}
