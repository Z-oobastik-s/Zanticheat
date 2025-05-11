package org.zoobastiks.zanticheat.detection.detectors

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.detection.CheatDetector
import org.zoobastiks.zanticheat.utils.Logger

class ElytraFlyDetector(private val plugin: ZAnticheat) : CheatDetector {
    
    override val name = "ElytraFly"
    override val description = "Обнаружение неправомерного полета на элитрах"
    
    // Карта для хранения последних состояний игроков
    private val lastPositions = mutableMapOf<String, LastState>()
    
    // Класс для хранения данных о последнем состоянии
    private data class LastState(
        val y: Double,
        val velocity: Double,
        val isGliding: Boolean,
        val timeGliding: Int
    )
    
    override fun check(player: Player): Boolean {
        // Проверка на возможность проведения проверки
        if (!plugin.detectionManager.canCheck(player, name)) {
            return false
        }
        
        // Пропуск проверки для определенных условий
        if (shouldSkipCheck(player)) {
            return false
        }
        
        val playerName = player.name
        val currentY = player.location.y
        val currentVelocity = player.velocity.length()
        val isGliding = player.isGliding
        
        // Получение или создание последнего состояния
        val lastState = lastPositions[playerName] ?: LastState(currentY, currentVelocity, isGliding, 0)
        
        // Обновляем время глайдинга
        val newTimeGliding = if (isGliding) lastState.timeGliding + 1 else 0
        
        // Обновляем состояние
        lastPositions[playerName] = LastState(currentY, currentVelocity, isGliding, newTimeGliding)
        
        // Проверяем наличие элитр в слоте нагрудника
        val chestplate = player.inventory.chestplate
        val hasElytra = chestplate != null && chestplate.type == Material.ELYTRA
        
        // Если игрок летит (isGliding), но у него нет элитр - это явный чит
        if (isGliding && !hasElytra) {
            val description = "Полет на элитрах без экипированных элитр"
            plugin.detectionManager.registerViolation(player, name, description)
            Logger.debug("Обнаружен ElytraFly у ${player.name}: $description")
            return true
        }
        
        // Проверка на неестественное поведение при полете на элитрах
        if (isGliding && hasElytra && newTimeGliding > 20) {
            // Получаем порог из конфига
            val minVelocity = plugin.configManager.config.getDouble("detectors.ElytraFly.min_velocity", 0.1)
            
            // Если скорость слишком низкая для глайдинга, но игрок не падает - это чит
            if (currentVelocity < minVelocity && currentY >= lastState.y) {
                val description = "Неестественный полет на элитрах: скорость ${String.format("%.2f", currentVelocity)}, Y: ${String.format("%.2f", currentY)}"
                plugin.detectionManager.registerViolation(player, name, description)
                Logger.debug("Обнаружен ElytraFly у ${player.name}: $description")
                return true
            }
        }
        
        return false
    }
    
    /**
     * Проверяет, следует ли пропустить проверку для данного игрока
     */
    private fun shouldSkipCheck(player: Player): Boolean {
        // Пропуск для креативного и спектатора режимов
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            return true
        }
        
        // Пропуск для разрешенных полетов
        if (player.isFlying || player.allowFlight) {
            return true
        }
        
        // Пропуск если игрок в воде или лаве
        if (player.location.block.isLiquid) {
            return true
        }
        
        // Пропуск если игрок недавно получил урон
        if (player.noDamageTicks > 0) {
            return true
        }
        
        // Пропуск если игрок находится в транспорте
        if (player.isInsideVehicle) {
            return true
        }
        
        return false
    }
} 