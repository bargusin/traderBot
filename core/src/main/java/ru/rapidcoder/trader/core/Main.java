package ru.rapidcoder.trader.core;

import com.google.protobuf.Timestamp;
import com.google.type.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.core.InvestApi;

import java.time.DateTimeException;
import java.util.Date;

public class Main {

    public static final String SANDBOX_TOKEN = ResourcesAdapter.getProperties("trading.properties").get("sandboxToken").toString();
    public static final String PRODUCTION_TOKEN = ResourcesAdapter.getProperties("trading.properties").get("productionToken").toString();
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        //Можно создать экземпляр sandbox - тогда все вызовы будут переадресованы в песочницу
//        InvestApi sandboxApi = InvestApi.createSandbox(PRODUCTION_TOKEN);
//        sandboxServiceExample(sandboxApi);

        InvestApi productionApi = InvestApi.create(PRODUCTION_TOKEN);
        productionServiceExample(productionApi);
    }

    private static void productionServiceExample(InvestApi api) {
        var accounts = api.getUserService().getAccountsSync();
        for (Account account : accounts) {
            logger.info("Production account id: {} ({})", account.getName(), account.getAccessLevel());

            var portfolio = api.getOperationsService().getPortfolioSync(account.getId());
            var totalAmountShares = portfolio.getTotalAmountShares();
            logger.info("Общая стоимость акций в портфеле {} ({})", totalAmountShares.getValue(), totalAmountShares.getCurrency());
        }
    }

    private static void sandboxServiceExample(InvestApi sandboxApi) {
        //Открываем новый счет в песочнице
//        var accountId = sandboxApi.getSandboxService().openAccountSync();
//        logger.info("открыт новый аккаунт в песочнице {}", accountId);

        //В sandbox режиме можно делать запросы в те же методы, что и в обычном API
        //Поэтому не придется писать отдельный код для песочницы, чтоб проверить свою стратегию
        var accounts = sandboxApi.getUserService().getAccountsSync();
        var mainAccount = accounts.get(0);
        for (Account account : accounts) {
            logger.info("sandbox account id: {}, access level: {}", account.getId(), account.getAccessLevel().name());
        }

        //Убеждаемся, что мы в режиме песочницы
        logger.info("тариф должен быть sandbox. фактический тариф: {}", sandboxApi.getUserService().getInfoSync().getTariff());

        //пополняем счет песочницы на 10_000 рублей и 10_000 долларов
        /*
        sandboxApi.getSandboxService().payIn(mainAccount.getId(), MoneyValue.newBuilder().setUnits(1000).setCurrency("RUB").build());
        sandboxApi.getSandboxService().payIn(mainAccount.getId(), MoneyValue.newBuilder().setUnits(1000).setCurrency("USD").build());
        */

        getPortfolioExample(sandboxApi);
    }

    private static void getPortfolioExample(InvestApi api) {
        var accounts = api.getUserService().getAccountsSync();
        var mainAccount = accounts.get(0).getId();

        //Получаем и печатаем портфолио
        var portfolio = api.getOperationsService().getPortfolioSync(mainAccount);
        var totalAmountBonds = portfolio.getTotalAmountBonds();
        logger.info("общая стоимость облигаций в портфеле {}", totalAmountBonds);

        var totalAmountEtf = portfolio.getTotalAmountEtfs();
        logger.info("общая стоимость фондов в портфеле {}", totalAmountEtf);

        var totalAmountCurrencies = portfolio.getTotalAmountCurrencies();
        logger.info("общая стоимость валют в портфеле {}", totalAmountCurrencies.getValue());

        var totalAmountFutures = portfolio.getTotalAmountFutures();
        logger.info("общая стоимость фьючерсов в портфеле {}", totalAmountFutures);

        var totalAmountShares = portfolio.getTotalAmountShares();
        logger.info("общая стоимость акций в портфеле {}", totalAmountShares);

        logger.info("текущая доходность портфеля {}", portfolio.getExpectedYield());

        var positions = portfolio.getPositions();
        logger.info("в портфолио {} позиций", positions.size());
        for (int i = 0; i < Math.min(positions.size(), 5); i++) {
            var position = positions.get(i);
            var figi = position.getFigi();
            var quantity = position.getQuantity();
            var currentPrice = position.getCurrentPrice();
            var expectedYield = position.getExpectedYield();
            logger.info("позиция с figi: {}, количество инструмента: {}, текущая цена инструмента: {}, текущая расчитанная " + "доходность: {}", figi, quantity, currentPrice, expectedYield);
        }

    }
}