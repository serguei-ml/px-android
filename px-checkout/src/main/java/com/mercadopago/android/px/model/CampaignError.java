package com.mercadopago.android.px.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import java.io.Serializable;

public class CampaignError implements Serializable, Parcelable {
    @NonNull
    private final String code;

    public CampaignError(@NonNull final String code) {
        this.code = code;
    }

    @NonNull
    public String getCode() {
        return code;
    }

    /* default */ CampaignError(final Parcel in) {
        code = in.readString();
    }

    public static final Creator<CampaignError> CREATOR = new Creator<CampaignError>() {
        @Override
        public CampaignError createFromParcel(final Parcel in) {
            return new CampaignError(in);
        }

        @Override
        public CampaignError[] newArray(final int size) {
            return new CampaignError[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(code);
    }
}
