package com.mercadopago.android.px.tracking.strategies;

import android.content.Context;
import com.mercadopago.android.px.tracking.model.AppInformation;
import com.mercadopago.android.px.tracking.model.Event;
import com.mercadopago.android.px.tracking.model.EventTrackIntent;
import com.mercadopago.android.px.tracking.services.MPTrackingService;
import java.util.ArrayList;
import java.util.List;

public class RealTimeTrackingStrategy extends TrackingStrategy {
    private final MPTrackingService trackingService;

    public RealTimeTrackingStrategy(MPTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @Override
    public void trackEvent(Event event, Context context) {
        List<Event> events = new ArrayList<>();
        events.add(event);
        //Adapt to service v2.
        AppInformation appInformation = getAppInformation().copy();
        appInformation.setFlowId(event.getFlowId());
        EventTrackIntent eventTrackIntent = new EventTrackIntent(appInformation, getDeviceInfo(), events);
        trackingService.trackEvents(getPublicKey(), eventTrackIntent, context);
    }

    @Override
    public boolean readsEventFromDB() {
        return false;
    }
}
