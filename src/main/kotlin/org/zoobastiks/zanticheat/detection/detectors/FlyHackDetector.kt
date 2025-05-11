package org.zoobastiks.zanticheat.detection.detectors

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.detection.CheatDetector
import org.zoobastiks.zanticheat.utils.Logger

class FlyHackDetector(private val plugin: ZAnticheat) : CheatDetector {
    
    override val name = "FlyHack"
    override val description = "Обнаружение несанкционированного полета"
    
    // Карта для хранения последних состояний игроков
    private val lastPositions = mutableMapOf<String, LastState>()
    
    // Карта для отслеживания использования элитр
    private val elytraUsage = mutableMapOf<String, Long>()
    
    // Класс для хранения данных о последнем состоянии
    private data class LastState(
        val y: Double,
        val onGround: Boolean,
        val timeInAir: Int
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
        
        // Отслеживание использования элитр
        if (player.isGliding) {
            elytraUsage[player.name] = System.currentTimeMillis()
            return false
        }
        
        val playerName = player.name
        val currentY = player.location.y
        val isOnGround = player.isOnGround
        
        // Получение или создание последнего состояния
        val lastState = lastPositions[playerName] ?: LastState(currentY, isOnGround, 0)
        
        // Рассчитываем новое состояние
        val newTimeInAir = if (isOnGround) 0 else lastState.timeInAir + 1
        
        // Обновляем состояние
        lastPositions[playerName] = LastState(currentY, isOnGround, newTimeInAir)
        
        // Получаем порог из конфига
        val airTimeThreshold = plugin.configManager.config.getInt("detectors.FlyHack.air_time_threshold", 40)
        
        // Проверка на подозрительный полет
        // Если игрок не на земле долгое время, и y-координата не уменьшается
        if (newTimeInAir > airTimeThreshold && currentY >= lastState.y) {
            // Учет различных краевых случаев
            
            // Проверяем блоки вокруг игрока, чтобы исключить лестницы, воду и т.д.
            val location = player.location
            
            // Проверка блоков вокруг для выявления ложных срабатываний
            if (hasNearbyClimbableBlocks(player) || hasNearbyWaterOrLava(player) || isUsingElytra(player)) {
                return false
            }
            
            // Проверка на недавнее использование элитр
            val lastElytraUse = elytraUsage[playerName] ?: 0L
            val elytraCooldown = System.currentTimeMillis() - lastElytraUse
            val elytraCooldownMs = plugin.configManager.config.getLong("detection.elytra.cooldown_ms", 5000L)
            
            // Если игрок недавно использовал элитры, пропускаем проверку
            if (elytraCooldown < elytraCooldownMs) {
                Logger.debug("Игрок ${player.name} недавно использовал элитры, пропускаем проверку FlyHack")
                return false
            }
            
            val description = "Время в воздухе: $newTimeInAir тиков, Y: ${"%.2f".format(currentY)}"
            plugin.detectionManager.registerViolation(player, name, description)
            
            Logger.debug("Обнаружен FlyHack у ${player.name}: $description")
            return true
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
        
        // Проверка настройки игнорирования элитр
        val ignoreElytra = plugin.configManager.config.getBoolean("detectors.FlyHack.ignore_elytra", true)
        
        // Пропуск если игрок использует элитры и настройка включена
        if (ignoreElytra && isUsingElytra(player)) {
            return true
        }
        
        // Пропуск если игрок имеет эффект медленного падения
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
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
        
        // Пропуск если игрок находится в транспорте (лодка и т.д.)
        if (player.isInsideVehicle) {
            return true
        }
        
        return false
    }
    
    /**
     * Проверяет, использует ли игрок элитры
     */
    private fun isUsingElytra(player: Player): Boolean {
        // Проверка на наличие элитр на игроке
        val chestplate = player.inventory.chestplate
        
        // Проверка на активное использование элитр
        if (player.isGliding) {
            return true
        }
        
        // Проверка на наличие элитр в слоте брони
        if (chestplate != null && chestplate.type == Material.ELYTRA) {
            // Дополнительная проверка, если игрок недавно использовал элитры
            val velocity = player.velocity
            // Если игрок падает с высокой горизонтальной скоростью, вероятно, это полет на элитрах
            if (velocity.length() > 0.5 && Math.abs(velocity.y) < 0.8) {
                return true
            }
            
            // Проверяем, использовал ли игрок недавно элитры
            val playerName = player.name
            val lastElytraUse = elytraUsage[playerName] ?: 0L
            val elytraCooldown = System.currentTimeMillis() - lastElytraUse
            val elytraCooldownMs = plugin.configManager.config.getLong("detection.elytra.cooldown_ms", 5000L)
            
            if (elytraCooldown < elytraCooldownMs) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Проверяет, есть ли рядом с игроком блоки, по которым можно карабкаться
     */
    private fun hasNearbyClimbableBlocks(player: Player): Boolean {
        val loc = player.location
        
        // Проверка текущего блока и блоков вокруг
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    val block = loc.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
                    val type = block.type
                    
                    // Список блоков, по которым можно карабкаться
                    when (type) {
                        Material.LADDER, Material.VINE, Material.TWISTING_VINES, 
                        Material.WEEPING_VINES, Material.SCAFFOLDING, Material.BAMBOO -> return true
                        else -> {}
                    }
                }
            }
        }
        
        return false
    }
    
    /**
     * Проверяет, есть ли рядом с игроком вода или лава
     */
    private fun hasNearbyWaterOrLava(player: Player): Boolean {
        val loc = player.location
        
        // Проверка текущего блока и блоков вокруг
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    val block = loc.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
                    
                    if (block.isLiquid) {
                        return true
                    }
                }
            }
        }
        
        return false
    }
} 