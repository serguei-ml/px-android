package com.mercadopago.android.px.model;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.mercadopago.android.px.model.exceptions.CardTokenException;
import java.util.Calendar;
import java.util.Locale;

public class CardToken {

    private static final int MIN_LENGTH_NUMBER = 10;
    private static final int MAX_LENGTH_NUMBER = 19;
    @SuppressWarnings("UseOfObsoleteDateTimeApi") private static final Calendar NOW = Calendar.getInstance();

    private Cardholder cardholder;
    private String cardNumber;
    private Device device;
    private Integer expirationMonth;
    private Integer expirationYear;
    private String securityCode;

    public CardToken(final String cardNumber, @Nullable final Integer expirationMonth, @Nullable final Integer expirationYear,
        final String securityCode, final String cardholderName, final String identificationType,
        final String identificationNumber) {
        this.cardNumber = normalizeCardNumber(cardNumber);
        this.expirationMonth = expirationMonth;
        this.expirationYear = normalizeYear(expirationYear);
        this.securityCode = securityCode;
        cardholder = new Cardholder();
        cardholder.setName(cardholderName);
        final Identification identification = new Identification();
        identification.setNumber(identificationNumber);
        identification.setType(identificationType);
        cardholder.setIdentification(identification);
    }

    public static boolean validateSecurityCode(final CharSequence securityCode) {
        return securityCode == null ||
            (!TextUtils.isEmpty(securityCode) && securityCode.length() >= 3 && securityCode.length() <= 4);
    }

    public static void validateSecurityCode(final String securityCode, final PaymentMethod paymentMethod,
        final String bin) throws CardTokenException {

        if (paymentMethod != null) {
            final Setting setting = Setting.getSettingByBin(paymentMethod.getSettings(), bin);

            // Validate security code length
            if (setting != null) {
                final int cvvLength = setting.getSecurityCode().getLength();
                if ((securityCode == null) || ((cvvLength != 0) && (securityCode.trim().length() != cvvLength))) {
                    throw new CardTokenException(CardTokenException.INVALID_CVV_LENGTH, String.valueOf(cvvLength));
                }
            } else {
                throw new CardTokenException(CardTokenException.INVALID_FIELD);
            }
        }
    }

    public static boolean validateExpiryDate(final Integer month, final Integer year) {
        return validateExpMonth(month) && validateExpYear(year) && !hasMonthPassed(month, year);
    }

    @SuppressWarnings("MagicNumber")
    private static boolean validateExpMonth(final Integer month) {
        return month != null && (month >= 1 && month <= 12);
    }

    private static boolean validateExpYear(final Integer year) {
        return year != null && !hasYearPassed(year);
    }

    public static boolean checkLuhn(final String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        if ((cardNumber == null) || (cardNumber.isEmpty())) {
            return false;
        }
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private static boolean hasYearPassed(final int year) {
        final int normalized = normalizeYear(year);
        return normalized < NOW.get(Calendar.YEAR);
    }

    private static boolean hasMonthPassed(final int month, final int year) {
        return hasYearPassed(year) ||
            normalizeYear(year) == NOW.get(Calendar.YEAR) && month < (NOW.get(Calendar.MONTH) + 1);
    }

    private static Integer normalizeYear(final Integer year) {
        Integer normalizedYear = year;
        if ((year != null) && (year < 100 && year >= 0)) {
            final String currentYear = String.valueOf(NOW.get(Calendar.YEAR));
            final String prefix = currentYear.substring(0, currentYear.length() - 2);
            normalizedYear = Integer.parseInt(String.format(Locale.US, "%s%02d", prefix, year));
        }
        return normalizedYear;
    }

    public Cardholder getCardholder() {
        return cardholder;
    }

    public void setCardholder(final Cardholder cardholder) {
        this.cardholder = cardholder;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(final String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(final Context context) {
        device = new Device(context);
    }

    public Integer getExpirationMonth() {
        return expirationMonth;
    }

    public void setExpirationMonth(@Nullable final Integer expirationMonth) {
        this.expirationMonth = expirationMonth;
    }

    public Integer getExpirationYear() {
        return expirationYear;
    }

    public void setExpirationYear(@Nullable final Integer expirationYear) {
        this.expirationYear = CardToken.normalizeYear(expirationYear);
    }

    public String getSecurityCode() {
        return securityCode;
    }

    public void setSecurityCode(final String securityCode) {
        this.securityCode = securityCode;
    }

    public boolean validate(final boolean includeSecurityCode) {
        boolean result =
            validateCardNumber() && validateExpiryDate() && validateIdentification() && validateCardholderName();
        if (includeSecurityCode) {
            result = result && validateSecurityCode();
        }
        return result;
    }

    public boolean validateCardNumber() {
        return !TextUtils.isEmpty(cardNumber) && (cardNumber.length() > MIN_LENGTH_NUMBER) &&
            (cardNumber.length() < MAX_LENGTH_NUMBER);
    }

    public void validateCardNumber(final PaymentMethod paymentMethod) throws CardTokenException {
        // Empty field
        if (cardNumber == null || cardNumber.isEmpty()) {
            throw new CardTokenException(CardTokenException.INVALID_EMPTY_CARD);
        }

        final Setting setting = Setting.getSettingByBin(paymentMethod.getSettings(), (cardNumber.length()
            >= Bin.BIN_LENGTH ? cardNumber.substring(0, Bin.BIN_LENGTH) : ""));

        if (setting == null) {
            // Invalid bin
            throw new CardTokenException(CardTokenException.INVALID_CARD_BIN);
        } else {
            // Validate cards length
            final int cardLength = setting.getCardNumber().getLength();
            if (cardNumber.trim().length() != cardLength) {

                throw new CardTokenException(CardTokenException.INVALID_CARD_LENGTH, String.valueOf(cardLength));
            }

            // Validate luhn
            final String luhnAlgorithm = setting.getCardNumber().getValidation();
            if (("standard".equals(luhnAlgorithm)) && (!checkLuhn(cardNumber))) {
                throw new CardTokenException(CardTokenException.INVALID_CARD_LUHN);
            }
        }
    }

    public boolean validateSecurityCode() {
        return validateSecurityCode(securityCode);
    }

    public void validateSecurityCode(@Nullable final PaymentMethod paymentMethod) throws CardTokenException {
        validateSecurityCode(securityCode, paymentMethod, (((cardNumber != null) ?
            cardNumber.length() : 0) >= Bin.BIN_LENGTH ?
            cardNumber.substring(0, Bin.BIN_LENGTH) : ""));
    }

    public boolean validateIdentificationNumber(final IdentificationType identificationType) {
        if (identificationType != null) {
            if ((cardholder != null) &&
                (cardholder.getIdentification() != null) &&
                (cardholder.getIdentification().getNumber() != null)) {
                final int len = cardholder.getIdentification().getNumber().length();
                final Integer min = identificationType.getMinLength();
                final Integer max = identificationType.getMaxLength();
                if ((min != null) && (max != null)) {
                    return ((len <= max) && (len >= min));
                } else {
                    return validateIdentificationNumber();
                }
            } else {
                return false;
            }
        } else {
            return validateIdentificationNumber();
        }
    }

    public boolean validateExpiryDate() {
        return validateExpiryDate(expirationMonth, expirationYear);
    }

    public boolean validateIdentification() {
        return validateIdentificationType() && validateIdentificationNumber();
    }

    private boolean validateIdentificationType() {
        return cardholder != null &&
            (cardholder.getIdentification() != null && !TextUtils.isEmpty(cardholder.getIdentification().getType()));
    }

    public boolean validateIdentificationNumber() {
        return cardholder != null && (cardholder.getIdentification() != null &&
            (validateIdentificationType() && !TextUtils.isEmpty(cardholder.getIdentification().getNumber())));
    }

    public boolean validateCardholderName() {
        return cardholder != null && cardholder.getName() != null && !cardholder.getName().isEmpty();
    }

    private String normalizeCardNumber(final String number) {
        return number.trim().replaceAll("\\s+|-", "");
    }
}
