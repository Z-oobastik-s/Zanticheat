package org.zoobastiks.zanticheat.detection

import org.bukkit.entity.Player

/**
 * Интерфейс для всех детекторов читов
 */
interface CheatDetector : Reloadable {
    /**
     * Уникальное имя детектора
     */
    val name: String
    
    /**
     * Описание детектора
     */
    val description: String
    
    /**
     * Проверяет игрока на наличие читов
     * Возвращает true, если чит обнаружен
     */
    fun check(player: Player): Boolean
    
    /**
     * Реализация метода reload() из интерфейса Reloadable
     * По умолчанию ничего не делает, детекторы могут переопределить этот метод
     */
    override fun reload() {
        // По умолчанию ничего не делаем
    }
} 