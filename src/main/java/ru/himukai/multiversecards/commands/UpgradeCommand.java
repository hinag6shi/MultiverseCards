package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.Response;
import ru.himukai.multiversecards.domain.card.TemplateId;
import ru.himukai.multiversecards.domain.card.UpgradePayment;
import ru.himukai.multiversecards.domain.card.UpgradeStat;
import ru.himukai.multiversecards.domain.player.PlayerId;
import ru.himukai.multiversecards.service.CardUpgradeService;

import java.util.Locale;
import java.util.Objects;

public final class UpgradeCommand implements Command {

    private final CardUpgradeService upgrades;

    public UpgradeCommand(CardUpgradeService upgrades) {
        this.upgrades = Objects.requireNonNull(upgrades, "upgrades");
    }

    @Override
    public String name() {
        return "upgrade";
    }

    @Override
    public String description() {
        return "Усилить характеристику карточки за золото или осколки";
    }

    @Override
    public String usage() {
        return "/upgrade <id> <hp|st|df> <gold|shards>";
    }

    @Override
    public Response execute(CommandContext ctx) {
        if (ctx.args().size() != 3) {
            return Response.text("Формат: " + usage());
        }
        try {
            TemplateId cardId = new TemplateId(ctx.args().get(0));
            UpgradeStat stat = UpgradeStat.valueOf(ctx.args().get(1).toUpperCase(Locale.ROOT));
            UpgradePayment payment = switch (ctx.args().get(2).toLowerCase(Locale.ROOT)) {
                case "gold" -> UpgradePayment.GOLD;
                case "shards" -> UpgradePayment.SHARDS;
                default -> throw new IllegalArgumentException("Неизвестный способ оплаты");
            };
            boolean upgraded = upgrades.upgrade(playerId(ctx.userId()), cardId, stat, payment);
            return upgraded
                    ? Response.text("Карточка усилена: " + cardId.value() + ". Посмотри новый уровень через /cards.")
                    : Response.text("Усиление не выполнено: проверь карточку, валюту и лимит усилений.");
        } catch (IllegalArgumentException e) {
            return Response.text("Формат: " + usage());
        }
    }

    private static PlayerId playerId(String userId) {
        return new PlayerId(Long.parseLong(userId.substring(userId.indexOf(':') + 1)));
    }
}
