package com.mercadopago.android.px.presenters;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.mercadopago.android.px.callbacks.FailureRecovery;
import com.mercadopago.android.px.controllers.CheckoutTimer;
import com.mercadopago.android.px.controllers.PaymentMethodGuessingController;
import com.mercadopago.android.px.exceptions.MercadoPagoError;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.GroupsRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.repository.UserSelectionRepository;
import com.mercadopago.android.px.model.BankDeal;
import com.mercadopago.android.px.model.Bin;
import com.mercadopago.android.px.model.CardToken;
import com.mercadopago.android.px.model.Cardholder;
import com.mercadopago.android.px.model.DifferentialPricing;
import com.mercadopago.android.px.model.Identification;
import com.mercadopago.android.px.model.IdentificationType;
import com.mercadopago.android.px.model.Installment;
import com.mercadopago.android.px.model.Issuer;
import com.mercadopago.android.px.model.PayerCost;
import com.mercadopago.android.px.model.PaymentMethod;
import com.mercadopago.android.px.model.PaymentMethodSearch;
import com.mercadopago.android.px.model.PaymentRecovery;
import com.mercadopago.android.px.model.PaymentType;
import com.mercadopago.android.px.model.SecurityCode;
import com.mercadopago.android.px.model.Setting;
import com.mercadopago.android.px.model.Token;
import com.mercadopago.android.px.mvp.MvpPresenter;
import com.mercadopago.android.px.mvp.TaggedCallback;
import com.mercadopago.android.px.preferences.PaymentPreference;
import com.mercadopago.android.px.providers.GuessingCardProvider;
import com.mercadopago.android.px.services.callbacks.Callback;
import com.mercadopago.android.px.services.exceptions.ApiException;
import com.mercadopago.android.px.services.exceptions.CardTokenException;
import com.mercadopago.android.px.tracker.MPTrackingContext;
import com.mercadopago.android.px.uicontrollers.card.CardView;
import com.mercadopago.android.px.uicontrollers.card.FrontCardView;
import com.mercadopago.android.px.util.ApiUtil;
import com.mercadopago.android.px.util.MPCardMaskUtil;
import com.mercadopago.android.px.views.GuessingCardActivityView;
import java.util.ArrayList;
import java.util.List;

public class GuessingCardPresenter extends MvpPresenter<GuessingCardActivityView, GuessingCardProvider> {

    public static final int CARD_DEFAULT_SECURITY_CODE_LENGTH = 4;
    public static final int CARD_DEFAULT_IDENTIFICATION_NUMBER_LENGTH = 12;
    @NonNull private final AmountRepository amountRepository;
    @NonNull private final UserSelectionRepository userSelectionRepository;
    @NonNull private final PaymentSettingRepository paymentSettingRepository;
    private final GroupsRepository groupsRepository;

    //Card controller
    private PaymentMethodGuessingController mPaymentMethodGuessingController;
    private List<IdentificationType> mIdentificationTypes;

    private FailureRecovery mFailureRecovery;

    //Activity parameters
    private String mPublicKey;
    private PaymentRecovery mPaymentRecovery;

    private Identification mIdentification;
    private boolean mIdentificationNumberRequired;
    private PaymentPreference mPaymentPreference;

    //Card Settings
    private int mSecurityCodeLength;
    private String mSecurityCodeLocation;
    private boolean mIsSecurityCodeRequired;
    private boolean mEraseSpace;

    //Card Info
    private String mBin;
    private String mCardNumber;
    private String mCardholderName;
    private String mExpiryMonth;
    private String mExpiryYear;
    private String mSecurityCode;
    private IdentificationType mIdentificationType;
    private String mIdentificationNumber;
    private CardToken mCardToken;
    private Token mToken;

    //Extra info
    private List<BankDeal> mBankDealsList;
    private boolean showPaymentTypes;
    private List<PaymentType> mPaymentTypesList;
    private Boolean mShowBankDeals;

    //Discount
    private String mPayerEmail;
    private String mPrivateKey;
    private int mCurrentNumberLength;
    private Issuer mIssuer;

    public GuessingCardPresenter(@NonNull final AmountRepository amountRepository,
        @NonNull final UserSelectionRepository userSelectionRepository,
        @NonNull final PaymentSettingRepository paymentSettingRepository,
        @NonNull final GroupsRepository groupsRepository) {
        this.amountRepository = amountRepository;
        this.userSelectionRepository = userSelectionRepository;
        this.paymentSettingRepository = paymentSettingRepository;
        this.groupsRepository = groupsRepository;
        mShowBankDeals = true;
        mEraseSpace = true;
    }

    public void initialize() {
        try {
            validateParameters();
            onValidStart();
        } catch (IllegalStateException exception) {
            getView().showError(new MercadoPagoError(exception.getMessage(), false), "");
        }
    }

    private boolean isTimerEnabled() {
        return CheckoutTimer.getInstance().isTimerEnabled();
    }

    private void validateParameters() throws IllegalStateException {
        if (mPublicKey == null) {
            throw new IllegalStateException(getResourcesProvider().getMissingPublicKeyErrorMessage());
        }
    }

    private void onValidStart() {
        initializeCardToken();
        getView().onValidStart();
        if (isTimerEnabled()) {
            getView().initializeTimer();
        } else {
            resolveBankDeals();
        }
        loadPaymentMethods();
        fillRecoveryFields();
    }

    public void setCurrentNumberLength(int currentNumberLength) {
        mCurrentNumberLength = currentNumberLength;
    }

    public FailureRecovery getFailureRecovery() {
        return mFailureRecovery;
    }

    public void setFailureRecovery(FailureRecovery failureRecovery) {
        mFailureRecovery = failureRecovery;
    }

    public String getPublicKey() {
        return mPublicKey;
    }

    public void setPublicKey(String publicKey) {
        mPublicKey = publicKey;
    }

    public PaymentRecovery getPaymentRecovery() {
        return mPaymentRecovery;
    }

    public void setPaymentRecovery(PaymentRecovery paymentRecovery) {
        mPaymentRecovery = paymentRecovery;
        if (recoverWithCardholder()) {
            saveCardholderName(paymentRecovery.getToken().getCardHolder().getName());
            saveIdentificationNumber(paymentRecovery.getToken().getCardHolder().getIdentification().getNumber());
        }
    }

    private void fillRecoveryFields() {
        if (recoverWithCardholder()) {
            getView().setCardholderName(mPaymentRecovery.getToken().getCardHolder().getName());
            getView()
                .setIdentificationNumber(mPaymentRecovery.getToken().getCardHolder().getIdentification().getNumber());
        }
    }

    private boolean recoverWithCardholder() {
        return mPaymentRecovery != null && mPaymentRecovery.getToken() != null &&
            mPaymentRecovery.getToken().getCardHolder() != null;
    }

    public PaymentMethod getPaymentMethod() {
        return userSelectionRepository.getPaymentMethod();
    }

    public List<IdentificationType> getIdentificationTypes() {
        return mIdentificationTypes;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        userSelectionRepository.select(paymentMethod);
        if (paymentMethod == null) {
            clearCardSettings();
        }
    }

    public boolean hasToShowPaymentTypes() {
        return showPaymentTypes;
    }

    public boolean isSecurityCodeRequired() {
        return mIsSecurityCodeRequired;
    }

    public void setSecurityCodeRequired(boolean required) {
        mIsSecurityCodeRequired = required;
    }

    public void setSecurityCodeLength(int securityCodeLength) {
        mSecurityCodeLength = securityCodeLength;
    }

    private void clearCardSettings() {
        mSecurityCodeLength = CARD_DEFAULT_SECURITY_CODE_LENGTH;
        mSecurityCodeLocation = CardView.CARD_SIDE_BACK;
        mIsSecurityCodeRequired = true;
        mBin = "";
    }

    public String getSecurityCodeLocation() {
        return mSecurityCodeLocation;
    }

    public int getSecurityCodeLength() {
        return mSecurityCodeLength;
    }

    public void setToken(Token token) {
        mToken = token;
    }

    public Token getToken() {
        return mToken;
    }

    public CardToken getCardToken() {
        return mCardToken;
    }

    public void setCardToken(CardToken cardToken) {
        mCardToken = cardToken;
    }

    public void setPaymentTypesList(List<PaymentType> paymentTypesList) {
        mPaymentTypesList = paymentTypesList;
    }

    public void setIdentificationTypesList(List<IdentificationType> identificationTypesList) {
        mIdentificationTypes = identificationTypesList;
    }

    public void setBankDealsList(List<BankDeal> bankDealsList) {
        mBankDealsList = bankDealsList;
    }

    public Identification getIdentification() {
        return mIdentification;
    }

    public void setIdentification(Identification identification) {
        mIdentification = identification;
    }

    public boolean isIdentificationNumberRequired() {
        return mIdentificationNumberRequired;
    }

    public void setIdentificationNumberRequired(boolean identificationNumberRequired) {
        mIdentificationNumberRequired = identificationNumberRequired;
        if (identificationNumberRequired) {
            getView().showIdentificationInput();
        }
    }

    public PaymentPreference getPaymentPreference() {
        return mPaymentPreference;
    }

    public void setPaymentPreference(PaymentPreference paymentPreference) {
        mPaymentPreference = paymentPreference;
    }

    private void initializeCardToken() {
        mCardToken = new CardToken("", null, null, "", "", "", "");
    }

    public String getSecurityCodeFront() {
        String securityCode = null;
        if (mSecurityCodeLocation.equals(CardView.CARD_SIDE_FRONT)) {
            securityCode = getSecurityCode();
        }
        return securityCode;
    }

    private boolean isCardLengthResolved() {
        return userSelectionRepository.getPaymentMethod() != null && mBin != null;
    }

    public Integer getCardNumberLength() {
        return PaymentMethodGuessingController.getCardNumberLength(userSelectionRepository.getPaymentMethod(), mBin);
    }

    public void initializeGuessingCardNumberController() {
        groupsRepository.getGroups().enqueue(new Callback<PaymentMethodSearch>() {
            @Override
            public void success(final PaymentMethodSearch paymentMethodSearch) {
                loadSupportedPaymentMethods(paymentMethodSearch);
            }

            @Override
            public void failure(final ApiException apiException) {
                finishCardFlow();
            }
        });
    }

    /* default */ void loadSupportedPaymentMethods(final PaymentMethodSearch paymentMethodSearch) {
        final List<PaymentMethod> supportedPaymentMethods =
            mPaymentPreference.getSupportedPaymentMethods(paymentMethodSearch.getPaymentMethods());
        mPaymentMethodGuessingController = new PaymentMethodGuessingController(
            supportedPaymentMethods, mPaymentPreference.getDefaultPaymentTypeId(),
            mPaymentPreference.getExcludedPaymentTypes());

        startGuessingForm();
    }

    public List<PaymentMethod> getAllSupportedPaymentMethods() {
        List<PaymentMethod> list = null;
        if (mPaymentMethodGuessingController != null) {
            list = mPaymentMethodGuessingController.getAllSupportedPaymentMethods();
        }
        return list;
    }

    private void startGuessingForm() {
        getView().initializeTitle();
        getView().setCardNumberListeners(mPaymentMethodGuessingController);
        getView().setCardholderNameListeners();
        getView().setExpiryDateListeners();
        getView().setSecurityCodeListeners();
        getView().setIdentificationTypeListeners();
        getView().setIdentificationNumberListeners();
        getView().setNextButtonListeners();
        getView().setBackButtonListeners();
        getView().setErrorContainerListener();
        getView().setContainerAnimationListeners();
        checkPaymentMethodsSupported(false);
    }

    private void checkPaymentMethodsSupported(final boolean withAnimation) {
        if (onlyOnePaymentMethodSupported()) {
            getView().setExclusionWithOneElementInfoView(getAllSupportedPaymentMethods().get(0), withAnimation);
        }
    }

    private boolean onlyOnePaymentMethodSupported() {
        List<PaymentMethod> supportedPaymentMethods = getAllSupportedPaymentMethods();
        return supportedPaymentMethods != null && supportedPaymentMethods.size() == 1;
    }

    private void setInvalidCardMessage() {
        if (onlyOnePaymentMethodSupported()) {
            getView().setInvalidCardOnePaymentMethodErrorView();
        } else {
            getView().setInvalidCardMultipleErrorView();
        }
    }

    public String getPaymentTypeId() {
        if (mPaymentMethodGuessingController == null) {
            if (mPaymentPreference == null) {
                return null;
            } else {
                return mPaymentPreference.getDefaultPaymentTypeId();
            }
        } else {
            return mPaymentMethodGuessingController.getPaymentTypeId();
        }
    }

    public void setPayerEmail(String payerEmail) {
        mPayerEmail = payerEmail;
    }

    public String getPayerEmail() {
        return mPayerEmail;
    }

    private void loadPaymentMethods() {
        getView().showInputContainer();
        initializeGuessingCardNumberController();
    }

    public void resolveBankDeals() {
        if (mShowBankDeals) {
            getBankDealsAsync();
        } else {
            getView().hideBankDeals();
        }
    }

    public void resolvePaymentMethodListSet(List<PaymentMethod> paymentMethodList, String bin) {
        saveBin(bin);
        if (paymentMethodList.isEmpty()) {
            getView().setCardNumberInputMaxLength(Bin.BIN_LENGTH);
            setInvalidCardMessage();
        } else if (paymentMethodList.size() == 1) {
            onPaymentMethodSet(paymentMethodList.get(0));
        } else if (shouldAskPaymentType(paymentMethodList)) {
            enablePaymentTypeSelection(paymentMethodList);
            onPaymentMethodSet(paymentMethodList.get(0));
        } else {
            onPaymentMethodSet(paymentMethodList.get(0));
        }
    }

    public void onPaymentMethodSet(PaymentMethod paymentMethod) {
        if (!userSelectionRepository.hasSelectedPaymentMethod()) {
            setPaymentMethod(paymentMethod);
            configureWithSettings(paymentMethod);
            loadIdentificationTypes(paymentMethod);
            getView().setPaymentMethod(paymentMethod);
        }
        getView().resolvePaymentMethodSet(paymentMethod);
    }

    public void resolvePaymentMethodCleared() {
        getView().clearErrorView();
        getView().hideRedErrorContainerView(true);
        getView().restoreBlackInfoContainerView();
        getView().clearCardNumberInputLength();

        if (!userSelectionRepository.hasSelectedPaymentMethod()) {
            return;
        }
        clearSpaceErasableSettings();
        getView().clearCardNumberEditTextMask();
        setPaymentMethod(null);
        getView().clearSecurityCodeEditText();
        initializeCardToken();
        setIdentificationNumberRequired(true);
        setSecurityCodeRequired(true);
        disablePaymentTypeSelection();
        getView().checkClearCardView();
        checkPaymentMethodsSupported(true);
    }

    public void setSelectedPaymentType(PaymentType paymentType) {
        if (mPaymentMethodGuessingController == null) {
            return;
        }
        for (PaymentMethod paymentMethod : mPaymentMethodGuessingController.getGuessedPaymentMethods()) {
            if (paymentMethod.getPaymentTypeId().equals(paymentType.getId())) {
                setPaymentMethod(paymentMethod);
            }
        }
    }

    public String getSavedBin() {
        return mBin;
    }

    public void saveBin(String bin) {
        mBin = bin;
        mPaymentMethodGuessingController.saveBin(bin);
    }

    private void configureWithSettings(final PaymentMethod paymentMethod) {
        if (paymentMethod != null) {
            mIsSecurityCodeRequired = paymentMethod.isSecurityCodeRequired(mBin);
            if (!mIsSecurityCodeRequired) {
                getView().hideSecurityCodeInput();
            }
            Setting setting = PaymentMethodGuessingController.getSettingByPaymentMethodAndBin(paymentMethod, mBin);
            if (setting == null) {
                getView()
                    .showError(
                        new MercadoPagoError(getResourcesProvider().getSettingNotFoundForBinErrorMessage(), false),
                        "");
            } else {
                int cardNumberLength = getCardNumberLength();
                int spaces = FrontCardView.CARD_DEFAULT_AMOUNT_SPACES;

                if (cardNumberLength == FrontCardView.CARD_NUMBER_DINERS_LENGTH ||
                    cardNumberLength == FrontCardView.CARD_NUMBER_AMEX_LENGTH ||
                    cardNumberLength == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_1_LENGTH) {
                    spaces = FrontCardView.CARD_AMEX_DINERS_AMOUNT_SPACES;
                } else if (cardNumberLength == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_2_LENGTH) {
                    spaces = FrontCardView.CARD_NUMBER_MAESTRO_SETTING_2_AMOUNT_SPACES;
                }
                getView().setCardNumberInputMaxLength(cardNumberLength + spaces);
                SecurityCode securityCode = setting.getSecurityCode();
                if (securityCode == null) {
                    mSecurityCodeLength = CARD_DEFAULT_SECURITY_CODE_LENGTH;
                    mSecurityCodeLocation = CardView.CARD_SIDE_BACK;
                } else {
                    mSecurityCodeLength = securityCode.getLength();
                    mSecurityCodeLocation = securityCode.getCardLocation();
                }
                getView().setSecurityCodeInputMaxLength(mSecurityCodeLength);
                getView().setSecurityCodeViewLocation(mSecurityCodeLocation);
            }
        }
    }

    private void loadIdentificationTypes(final PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return;
        }
        mIdentificationNumberRequired = paymentMethod.isIdentificationNumberRequired();
        if (mIdentificationNumberRequired) {
            getIdentificationTypesAsync();
        } else {
            getView().hideIdentificationInput();
        }
    }

    private void getIdentificationTypesAsync() {
        getResourcesProvider().getIdentificationTypesAsync(
            new TaggedCallback<List<IdentificationType>>(ApiUtil.RequestOrigin.GET_IDENTIFICATION_TYPES) {
                @Override
                public void onSuccess(List<IdentificationType> identificationTypes) {
                    resolveIdentificationTypes(identificationTypes);
                }

                @Override
                public void onFailure(MercadoPagoError error) {
                    if (isViewAttached()) {
                        getView().showError(error, ApiUtil.RequestOrigin.GET_IDENTIFICATION_TYPES);
                        setFailureRecovery(new FailureRecovery() {
                            @Override
                            public void recover() {
                                getIdentificationTypesAsync();
                            }
                        });
                    }
                }
            });
    }

    private void resolveIdentificationTypes(List<IdentificationType> identificationTypes) {
        if (identificationTypes.isEmpty()) {
            getView().showError(
                new MercadoPagoError(getResourcesProvider().getMissingIdentificationTypesErrorMessage(), false),
                ApiUtil.RequestOrigin.GET_IDENTIFICATION_TYPES);
        } else {
            mIdentificationType = identificationTypes.get(0);
            getView().initializeIdentificationTypes(identificationTypes);
            mIdentificationTypes = identificationTypes;
        }
    }

    public List<BankDeal> getBankDealsList() {
        return mBankDealsList;
    }

    private void getBankDealsAsync() {
        getResourcesProvider()
            .getBankDealsAsync(new TaggedCallback<List<BankDeal>>(ApiUtil.RequestOrigin.GET_BANK_DEALS) {
                @Override
                public void onSuccess(List<BankDeal> bankDeals) {
                    resolveBankDeals(bankDeals);
                }

                @Override
                public void onFailure(MercadoPagoError error) {
                    if (isViewAttached()) {
                        setFailureRecovery(new FailureRecovery() {
                            @Override
                            public void recover() {
                                getBankDealsAsync();
                            }
                        });
                    }
                }
            });
    }

    private void resolveBankDeals(List<BankDeal> bankDeals) {
        if (isViewAttached()) {
            if (bankDeals == null || bankDeals.isEmpty()) {
                getView().hideBankDeals();
            } else {
                mBankDealsList = bankDeals;
                getView().showBankDeals();
            }
        }
    }

    public void enablePaymentTypeSelection(List<PaymentMethod> paymentMethodList) {
        List<PaymentType> paymentTypesList = new ArrayList<>();
        for (PaymentMethod pm : paymentMethodList) {
            PaymentType type = new PaymentType(pm.getPaymentTypeId());
            paymentTypesList.add(type);
        }
        mPaymentTypesList = paymentTypesList;

        showPaymentTypes = true;
    }

    public void disablePaymentTypeSelection() {
        showPaymentTypes = false;
        mPaymentTypesList = null;
    }

    public PaymentMethodGuessingController getGuessingController() {
        return mPaymentMethodGuessingController;
    }

    public List<PaymentMethod> getGuessedPaymentMethods() {
        if (mPaymentMethodGuessingController == null) {
            return null;
        }
        return mPaymentMethodGuessingController.getGuessedPaymentMethods();
    }

    public List<PaymentType> getPaymentTypes() {
        return mPaymentTypesList;
    }

    public void saveCardNumber(String cardNumber) {
        mCardNumber = cardNumber;
    }

    public void saveCardholderName(String cardholderName) {
        mCardholderName = cardholderName;
    }

    public void saveExpiryMonth(String expiryMonth) {
        mExpiryMonth = expiryMonth;
    }

    public void saveExpiryYear(String expiryYear) {
        mExpiryYear = expiryYear;
    }

    public void saveSecurityCode(String securityCode) {
        mSecurityCode = securityCode;
    }

    public void saveIdentificationNumber(String identificationNumber) {
        mIdentificationNumber = identificationNumber;
    }

    public void saveIdentificationType(IdentificationType identificationType) {
        mIdentificationType = identificationType;
        if (identificationType != null) {
            mIdentification.setType(identificationType.getId());
            getView().setIdentificationNumberRestrictions(identificationType.getType());
        }
    }

    public IdentificationType getIdentificationType() {
        return mIdentificationType;
    }

    public void setIdentificationNumber(String number) {
        mIdentificationNumber = number;
        mIdentification.setNumber(number);
    }

    public String getCardNumber() {
        return mCardNumber;
    }

    public void setCardNumber(String cardNumber) {
        mCardNumber = cardNumber;
    }

    public String getCardholderName() {
        return mCardholderName;
    }

    public void setCardholderName(String name) {
        mCardholderName = name;
    }

    public String getExpiryMonth() {
        return mExpiryMonth;
    }

    public String getExpiryYear() {
        return mExpiryYear;
    }

    public void setExpiryMonth(String expiryMonth) {
        mExpiryMonth = expiryMonth;
    }

    public void setExpiryYear(String expiryYear) {
        mExpiryYear = expiryYear;
    }

    public String getSecurityCode() {
        return mSecurityCode;
    }

    public String getIdentificationNumber() {
        return mIdentificationNumber;
    }

    public int getIdentificationNumberMaxLength() {
        int maxLength = CARD_DEFAULT_IDENTIFICATION_NUMBER_LENGTH;
        if (mIdentificationType != null) {
            maxLength = mIdentificationType.getMaxLength();
        }
        return maxLength;
    }

    public boolean validateCardNumber() {
        mCardToken.setCardNumber(getCardNumber());
        try {
            final PaymentMethod paymentMethod = userSelectionRepository.getPaymentMethod();
            if (paymentMethod == null) {
                if (getCardNumber() == null || getCardNumber().length() < Bin.BIN_LENGTH) {
                    throw new CardTokenException(CardTokenException.INVALID_CARD_NUMBER_INCOMPLETE);
                } else if (getCardNumber().length() == Bin.BIN_LENGTH) {
                    throw new CardTokenException(CardTokenException.INVALID_PAYMENT_METHOD);
                } else {
                    throw new CardTokenException(CardTokenException.INVALID_PAYMENT_METHOD);
                }
            }
            mCardToken.validateCardNumber(paymentMethod);
            getView().clearErrorView();
            return true;
        } catch (CardTokenException e) {
            getView().setErrorView(e);
            getView().setErrorCardNumber();
            return false;
        }
    }

    public boolean validateCardName() {
        Cardholder cardHolder = new Cardholder();
        cardHolder.setName(getCardholderName());
        cardHolder.setIdentification(mIdentification);
        mCardToken.setCardholder(cardHolder);
        if (mCardToken.validateCardholderName()) {
            getView().clearErrorView();
            return true;
        } else {
            getView().setErrorView(getResourcesProvider().getInvalidEmptyNameErrorMessage());
            getView().setErrorCardholderName();
            return false;
        }
    }

    public boolean validateExpiryDate() {
        String monthString = getExpiryMonth();
        String yearString = getExpiryYear();
        Integer month = (monthString == null || monthString.isEmpty()) ? null : Integer.valueOf(monthString);
        Integer year = (yearString == null || yearString.isEmpty()) ? null : Integer.valueOf(yearString);
        mCardToken.setExpirationMonth(month);
        mCardToken.setExpirationYear(year);
        if (mCardToken.validateExpiryDate()) {
            getView().clearErrorView();
            return true;
        } else {
            getView().setErrorView(getResourcesProvider().getInvalidExpiryDateErrorMessage());
            getView().setErrorExpiryDate();
            return false;
        }
    }

    public boolean validateSecurityCode() {
        mCardToken.setSecurityCode(getSecurityCode());
        try {
            mCardToken.validateSecurityCode(userSelectionRepository.getPaymentMethod());
            getView().clearErrorView();
            return true;
        } catch (CardTokenException e) {
            setCardSecurityCodeErrorView(e);
            return false;
        }
    }

    private void setCardSecurityCodeErrorView(CardTokenException exception) {
        if (!isSecurityCodeRequired()) {
            return;
        }
        getView().setErrorView(exception);
        getView().setErrorSecurityCode();
    }

    public boolean validateIdentificationNumber() {
        mIdentification.setNumber(getIdentificationNumber());
        mCardToken.getCardholder().setIdentification(mIdentification);
        boolean ans = mCardToken.validateIdentificationNumber(mIdentificationType);
        if (ans) {
            getView().clearErrorView();
            getView().clearErrorIdentificationNumber();
        } else {
            setCardIdentificationErrorView(getResourcesProvider().getInvalidIdentificationNumberErrorMessage());
        }
        return ans;
    }

    private void setCardIdentificationErrorView(String message) {
        getView().setErrorView(message);
        getView().setErrorIdentificationNumber();
    }

    public boolean checkIsEmptyOrValidCardholderName() {
        return TextUtils.isEmpty(mCardholderName) || validateCardName();
    }

    public boolean checkIsEmptyOrValidExpiryDate() {
        return TextUtils.isEmpty(mExpiryMonth) || validateExpiryDate();
    }

    public boolean checkIsEmptyOrValidSecurityCode() {
        return TextUtils.isEmpty(mSecurityCode) || validateSecurityCode();
    }

    public boolean checkIsEmptyOrValidIdentificationNumber() {
        return TextUtils.isEmpty(mIdentificationNumber) || validateIdentificationNumber();
    }

    public void recoverFromFailure() {
        if (mFailureRecovery != null) {
            mFailureRecovery.recover();
        }
    }

    public void setShowBankDeals(Boolean showBankDeals) {
        mShowBankDeals = showBankDeals;
    }

    public boolean isDefaultSpaceErasable() {

        if (MPCardMaskUtil.isDefaultSpaceErasable(mCurrentNumberLength)) {
            mEraseSpace = true;
        }

        if (isCardLengthResolved() && mEraseSpace &&
            (getCardNumberLength() == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_1_LENGTH ||
                getCardNumberLength() == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_2_LENGTH)) {
            mEraseSpace = false;
            return true;
        }
        return false;
    }

    public void setPrivateKey(String privateKey) {
        mPrivateKey = privateKey;
    }

    public String getPrivateKey() {
        return mPrivateKey;
    }

    public void clearSpaceErasableSettings() {
        mEraseSpace = true;
    }

    public void finishCardFlow() {
        createToken();
    }

    private void createToken() {
        getResourcesProvider()
            .createTokenAsync(mCardToken, new TaggedCallback<Token>(ApiUtil.RequestOrigin.CREATE_TOKEN) {
                @Override
                public void onSuccess(Token token) {
                    resolveTokenRequest(token);
                }

                @Override
                public void onFailure(MercadoPagoError error) {
                    resolveTokenCreationError(error, ApiUtil.RequestOrigin.CREATE_TOKEN);
                }
            });
    }

    public void resolveTokenRequest(Token token) {
        mToken = token;
        getIssuers();
    }

    private void resolveTokenCreationError(MercadoPagoError error, String requestOrigin) {
        if (wrongIdentificationNumber(error)) {
            showIdentificationNumberError();
        } else {
            setFailureRecovery(new FailureRecovery() {
                @Override
                public void recover() {
                    createToken();
                }
            });
            getView().showError(error, requestOrigin);
        }
    }

    private boolean wrongIdentificationNumber(MercadoPagoError error) {
        boolean answer = false;
        if (error.isApiException()) {
            ApiException apiException = error.getApiException();
            answer = apiException.containsCause(ApiException.ErrorCodes.INVALID_CARD_HOLDER_IDENTIFICATION_NUMBER);
        }
        return answer;
    }

    private void showIdentificationNumberError() {
        getView().hideProgress();
        getView().setErrorView(getResourcesProvider().getInvalidFieldErrorMessage());
        getView().setErrorIdentificationNumber();
    }

    private void getIssuers() {

        getResourcesProvider().getIssuersAsync(userSelectionRepository.getPaymentMethod().getId(), mBin,
            new TaggedCallback<List<Issuer>>(ApiUtil.RequestOrigin.GET_ISSUERS) {
                @Override
                public void onSuccess(List<Issuer> issuers) {
                    resolveIssuersList(issuers);
                }

                @Override
                public void onFailure(MercadoPagoError error) {
                    setFailureRecovery(new FailureRecovery() {
                        @Override
                        public void recover() {
                            getIssuers();
                        }
                    });
                    getView().showError(error, ApiUtil.RequestOrigin.GET_ISSUERS);
                }
            });
    }

    private void resolveIssuersList(List<Issuer> issuers) {
        if (issuers.size() == 1) {
            mIssuer = issuers.get(0);
            getInstallments();
        } else {
            getView().finishCardFlow(userSelectionRepository.getPaymentMethod(), mToken, issuers);
        }
    }

    private void getInstallments() {

        final DifferentialPricing differentialPricing =
            paymentSettingRepository.getCheckoutPreference().getDifferentialPricing();
        final Integer differentialPricingId = differentialPricing == null ? null : differentialPricing.getId();

        getResourcesProvider().getInstallmentsAsync(mBin, amountRepository.getAmountToPay(), mIssuer.getId(),
            userSelectionRepository.getPaymentMethod().getId(), differentialPricingId,
                new TaggedCallback<List<Installment>>(ApiUtil.RequestOrigin.GET_INSTALLMENTS) {
                    @Override
                    public void onSuccess(List<Installment> installments) {
                        resolveInstallments(installments);
                    }

                    @Override
                    public void onFailure(MercadoPagoError error) {
                        setFailureRecovery(new FailureRecovery() {
                            @Override
                            public void recover() {
                                getInstallments();
                            }
                        });
                        getView().showError(error, ApiUtil.RequestOrigin.GET_INSTALLMENTS);
                    }
                });
    }

    private void resolveInstallments(List<Installment> installments) {
        String errorMessage = null;
        if (installments == null || installments.size() == 0) {
            errorMessage = getResourcesProvider().getMissingInstallmentsForIssuerErrorMessage();
        } else if (installments.size() == 1) {
            resolvePayerCosts(installments.get(0).getPayerCosts());
        } else {
            errorMessage = getResourcesProvider().getMultipleInstallmentsForIssuerErrorMessage();
        }
        if (errorMessage != null && isViewAttached()) {
            getView().showError(new MercadoPagoError(errorMessage, false), ApiUtil.RequestOrigin.GET_INSTALLMENTS);
        }
    }

    private void resolvePayerCosts(List<PayerCost> payerCosts) {
        PayerCost defaultPayerCost = mPaymentPreference.getDefaultInstallments(payerCosts);
        if (defaultPayerCost != null) {
            userSelectionRepository.select(defaultPayerCost);
            getView().finishCardFlow(userSelectionRepository.getPaymentMethod(), mToken, mIssuer,
                defaultPayerCost);
        } else if (payerCosts.isEmpty()) {
            getView().showError(new MercadoPagoError(getResourcesProvider().getMissingPayerCostsErrorMessage(), false),
                ApiUtil.RequestOrigin.GET_INSTALLMENTS);
        } else if (payerCosts.size() == 1) {
            final PayerCost payerCost = payerCosts.get(0);
            userSelectionRepository.select(payerCost);
            getView().finishCardFlow(userSelectionRepository.getPaymentMethod(), mToken, mIssuer,
                payerCost);
        } else {
            getView().finishCardFlow(userSelectionRepository.getPaymentMethod(), mToken, mIssuer, payerCosts);
        }
    }

    public MPTrackingContext getTrackingContext() {
        return getResourcesProvider().getTrackingContext();
    }

    public void checkFinishWithCardToken() {
        if (hasToShowPaymentTypes() && getGuessedPaymentMethods() != null) {
            getView().askForPaymentType();
        } else {
            getView().showFinishCardFlow();
        }
    }

    public boolean shouldAskPaymentType(List<PaymentMethod> paymentMethodList) {

        boolean paymentTypeUndefined = false;
        String paymentType;

        if (paymentMethodList == null || paymentMethodList.isEmpty()) {
            paymentTypeUndefined = true;
        } else {
            paymentType = paymentMethodList.get(0).getPaymentTypeId();
            for (PaymentMethod currentPaymentMethod : paymentMethodList) {
                if (!paymentType.equals(currentPaymentMethod.getPaymentTypeId())) {
                    paymentTypeUndefined = true;
                    break;
                }
            }
        }
        return paymentTypeUndefined;
    }
}
