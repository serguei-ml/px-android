package com.mercadopago;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import com.mercadopago.android.px.core.MercadoPagoCheckout;
import com.mercadopago.android.px.testcheckout.flows.CreditCardTestFlow;
import com.mercadopago.android.px.testcheckout.idleresources.CheckoutResource;
import com.mercadopago.android.px.testcheckout.input.Country;
import com.mercadopago.android.px.testcheckout.input.FakeCard;
import com.mercadopago.android.px.testcheckout.input.Visa;
import com.mercadopago.android.px.testcheckout.pages.CongratsPage;
import com.mercadopago.android.testlib.HttpResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DiscountTest {

    @Rule
    public HttpResource httpResource = new CheckoutResource();

    @Rule
    public ActivityTestRule<CheckoutExampleActivity> activityRule =
        new ActivityTestRule<>(CheckoutExampleActivity.class);

    private CreditCardTestFlow creditCardTestFlow;

    @Before
    public void setUp() {
        MercadoPagoCheckout.Builder builder =
            new MercadoPagoCheckout.Builder("APP_USR-b8925182-e1bf-4c0e-bc38-1d893a19ab45",
                "241261700-459d4126-903c-4bad-bc05-82e5f13fa7d3");
        creditCardTestFlow = new CreditCardTestFlow(builder.build(), activityRule.getActivity());
    }

    @Test
    public void withDirectDiscountFlowIsOk() {
        //TODO
        Visa card = new Visa(FakeCard.CardState.APRO, Country.ARGENTINA);
        CongratsPage congratsPage = creditCardTestFlow.runCreditCardPaymentFlowInstallmentsFirstOption(card);
        assertNotNull(congratsPage);
    }

    @Test
    public void withCodeDiscountFlowIsOk() {
        //TODO
    }
}
