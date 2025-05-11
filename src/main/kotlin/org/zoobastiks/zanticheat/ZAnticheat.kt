package org.zoobastiks.zanticheat

import org.bukkit.plugin.java.JavaPlugin
import org.zoobastiks.zanticheat.commands.ZAnticheatCommand
import org.zoobastiks.zanticheat.config.ConfigManager
import org.zoobastiks.zanticheat.detection.DetectionManager
import org.zoobastiks.zanticheat.hooks.WorldGuardHook
import org.zoobastiks.zanticheat.listeners.PlayerMovementListener
import org.zoobastiks.zanticheat.punishment.PunishmentManager
import org.zoobastiks.zanticheat.utils.Logger

class ZAnticheat : JavaPlugin() {
    
    companion object {
        lateinit var instance: ZAnticheat
            private set
    }
    
    lateinit var configManager: ConfigManager
        private set
    
    lateinit var detectionManager: DetectionManager
        private set
    
    lateinit var punishmentManager: PunishmentManager
        private set
    
    lateinit var worldGuardHook: WorldGuardHook
        private set
    
    override fun onEnable() {
        instance = this
        
        // Инициализация логгера (без обновления префикса)
        Logger.init(this)
        Logger.info("&aЗапуск плагина Zanticheat v${description.version}...")
        
        // Загрузка конфигурации
        configManager = ConfigManager(this)
        configManager.loadConfig()
        
        // Теперь можно обновить префикс логгера
        Logger.updatePrefix()
        
        // Инициализация менеджеров
        detectionManager = DetectionManager(this)
        punishmentManager = PunishmentManager(this)
        
        // Проверка и инициализация WorldGuard
        if (server.pluginManager.getPlugin("WorldGuard") != null) {
            worldGuardHook = WorldGuardHook(this)
            if (worldGuardHook.initialize()) {
                Logger.info("&aИнтеграция с WorldGuard успешно подключена!")
            } else {
                Logger.warning("&eНе удалось инициализировать интеграцию с WorldGuard")
            }
        } else {
            Logger.warning("&eWorldGuard не обнаружен. Функции защиты регионов недоступны")
        }
        
        // Регистрация ивентов
        registerEvents()
        
        // Регистрация команд
        registerCommands()
        
        Logger.info("&aПлагин Zanticheat успешно запущен!")
    }
    
    override fun onDisable() {
        Logger.info("&cПлагин Zanticheat выключается...")
        
        // Выполнить необходимую очистку ресурсов
        
        Logger.info("&cПлагин Zanticheat выключен!")
    }
    
    private fun registerEvents() {
        server.pluginManager.registerEvents(PlayerMovementListener(this), this)
    }
    
    private fun registerCommands() {
        getCommand("zanticheat")?.setExecutor(ZAnticheatCommand(this))
        getCommand("zanticheat")?.tabCompleter = ZAnticheatCommand(this)
    }
    
    fun reload() {
        // Используем новый метод для перезагрузки конфигурации
        configManager.reloadConfig()
        // Обновляем настройки логгера
        Logger.updatePrefix()
        // Обновляем настройки детекторов
        detectionManager.reloadDetectors()
        Logger.info("&aПлагин Zanticheat успешно перезагружен!")
    }
} 