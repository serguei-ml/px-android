package com.mercadopago.android.px.internal.features.guessing_card;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.mercadopago.android.px.configuration.AdvancedConfiguration;
import com.mercadopago.android.px.internal.base.MvpPresenter;
import com.mercadopago.android.px.internal.callbacks.FailureRecovery;
import com.mercadopago.android.px.internal.callbacks.TaggedCallback;
import com.mercadopago.android.px.internal.controllers.PaymentMethodGuessingController;
import com.mercadopago.android.px.internal.features.providers.GuessingCardProvider;
import com.mercadopago.android.px.internal.features.uicontrollers.card.CardView;
import com.mercadopago.android.px.internal.features.uicontrollers.card.FrontCardView;
import com.mercadopago.android.px.internal.repository.AmountRepository;
import com.mercadopago.android.px.internal.repository.GroupsRepository;
import com.mercadopago.android.px.internal.repository.PaymentSettingRepository;
import com.mercadopago.android.px.internal.repository.UserSelectionRepository;
import com.mercadopago.android.px.internal.tracker.FlowHandler;
import com.mercadopago.android.px.internal.tracker.MPTrackingContext;
import com.mercadopago.android.px.internal.util.ApiUtil;
import com.mercadopago.android.px.internal.util.MPCardMaskUtil;
import com.mercadopago.android.px.model.BankDeal;
import com.mercadopago.android.px.model.Bin;
import com.mercadopago.android.px.model.Card;
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
import com.mercadopago.android.px.model.ScreenViewEvent;
import com.mercadopago.android.px.model.SecurityCode;
import com.mercadopago.android.px.model.Setting;
import com.mercadopago.android.px.model.Token;
import com.mercadopago.android.px.model.exceptions.ApiException;
import com.mercadopago.android.px.model.exceptions.CardTokenException;
import com.mercadopago.android.px.model.exceptions.MercadoPagoError;
import com.mercadopago.android.px.preferences.CheckoutPreference;
import com.mercadopago.android.px.preferences.PaymentPreference;
import com.mercadopago.android.px.services.Callback;
import com.mercadopago.android.px.tracking.internal.utils.TrackingUtil;
import java.util.ArrayList;
import java.util.List;

import static com.mercadopago.android.px.model.Card.CARD_DEFAULT_SECURITY_CODE_LENGTH;

public class GuessingCardPresenter extends MvpPresenter<GuessingCardActivityView, GuessingCardProvider> {

    @NonNull private final AmountRepository mAmountRepository;
    @NonNull private final UserSelectionRepository mUserSelectionRepository;
    @NonNull private final PaymentSettingRepository mPaymentSettingRepository;
    @NonNull private final GroupsRepository mGroupsRepository;
    @NonNull private final AdvancedConfiguration mAdvancedConfiguration;

    //Card controller
    @SuppressWarnings("WeakerAccess") protected PaymentMethodGuessingController mPaymentMethodGuessingController;
    @SuppressWarnings("WeakerAccess") protected PaymentPreference mPaymentPreference;
    private List<IdentificationType> mIdentificationTypes;
    private FailureRecovery mFailureRecovery;
    //Activity parameters
    private PaymentRecovery mPaymentRecovery;
    private Identification mIdentification;
    private boolean mIdentificationNumberRequired;
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

    //Discount
    private int mCurrentNumberLength;
    private Issuer mIssuer;

    public GuessingCardPresenter(@NonNull final AmountRepository amountRepository,
        @NonNull final UserSelectionRepository userSelectionRepository,
        @NonNull final PaymentSettingRepository paymentSettingRepository,
        @NonNull final GroupsRepository groupsRepository,
        @NonNull final AdvancedConfiguration advancedConfiguration,
        @NonNull final PaymentPreference paymentPreference,
        @NonNull final PaymentRecovery paymentRecovery
        ) {
        mAmountRepository = amountRepository;
        mUserSelectionRepository = userSelectionRepository;
        mPaymentSettingRepository = paymentSettingRepository;
        mGroupsRepository = groupsRepository;
        mAdvancedConfiguration = advancedConfiguration;
        mPaymentPreference = paymentPreference;
        mPaymentRecovery = paymentRecovery;
        mToken = new Token();
        mIdentification = new Identification();
        mEraseSpace = true;
    }

    public void initialize() {
        getView().onValidStart();
        trackScreen();
        initializeCardToken();
        resolveBankDeals();
        getPaymentMethods();
        if (recoverWithCardHolder()) {
            fillRecoveryFields();
        }
    }

    public void trackScreen() {
        final String paymentTypeId = getPaymentTypeId();
        final ScreenViewEvent event = new ScreenViewEvent.Builder()
            .setFlowId(FlowHandler.getInstance().getFlowId())
            .setScreenId(String.format("%s%s", TrackingUtil.SCREEN_ID_CARD_FORM, paymentTypeId))
            .setScreenName(TrackingUtil.SCREEN_NAME_CARD_FORM + " " + paymentTypeId)
            .build();

        getTrackingContext().trackEvent(event);
    }

    public void setCurrentNumberLength(final int currentNumberLength) {
        mCurrentNumberLength = currentNumberLength;
    }

    public FailureRecovery getFailureRecovery() {
        return mFailureRecovery;
    }

    public void setFailureRecovery(final FailureRecovery failureRecovery) {
        mFailureRecovery = failureRecovery;
    }

    public PaymentRecovery getPaymentRecovery() {
        return mPaymentRecovery;
    }

    public void setPaymentRecovery(final PaymentRecovery paymentRecovery) {
        mPaymentRecovery = paymentRecovery;
        if (recoverWithCardHolder()) {
            saveCardholderName(paymentRecovery.getToken().getCardHolder().getName());
            saveIdentificationNumber(paymentRecovery.getToken().getCardHolder().getIdentification().getNumber());
        }
    }

    private void fillRecoveryFields() {
        getView().setCardholderName(mPaymentRecovery.getToken().getCardHolder().getName());
        getView()
            .setIdentificationNumber(mPaymentRecovery.getToken().getCardHolder().getIdentification().getNumber());
    }

    private boolean recoverWithCardHolder() {
        return mPaymentRecovery != null && mPaymentRecovery.getToken() != null &&
            mPaymentRecovery.getToken().getCardHolder() != null;
    }

    @Nullable
    public PaymentMethod getPaymentMethod() {
        return mUserSelectionRepository.getPaymentMethod();
    }

    public void setPaymentMethod(@Nullable final PaymentMethod paymentMethod) {
        mUserSelectionRepository.select(paymentMethod);
        if (paymentMethod == null) {
            clearCardSettings();
        }
    }

    public List<IdentificationType> getIdentificationTypes() {
        return mIdentificationTypes;
    }

    public boolean hasToShowPaymentTypes() {
        return showPaymentTypes;
    }

    public boolean isSecurityCodeRequired() {
        return mIsSecurityCodeRequired;
    }

    public void setSecurityCodeRequired(final boolean required) {
        mIsSecurityCodeRequired = required;
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

    public Token getToken() {
        return mToken;
    }

    public void setToken(final Token token) {
        mToken = token;
    }

    public CardToken getCardToken() {
        return mCardToken;
    }

    public void setCardToken(final CardToken cardToken) {
        mCardToken = cardToken;
    }

    public void setPaymentTypesList(@Nullable final List<PaymentType> paymentTypesList) {
        mPaymentTypesList = paymentTypesList;
    }

    public void setIdentificationTypesList(@Nullable final List<IdentificationType> identificationTypesList) {
        mIdentificationTypes = identificationTypesList;
    }

    public Identification getIdentification() {
        return mIdentification;
    }

    public void setIdentification(final Identification identification) {
        mIdentification = identification;
    }

    public boolean isIdentificationNumberRequired() {
        return mIdentificationNumberRequired;
    }

    public void setIdentificationNumberRequired(final boolean identificationNumberRequired) {
        mIdentificationNumberRequired = identificationNumberRequired;
        if (identificationNumberRequired) {
            getView().showIdentificationInput();
        }
    }

    public PaymentPreference getPaymentPreference() {
        return mPaymentPreference;
    }

    public void setPaymentPreference(final PaymentPreference paymentPreference) {
        mPaymentPreference = paymentPreference;
    }

    @Nullable
    public String getSecurityCodeFront() {
        String securityCode = null;
        if (mSecurityCodeLocation.equals(CardView.CARD_SIDE_FRONT)) {
            securityCode = getSecurityCode();
        }
        return securityCode;
    }

    private boolean isCardLengthResolved() {
        return mUserSelectionRepository.getPaymentMethod() != null && mBin != null;
    }

    public Integer getCardNumberLength() {
        return PaymentMethodGuessingController.getCardNumberLength(mUserSelectionRepository.getPaymentMethod(), mBin);
    }

    public void getPaymentMethods() {
        mGroupsRepository.getGroups().enqueue(new Callback<PaymentMethodSearch>() {
            @Override
            public void success(final PaymentMethodSearch paymentMethodSearch) {
                mPaymentMethodGuessingController = new PaymentMethodGuessingController(
                    mPaymentPreference.getSupportedPaymentMethods(paymentMethodSearch.getPaymentMethods()),
                    mPaymentPreference.getDefaultPaymentTypeId(),
                    mPaymentPreference.getExcludedPaymentTypes());
                startGuessingForm();
            }

            @Override
            public void failure(final ApiException apiException) {
                finishCardFlow();
            }
        });
    }

    @Nullable
    public List<PaymentMethod> getAllSupportedPaymentMethods() {
        List<PaymentMethod> list = null;
        if (mPaymentMethodGuessingController != null) {
            list = mPaymentMethodGuessingController.getAllSupportedPaymentMethods();
        }
        return list;
    }

    @SuppressWarnings("WeakerAccess")
    protected void startGuessingForm() {
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

    private void initializeCardToken() {
        mCardToken = new CardToken("", null, null, "", "", "", "");
    }

    private void checkPaymentMethodsSupported(final boolean withAnimation) {
        if (onlyOnePaymentMethodSupported() && getAllSupportedPaymentMethods() != null) {
            getView().setExclusionWithOneElementInfoView(getAllSupportedPaymentMethods().get(0), withAnimation);
        }
    }

    private boolean onlyOnePaymentMethodSupported() {
        final List<PaymentMethod> supportedPaymentMethods = getAllSupportedPaymentMethods();
        return supportedPaymentMethods != null && supportedPaymentMethods.size() == 1;
    }

    private void setInvalidCardMessage() {
        if (onlyOnePaymentMethodSupported()) {
            getView().setInvalidCardOnePaymentMethodErrorView();
        } else {
            getView().setInvalidCardMultipleErrorView();
        }
    }

    @Nullable
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

    private void resolveBankDeals() {
        if (mAdvancedConfiguration.isBankDealsEnabled()) {
            getBankDealsAsync();
        } else {
            getView().hideBankDeals();
        }
    }

    public void resolvePaymentMethodListSet(final List<PaymentMethod> paymentMethodList, final String bin) {
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

    public void onPaymentMethodSet(final PaymentMethod paymentMethod) {
        if (!mUserSelectionRepository.hasSelectedPaymentMethod()) {
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

        if (!mUserSelectionRepository.hasSelectedPaymentMethod()) {
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

    public void setSelectedPaymentType(final PaymentType paymentType) {
        if (mPaymentMethodGuessingController == null) {
            return;
        }
        for (final PaymentMethod paymentMethod : mPaymentMethodGuessingController.getGuessedPaymentMethods()) {
            if (paymentMethod.getPaymentTypeId().equals(paymentType.getId())) {
                setPaymentMethod(paymentMethod);
            }
        }
    }

    public String getSavedBin() {
        return mBin;
    }

    public void saveBin(@Nullable final String bin) {
        mBin = bin;
        if (mPaymentMethodGuessingController != null) {
            mPaymentMethodGuessingController.saveBin(bin);
        }
    }

    private void configureWithSettings(final PaymentMethod paymentMethod) {
        if (paymentMethod != null) {
            mIsSecurityCodeRequired = paymentMethod.isSecurityCodeRequired(mBin);
            if (!mIsSecurityCodeRequired) {
                getView().hideSecurityCodeInput();
            }
            final Setting setting =
                Setting.getSettingByPaymentMethodAndBin(paymentMethod, mBin);
            if (setting == null) {
                getView()
                    .showError(
                        new MercadoPagoError(getResourcesProvider().getSettingNotFoundForBinErrorMessage(), false),
                        "");
            } else {
                final int cardNumberLength = getCardNumberLength();
                int spaces = FrontCardView.CARD_DEFAULT_AMOUNT_SPACES;

                if (cardNumberLength == FrontCardView.CARD_NUMBER_DINERS_LENGTH ||
                    cardNumberLength == FrontCardView.CARD_NUMBER_AMEX_LENGTH ||
                    cardNumberLength == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_1_LENGTH) {
                    spaces = FrontCardView.CARD_AMEX_DINERS_AMOUNT_SPACES;
                } else if (cardNumberLength == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_2_LENGTH) {
                    spaces = FrontCardView.CARD_NUMBER_MAESTRO_SETTING_2_AMOUNT_SPACES;
                }
                getView().setCardNumberInputMaxLength(cardNumberLength + spaces);
                final SecurityCode securityCode = setting.getSecurityCode();
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

    @SuppressWarnings("WeakerAccess")
    protected void getIdentificationTypesAsync() {
        getResourcesProvider().getIdentificationTypesAsync(
            new TaggedCallback<List<IdentificationType>>(ApiUtil.RequestOrigin.GET_IDENTIFICATION_TYPES) {
                @Override
                public void onSuccess(final List<IdentificationType> identificationTypes) {
                    resolveIdentificationTypes(identificationTypes);
                }

                @Override
                public void onFailure(final MercadoPagoError error) {
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

    @SuppressWarnings("WeakerAccess")
    protected void resolveIdentificationTypes(final List<IdentificationType> identificationTypes) {
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

    public void setBankDealsList(@Nullable final List<BankDeal> bankDealsList) {
        mBankDealsList = bankDealsList;
    }

    @SuppressWarnings("WeakerAccess")
    protected void getBankDealsAsync() {
        getResourcesProvider()
            .getBankDealsAsync(new TaggedCallback<List<BankDeal>>(ApiUtil.RequestOrigin.GET_BANK_DEALS) {
                @Override
                public void onSuccess(final List<BankDeal> bankDeals) {
                    resolveBankDeals(bankDeals);
                }

                @Override
                public void onFailure(final MercadoPagoError error) {
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

    @SuppressWarnings("WeakerAccess")
    protected void resolveBankDeals(final List<BankDeal> bankDeals) {
        if (isViewAttached()) {
            if (bankDeals == null || bankDeals.isEmpty()) {
                getView().hideBankDeals();
            } else {
                mBankDealsList = bankDeals;
                getView().showBankDeals();
            }
        }
    }

    private void enablePaymentTypeSelection(final Iterable<PaymentMethod> paymentMethodList) {
        final List<PaymentType> paymentTypesList = new ArrayList<>();
        for (final PaymentMethod pm : paymentMethodList) {
            final PaymentType type = new PaymentType(pm.getPaymentTypeId());
            paymentTypesList.add(type);
        }
        mPaymentTypesList = paymentTypesList;

        showPaymentTypes = true;
    }

    private void disablePaymentTypeSelection() {
        showPaymentTypes = false;
        mPaymentTypesList = null;
    }

    public PaymentMethodGuessingController getGuessingController() {
        return mPaymentMethodGuessingController;
    }

    @Nullable
    public List<PaymentMethod> getGuessedPaymentMethods() {
        if (mPaymentMethodGuessingController == null) {
            return null;
        }
        return mPaymentMethodGuessingController.getGuessedPaymentMethods();
    }

    public List<PaymentType> getPaymentTypes() {
        return mPaymentTypesList;
    }

    public void saveCardNumber(final String cardNumber) {
        mCardNumber = cardNumber;
    }

    public void saveCardholderName(final String cardholderName) {
        mCardholderName = cardholderName;
    }

    public void saveExpiryMonth(final String expiryMonth) {
        mExpiryMonth = expiryMonth;
    }

    public void saveExpiryYear(final String expiryYear) {
        mExpiryYear = expiryYear;
    }

    public void saveSecurityCode(final String securityCode) {
        mSecurityCode = securityCode;
    }

    public void saveIdentificationNumber(final String identificationNumber) {
        mIdentificationNumber = identificationNumber;
    }

    public void saveIdentificationType(final IdentificationType identificationType) {
        mIdentificationType = identificationType;
        if (identificationType != null) {
            mIdentification.setType(identificationType.getId());
            getView().setIdentificationNumberRestrictions(identificationType.getType());
        }
    }

    public IdentificationType getIdentificationType() {
        return mIdentificationType;
    }

    public String getCardNumber() {
        return mCardNumber;
    }

    public void setCardNumber(@Nullable final String cardNumber) {
        mCardNumber = cardNumber;
    }

    public String getCardholderName() {
        return mCardholderName;
    }

    public void setCardholderName(@Nullable final String name) {
        mCardholderName = name;
    }

    public String getExpiryMonth() {
        return mExpiryMonth;
    }

    public void setExpiryMonth(@Nullable final String expiryMonth) {
        mExpiryMonth = expiryMonth;
    }

    public String getExpiryYear() {
        return mExpiryYear;
    }

    public void setExpiryYear(@Nullable final String expiryYear) {
        mExpiryYear = expiryYear;
    }

    public String getSecurityCode() {
        return mSecurityCode;
    }

    public String getIdentificationNumber() {
        return mIdentificationNumber;
    }

    public void setIdentificationNumber(@Nullable final String number) {
        mIdentificationNumber = number;
        mIdentification.setNumber(number);
    }

    public int getIdentificationNumberMaxLength() {
        int maxLength = Card.CARD_DEFAULT_IDENTIFICATION_NUMBER_LENGTH;
        if (mIdentificationType != null) {
            maxLength = mIdentificationType.getMaxLength();
        }
        return maxLength;
    }

    public boolean validateCardNumber() {
        mCardToken.setCardNumber(getCardNumber());
        try {
            final PaymentMethod paymentMethod = mUserSelectionRepository.getPaymentMethod();
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
        } catch (final CardTokenException e) {
            getView().setErrorView(e);
            getView().setErrorCardNumber();
            return false;
        }
    }

    public boolean validateCardName() {
        final Cardholder cardHolder = new Cardholder();
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
        final String monthString = getExpiryMonth();
        final String yearString = getExpiryYear();
        final Integer month = (monthString == null || monthString.isEmpty()) ? null : Integer.valueOf(monthString);
        final Integer year = (yearString == null || yearString.isEmpty()) ? null : Integer.valueOf(yearString);
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
            mCardToken.validateSecurityCode(mUserSelectionRepository.getPaymentMethod());
            getView().clearErrorView();
            return true;
        } catch (final CardTokenException e) {
            setCardSecurityCodeErrorView(e);
            return false;
        }
    }

    private void setCardSecurityCodeErrorView(final CardTokenException exception) {
        if (!isSecurityCodeRequired()) {
            return;
        }
        getView().setErrorView(exception);
        getView().setErrorSecurityCode();
    }

    public boolean validateIdentificationNumber() {
        mIdentification.setNumber(getIdentificationNumber());
        mCardToken.getCardholder().setIdentification(mIdentification);
        final boolean ans = mCardToken.validateIdentificationNumber(mIdentificationType);
        if (ans) {
            getView().clearErrorView();
            getView().clearErrorIdentificationNumber();
        } else {
            setCardIdentificationErrorView(getResourcesProvider().getInvalidIdentificationNumberErrorMessage());
        }
        return ans;
    }

    private void setCardIdentificationErrorView(final String message) {
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

    private void clearSpaceErasableSettings() {
        mEraseSpace = true;
    }

    public void finishCardFlow() {
        createToken();
    }

    @SuppressWarnings("WeakerAccess")
    protected void createToken() {
        getResourcesProvider()
            .createTokenAsync(mCardToken, new TaggedCallback<Token>(ApiUtil.RequestOrigin.CREATE_TOKEN) {
                @Override
                public void onSuccess(final Token token) {
                    resolveTokenRequest(token);
                }

                @Override
                public void onFailure(final MercadoPagoError error) {
                    resolveTokenCreationError(error, ApiUtil.RequestOrigin.CREATE_TOKEN);
                }
            });
    }

    public void resolveTokenRequest(final Token token) {
        mToken = token;
        mPaymentSettingRepository.configure(mToken);
        getIssuers();
    }

    @SuppressWarnings("WeakerAccess")
    protected void resolveTokenCreationError(final MercadoPagoError error, final String requestOrigin) {
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

    private boolean wrongIdentificationNumber(final MercadoPagoError error) {
        boolean answer = false;
        if (error.isApiException()) {
            final ApiException apiException = error.getApiException();
            answer = apiException.containsCause(ApiException.ErrorCodes.INVALID_CARD_HOLDER_IDENTIFICATION_NUMBER);
        }
        return answer;
    }

    private void showIdentificationNumberError() {
        getView().hideProgress();
        getView().setErrorView(getResourcesProvider().getInvalidFieldErrorMessage());
        getView().setErrorIdentificationNumber();
    }

    @SuppressWarnings("WeakerAccess")
    protected void getIssuers() {
        final PaymentMethod paymentMethod = mUserSelectionRepository.getPaymentMethod();
        if (paymentMethod != null) {
            getResourcesProvider().getIssuersAsync(paymentMethod.getId(), mBin,
                new TaggedCallback<List<Issuer>>(ApiUtil.RequestOrigin.GET_ISSUERS) {
                    @Override
                    public void onSuccess(final List<Issuer> issuers) {
                        resolveIssuersList(issuers);
                    }

                    @Override
                    public void onFailure(final MercadoPagoError error) {
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
    }

    @SuppressWarnings("WeakerAccess")
    protected void resolveIssuersList(final List<Issuer> issuers) {
        if (issuers.size() == 1) {
            mIssuer = issuers.get(0);
            mUserSelectionRepository.select(mIssuer);
            getInstallments();
        } else {
            getView().finishCardFlow(mUserSelectionRepository.getPaymentMethod(), mToken, issuers);
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected  void getInstallments() {
        final CheckoutPreference checkoutPreference = mPaymentSettingRepository.getCheckoutPreference();
        if (checkoutPreference != null) {
            final DifferentialPricing differentialPricing = checkoutPreference.getDifferentialPricing();
            final Integer differentialPricingId = differentialPricing == null ? null : differentialPricing.getId();
            final PaymentMethod paymentMethod = mUserSelectionRepository.getPaymentMethod();
            if (paymentMethod != null) {
                getResourcesProvider().getInstallmentsAsync(mBin, mAmountRepository.getAmountToPay(), mIssuer.getId(),
                    paymentMethod.getId(), differentialPricingId,
                    new TaggedCallback<List<Installment>>(ApiUtil.RequestOrigin.GET_INSTALLMENTS) {
                        @Override
                        public void onSuccess(final List<Installment> installments) {
                            resolveInstallments(installments);
                        }

                        @Override
                        public void onFailure(final MercadoPagoError error) {
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
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected void resolveInstallments(final List<Installment> installments) {
        String errorMessage = null;
        if (installments == null || installments.isEmpty()) {
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

    private void resolvePayerCosts(final List<PayerCost> payerCosts) {
        final PayerCost defaultPayerCost = mPaymentPreference.getDefaultInstallments(payerCosts);
        if (defaultPayerCost != null) {
            mUserSelectionRepository.select(defaultPayerCost);
            getView().finishCardFlow(mUserSelectionRepository.getPaymentMethod(), mToken, mIssuer,
                defaultPayerCost);
        } else if (payerCosts.isEmpty()) {
            getView().showError(new MercadoPagoError(getResourcesProvider().getMissingPayerCostsErrorMessage(), false),
                ApiUtil.RequestOrigin.GET_INSTALLMENTS);
        } else if (payerCosts.size() == 1) {
            final PayerCost payerCost = payerCosts.get(0);
            mUserSelectionRepository.select(payerCost);
            getView().finishCardFlow(mUserSelectionRepository.getPaymentMethod(), mToken, mIssuer,
                payerCost);
        } else {
            getView().finishCardFlow(mUserSelectionRepository.getPaymentMethod(), mToken, mIssuer, payerCosts);
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

    public boolean shouldAskPaymentType(@Nullable final List<PaymentMethod> paymentMethodList) {

        boolean paymentTypeUndefined = false;
        final String paymentType;

        if (paymentMethodList == null || paymentMethodList.isEmpty()) {
            paymentTypeUndefined = true;
        } else {
            paymentType = paymentMethodList.get(0).getPaymentTypeId();
            for (final PaymentMethod currentPaymentMethod : paymentMethodList) {
                if (!paymentType.equals(currentPaymentMethod.getPaymentTypeId())) {
                    paymentTypeUndefined = true;
                    break;
                }
            }
        }
        return paymentTypeUndefined;
    }
}
