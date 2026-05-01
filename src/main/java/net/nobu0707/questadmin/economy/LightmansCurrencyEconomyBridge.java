package net.nobu0707.questadmin.economy;

import com.mojang.logging.LogUtils;
import io.github.lightman314.lightmanscurrency.api.money.bank.BankAPI;
import io.github.lightman314.lightmanscurrency.api.money.bank.IBankAccount;
import io.github.lightman314.lightmanscurrency.api.money.bank.reference.BankReference;
import io.github.lightman314.lightmanscurrency.api.money.bank.reference.builtin.PlayerBankReference;
import io.github.lightman314.lightmanscurrency.api.money.coins.CoinAPI;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue;
import io.github.lightman314.lightmanscurrency.api.money.value.builtin.CoinValue;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.UUID;

public final class LightmansCurrencyEconomyBridge implements EconomyBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LIGHTMANS_CURRENCY_MOD_ID = "lightmanscurrency";

    @Override
    public boolean isAvailable() {
        if (!ModList.get().isLoaded(LIGHTMANS_CURRENCY_MOD_ID)) {
            return false;
        }

        try {
            return BankAPI.getApi() != null && CoinAPI.getApi() != null;
        } catch (LinkageError | RuntimeException exception) {
            LOGGER.warn("Lightman's Currency API is not available.", exception);
            return false;
        }
    }

    @Override
    public boolean deposit(UUID playerUuid, long amount) {
        if (playerUuid == null || amount <= 0 || !isAvailable()) {
            return false;
        }

        try {
            BankReference reference = PlayerBankReference.of(playerUuid);
            IBankAccount account = reference.get();
            if (account == null) {
                LOGGER.warn("Lightman's Currency bank account was not found for player UUID {}.", playerUuid);
                return false;
            }

            MoneyValue moneyValue = CoinValue.fromNumber(CoinAPI.MAIN_CHAIN, amount);
            if (moneyValue == null || moneyValue.isInvalid() || moneyValue.isEmpty()) {
                LOGGER.warn("Failed to create a valid Lightman's Currency money value for amount {}.", amount);
                return false;
            }

            return BankAPI.getApi().BankDepositFromServer(account, moneyValue);
        } catch (LinkageError | RuntimeException exception) {
            LOGGER.error("Failed to deposit Lightman's Currency reward for player UUID {}.", playerUuid, exception);
            return false;
        }
    }

    @Override
    public String getCurrencyName() {
        return LIGHTMANS_CURRENCY_MOD_ID;
    }

    @Override
    public String getBridgeName() {
        return "LightmansCurrencyEconomyBridge";
    }
}
