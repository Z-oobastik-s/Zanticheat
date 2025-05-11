package org.zoobastiks.zanticheat.detection

import org.bukkit.entity.Player
import java.util.UUID
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.detection.detectors.FlyHackDetector
import org.zoobastiks.zanticheat.detection.detectors.NoFallDetector
import org.zoobastiks.zanticheat.detection.detectors.SpeedHackDetector
import org.zoobastiks.zanticheat.detection.detectors.ElytraFlyDetector
import org.zoobastiks.zanticheat.detection.detectors.ElytraBoostDetector
import org.zoobastiks.zanticheat.utils.Logger

/**
 * Менеджер обнаружения читов
 */
class DetectionManager(private val plugin: ZAnticheat) {
    
    // Карта детекторов
    private val detectors = mutableMapOf<String, CheatDetector>()
    
    // Карта для хранения уровней нарушений игроков
    private val violationLevels = mutableMapOf<UUID, MutableMap<String, Int>>()
    
    // Карта для хранения времени последней проверки
    private val lastDetectionTime = mutableMapOf<UUID, MutableMap<String, Long>>()
    
    init {
        // Регистрация детекторов
        registerDetector(FlyHackDetector(plugin))
        registerDetector(SpeedHackDetector(plugin))
        registerDetector(NoFallDetector(plugin))
        registerDetector(ElytraFlyDetector(plugin))
        registerDetector(ElytraBoostDetector(plugin))
        
        Logger.info("&aЗарегистрировано ${detectors.size} детекторов")
    }
    
    /**
     * Регистрирует детектор
     */
    private fun registerDetector(detector: CheatDetector) {
        detectors[detector.name] = detector
        Logger.debug("Зарегистрирован детектор: ${detector.name}")
    }
    
    /**
     * Перезагружает все детекторы
     */
    fun reloadDetectors() {
        // Обновляем настройки каждого детектора
        detectors.values.forEach { detector ->
            if (detector is Reloadable) {
                detector.reload()
                Logger.debug("Перезагружен детектор: ${detector.name}")
            }
        }
        Logger.debug("Все детекторы перезагружены")
    }
    
    /**
     * Получает детектор по имени
     */
    fun getDetector(name: String): CheatDetector? {
        return detectors[name]
    }
    
    /**
     * Проверяет, находится ли игрок в белом списке
     */
    fun isPlayerWhitelisted(player: Player): Boolean {
        // Проверка на оператора сервера
        if (player.isOp() && plugin.configManager.config.getBoolean("whitelist.bypass_for_ops", true)) {
            return true
        }
        
        // Проверка на специальное разрешение
        if (player.hasPermission("zanticheat.bypass") && 
            plugin.configManager.config.getBoolean("whitelist.bypass_with_permission", true)) {
            return true
        }
        
        // Проверка по имени игрока
        return plugin.configManager.getWhitelistedPlayers().contains(player.name)
    }
    
    /**
     * Проверяет, можно ли сейчас проводить проверку для указанного детектора
     */
    fun canCheck(player: Player, detectorName: String): Boolean {
        // Проверка на белый список
        if (isPlayerWhitelisted(player)) {
            return false
        }
        
        // Проверка на регион WorldGuard
        if (plugin.configManager.isRegionProtectionEnabled() && plugin.worldGuardHook.isInProtectedRegion(player)) {
            return false
        }
        
        // Проверка на активацию детектора
        if (!plugin.configManager.isDetectorEnabled(detectorName)) {
            return false
        }
        
        // Проверка на кулдаун между проверками
        val playerId = player.uniqueId
        val cooldownTime = plugin.configManager.config.getLong("detection.cooldown_ms", 5000)
        
        val lastTimes = lastDetectionTime.getOrPut(playerId) { mutableMapOf() }
        val lastTime = lastTimes[detectorName] ?: 0L
        
        return System.currentTimeMillis() - lastTime >= cooldownTime
    }
    
    /**
     * Регистрирует нарушение и возвращает обновленный уровень нарушений
     */
    fun registerViolation(player: Player, detectorName: String, description: String): Int {
        val playerId = player.uniqueId
        
        // Обновление времени последней проверки
        val lastTimes = lastDetectionTime.getOrPut(playerId) { mutableMapOf() }
        lastTimes[detectorName] = System.currentTimeMillis()
        
        // Увеличение счетчика нарушений
        val violations = violationLevels.getOrPut(playerId) { mutableMapOf() }
        val currentLevel = violations.getOrDefault(detectorName, 0) + 1
        violations[detectorName] = currentLevel
        
        // Логирование нарушения
        logViolation(player, detectorName, description, currentLevel)
        
        // Проверка на необходимость наказания
        val thresholdLevel = plugin.configManager.config.getInt("detectors.$detectorName.threshold_violations", 5)
        if (currentLevel >= thresholdLevel) {
            plugin.punishmentManager.executePunishment(player, detectorName, currentLevel)
        }
        
        return currentLevel
    }
    
    /**
     * Логирует нарушение в консоль и файл
     */
    private fun logViolation(player: Player, detectorName: String, description: String, level: Int) {
        val location = player.location
        val message = "&c[Нарушение] &f${player.name} &7возможно использует &f$detectorName &7(&f$level&7) - $description " +
                "&7в &f${location.world.name} &7на координатах &f${location.blockX}, ${location.blockY}, ${location.blockZ}"
        
        // Логирование в консоль
        Logger.warning(message)
        
        // Оповещение админов
        plugin.server.onlinePlayers.forEach { 
            if (it.hasPermission("zanticheat.notify")) {
                Logger.sendMessage(it, message)
            }
        }
        
        // Логирование в файл
        if (plugin.configManager.config.getBoolean("logging.enabled", true)) {
            // Тут будет добавлено логирование в файл
        }
    }
    
    /**
     * Сбрасывает все нарушения для указанного игрока
     */
    fun resetViolations(player: Player) {
        violationLevels.remove(player.uniqueId)
        lastDetectionTime.remove(player.uniqueId)
    }
    
    /**
     * Выполняет ручную проверку игрока всеми детекторами
     */
    fun checkPlayer(player: Player): Boolean {
        // Если игрок в белом списке, пропускаем проверку
        if (isPlayerWhitelisted(player)) {
            return false
        }
        
        var detected = false
        
        for ((name, detector) in detectors) {
            if (plugin.configManager.isDetectorEnabled(name)) {
                if (detector.check(player)) {
                    detected = true
                }
            }
        }
        
        return detected
    }
} 