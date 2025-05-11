package org.zoobastiks.zanticheat.commands

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.utils.Logger

class ZAnticheatCommand(private val plugin: ZAnticheat) : CommandExecutor, TabCompleter {
    
    /**
     * Обработка команды /zanticheat
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Проверка разрешения на использование команды
        if (!sender.hasPermission("zanticheat.admin")) {
            Logger.sendMessage(sender as? Player ?: return true, 
                plugin.configManager.getMessage("messages.no_permission", "&cУ вас нет прав на использование этой команды"))
            return true
        }
        
        // Если нет аргументов, показываем помощь
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }
        
        // Обработка команд
        when (args[0].lowercase()) {
            "reload" -> {
                // Команда перезагрузки конфигурации
                if (!sender.hasPermission("zanticheat.reload")) {
                    Logger.sendMessage(sender as? Player ?: return true, 
                        plugin.configManager.getMessage("messages.no_permission", "&cУ вас нет прав на использование этой команды"))
                    return true
                }
                
                plugin.reload()
                Logger.sendMessage(sender as? Player ?: return true,
                    plugin.configManager.getMessage("messages.config_reloaded", "&aКонфигурация успешно перезагружена!"))
            }
            
            "check" -> {
                // Команда проверки игрока
                if (!sender.hasPermission("zanticheat.check")) {
                    Logger.sendMessage(sender as? Player ?: return true, 
                        plugin.configManager.getMessage("messages.no_permission", "&cУ вас нет прав на использование этой команды"))
                    return true
                }
                
                // Проверяем, есть ли указанный игрок
                if (args.size < 2) {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.player_not_specified", "&cНе указан игрок для проверки"))
                    return true
                }
                
                val targetName = args[1]
                val targetPlayer = Bukkit.getPlayerExact(targetName)
                
                if (targetPlayer == null) {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.player_not_found", "&cИгрок &f%player% &cне найден").replace("%player%", targetName))
                    return true
                }
                
                // Проверка игрока на читы
                Logger.sendMessage(sender as? Player ?: return true,
                    plugin.configManager.getMessage("messages.checking_player", "&7Проверка игрока &f%player%&7...").replace("%player%", targetName))
                
                val detected = plugin.detectionManager.checkPlayer(targetPlayer)
                
                if (detected) {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.check_positive", "&cУ игрока &f%player% &cобнаружены подозрительные действия").replace("%player%", targetName))
                } else {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.check_negative", "&aУ игрока &f%player% &aне обнаружено нарушений").replace("%player%", targetName))
                }
            }
            
            "add" -> {
                // Команда добавления игрока в белый список
                if (!sender.hasPermission("zanticheat.whitelist")) {
                    Logger.sendMessage(sender as? Player ?: return true, 
                        plugin.configManager.getMessage("messages.no_permission", "&cУ вас нет прав на использование этой команды"))
                    return true
                }
                
                // Проверяем, есть ли указанный игрок
                if (args.size < 2) {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.whitelist_player_not_specified", "&cНе указан игрок для добавления в белый список"))
                    return true
                }
                
                val playerName = args[1]
                
                // Добавляем игрока в белый список
                val added = plugin.configManager.addPlayerToWhitelist(playerName)
                
                if (added) {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.whitelist_player_added", "&aИгрок &f%player% &aдобавлен в белый список")
                            .replace("%player%", playerName))
                } else {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.whitelist_player_already_added", "&eИгрок &f%player% &eуже находится в белом списке")
                            .replace("%player%", playerName))
                }
            }
            
            "remove" -> {
                // Команда удаления игрока из белого списка
                if (!sender.hasPermission("zanticheat.whitelist")) {
                    Logger.sendMessage(sender as? Player ?: return true, 
                        plugin.configManager.getMessage("messages.no_permission", "&cУ вас нет прав на использование этой команды"))
                    return true
                }
                
                // Проверяем, есть ли указанный игрок
                if (args.size < 2) {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.whitelist_player_not_specified", "&cНе указан игрок для удаления из белого списка"))
                    return true
                }
                
                val playerName = args[1]
                
                // Удаляем игрока из белого списка
                val removed = plugin.configManager.removePlayerFromWhitelist(playerName)
                
                if (removed) {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.whitelist_player_removed", "&aИгрок &f%player% &aудален из белого списка")
                            .replace("%player%", playerName))
                } else {
                    Logger.sendMessage(sender as? Player ?: return true,
                        plugin.configManager.getMessage("messages.whitelist_player_not_found", "&eИгрок &f%player% &eне найден в белом списке")
                            .replace("%player%", playerName))
                }
            }
            
            "status" -> {
                // Команда просмотра статуса системы
                if (!sender.hasPermission("zanticheat.status")) {
                    Logger.sendMessage(sender as? Player ?: return true, 
                        plugin.configManager.getMessage("messages.no_permission", "&cУ вас нет прав на использование этой команды"))
                    return true
                }
                
                // Информация о статусе системы
                Logger.sendMessage(sender as? Player ?: return true, "&7--- &f&lZanticheat Статус &7---")
                Logger.sendMessage(sender as? Player ?: return true, "&7Версия: &f${plugin.description.version}")
                
                // Статус детекторов
                Logger.sendMessage(sender as? Player ?: return true, "&7Детекторы:")
                Logger.sendMessage(sender as? Player ?: return true, "&7- SpeedHack: ${getStatusText("SpeedHack")}")
                Logger.sendMessage(sender as? Player ?: return true, "&7- FlyHack: ${getStatusText("FlyHack")}")
                Logger.sendMessage(sender as? Player ?: return true, "&7- NoFall: ${getStatusText("NoFall")}")
                Logger.sendMessage(sender as? Player ?: return true, "&7- ElytraFly: ${getStatusText("ElytraFly")}")
                Logger.sendMessage(sender as? Player ?: return true, "&7- ElytraBoost: ${getStatusText("ElytraBoost")}")
                
                // Статус WorldGuard интеграции
                val wgStatus = if (plugin.server.pluginManager.getPlugin("WorldGuard") != null) "&aПодключен" else "&cНе найден"
                Logger.sendMessage(sender as? Player ?: return true, "&7WorldGuard: $wgStatus")
                
                // Дополнительная информация
                val regionProtection = if (plugin.configManager.isRegionProtectionEnabled()) "&aВключено" else "&cВыключено"
                Logger.sendMessage(sender as? Player ?: return true, "&7Защита регионов: $regionProtection")
                
                // Информация о белом списке
                val whitelistedPlayers = plugin.configManager.getWhitelistedPlayers()
                Logger.sendMessage(sender as? Player ?: return true, 
                    plugin.configManager.getMessage("messages.whitelist_status", "&7Игроков в белом списке: &f%count%")
                        .replace("%count%", whitelistedPlayers.size.toString()))
                
                if (whitelistedPlayers.isNotEmpty()) {
                    Logger.sendMessage(sender as? Player ?: return true, 
                        plugin.configManager.getMessage("messages.whitelist_list", "&7Белый список: &f%players%")
                            .replace("%players%", whitelistedPlayers.joinToString(", ")))
                }
            }
            
            "help" -> {
                showHelp(sender)
            }
            
            else -> {
                // Неизвестная команда
                Logger.sendMessage(sender as? Player ?: return true,
                    plugin.configManager.getMessage("messages.unknown_command", "&cНеизвестная команда. Используйте &f/zac help &cдля справки"))
            }
        }
        
        return true
    }
    
    /**
     * Отображает помощь по командам
     */
    private fun showHelp(sender: CommandSender) {
        Logger.sendMessage(sender as? Player ?: return, "&7--- &f&lZanticheat Помощь &7---")
        Logger.sendMessage(sender, "&f/zac reload &7- Перезагрузить конфигурацию")
        Logger.sendMessage(sender, "&f/zac check <игрок> &7- Проверить игрока на читы")
        Logger.sendMessage(sender, "&f/zac add <игрок> &7- Добавить игрока в белый список")
        Logger.sendMessage(sender, "&f/zac remove <игрок> &7- Удалить игрока из белого списка")
        Logger.sendMessage(sender, "&f/zac status &7- Показать статус системы")
        Logger.sendMessage(sender, "&f/zac help &7- Показать это сообщение")
    }
    
    /**
     * Возвращает текст статуса для детектора
     */
    private fun getStatusText(detectorName: String): String {
        return if (plugin.configManager.isDetectorEnabled(detectorName)) "&aВключен" else "&cВыключен"
    }
    
    /**
     * TabCompleter для команды
     */
    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String>? {
        // Если у игрока нет прав на использование команды, не предлагаем подсказки
        if (!sender.hasPermission("zanticheat.admin")) {
            return emptyList()
        }
        
        if (args.size == 1) {
            // Первый аргумент - подкоманда
            val options = mutableListOf<String>()
            
            if (sender.hasPermission("zanticheat.reload")) options.add("reload")
            if (sender.hasPermission("zanticheat.check")) options.add("check")
            if (sender.hasPermission("zanticheat.whitelist")) {
                options.add("add")
                options.add("remove")
            }
            if (sender.hasPermission("zanticheat.status")) options.add("status")
            options.add("help")
            
            return options.filter { it.startsWith(args[0].lowercase()) }
        } else if (args.size == 2) {
            when (args[0].lowercase()) {
                "check" -> {
                    // Если команда "check", предлагаем ники игроков
                    if (sender.hasPermission("zanticheat.check")) {
                        return Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                    }
                }
                "add" -> {
                    // Если команда "add", предлагаем ники игроков, которых нет в белом списке
                    if (sender.hasPermission("zanticheat.whitelist")) {
                        val whitelistedPlayers = plugin.configManager.getWhitelistedPlayers()
                        return Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { !whitelistedPlayers.contains(it) }
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                    }
                }
                "remove" -> {
                    // Если команда "remove", предлагаем ники игроков из белого списка
                    if (sender.hasPermission("zanticheat.whitelist")) {
                        val whitelistedPlayers = plugin.configManager.getWhitelistedPlayers()
                        return whitelistedPlayers
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                    }
                }
            }
        }
        
        return emptyList()
    }
} 