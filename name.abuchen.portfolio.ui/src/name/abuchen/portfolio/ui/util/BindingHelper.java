package name.abuchen.portfolio.ui.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.MessageFormat;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.util.Isin;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class BindingHelper
{
    public abstract static class Model
    {
        private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

        private Client client;

        public Model()
        {}

        public Model(Client client)
        {
            this.client = client;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
            propertyChangeSupport.addPropertyChangeListener(listener);
        }

        public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
        {
            propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }

        public Client getClient()
        {
            return client;
        }

        protected void firePropertyChange(String attribute, Object oldValue, Object newValue)
        {
            propertyChangeSupport.firePropertyChange(attribute, oldValue, newValue);
        }

        protected void firePropertyChange(String attribute, long oldValue, long newValue)
        {
            propertyChangeSupport.firePropertyChange(attribute, oldValue, newValue);
        }

        public abstract void applyChanges();
    }

    private static final class StatusTextConverter implements IConverter
    {
        @Override
        public Object getToType()
        {
            return String.class;
        }

        @Override
        public Object getFromType()
        {
            return IStatus.class;
        }

        @Override
        public Object convert(Object fromObject)
        {
            IStatus status = (IStatus) fromObject;
            return status.isOK() ? "" : status.getMessage(); //$NON-NLS-1$
        }
    }

    class ModelStatusListener
    {
        public void setStatus(IStatus status)
        {
            onValidationStatusChanged(status);
        }

        public IStatus getStatus()
        {
            // irrelevant
            return ValidationStatus.ok();
        }
    }

    private Model model;
    private ModelStatusListener listener = new ModelStatusListener();
    private DataBindingContext context;

    /** average char width needed to resize input fields on length */
    private int averageCharWidth = -1;

    public BindingHelper(Model model)
    {
        this.model = model;
        this.context = new DataBindingContext();

        context.bindValue(PojoObservables.observeValue(listener, "status"), //$NON-NLS-1$
                        new AggregateValidationStatus(context, AggregateValidationStatus.MAX_SEVERITY));
    }

    public void onValidationStatusChanged(IStatus status)
    {}

    public DataBindingContext getBindingContext()
    {
        return context;
    }

    public final void createErrorLabel(Composite editArea)
    {
        // error label
        Label errorLabel = new Label(editArea, SWT.NONE);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(errorLabel);

        // error label
        context.bindValue(SWTObservables.observeText(errorLabel), //
                        new AggregateValidationStatus(context, AggregateValidationStatus.MAX_SEVERITY), //
                        null, //
                        new UpdateValueStrategy().setConverter(new StatusTextConverter()));
    }

    public final void createLabel(Composite editArea, String text)
    {
        Label lblTransactionType = new Label(editArea, SWT.NONE);
        lblTransactionType.setText(text);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(lblTransactionType);
    }

    public final void bindLabel(Composite editArea, String property)
    {
        Label label = new Label(editArea, SWT.NONE);
        context.bindValue(SWTObservables.observeText(label), BeansObservables.observeValue(model, property));
        GridDataFactory.fillDefaults().span(1, 1).grab(true, false).applyTo(label);
    }

    public final void bindSpinner(Composite editArea, String label, String property, int min, int max, int selection,
                    int increment)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        Spinner spinner = new Spinner(editArea, SWT.NONE);
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setSelection(selection);
        spinner.setIncrement(increment);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(spinner);
        context.bindValue(SWTObservables.observeSelection(spinner), BeansObservables.observeValue(model, property));
    }

    public final ComboViewer bindComboViewer(Composite editArea, String label, String property,
                    IBaseLabelProvider labelProvider, Object input)
    {
        return bindComboViewer(editArea, label, property, labelProvider, null, input);
    }

    public final ComboViewer bindComboViewer(Composite editArea, String label, String property,
                    IBaseLabelProvider labelProvider, IValidator validator, Object input)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        ComboViewer combo = new ComboViewer(editArea, SWT.READ_ONLY);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setLabelProvider(labelProvider);
        combo.setInput(input);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(combo.getControl());

        UpdateValueStrategy strategy = new UpdateValueStrategy();
        if (validator != null)
            strategy.setAfterConvertValidator(validator);

        context.bindValue(ViewersObservables.observeSingleSelection(combo), //
                        BeansObservables.observeValue(model, property), strategy, null);
        return combo;
    }

    public final void bindDatePicker(Composite editArea, String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);
        DateTime boxDate = new DateTime(editArea, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(boxDate);

        context.bindValue(new SimpleDateTimeSelectionProperty().observe(boxDate),
                        BeansObservables.observeValue(model, property));
    }

    public final void bindAmountInput(Composite editArea, String label, String property)
    {
        Text txtValue = createTextInput(editArea, label);

        context.bindValue(
                        SWTObservables.observeText(txtValue, SWT.Modify), //
                        BeansObservables.observeValue(model, property), //
                        new UpdateValueStrategy().setConverter(new StringToCurrencyConverter(Values.Amount)),
                        new UpdateValueStrategy().setConverter(new CurrencyToStringConverter(Values.Amount)));
    }

    public final Control bindMandatoryAmountInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label);
        bindMandatoryDecimalInput(label, property, txtValue, Values.Amount);
        return txtValue;
    }

    public final Control bindMandatorySharesInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label);
        bindMandatoryDecimalInput(label, property, txtValue, Values.Share);
        return txtValue;
    }

    private void bindMandatoryDecimalInput(final String label, String property, Text txtValue, Values<?> type)
    {
        context.bindValue(SWTObservables.observeText(txtValue, SWT.Modify), //
                        BeansObservables.observeValue(model, property), //
                        new UpdateValueStrategy() //
                                        .setConverter(new StringToCurrencyConverter(type)) //
                                        .setAfterConvertValidator(new IValidator()
                                        {
                                            @Override
                                            public IStatus validate(Object value)
                                            {
                                                Long v = (Long) value;
                                                return v != null && v.longValue() > 0 ? ValidationStatus.ok()
                                                                : ValidationStatus.error(MessageFormat.format(
                                                                                Messages.MsgDialogInputRequired, label));
                                            }
                                        }), // ,
                        new UpdateValueStrategy().setConverter(new CurrencyToStringConverter(type)));
    }

    private Text createTextInput(Composite editArea, final String label)
    {
        return createTextInput(editArea, label, SWT.NONE, SWT.DEFAULT);
    }

    private Text createTextInput(Composite editArea, final String label, int style, int lenghtInCharacters)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        final Text txtValue = new Text(editArea, SWT.BORDER | style);
        txtValue.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                txtValue.selectAll();
            }
        });

        if (lenghtInCharacters == SWT.DEFAULT)
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(txtValue);
        else
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL)
                            .hint((lenghtInCharacters + 5) * getAverageCharWidth(txtValue), SWT.DEFAULT)
                            .applyTo(txtValue);

        return txtValue;
    }

    public final Control bindMandatoryLongInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label);

        context.bindValue(SWTObservables.observeText(txtValue, SWT.Modify), //
                        BeansObservables.observeValue(model, property), //
                        new UpdateValueStrategy().setAfterConvertValidator(new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                Long v = (Long) value;
                                return v != null && v.longValue() > 0 ? ValidationStatus.ok() : ValidationStatus
                                                .error(MessageFormat.format(Messages.MsgDialogInputRequired, label));
                            }
                        }), //
                        null);
        return txtValue;
    }

    public final IObservableValue bindStringInput(Composite editArea, final String label, String property)
    {
        return bindStringInput(editArea, label, property, SWT.NONE, SWT.DEFAULT);
    }

    public final IObservableValue bindStringInput(Composite editArea, final String label, String property, int style)
    {
        return bindStringInput(editArea, label, property, style, SWT.DEFAULT);
    }

    public final IObservableValue bindStringInput(Composite editArea, final String label, String property, int style,
                    int lenghtInCharacters)
    {
        Text txtValue = createTextInput(editArea, label, style, lenghtInCharacters);

        ISWTObservableValue observeText = SWTObservables.observeText(txtValue, SWT.Modify);
        context.bindValue(observeText, BeansObservables.observeValue(model, property));

        return observeText;
    }

    public final Control bindMandatoryStringInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label);

        context.bindValue(SWTObservables.observeText(txtValue, SWT.Modify), //
                        BeansObservables.observeValue(model, property), //
                        new UpdateValueStrategy().setAfterConvertValidator(new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                String v = (String) value;
                                return v != null && v.trim().length() > 0 ? ValidationStatus.ok() : ValidationStatus
                                                .error(MessageFormat.format(Messages.MsgDialogInputRequired, label));
                            }
                        }), //
                        null);
        return txtValue;
    }

    public final Control bindISINInput(Composite editArea, final String label, String property)
    {
        Text txtValue = createTextInput(editArea, label, SWT.NONE, 12);
        txtValue.setTextLimit(12);

        context.bindValue(SWTObservables.observeText(txtValue, SWT.Modify), //
                        BeansObservables.observeValue(model, property), //
                        new UpdateValueStrategy().setAfterConvertValidator(new IValidator()
                        {
                            @Override
                            public IStatus validate(Object value)
                            {
                                String v = (String) value;
                                return v == null || v.trim().length() == 0 || Isin.isValid(v) ? ValidationStatus.ok()
                                                : ValidationStatus.error(MessageFormat.format(
                                                                Messages.MsgDialogNotAValidISIN, label));
                            }
                        }), //
                        null);
        return txtValue;
    }

    public final Control bindBooleanInput(Composite editArea, final String label, String property)
    {
        Label l = new Label(editArea, SWT.NONE);
        l.setText(label);

        final Button btnCheckbox = new Button(editArea, SWT.CHECK);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(btnCheckbox);

        context.bindValue(SWTObservables.observeSelection(btnCheckbox), //
                        BeansObservables.observeValue(model, property));
        return btnCheckbox;
    }

    private int getAverageCharWidth(Control control)
    {
        if (averageCharWidth > 0)
            return averageCharWidth;

        GC gc = new GC(control);
        FontMetrics fm = gc.getFontMetrics();
        this.averageCharWidth = fm.getAverageCharWidth();
        gc.dispose();

        return averageCharWidth;
    }
}
