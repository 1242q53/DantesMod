package net.salam.entity.custom;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.salam.entity.BulletEntity;

/**
 * Обрабатывает смерть Дантеса:
 * — при первом убийстве запускает экран победы (титры как после дракона)
 * — при повторных — просто сообщение в чат
 */
public class DantesDeathHandler {

    // Тег, который вешается на игрока после первой победы
    private static final String DEFEATED_TAG = "dantes_defeated";

    public static void onDantesKilled(DamageSource source, DantesEntity dantes) {
        ServerPlayerEntity killer = findKiller(source);
        if (killer == null) return;

        boolean firstKill = !killer.getCommandTags().contains(DEFEATED_TAG);

        if (firstKill) {
            // Запоминаем победу — следующие убийства уже не дадут титры
            killer.addCommandTag(DEFEATED_TAG);

            // Запускаем экран с титрами — тот же пакет, что игра шлёт после убийства дракона
            // GameStateChangeS2CPacket reason 4 = "The End" credits screen
            killer.networkHandler.sendPacket(
                    new GameStateChangeS2CPacket(GameStateChangeS2CPacket.GAME_WON, 1.0f)
            );
        } else {
            // Повторная победа — просто текст
            killer.sendMessage(
                    net.minecraft.text.Text.literal("§6Вы снова победили Дантеса!"),
                    false
            );
        }
    }

    /**
     * Определяет убийцу из DamageSource.
     * Поддерживает:
     *  - прямой удар игрока
     *  - попадание пули игрока
     */
    private static ServerPlayerEntity findKiller(DamageSource source) {
        // Прямой урон от игрока (ближний бой)
        if (source.getAttacker() instanceof ServerPlayerEntity player) {
            return player;
        }
        // Урон от пули, выпущенной игроком
        if (source.getSource() instanceof BulletEntity bullet
                && bullet.getOwner() instanceof ServerPlayerEntity player) {
            return player;
        }
        return null;
    }
}