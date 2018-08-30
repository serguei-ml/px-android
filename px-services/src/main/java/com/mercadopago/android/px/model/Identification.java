package com.mercadopago.android.px.model;

import java.io.Serializable;

public class Identification implements Serializable {

    private String number;
    private String type;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(final String number) {
        this.number = number;
    }
}
