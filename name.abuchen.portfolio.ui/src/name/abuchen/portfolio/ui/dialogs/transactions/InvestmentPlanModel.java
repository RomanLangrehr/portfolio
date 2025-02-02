package name.abuchen.portfolio.ui.dialogs.transactions;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.ui.Messages;

public class InvestmentPlanModel extends AbstractModel
{
    public enum Properties
    {
        calculationStatus, name, security, securityCurrencyCode, portfolio, account, accountCurrencyCode, start, interval, amount, fees, transactionCurrencyCode, autoGenerate; // NOSONAR
    }

    public static final Account DELIVERY = new Account(Messages.InvestmentPlanOptionDelivery);
    private static final Portfolio DEPOSIT = new Portfolio(Messages.InvestmentPlanOptionDeposit);

    private final Client client;

    private InvestmentPlan source;

    private String name;
    private Security security;
    private Portfolio portfolio;
    private Account account;

    private boolean autoGenerate;

    private LocalDate start = LocalDate.now(ZoneOffset.UTC);

    private int interval = 1;
    private long amount;
    private long fees;

    private IStatus calculationStatus = ValidationStatus.ok();

    public InvestmentPlanModel(Client client, Class<? extends Transaction> planType)
    {
        this.client = client;
        
        if (planType == AccountTransaction.class)
            portfolio = DEPOSIT;
    }

    @Override
    public String getHeading()
    {
        return source != null ? Messages.InvestmentPlanTitleEditPlan : Messages.InvestmentPlanTitleNewPlan;
    }

    @Override
    public void applyChanges()
    {
        if (security == null && !DEPOSIT.equals(portfolio))
            throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
        if (portfolio == null)
            throw new UnsupportedOperationException(Messages.MsgMissingPortfolio);
        if (account == null)
            throw new UnsupportedOperationException(Messages.MsgMissingAccount);

        InvestmentPlan plan = source;

        if (plan == null)
        {
            plan = new InvestmentPlan();
            this.client.addPlan(plan);
        }

        plan.setName(name);
        plan.setSecurity(portfolio.equals(DEPOSIT) ? null : security);
        plan.setPortfolio(portfolio.equals(DEPOSIT) ? null : portfolio);
        plan.setAccount(account.equals(DELIVERY) ? null : account);
        plan.setAutoGenerate(autoGenerate);
        plan.setStart(start);
        plan.setInterval(interval);
        plan.setAmount(amount);
        plan.setFees(fees);
    }

    @Override
    public void resetToNewTransaction()
    {
        this.source = null;

        setName(null);
        setAutoGenerate(false);
        setAmount(0);
        setFees(0);
    }

    public void setSource(InvestmentPlan plan)
    {
        this.source = plan;

        this.name = plan.getName();
        this.security = plan.getSecurity();
        this.portfolio = plan.getPortfolio() != null ? plan.getPortfolio() : DEPOSIT;
        this.account = plan.getAccount() != null ? plan.getAccount() : DELIVERY;
        this.autoGenerate = plan.isAutoGenerate();
        this.start = plan.getStart();
        this.interval = plan.getInterval();
        this.amount = plan.getAmount();
        this.fees = plan.getFees();
    }

    @Override
    public IStatus getCalculationStatus()
    {
        return calculationStatus;
    }

    private IStatus calculateStatus()
    {
        if (account != null && account.equals(DELIVERY) && portfolio != null && portfolio.equals(DEPOSIT))
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnPeer));

        if (name == null || name.trim().length() == 0)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnName));

        if (security == null && portfolio != null && !portfolio.equals(DEPOSIT))
            return ValidationStatus
                            .error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.MsgMissingSecurity));

        if (amount == 0L)
            return ValidationStatus.error(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnAmount));

        return ValidationStatus.ok();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        firePropertyChange(Properties.name.name(), this.name, this.name = name); // NOSONAR
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        String oldSecurityCurrency = getSecurityCurrencyCode();
        String oldTransactionCurrency = getTransactionCurrencyCode();
        firePropertyChange(Properties.security.name(), this.security, this.security = security); // NOSONAR
        firePropertyChange(Properties.securityCurrencyCode.name(), oldSecurityCurrency, getSecurityCurrencyCode());
        firePropertyChange(Properties.transactionCurrencyCode.name(), oldTransactionCurrency,
                        getTransactionCurrencyCode());
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        String oldTransactionCurrency = getTransactionCurrencyCode();
        if (DEPOSIT.equals(portfolio))
            firePropertyChange(Properties.security.name(), this.security, this.security = null); // NOSONAR
        firePropertyChange(Properties.portfolio.name(), this.portfolio, this.portfolio = portfolio); // NOSONAR
        firePropertyChange(Properties.transactionCurrencyCode.name(), oldTransactionCurrency,
                        getTransactionCurrencyCode());
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        String oldAccountCurrency = getAccountCurrencyCode();
        String oldTransactionCurrency = getTransactionCurrencyCode();
        firePropertyChange(Properties.account.name(), this.account, this.account = account); // NOSONAR
        firePropertyChange(Properties.accountCurrencyCode.name(), oldAccountCurrency, getAccountCurrencyCode());
        firePropertyChange(Properties.transactionCurrencyCode.name(), oldTransactionCurrency,
                        getTransactionCurrencyCode());
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public boolean isAutoGenerate()
    {
        return autoGenerate;
    }

    public void setAutoGenerate(boolean autoGenerate)
    {
        firePropertyChange(Properties.autoGenerate.name(), this.autoGenerate, this.autoGenerate = autoGenerate); // NOSONAR
    }

    public LocalDate getStart()
    {
        return start;
    }

    public void setStart(LocalDate start)
    {
        firePropertyChange(Properties.start.name(), this.start, this.start = start); // NOSONAR
    }

    public int getInterval()
    {
        return interval;
    }

    public void setInterval(int interval)
    {
        firePropertyChange(Properties.interval.name(), this.interval, this.interval = interval); // NOSONAR
    }

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        firePropertyChange(Properties.amount.name(), this.amount, this.amount = amount); // NOSONAR
        firePropertyChange(Properties.calculationStatus.name(), this.calculationStatus,
                        this.calculationStatus = calculateStatus()); // NOSONAR
    }

    public long getFees()
    {
        return fees;
    }

    public void setFees(long fees)
    {
        firePropertyChange(Properties.fees.name(), this.fees, this.fees = fees); // NOSONAR
    }

    public String getSecurityCurrencyCode()
    {
        return security != null ? security.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getAccountCurrencyCode()
    {
        return account != null && !DELIVERY.equals(account) ? account.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getReferenceAccountCurrencyCode()
    {
        return portfolio != null && !DEPOSIT.equals(portfolio) ? portfolio.getReferenceAccount().getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getTransactionCurrencyCode()
    {
        // transactions will be generated in currency of the account unless it
        // is an inbound delivery (which will be created in the currency of the
        // reference account)
        return account != null && !DELIVERY.equals(account) ? account.getCurrencyCode()
                        : getReferenceAccountCurrencyCode();
    }
}
