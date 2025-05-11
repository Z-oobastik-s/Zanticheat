package org.zoobastiks.zanticheat.detection.detectors

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.detection.CheatDetector
import org.zoobastiks.zanticheat.utils.Logger
import kotlin.math.hypot

class SpeedHackDetector(private val plugin: ZAnticheat) : CheatDetector {
    
    override val name = "SpeedHack"
    override val description = "Обнаружение аномальной скорости передвижения"
    
    // Карта для хранения последних позиций игроков
    private val lastPositions = mutableMapOf<String, LastPosition>()
    
    // Карта для отслеживания использования элитр
    private val elytraUsage = mutableMapOf<String, Long>()
    
    // Класс для хранения данных о последней позиции
    private data class LastPosition(
        val x: Double,
        val y: Double,
        val z: Double,
        val timeStamp: Long
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
        
        // Текущая позиция и время
        val currentX = player.location.x
        val currentY = player.location.y
        val currentZ = player.location.z
        val currentTime = System.currentTimeMillis()
        
        val playerName = player.name
        
        // Получение последней сохраненной позиции
        val lastPos = lastPositions[playerName]
        
        // Сохранение текущей позиции для следующей проверки
        lastPositions[playerName] = LastPosition(currentX, currentY, currentZ, currentTime)
        
        // Если это первое обнаружение, просто сохраняем позицию
        if (lastPos == null) {
            return false
        }
        
        // Расчет времени между проверками в секундах
        val timeDiff = (currentTime - lastPos.timeStamp) / 1000.0
        
        // Если прошло слишком много времени, сбрасываем
        if (timeDiff > 5.0) {
            return false
        }
        
        // Расчет пройденного расстояния (2D - только по горизонтали)
        val distance = hypot(currentX - lastPos.x, currentZ - lastPos.z)
        
        // Расчет скорости в блоках в секунду
        val speed = distance / timeDiff
        
        // Получение максимальной разрешенной скорости из конфига
        val maxAllowedSpeed = getMaxAllowedSpeed(player)
        
        // Проверка на превышение скорости
        if (speed > maxAllowedSpeed) {
            // Проверка на недавнее использование элитр
            val lastElytraUse = elytraUsage[playerName] ?: 0L
            val elytraCooldown = System.currentTimeMillis() - lastElytraUse
            val elytraCooldownMs = plugin.configManager.config.getLong("detection.elytra.cooldown_ms", 5000L)
            
            // Если игрок недавно использовал элитры, пропускаем проверку
            if (elytraCooldown < elytraCooldownMs) {
                Logger.debug("Игрок ${player.name} недавно использовал элитры, пропускаем проверку SpeedHack")
                return false
            }
            
            // Регистрация нарушения
            val description = "Скорость: ${"%.2f".format(speed)} блоков/сек (макс: ${"%.2f".format(maxAllowedSpeed)})"
            plugin.detectionManager.registerViolation(player, name, description)
            
            Logger.debug("Обнаружен SpeedHack у ${player.name}: $description")
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
        
        // Пропуск для полетов
        if (player.isFlying || player.allowFlight) {
            return true
        }
        
        // Пропуск для игроков, использующих элитры
        if (isUsingElytra(player)) {
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
        
        // Пропуск если игрок находится в транспорте (лодка, лошадь и т.д.)
        if (player.isInsideVehicle) {
            return true
        }
        
        return false
    }
    
    /**
     * Проверяет, использует ли игрок элитры
     */
    private fun isUsingElytra(player: Player): Boolean {
        // Проверка на активное использование элитр
        if (player.isGliding) {
            return true
        }
        
        // Проверка на наличие элитр в слоте брони
        val chestplate = player.inventory.chestplate
        if (chestplate != null && chestplate.type == Material.ELYTRA) {
            // Дополнительная проверка на скорость движения
            val velocity = player.velocity
            if (velocity.length() > 0.5) {
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
     * Рассчитывает максимальную разрешенную скорость для игрока
     */
    private fun getMaxAllowedSpeed(player: Player): Double {
        var baseSpeed = plugin.configManager.config.getDouble("detectors.SpeedHack.max_speed", 7.0)
        
        // Увеличение макс. скорости при эффекте зелья скорости
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            val speedEffect = player.getPotionEffect(PotionEffectType.SPEED)
            if (speedEffect != null) {
                // Каждый уровень эффекта увеличивает скорость на 20%
                baseSpeed += baseSpeed * (speedEffect.amplifier + 1) * 0.2
            }
        }
        
        // Увеличение при скоростном зачаровании предмета в руке
        val mainHand = player.inventory.itemInMainHand
        if (mainHand.hasItemMeta() && mainHand.itemMeta.hasEnchant(org.bukkit.enchantments.Enchantment.SOUL_SPEED)) {
            val level = mainHand.itemMeta.getEnchantLevel(org.bukkit.enchantments.Enchantment.SOUL_SPEED)
            baseSpeed += level * 0.5
        }
        
        // Увеличение скорости для игроков с элитрами, которые только что приземлились
        val chestplate = player.inventory.chestplate
        if (chestplate != null && chestplate.type == Material.ELYTRA) {
            val elytraSpeedMultiplier = plugin.configManager.config.getDouble("detectors.SpeedHack.elytra_speed_multiplier", 1.5)
            baseSpeed *= elytraSpeedMultiplier
        }
        
        return baseSpeed
    }
} 