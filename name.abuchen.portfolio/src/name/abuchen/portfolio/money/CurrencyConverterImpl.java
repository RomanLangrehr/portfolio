package name.abuchen.portfolio.money;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

import name.abuchen.portfolio.Messages;

public class CurrencyConverterImpl implements CurrencyConverter
{
    private static final ExchangeRate FALLBACK_EXCHANGE_RATE = new ExchangeRate(LocalDate.now(ZoneOffset.UTC), BigDecimal.ONE);

    private final ExchangeRateProviderFactory factory;
    private final String termCurrency;

    public CurrencyConverterImpl(ExchangeRateProviderFactory factory, String termCurrency)
    {
        this.factory = Objects.requireNonNull(factory);
        this.termCurrency = Objects.requireNonNull(termCurrency);
    }

    @Override
    public String getTermCurrency()
    {
        return termCurrency;
    }

    @Override
    public ExchangeRate getRate(LocalDate date, String currencyCode)
    {
        if (termCurrency.equals(currencyCode))
            return new ExchangeRate(date, BigDecimal.ONE);

        ExchangeRateTimeSeries series = lookupSeries(currencyCode);
        return series.lookupRate(date).orElse(FALLBACK_EXCHANGE_RATE);
    }

    private ExchangeRateTimeSeries lookupSeries(String currencyCode) // NOSONAR
    {
        ExchangeRateTimeSeries series = factory.getTimeSeries(currencyCode, termCurrency);

        // should not happen b/c an empty time series is created if no
        // time series exists
        if (series == null)
            throw new MonetaryException(MessageFormat.format(Messages.MsgNoExchangeRateTimeSeriesFound, currencyCode,
                            termCurrency));

        return series;
    }

    @Override
    public CurrencyConverter with(String currencyCode)
    {
        if (currencyCode.equals(termCurrency))
            return this;

        return new CurrencyConverterImpl(factory, currencyCode);
    }
}
