package org.zoobastiks.zanticheat.punishment

/**
 * Sealed класс для описания различных типов наказаний
 */
sealed class PunishmentAction {
    /**
     * Наказание: выполнение команды от имени консоли
     * @param command команда для выполнения
     */
    data class Command(val command: String) : PunishmentAction()
    
    /**
     * Наказание: выполнение нескольких команд от имени консоли
     * @param commands список команд для выполнения
     */
    data class MultiCommands(val commands: List<String>) : PunishmentAction()
} 