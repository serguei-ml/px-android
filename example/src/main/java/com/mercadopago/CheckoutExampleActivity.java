package com.mercadopago;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import com.mercadopago.android.px.core.MercadoPagoCheckout;
import com.mercadopago.android.px.customviews.MPButton;
import com.mercadopago.android.px.services.core.Settings;
import com.mercadopago.android.px.tracking.constants.TrackingEnvironments;
import com.mercadopago.android.px.utils.ExamplesUtils;
import com.mercadopago.example.R;

import static com.mercadopago.android.px.utils.ExamplesUtils.resolveCheckoutResult;

public class CheckoutExampleActivity extends AppCompatActivity {

    private ProgressBar mProgressBar;
    private View mRegularLayout;
    private MPButton continueSimpleCheckout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog()
            .build());

        Settings.setTrackingEnvironment(TrackingEnvironments.STAGING);

        setContentView(R.layout.activity_checkout_example);
        mProgressBar = findViewById(R.id.progressBar);
        mRegularLayout = findViewById(R.id.regularLayout);

        final View jsonConfigurationButton = findViewById(R.id.jsonConfigButton);
        continueSimpleCheckout = findViewById(R.id.continueButton);

        final View selectCheckoutButton = findViewById(R.id.select_checkout);

        jsonConfigurationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startJsonInput();
            }
        });

        selectCheckoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                startActivity(new Intent(CheckoutExampleActivity.this, SelectCheckoutActivity.class));
            }
        });

        continueSimpleCheckout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onContinueClicked();
            }
        });
    }

    private void onContinueClicked() {
        MercadoPagoCheckout.Builder builder = ExamplesUtils.createBase();
        builder.build().startForPayment(this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        resolveCheckoutResult(this, requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        showRegularLayout();
    }

    private void showRegularLayout() {
        mProgressBar.setVisibility(View.GONE);
        mRegularLayout.setVisibility(View.VISIBLE);
    }

    private void startJsonInput() {
        Intent intent = new Intent(this, JsonSetupActivity.class);
        startActivityForResult(intent, MercadoPagoCheckout.CHECKOUT_REQUEST_CODE);
    }
}