package org.zoobastiks.zanticheat.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.utils.Logger
import java.io.File
import java.io.IOException

class ConfigManager(private val plugin: ZAnticheat) {
    
    lateinit var config: FileConfiguration
        private set
    
    private val configFile = File(plugin.dataFolder, "config.yml")
    private val messagesFile = File(plugin.dataFolder, "messages.yml")
    
    lateinit var messages: FileConfiguration
        private set
    
    /**
     * Загружает конфигурацию плагина
     */
    fun loadConfig() {
        try {
            // Создаем директорию плагина, если она не существует
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdirs()
            }
            
            // Создаем файл конфигурации, если он не существует
            if (!configFile.exists()) {
                plugin.saveResource("config.yml", false)
                Logger.info("&aФайл конфигурации создан")
            }
            
            // Создаем файл сообщений, если он не существует
            if (!messagesFile.exists()) {
                plugin.saveResource("messages.yml", false)
                Logger.info("&aФайл сообщений создан")
            }
            
            // Загружаем конфигурацию
            config = YamlConfiguration.loadConfiguration(configFile)
            messages = YamlConfiguration.loadConfiguration(messagesFile)
            
            Logger.info("&aКонфигурация успешно загружена")
        } catch (e: Exception) {
            Logger.error("&cОшибка при загрузке конфигурации: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Перезагружает конфигурацию плагина
     */
    fun reloadConfig() {
        try {
            // Перезагружаем файлы конфигурации
            config = YamlConfiguration.loadConfiguration(configFile)
            messages = YamlConfiguration.loadConfiguration(messagesFile)
            
            // Обновляем префикс в логгере
            Logger.updatePrefix()
            
            Logger.info("&aКонфигурация успешно перезагружена")
        } catch (e: Exception) {
            Logger.error("&cОшибка при перезагрузке конфигурации: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Сохраняет конфигурацию плагина
     */
    fun saveConfig() {
        try {
            config.save(configFile)
            Logger.info("&aКонфигурация успешно сохранена")
        } catch (e: IOException) {
            Logger.error("&cОшибка при сохранении конфигурации: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Сохраняет файл сообщений
     */
    fun saveMessages() {
        try {
            messages.save(messagesFile)
            Logger.info("&aФайл сообщений успешно сохранен")
        } catch (e: IOException) {
            Logger.error("&cОшибка при сохранении сообщений: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Возвращает сообщение из файла сообщений
     */
    fun getMessage(path: String, default: String = "Сообщение не найдено"): String {
        return messages.getString(path, default) ?: default
    }
    
    /**
     * Проверяет, включен ли указанный детектор
     */
    fun isDetectorEnabled(detectorName: String): Boolean {
        return config.getBoolean("detectors.$detectorName.enabled", false)
    }
    
    /**
     * Возвращает порог срабатывания для указанного детектора
     */
    fun getDetectorThreshold(detectorName: String): Double {
        return config.getDouble("detectors.$detectorName.threshold", 1.0)
    }
    
    /**
     * Проверяет, включена ли проверка регионов
     */
    fun isRegionProtectionEnabled(): Boolean {
        return config.getBoolean("region_protection.enabled", true)
    }
    
    /**
     * Проверяет, включен ли WorldGuard
     */
    fun isWorldGuardEnabled(): Boolean {
        return config.getBoolean("region_protection.use_worldguard", true)
    }
    
    /**
     * Возвращает список игроков из белого списка
     */
    fun getWhitelistedPlayers(): List<String> {
        return config.getStringList("whitelist.players")
    }
    
    /**
     * Добавляет игрока в белый список
     * @return true если игрок успешно добавлен, false если игрок уже был в белом списке
     */
    fun addPlayerToWhitelist(playerName: String): Boolean {
        val whitelist = config.getStringList("whitelist.players").toMutableList()
        
        // Проверяем, есть ли уже игрок в белом списке
        if (whitelist.contains(playerName)) {
            return false
        }
        
        // Добавляем игрока в белый список
        whitelist.add(playerName)
        config.set("whitelist.players", whitelist)
        saveConfig()
        return true
    }
    
    /**
     * Удаляет игрока из белого списка
     * @return true если игрок успешно удален, false если игрока не было в белом списке
     */
    fun removePlayerFromWhitelist(playerName: String): Boolean {
        val whitelist = config.getStringList("whitelist.players").toMutableList()
        
        // Проверяем, есть ли игрок в белом списке
        if (!whitelist.contains(playerName)) {
            return false
        }
        
        // Удаляем игрока из белого списка
        whitelist.remove(playerName)
        config.set("whitelist.players", whitelist)
        saveConfig()
        return true
    }
    
    /**
     * Проверяет, включен ли автобан
     */
    fun isAutoBanEnabled(): Boolean {
        return config.getBoolean("punishments.auto_ban.enabled", false)
    }
    
    /**
     * Возвращает причину автобана
     */
    fun getAutoBanReason(): String {
        return config.getString("punishments.auto_ban.reason", "Обнаружен чит") ?: "Обнаружен чит"
    }
} 