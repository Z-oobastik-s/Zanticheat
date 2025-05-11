package org.zoobastiks.zanticheat.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.zoobastiks.zanticheat.ZAnticheat

object Logger {
    private var prefix = "&8[&cZanticheat&8]&r "
    private lateinit var plugin: ZAnticheat
    private val miniMessage = MiniMessage.miniMessage()
    
    fun init(plugin: ZAnticheat) {
        this.plugin = plugin
        // Не загружаем префикс из конфигурации здесь, так как configManager еще не инициализирован
        // Префикс будет обновлен позже после загрузки конфигурации
    }
    
    /**
     * Обновляет префикс из конфигурации
     */
    fun updatePrefix() {
        if (::plugin.isInitialized) {
            try {
                prefix = plugin.configManager.config.getString("logging.prefix", "") ?: ""
                if (prefix.isNotEmpty() && !prefix.endsWith(" ")) {
                    prefix += " " // Добавляем пробел после префикса, если он не пустой
                }
            } catch (e: UninitializedPropertyAccessException) {
                // ConfigManager еще не инициализирован, используем префикс по умолчанию
            }
        }
    }
    
    fun info(message: String) {
        val formatted = colorize(prefix + message)
        plugin.server.consoleSender.sendMessage(formatted)
    }
    
    fun warning(message: String) {
        val formatted = colorize(prefix + message)
        plugin.server.consoleSender.sendMessage(formatted)
    }
    
    fun error(message: String) {
        val formatted = colorize(prefix + message)
        plugin.server.consoleSender.sendMessage(formatted)
    }
    
    fun debug(message: String) {
        try {
            if (::plugin.isInitialized && plugin.configManager.config.getBoolean("debug", false)) {
                val formatted = colorize(prefix + "&7[DEBUG] &f" + message)
                plugin.server.consoleSender.sendMessage(formatted)
            }
        } catch (e: UninitializedPropertyAccessException) {
            // ConfigManager еще не инициализирован, не выводим отладочные сообщения
        }
    }
    
    fun broadcast(message: String, permission: String) {
        val formatted = colorize(prefix + message)
        
        for (player in Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(formatted)
            }
        }
        
        // Также отправляем в консоль
        plugin.server.consoleSender.sendMessage(formatted)
    }
    
    fun sendMessage(player: Player, message: String) {
        val formatted = colorize(prefix + message)
        player.sendMessage(formatted)
    }
    
    /**
     * Преобразует строку с цветовыми кодами и поддерживает градиенты через MiniMessage
     */
    fun colorize(text: String): Component {
        // Сначала заменяем стандартные & цветовые коды на MiniMessage форматирование
        var processedText = text
            .replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            .replace("&r", "<reset>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>")
            .replace("&o", "<italic>")
            .replace("&k", "<obfuscated>")
        
        // Затем парсим текст с использованием MiniMessage
        return miniMessage.deserialize(processedText)
    }
} 