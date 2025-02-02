package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountTransferModel.Properties;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.SWTHelper;

public class AccountTransferDialog extends AbstractTransactionDialog // NOSONAR
{
    private final class AccountsMustBeDifferentValidator extends MultiValidator
    {
        IObservableValue<?> source;
        IObservableValue<?> target;

        public AccountsMustBeDifferentValidator(IObservableValue<?> source, IObservableValue<?> target)
        {
            this.source = source;
            this.target = target;
        }

        @Override
        protected IStatus validate()
        {
            Object from = source.getValue();
            Object to = target.getValue();

            return from != null && to != null && from != to ? ValidationStatus.ok()
                            : ValidationStatus.error(Messages.MsgAccountMustBeDifferent);
        }
    }

    @Inject
    private Client client;

    @Preference(value = UIConstants.Preferences.USE_INDIRECT_QUOTATION)
    @Inject
    private boolean useIndirectQuotation = false;

    @Inject
    public AccountTransferDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell)
    {
        super(parentShell);
    }

    @PostConstruct
    private void createModel(ExchangeRateProviderFactory factory) // NOSONAR
    {
        AccountTransferModel m = new AccountTransferModel(client);
        m.setExchangeRateProviderFactory(factory);
        setModel(m);
    }

    private AccountTransferModel model()
    {
        return (AccountTransferModel) this.model;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        //
        // input elements
        //

        // source account

        ComboInput source = new ComboInput(editArea, Messages.ColumnAccountFrom);
        source.value.setInput(including(client.getActiveAccounts(), model().getSourceAccount()));
        IObservableValue<?> sourceObservable = source.bindValue(Properties.sourceAccount.name(),
                        Messages.MsgAccountFromMissing);
        source.bindCurrency(Properties.sourceAccountCurrency.name());

        // target account

        ComboInput target = new ComboInput(editArea, Messages.ColumnAccountTo);
        target.value.setInput(including(client.getActiveAccounts(), model().getTargetAccount()));
        IObservableValue<?> targetObservable = target.bindValue(Properties.targetAccount.name(),
                        Messages.MsgAccountToMissing);
        target.bindCurrency(Properties.targetAccountCurrency.name());

        MultiValidator validator = new AccountsMustBeDifferentValidator(sourceObservable, targetObservable);
        context.addValidationStatusProvider(validator);

        // date + time

        DateTimeInput dateTime = new DateTimeInput(editArea, Messages.ColumnDate);
        dateTime.bindDate(Properties.date.name());
        dateTime.bindTime(Properties.time.name());
        dateTime.bindButton(() -> model().getTime(), time -> model().setTime(time));

        // other input fields

        Input fxAmount = new Input(editArea, Messages.ColumnAmount);
        fxAmount.bindValue(Properties.fxAmount.name(), Messages.ColumnAmount, Values.Amount, true);
        fxAmount.bindCurrency(Properties.sourceAccountCurrency.name());

        ExchangeRateInput exchangeRate = new ExchangeRateInput(editArea, useIndirectQuotation ? "/ " : "x "); //$NON-NLS-1$ //$NON-NLS-2$
        exchangeRate.bindBigDecimal(
                        useIndirectQuotation ? Properties.inverseExchangeRate.name() : Properties.exchangeRate.name(),
                        Values.ExchangeRate.pattern());
        exchangeRate.bindCurrency(useIndirectQuotation ? Properties.inverseExchangeRateCurrencies.name()
                        : Properties.exchangeRateCurrencies.name());
        exchangeRate.bindInvertAction(() -> model()
                        .setExchangeRate(BigDecimal.ONE.divide(model().getExchangeRate(), 10, RoundingMode.HALF_DOWN)));

        model().addPropertyChangeListener(Properties.exchangeRate.name(),
                        e -> exchangeRate.value.setToolTipText(AbstractModel.createCurrencyToolTip(
                                        model().getExchangeRate(), model().getTargetAccountCurrency(),
                                        model().getSourceAccountCurrency())));

        Input amount = new Input(editArea, "="); //$NON-NLS-1$
        amount.bindValue(Properties.amount.name(), Messages.ColumnAmount, Values.Amount, true);
        amount.bindCurrency(Properties.targetAccountCurrency.name());

        // note

        Label lblNote = new Label(editArea, SWT.LEFT);
        lblNote.setText(Messages.ColumnNote);
        Text valueNote = new Text(editArea, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        IObservableValue<?> targetNote = WidgetProperties.text(SWT.Modify).observe(valueNote);
        IObservableValue<?> noteObservable = BeanProperties.value(Properties.note.name()).observe(model);
        context.bindValue(targetNote, noteObservable);

        //
        // form layout
        //

        int amountWidth = amountWidth(amount.value);
        int currencyWidth = currencyWidth(fxAmount.currency);

        FormDataFactory forms = startingWith(source.value.getControl(), source.label).suffix(source.currency)
                        .thenBelow(target.value.getControl()).label(target.label).suffix(target.currency)
                        .thenBelow(dateTime.date.getControl()).label(dateTime.label).thenRight(dateTime.time)
                        .thenRight(dateTime.button, 0);

        // fxAmount - exchange rate - amount
        forms.thenBelow(fxAmount.value).width(amountWidth).label(fxAmount.label) //
                        .thenRight(fxAmount.currency).width(currencyWidth) //
                        .thenRight(exchangeRate.label) //
                        .thenRight(exchangeRate.value).width(amountWidth) //
                        .thenRight(exchangeRate.buttonInvertExchangeRate, 0) //
                        .thenRight(exchangeRate.currency).width(amountWidth) //
                        .thenRight(amount.label) //
                        .thenRight(amount.value).width(amountWidth) //
                        // note
                        .suffix(amount.currency, currencyWidth) //
                        .thenBelow(valueNote).height(SWTHelper.lineHeight(valueNote) * 3)
                        .left(target.value.getControl()).right(amount.value).label(lblNote);

        int widest = widest(source.label, target.label, dateTime.label, fxAmount.label, lblNote);
        startingWith(source.label).width(widest);

        //
        // hide / show exchange rate if necessary
        //

        model.addPropertyChangeListener(Properties.exchangeRateCurrencies.name(), event -> {
            String sourceCurrency = model().getSourceAccountCurrency();
            String targetCurrency = model().getTargetAccountCurrency();

            // make exchange rate visible if both are set but different

            boolean visible = sourceCurrency.length() > 0 && targetCurrency.length() > 0
                            && !sourceCurrency.equals(targetCurrency);

            exchangeRate.setVisible(visible);
            amount.setVisible(visible);
        });

        WarningMessages warnings = new WarningMessages(this);
        warnings.add(() -> model().getDate().isAfter(LocalDate.now(ZoneOffset.UTC)) ? Messages.MsgDateIsInTheFuture : null);
        model.addPropertyChangeListener(Properties.date.name(), e -> warnings.check());

        model.firePropertyChange(Properties.exchangeRateCurrencies.name(), "", model().getExchangeRateCurrencies()); //$NON-NLS-1$
    }

    @Override
    public void setAccount(Account account)
    {
        model().setSourceAccount(account);
    }

    public void setEntry(AccountTransferEntry entry)
    {
        model().setSource(entry);
    }

}
