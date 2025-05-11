package org.zoobastiks.zanticheat.detection.detectors

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.detection.CheatDetector
import org.zoobastiks.zanticheat.utils.Logger

class NoFallDetector(private val plugin: ZAnticheat) : CheatDetector {
    
    override val name = "NoFall"
    override val description = "Обнаружение отключения урона от падения"
    
    // Карта для хранения информации о падении
    private val fallData = mutableMapOf<String, FallData>()
    
    // Карта для отслеживания использования элитр
    private val elytraUsage = mutableMapOf<String, Long>()
    
    // Класс для хранения данных о падении
    private data class FallData(
        var maxFallDistance: Float = 0f,
        var wasFalling: Boolean = false,
        var startY: Double = 0.0
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
        val fallDistance = player.fallDistance
        val isOnGround = player.isOnGround
        val currentY = player.location.y
        
        // Получение или создание данных о падении
        val data = fallData.getOrPut(playerName) { FallData() }
        
        // Если игрок на земле и до этого падал
        if (isOnGround && data.wasFalling) {
            // Расчет реального расстояния падения (по Y координатам)
            val actualFallDistance = data.startY - currentY
            
            // Получаем минимальную высоту из конфига, которая вызывает урон от падения
            val minFallHeight = plugin.configManager.config.getDouble("detectors.NoFall.min_fall_height", 3.0)
            
            // Если расстояние достаточно для получения урона
            if (actualFallDistance >= minFallHeight) {
                // Проверяем, использовал ли игрок недавно элитры
                val lastElytraUse = elytraUsage[playerName] ?: 0L
                val elytraCooldown = System.currentTimeMillis() - lastElytraUse
                val elytraCooldownMs = plugin.configManager.config.getLong("detectors.NoFall.elytra_cooldown_ms", 5000L)
                
                // Если игрок недавно использовал элитры, пропускаем проверку
                if (elytraCooldown < elytraCooldownMs) {
                    Logger.debug("Игрок ${player.name} недавно использовал элитры, пропускаем проверку NoFall")
                    // Сброс данных
                    data.wasFalling = false
                    data.maxFallDistance = 0f
                    data.startY = currentY
                    return false
                }
                
                // Если игрок имеет маленькое значение fallDistance, это может быть NoFall
                if (fallDistance < minFallHeight * 0.5 && data.maxFallDistance >= minFallHeight) {
                    val description = "Падение с высоты: ${"%.2f".format(actualFallDistance)} блоков, " +
                            "отчетность клиента: ${"%.2f".format(fallDistance)}"
                    
                    plugin.detectionManager.registerViolation(player, name, description)
                    
                    Logger.debug("Обнаружен NoFall у ${player.name}: $description")
                    
                    // Сброс данных
                    data.wasFalling = false
                    data.maxFallDistance = 0f
                    data.startY = currentY
                    
                    return true
                }
            }
            
            // Сброс данных после падения
            data.wasFalling = false
            data.maxFallDistance = 0f
        }
        // Если игрок начал падать
        else if (fallDistance > 0 && !data.wasFalling) {
            data.wasFalling = true
            data.startY = currentY + fallDistance // Корректировка начальной Y-координаты
        }
        // Если игрок продолжает падать, обновляем максимальное расстояние
        else if (data.wasFalling && fallDistance > data.maxFallDistance) {
            data.maxFallDistance = fallDistance
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
        
        // Пропуск для игроков, использующих элитры
        if (player.isGliding) {
            return true
        }
        
        // Проверка на наличие элитр в слоте брони
        val chestplate = player.inventory.chestplate
        if (chestplate != null && chestplate.type == Material.ELYTRA) {
            // Если у игрока есть элитры и он движется быстро, вероятно он только что приземлился
            val velocity = player.velocity
            if (velocity.length() > 0.5) {
                return true
            }
            
            // Проверяем, использовал ли игрок недавно элитры
            val playerName = player.name
            val lastElytraUse = elytraUsage[playerName] ?: 0L
            val elytraCooldown = System.currentTimeMillis() - lastElytraUse
            val elytraCooldownMs = plugin.configManager.config.getLong("detectors.NoFall.elytra_cooldown_ms", 5000L)
            
            if (elytraCooldown < elytraCooldownMs) {
                return true
            }
        }
        
        // Пропуск если игрок имеет эффекты, снижающие урон от падения
        if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING) ||
            player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
            return true
        }
        
        // Пропуск если игрок в воде или лаве
        if (player.location.block.isLiquid) {
            return true
        }
        
        // Пропуск если игрок находится в транспорте (лодка, лошадь и т.д.)
        if (player.isInsideVehicle) {
            return true
        }
        
        return false
    }
    
    /**
     * Сбрасывает данные о падении игрока
     */
    fun resetFallData(player: Player) {
        fallData.remove(player.name)
        elytraUsage.remove(player.name)
    }
} 