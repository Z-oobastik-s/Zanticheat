package org.zoobastiks.zanticheat.punishment

import org.bukkit.entity.Player
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.utils.Logger

class PunishmentManager(private val plugin: ZAnticheat) {
    
    /**
     * Выполняет действия наказания для игрока, нарушившего правила
     */
    fun executePunishment(player: Player, detectorName: String, violationLevel: Int) {
        val configPath = "punishments.actions.$detectorName"
        
        // Проверяем, настроено ли наказание для данного детектора
        if (!plugin.configManager.config.contains(configPath)) {
            // Используем действия по умолчанию
            executeDefaultPunishment(player, detectorName, violationLevel)
            return
        }
        
        // Получаем список действий для текущего уровня нарушений
        val actions = getPunishmentActions(detectorName, violationLevel)
        
        if (actions.isEmpty()) {
            Logger.debug("Нет настроенных действий для $detectorName при уровне нарушений $violationLevel")
            return
        }
        
        // Выполняем каждое действие
        for (action in actions) {
            executePunishmentAction(player, action, detectorName)
        }
    }
    
    /**
     * Выполняет действия по умолчанию
     */
    private fun executeDefaultPunishment(player: Player, detectorName: String, violationLevel: Int) {
        val configPath = "punishments.default_actions"
        
        // Проверка на существование настроек по умолчанию
        if (!plugin.configManager.config.contains(configPath)) {
            // Если даже по умолчанию нет настроек, просто логируем
            Logger.debug("Нет действий по умолчанию для нарушений")
            return
        }
        
        // Получаем действия по умолчанию
        val actions = getDefaultPunishmentActions(violationLevel)
        
        if (actions.isEmpty()) {
            Logger.debug("Нет действий по умолчанию для уровня нарушений $violationLevel")
            return
        }
        
        // Выполняем каждое действие
        for (action in actions) {
            executePunishmentAction(player, action, detectorName)
        }
    }
    
    /**
     * Возвращает список действий для указанного детектора и уровня нарушений
     */
    private fun getPunishmentActions(detectorName: String, violationLevel: Int): List<PunishmentAction> {
        val result = mutableListOf<PunishmentAction>()
        val configPath = "punishments.actions.$detectorName"
        
        // Проверка наличия действий для данного уровня
        for (levelStr in plugin.configManager.config.getConfigurationSection(configPath)?.getKeys(false) ?: emptySet()) {
            val level = levelStr.toIntOrNull() ?: continue
            
            // Если уровень нарушения >= указанного в конфиге
            if (violationLevel >= level) {
                val actionPath = "$configPath.$levelStr"
                
                // Проверка на авто-команду
                if (plugin.configManager.config.getBoolean("$actionPath.auto_command.enabled", false)) {
                    val command = plugin.configManager.config.getString("$actionPath.auto_command.command", "")
                    if (!command.isNullOrEmpty()) {
                        result.add(PunishmentAction.Command(command))
                    }
                }
                
                // Проверка на множественные команды (старый формат)
                if (plugin.configManager.config.getBoolean("$actionPath.multi_commands.enabled", false)) {
                    val commands = plugin.configManager.config.getStringList("$actionPath.multi_commands.commands")
                    if (commands.isNotEmpty()) {
                        result.add(PunishmentAction.MultiCommands(commands))
                    }
                }
                
                // Проверка на множественные команды (новый формат)
                if (plugin.configManager.config.getBoolean("$actionPath.auto_commands.enabled", false)) {
                    val commands = plugin.configManager.config.getStringList("$actionPath.auto_commands.commands")
                    if (commands.isNotEmpty()) {
                        result.add(PunishmentAction.MultiCommands(commands))
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * Возвращает список действий по умолчанию для указанного уровня нарушений
     */
    private fun getDefaultPunishmentActions(violationLevel: Int): List<PunishmentAction> {
        val result = mutableListOf<PunishmentAction>()
        val configPath = "punishments.default_actions"
        
        // Проверка наличия действий для данного уровня
        for (levelStr in plugin.configManager.config.getConfigurationSection(configPath)?.getKeys(false) ?: emptySet()) {
            val level = levelStr.toIntOrNull() ?: continue
            
            // Если уровень нарушения >= указанного в конфиге
            if (violationLevel >= level) {
                val actionPath = "$configPath.$levelStr"
                
                // Проверка на авто-команду
                if (plugin.configManager.config.getBoolean("$actionPath.auto_command.enabled", false)) {
                    val command = plugin.configManager.config.getString("$actionPath.auto_command.command", "")
                    if (!command.isNullOrEmpty()) {
                        result.add(PunishmentAction.Command(command))
                    }
                }
                
                // Проверка на множественные команды (старый формат)
                if (plugin.configManager.config.getBoolean("$actionPath.multi_commands.enabled", false)) {
                    val commands = plugin.configManager.config.getStringList("$actionPath.multi_commands.commands")
                    if (commands.isNotEmpty()) {
                        result.add(PunishmentAction.MultiCommands(commands))
                    }
                }
                
                // Проверка на множественные команды (новый формат)
                if (plugin.configManager.config.getBoolean("$actionPath.auto_commands.enabled", false)) {
                    val commands = plugin.configManager.config.getStringList("$actionPath.auto_commands.commands")
                    if (commands.isNotEmpty()) {
                        result.add(PunishmentAction.MultiCommands(commands))
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * Выполняет указанное наказание для игрока
     */
    private fun executePunishmentAction(player: Player, action: PunishmentAction, detectorName: String) {
        val playerName = player.name
        
        // Подставляем переменные
        val replacementMap = mapOf(
            "%player%" to playerName,
            "%detector%" to detectorName,
            "%world%" to player.world.name,
            "%x%" to player.location.x.toInt().toString(),
            "%y%" to player.location.y.toInt().toString(),
            "%z%" to player.location.z.toInt().toString()
        )
        
        when (action) {
            is PunishmentAction.Command -> {
                val command = replaceVariables(action.command, replacementMap)
                Logger.debug("Выполнение команды за нарушение: $command")
                
                // Выполнить команду в основном потоке
                plugin.server.scheduler.runTask(plugin, Runnable {
                    plugin.server.dispatchCommand(plugin.server.consoleSender, command)
                })
            }
            
            is PunishmentAction.MultiCommands -> {
                for (cmd in action.commands) {
                    val command = replaceVariables(cmd, replacementMap)
                    Logger.debug("Выполнение команды из списка за нарушение: $command")
                    
                    // Выполнить команду в основном потоке
                    plugin.server.scheduler.runTask(plugin, Runnable {
                        plugin.server.dispatchCommand(plugin.server.consoleSender, command)
                    })
                }
            }
        }
    }
    
    /**
     * Заменяет переменные в строке на их значения
     */
    private fun replaceVariables(text: String, replacements: Map<String, String>): String {
        var result = text
        for ((key, value) in replacements) {
            result = result.replace(key, value)
        }
        return result
    }
} 