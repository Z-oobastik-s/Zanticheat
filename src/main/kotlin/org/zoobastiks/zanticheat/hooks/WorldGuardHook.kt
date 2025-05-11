package org.zoobastiks.zanticheat.hooks

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.regions.RegionContainer
import org.bukkit.Location
import org.bukkit.entity.Player
import org.zoobastiks.zanticheat.ZAnticheat
import org.zoobastiks.zanticheat.utils.Logger

class WorldGuardHook(private val plugin: ZAnticheat) {
    
    private var initialized = false
    private lateinit var regionContainer: RegionContainer
    
    /**
     * Инициализирует интеграцию с WorldGuard
     */
    fun initialize(): Boolean {
        return try {
            regionContainer = WorldGuard.getInstance().platform.regionContainer
            initialized = true
            Logger.debug("WorldGuard интеграция инициализирована")
            true
        } catch (e: Exception) {
            Logger.error("&cНе удалось инициализировать WorldGuard: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Проверяет, находится ли игрок в приватном регионе
     */
    fun isInProtectedRegion(player: Player): Boolean {
        if (!initialized || !plugin.configManager.isRegionProtectionEnabled()) return false
        
        try {
            val loc = player.location
            val world = BukkitAdapter.adapt(loc.world)
            val regionManager = regionContainer.get(world) ?: return false
            
            val position = BukkitAdapter.adapt(loc).toVector().toBlockPoint()
            val applicableRegions = regionManager.getApplicableRegions(position)
            
            // Проверка на наличие регионов и flag PVP
            if (applicableRegions.size() == 0) return false
            
            // Игнорируемые регионы
            val ignoredRegions = plugin.configManager.config.getStringList("region_protection.ignored_regions")
            for (region in applicableRegions) {
                val regionId = region.id
                
                // Если регион в списке игнорируемых, пропускаем его
                if (ignoredRegions.contains(regionId)) {
                    continue
                }
                
                // Если игрок владелец или член региона, считаем его защищенным
                if (region.owners.contains(player.uniqueId) || region.members.contains(player.uniqueId)) {
                    Logger.debug("Игрок ${player.name} находится в собственном регионе $regionId")
                    return true
                }
                
                // Проверка на PVP флаг (опционально)
                val pvpState = applicableRegions.queryValue(null, Flags.PVP)
                if (pvpState != null && pvpState == StateFlag.State.DENY) {
                    Logger.debug("Игрок ${player.name} находится в защищенном регионе $regionId (PVP выключен)")
                    return true
                }
            }
            
            return false
        } catch (e: Exception) {
            Logger.error("&cОшибка при проверке WorldGuard региона: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Возвращает имя региона, в котором находится игрок
     */
    fun getRegionName(location: Location): String? {
        if (!initialized) return null
        
        try {
            val world = BukkitAdapter.adapt(location.world)
            val regionManager = regionContainer.get(world) ?: return null
            
            val position = BukkitAdapter.adapt(location).toVector().toBlockPoint()
            val applicableRegions = regionManager.getApplicableRegions(position)
            
            if (applicableRegions.size() == 0) return null
            
            // Возвращаем имя первого региона (можно доработать для выбора наиболее приоритетного)
            return applicableRegions.iterator().next().id
        } catch (e: Exception) {
            Logger.error("&cОшибка при получении имени региона: ${e.message}")
            return null
        }
    }
} 