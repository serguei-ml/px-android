package com.mercadopago.android.px.internal.features.card;

import android.text.Editable;
import android.text.TextWatcher;
import com.mercadopago.android.px.internal.callbacks.PaymentMethodSelectionCallback;
import com.mercadopago.android.px.internal.callbacks.card.CardNumberEditTextCallback;
import com.mercadopago.android.px.internal.controllers.PaymentMethodGuessingController;
import com.mercadopago.android.px.model.Bin;
import com.mercadopago.android.px.model.PaymentMethod;
import java.util.List;

public class CardNumberTextWatcher implements TextWatcher {

    private final PaymentMethodGuessingController mController;
    private final PaymentMethodSelectionCallback mPaymentSelectionCallback;
    private final CardNumberEditTextCallback mEditTextCallback;

    public CardNumberTextWatcher(final PaymentMethodGuessingController controller,
        final PaymentMethodSelectionCallback paymentSelectionCallback,
        final CardNumberEditTextCallback editTextCallback) {
        mController = controller;
        mPaymentSelectionCallback = paymentSelectionCallback;
        mEditTextCallback = editTextCallback;
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        //Do nothing
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        mEditTextCallback.checkOpenKeyboard();
        mEditTextCallback.saveCardNumber(s.toString().replaceAll("\\s", ""));
        if (before == 0) {
            mEditTextCallback.appendSpace(s);
        }
        if (before == 1) {
            mEditTextCallback.deleteChar(s);
        }
    }

    @Override
    public void afterTextChanged(final Editable s) {
        mEditTextCallback.changeErrorView();
        mEditTextCallback.toggleLineColorOnError(false);
        if (mController == null) {
            return;
        }
        final String number = s.toString().replaceAll("\\s", "");
        if (number.length() == Bin.BIN_LENGTH - 1) {
            mPaymentSelectionCallback.onPaymentMethodCleared();
        } else if (number.length() == Bin.BIN_LENGTH) {
            final String mBin = number.subSequence(0, Bin.BIN_LENGTH).toString();
            final List<PaymentMethod> list = mController.guessPaymentMethodsByBin(mBin);
            mPaymentSelectionCallback.onPaymentMethodListSet(list, mBin);
        }
    }
}
