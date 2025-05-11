package org.zoobastiks.zanticheat.detection.detectors

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.detection.CheatDetector
import org.zoobastiks.zanticheat.utils.Logger
import kotlin.math.abs

class ElytraBoostDetector(private val plugin: ZAnticheat) : CheatDetector {
    
    override val name = "ElytraBoost"
    override val description = "Обнаружение неестественного ускорения при полете на элитрах"
    
    // Карта для хранения последних состояний игроков
    private val lastPositions = mutableMapOf<String, LastState>()
    
    // Карта для отслеживания использования фейерверков
    private val fireworkUsage = mutableMapOf<String, Long>()
    
    // Карта для отслеживания наличия фейерверков в инвентаре
    private val hadFireworks = mutableMapOf<String, Boolean>()
    
    // Карта для отслеживания количества фейерверков в инвентаре
    private val fireworkCount = mutableMapOf<String, Int>()
    
    // Карта для отслеживания ускорений без расхода фейерверков
    private val suspiciousAccelerations = mutableMapOf<String, MutableList<AccelerationEvent>>()
    
    // Карта для отслеживания последовательных ускорений
    private val consecutiveAccelerations = mutableMapOf<String, Int>()
    
    // Карта для отслеживания паттернов ускорения
    private val accelerationPatterns = mutableMapOf<String, MutableList<Double>>()
    
    // Настройки из конфигурации
    private var maxAcceleration = 1.5
    private var maxSpeed = 25.0
    private var fireworkCooldownMs = 3000L
    private var suspiciousAccelerationsThreshold = 2
    private var suspiciousTimeWindowMs = 10000L
    private var consecutiveAccelerationsThreshold = 4
    
    init {
        // Загружаем настройки из конфигурации
        loadConfig()
    }
    
    /**
     * Загружает настройки из конфигурации
     */
    private fun loadConfig() {
        maxAcceleration = plugin.configManager.config.getDouble("detectors.ElytraBoost.max_acceleration", 1.5)
        maxSpeed = plugin.configManager.config.getDouble("detectors.ElytraBoost.max_speed", 25.0)
        fireworkCooldownMs = plugin.configManager.config.getLong("detectors.ElytraBoost.firework_cooldown_ms", 3000L)
        suspiciousAccelerationsThreshold = plugin.configManager.config.getInt("detectors.ElytraBoost.suspicious_accelerations", 2)
        suspiciousTimeWindowMs = plugin.configManager.config.getLong("detectors.ElytraBoost.suspicious_time_window_ms", 10000L)
        consecutiveAccelerationsThreshold = plugin.configManager.config.getInt("detectors.ElytraBoost.consecutive_accelerations", 4)
    }
    
    /**
     * Перезагружает настройки детектора из конфигурации
     */
    override fun reload() {
        loadConfig()
        Logger.debug("Детектор $name перезагружен с новыми настройками")
    }
    
    // Класс для хранения данных о последнем состоянии
    private data class LastState(
        val x: Double,
        val y: Double,
        val z: Double,
        val velocity: Double,
        val direction: Direction,
        val timeStamp: Long
    )
    
    // Класс для хранения данных о событии ускорения
    private data class AccelerationEvent(
        val acceleration: Double,
        val timeStamp: Long
    )
    
    // Класс для представления направления движения
    private data class Direction(
        val x: Double,
        val y: Double,
        val z: Double
    )
    
    override fun check(player: Player): Boolean {
        // Пропускаем игроков в креативе или спектаторе
        if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR) {
            return false
        }
        
        // Проверяем, что игрок использует элитры
        if (!player.isGliding) {
            return false
        }
        
        val playerName = player.name
        
        // Проверка на использование фейерверков
        val currentTime = System.currentTimeMillis()
        val lastFireworkUse = fireworkUsage.getOrDefault(playerName, 0L)
        val fireworkCooldown = currentTime - lastFireworkUse
        
        // Если недавно был использован фейерверк, пропускаем проверку
        if (fireworkCooldown < fireworkCooldownMs) {
            return false
        }
        
        // Проверяем наличие фейерверков в инвентаре
        val currentFireworks = countFireworks(player)
        val hadFireworksLastCheck = hadFireworks.getOrDefault(playerName, false)
        val lastFireworkCount = fireworkCount.getOrDefault(playerName, 0)
        
        // Обновляем информацию о фейерверках
        hadFireworks[playerName] = currentFireworks > 0
        fireworkCount[playerName] = currentFireworks
        
        // Получаем текущую позицию и скорость
        val location = player.location
        val currentX = location.x
        val currentY = location.y
        val currentZ = location.z
        
        // Получаем направление движения
        val direction = player.location.direction
        val currentDirection = Direction(direction.x, direction.y, direction.z)
        
        // Если это первая проверка для игрока
        if (!lastPositions.containsKey(playerName)) {
            lastPositions[playerName] = LastState(
                currentX, currentY, currentZ, 0.0, currentDirection, currentTime
            )
            return false
        }
        
        // Получаем последнее состояние
        val lastState = lastPositions[playerName]!!
        val timeDelta = (currentTime - lastState.timeStamp) / 1000.0 // в секундах
        
        // Если прошло слишком мало времени, пропускаем проверку
        if (timeDelta < 0.1) {
            return false
        }
        
        // Вычисляем скорость и ускорение
        val dx = currentX - lastState.x
        val dy = currentY - lastState.y
        val dz = currentZ - lastState.z
        val distance = Math.sqrt(dx * dx + dz * dz) // Горизонтальное расстояние
        val currentVelocity = distance / timeDelta
        val acceleration = (currentVelocity - lastState.velocity) / timeDelta
        
        // Проверяем изменение направления
        val directionChange = Math.abs(currentDirection.x - lastState.direction.x) +
                              Math.abs(currentDirection.y - lastState.direction.y) +
                              Math.abs(currentDirection.z - lastState.direction.z)
        
        // Обновляем последнюю позицию
        lastPositions[playerName] = LastState(
            currentX, currentY, currentZ, currentVelocity, currentDirection, currentTime
        )
        
        // Проверяем на подозрительные ускорения
        if (acceleration > maxAcceleration && currentVelocity > maxSpeed) {
            // Проверяем, был ли расход фейерверков
            val usedFirework = hadFireworksLastCheck && currentFireworks < lastFireworkCount
            
            if (!usedFirework) {
                // Добавляем событие ускорения
                val accelerationList = suspiciousAccelerations.getOrPut(playerName) { mutableListOf() }
                accelerationList.add(AccelerationEvent(acceleration, currentTime))
                
                // Удаляем старые события
                val cutoffTime = currentTime - suspiciousTimeWindowMs
                suspiciousAccelerations[playerName] = accelerationList.filter { it.timeStamp >= cutoffTime }.toMutableList()
                
                // Проверяем количество подозрительных ускорений
                if (accelerationList.size >= suspiciousAccelerationsThreshold) {
                    plugin.detectionManager.registerViolation(
                        player,
                        name,
                        "Обнаружено $acceleration ускорение без использования фейерверков (скорость: $currentVelocity)"
                    )
                    return true
                }
            }
        }
        
        // Отслеживание последовательных ускорений
        if (acceleration > maxAcceleration * 0.8) {
            consecutiveAccelerations[playerName] = (consecutiveAccelerations[playerName] ?: 0) + 1
            
            // Проверяем на последовательные ускорения
            if (consecutiveAccelerations[playerName]!! >= consecutiveAccelerationsThreshold) {
                plugin.detectionManager.registerViolation(
                    player,
                    name,
                    "Обнаружено ${consecutiveAccelerations[playerName]} последовательных ускорений (текущее: $acceleration)"
                )
                consecutiveAccelerations[playerName] = 0
                return true
            }
        } else {
            // Сбрасываем счетчик, если ускорение недостаточное
            consecutiveAccelerations[playerName] = 0
        }
        
        // Отслеживание паттернов ускорения
        val accelerationPattern = accelerationPatterns.getOrPut(playerName) { mutableListOf() }
        accelerationPattern.add(acceleration)
        if (accelerationPattern.size > 10) {
            accelerationPattern.removeAt(0)
        }
        
        // Проверка на неестественные изменения направления при ускорении
        if (acceleration > maxAcceleration * 0.7 && directionChange > 0.5) {
            plugin.detectionManager.registerViolation(
                player,
                name,
                "Обнаружено неестественное изменение направления при ускорении (изменение: $directionChange, ускорение: $acceleration)"
            )
            return true
        }
        
        return false
    }
    
    /**
     * Подсчитывает количество фейерверков в инвентаре игрока
     */
    private fun countFireworks(player: Player): Int {
        var count = 0
        
        // Проверяем основной инвентарь
        for (item in player.inventory.contents) {
            if (item != null && item.type == Material.FIREWORK_ROCKET) {
                count += item.amount
            }
        }
        
        // Проверяем оффхенд
        val offHandItem = player.inventory.itemInOffHand
        if (offHandItem.type == Material.FIREWORK_ROCKET) {
            count += offHandItem.amount
        }
        
        return count
    }
    
    /**
     * Регистрирует использование фейерверка игроком
     */
    fun registerFireworkUse(playerName: String) {
        fireworkUsage[playerName] = System.currentTimeMillis()
    }
} 