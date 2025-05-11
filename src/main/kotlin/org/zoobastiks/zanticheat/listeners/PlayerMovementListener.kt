package org.zoobastiks.zanticheat.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.Material
import org.bukkit.scheduler.BukkitTask
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.detection.detectors.NoFallDetector
import org.zoobastiks.zanticheat.detection.detectors.ElytraBoostDetector
import org.zoobastiks.zanticheat.utils.Logger

class PlayerMovementListener(private val plugin: ZAnticheat) : Listener {
    
    private val playerCheckerTasks = mutableMapOf<String, BukkitTask>()
    
    // Карта для отслеживания времени входа игроков
    private val playerJoinTimes = mutableMapOf<String, Long>()
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Если событие отменено, не обрабатываем его
        if (event.isCancelled) return
        
        // Получаем параметры события
        val player = event.player
        
        // Проверяем, если игрок не перемещается по координатам X/Z, пропускаем
        val from = event.from
        val to = event.to ?: return
        
        // Если игрок только повернул голову, пропускаем
        if (from.x == to.x && from.y == to.y && from.z == to.z) return
        
        // Проверяем, прошло ли достаточно времени после входа игрока
        val joinTime = playerJoinTimes[player.name] ?: 0L
        val currentTime = System.currentTimeMillis()
        val joinDelay = plugin.configManager.config.getLong("detection.join_delay_ms", 30000L)
        
        if (currentTime - joinTime < joinDelay) {
            return // Пропускаем проверку, если не прошло достаточно времени после входа
        }
        
        // Проверяем игрока на читы
        checkPlayer(player)
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Записываем время входа игрока
        playerJoinTimes[player.name] = System.currentTimeMillis()
        
        // Устанавливаем задачу периодической проверки с задержкой
        val joinDelay = plugin.configManager.config.getLong("detection.join_delay_ms", 30000L) / 50 // Конвертация в тики
        
        // Запускаем задачу с задержкой
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            startPlayerChecker(player)
        }, joinDelay)
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // Останавливаем задачу проверки
        stopPlayerChecker(player)
        
        // Удаляем время входа
        playerJoinTimes.remove(player.name)
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        // При телепорте сбрасываем данные для проверок
        if (event.isCancelled) return
        
        val player = event.player
        
        // Сбрасываем данные о проверках для предотвращения ложных срабатываний
        resetPlayerCheckData(player)
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        
        // Сбрасываем данные о проверках
        resetPlayerCheckData(player)
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDamage(event: EntityDamageEvent) {
        // Отслеживаем урон от падения для NoFall детектора
        if (event.isCancelled) return
        
        if (event.entity !is Player) return
        
        val player = event.entity as Player
        
        // Если это урон от падения
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            // Вызываем детектор NoFall напрямую при падении
            val detector = plugin.detectionManager.getDetector("NoFall") as? NoFallDetector
            if (detector != null) {
                detector.check(player)
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Отслеживаем использование фейерверков для ElytraBoostDetector
        if (event.isCancelled) return
        
        val player = event.player
        val item = event.item ?: return
        
        // Если игрок использует фейерверк и находится в режиме глайдинга
        if (item.type == Material.FIREWORK_ROCKET && player.isGliding) {
            // Регистрируем использование фейерверка
            val detector = plugin.detectionManager.getDetector("ElytraBoost") as? ElytraBoostDetector
            if (detector != null) {
                detector.registerFireworkUse(player.name)
            }
        }
    }
    
    /**
     * Запускает проверки для игрока через определенные интервалы
     */
    private fun startPlayerChecker(player: Player) {
        // Останавливаем существующую задачу, если она есть
        stopPlayerChecker(player)
        
        // Интервал проверки из конфига (в тиках)
        val interval = plugin.configManager.config.getLong("detection.check_interval_ticks", 10L)
        
        // Создаем новую задачу
        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            // Проверяем, находится ли игрок в защищенной зоне
            if (plugin.configManager.isRegionProtectionEnabled() && 
                plugin.worldGuardHook.isInProtectedRegion(player)) {
                return@Runnable
            }
            
            // Проверяем, прошло ли достаточно времени после входа игрока
            val joinTime = playerJoinTimes[player.name] ?: 0L
            val currentTime = System.currentTimeMillis()
            val joinDelay = plugin.configManager.config.getLong("detection.join_delay_ms", 30000L)
            
            if (currentTime - joinTime < joinDelay) {
                return@Runnable // Пропускаем проверку, если не прошло достаточно времени после входа
            }
            
            // Выполняем проверки
            if (plugin.configManager.isDetectorEnabled("SpeedHack")) {
                plugin.detectionManager.getDetector("SpeedHack")?.check(player)
            }
            
            if (plugin.configManager.isDetectorEnabled("FlyHack")) {
                plugin.detectionManager.getDetector("FlyHack")?.check(player)
            }
            
            if (plugin.configManager.isDetectorEnabled("ElytraFly")) {
                plugin.detectionManager.getDetector("ElytraFly")?.check(player)
            }
            
            if (plugin.configManager.isDetectorEnabled("ElytraBoost")) {
                plugin.detectionManager.getDetector("ElytraBoost")?.check(player)
            }
        }, 20L, interval)
        
        // Сохраняем задачу
        playerCheckerTasks[player.name] = task
    }
    
    /**
     * Останавливает задачу проверки игрока
     */
    private fun stopPlayerChecker(player: Player) {
        val playerName = player.name
        val task = playerCheckerTasks.remove(playerName)
        task?.cancel()
    }
    
    /**
     * Сбрасывает данные для проверок игрока
     */
    private fun resetPlayerCheckData(player: Player) {
        // Сброс нарушений
        plugin.detectionManager.resetViolations(player)
    }
    
    /**
     * Проверяет игрока на читы
     */
    private fun checkPlayer(player: Player) {
        // NoFall не проверяется здесь, он проверяется по событию урона от падения
        
        // Можно добавить другие проверки при движении, если это необходимо
    }
} 