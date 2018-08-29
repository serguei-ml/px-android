package com.mercadopago.android.px.model;

import android.support.annotation.Nullable;
import java.util.Date;

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class Card implements CardInformation {

    public static final int CARD_DEFAULT_IDENTIFICATION_NUMBER_LENGTH = 12;
    public static final int CARD_DEFAULT_SECURITY_CODE_LENGTH = 4;
    private static final String CARD_DEFAULT_SECURITY_CODE_LOCATION = "back";
    public static final Integer CARD_NUMBER_MAX_LENGTH = 16;

    private Cardholder cardHolder;
    private String customerId;
    private Date dateCreated;
    private Date dateLastUpdated;
    private Integer expirationMonth;
    private Integer expirationYear;
    private String firstSixDigits;
    private String id;
    private Issuer issuer;
    private String lastFourDigits;
    private PaymentMethod paymentMethod;
    private SecurityCode securityCode;

    @Override
    public Cardholder getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(final Cardholder cardHolder) {
        this.cardHolder = cardHolder;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(final String customerId) {
        this.customerId = customerId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(final Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateLastUpdated() {
        return dateLastUpdated;
    }

    public void setDateLastUpdated(final Date dateLastUpdated) {
        this.dateLastUpdated = dateLastUpdated;
    }

    @Override
    public Integer getExpirationMonth() {
        return expirationMonth;
    }

    public void setExpirationMonth(final Integer expirationMonth) {
        this.expirationMonth = expirationMonth;
    }

    @Override
    public Integer getExpirationYear() {
        return expirationYear;
    }

    public void setExpirationYear(final Integer expirationYear) {
        this.expirationYear = expirationYear;
    }

    @Override
    public String getFirstSixDigits() {
        return firstSixDigits;
    }

    public void setFirstSixDigits(final String firstSixDigits) {
        this.firstSixDigits = firstSixDigits;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Nullable
    public Issuer getIssuer() {
        return issuer;
    }

    public void setIssuer(@Nullable final Issuer issuer) {
        this.issuer = issuer;
    }

    @Nullable
    @Override
    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public void setLastFourDigits(@Nullable final String lastFourDigits) {
        this.lastFourDigits = lastFourDigits;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(@Nullable final PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public SecurityCode getSecurityCode() {
        return securityCode;
    }

    public void setSecurityCode(@Nullable final SecurityCode securityCode) {
        this.securityCode = securityCode;
    }

    public boolean isSecurityCodeRequired() {
        return securityCode != null && securityCode.getLength() != 0;
    }

    @Override
    public Integer getSecurityCodeLength() {
        return securityCode != null ? securityCode.getLength() : CARD_DEFAULT_SECURITY_CODE_LENGTH;
    }

    public String getSecurityCodeLocation() {
        return securityCode != null ? securityCode.getCardLocation() : CARD_DEFAULT_SECURITY_CODE_LOCATION;
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public String toString() {
        return "Card{" +
            "cardHolder=" + cardHolder +
            ", customerId='" + customerId + '\'' +
            ", dateCreated=" + dateCreated +
            ", dateLastUpdated=" + dateLastUpdated +
            ", expirationMonth=" + expirationMonth +
            ", expirationYear=" + expirationYear +
            ", firstSixDigits='" + firstSixDigits + '\'' +
            ", id='" + id + '\'' +
            ", issuer=" + issuer +
            ", lastFourDigits='" + lastFourDigits + '\'' +
            ", paymentMethod=" + paymentMethod +
            ", securityCode=" + securityCode +
            '}';
    }
}