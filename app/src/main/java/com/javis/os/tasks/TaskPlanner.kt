package com.javis.os.tasks

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.javis.os.apps.AppDiscoveryService
import com.javis.os.contacts.ContactsManager
import com.javis.os.domain.model.TaskAction
import com.javis.os.features.device.DeviceControlManager
import com.javis.os.features.weather.WeatherService
import com.javis.os.features.whatsapp.WhatsAppAutomation
import com.javis.os.voice.TextToSpeechManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskPlanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDiscoveryService: AppDiscoveryService,
    private val contactsManager: ContactsManager,
    private val ttsManager: TextToSpeechManager,
    private val weatherService: WeatherService,
    private val deviceControl: DeviceControlManager,
    private val whatsApp: WhatsAppAutomation
) {
    fun parseActionsFromResponse(response: String): List<TaskAction> {
        val actions = mutableListOf<TaskAction>()
        val actionRegex = Regex("""\[ACTION:(\w+):([^\]]*)]""")
        actionRegex.findAll(response).forEach { match ->
            val type = match.groupValues[1]
            val params = match.groupValues[2]
            when (type) {
                "CALL" -> {
                    val parts = params.split(":", limit = 2)
                    if (parts.size >= 2) actions.add(TaskAction.MakeCall(parts[0].trim(), parts[1].trim()))
                    else if (parts.isNotEmpty()) actions.add(TaskAction.MakeCall(parts[0].trim(), parts[0].trim()))
                }
                "LAUNCH" -> {
                    val parts = params.split(":", limit = 2)
                    if (parts.size >= 2) actions.add(TaskAction.LaunchApp(parts[0].trim(), parts[1].trim()))
                }
                "SEARCH" -> {
                    val parts = params.split(":", limit = 2)
                    if (parts.size >= 2) actions.add(TaskAction.Search(parts[1].trim(), parts[0].trim()))
                }
                "MESSAGE" -> {
                    val parts = params.split(":", limit = 3)
                    if (parts.size >= 3) actions.add(TaskAction.SendMessage(parts[0].trim(), parts[2].trim(), parts[1].trim()))
                }
                "ALARM" -> {
                    val parts = params.split(":")
                    if (parts.size >= 2) {
                        actions.add(TaskAction.SetAlarm(
                            parts[0].trim().toIntOrNull() ?: 7,
                            parts[1].trim().toIntOrNull() ?: 0,
                            if (parts.size >= 3) parts[2].trim() else "Alarm"
                        ))
                    }
                }
                "TIMER" -> actions.add(TaskAction.SetTimer(params.trim().toIntOrNull() ?: 5))
                "VOLUME_UP" -> actions.add(TaskAction.VolumeUp)
                "VOLUME_DOWN" -> actions.add(TaskAction.VolumeDown)
                "VOLUME_MUTE" -> actions.add(TaskAction.VolumeMute)
                "FLASHLIGHT_ON" -> actions.add(TaskAction.FlashlightOn)
                "FLASHLIGHT_OFF" -> actions.add(TaskAction.FlashlightOff)
                "FLASHLIGHT_TOGGLE" -> actions.add(TaskAction.FlashlightToggle)
                "WEATHER" -> actions.add(TaskAction.GetWeather(params.trim().ifBlank { "auto" }))
                "BATTERY" -> actions.add(TaskAction.GetBattery)
                "OPEN_SETTINGS" -> actions.add(TaskAction.OpenSettings(params.trim()))
                "WHATSAPP" -> {
                    val parts = params.split(":", limit = 3)
                    if (parts.size >= 3) actions.add(TaskAction.WhatsAppMessage(parts[0].trim(), parts[1].trim(), parts[2].trim()))
                }
            }
        }
        return actions
    }

    fun extractIntent(userInput: String): UserIntent {
        val input = userInput.lowercase()
        return when {
            input.contains("call") || input.contains("dial") || input.contains("phone") -> UserIntent.CALL
            input.contains("message") || input.contains("msg") || input.contains("text") ||
                    input.contains("whatsapp") || input.contains("send") -> UserIntent.MESSAGE
            input.contains("open") || input.contains("launch") || input.contains("start") -> UserIntent.LAUNCH_APP
            input.contains("search") || input.contains("find") || input.contains("look for") -> UserIntent.SEARCH
            input.contains("alarm") -> UserIntent.SET_ALARM
            input.contains("timer") -> UserIntent.SET_TIMER
            input.contains("remind") -> UserIntent.SET_REMINDER
            input.contains("weather") || input.contains("temperature") || input.contains("forecast") -> UserIntent.WEATHER
            input.contains("news") -> UserIntent.NEWS
            input.contains("volume") || input.contains("louder") || input.contains("quieter") ||
                    input.contains("mute") -> UserIntent.VOLUME
            input.contains("flashlight") || input.contains("torch") || input.contains("flash") -> UserIntent.FLASHLIGHT
            input.contains("battery") || input.contains("charge") -> UserIntent.BATTERY
            input.contains("wifi") || input.contains("bluetooth") || input.contains("settings") -> UserIntent.DEVICE_SETTINGS
            else -> UserIntent.CONVERSATION
        }
    }

    suspend fun executeAction(action: TaskAction): ActionResult {
        return try {
            when (action) {
                is TaskAction.LaunchApp -> {
                    val success = appDiscoveryService.launchApp(action.packageName)
                    if (success) ActionResult.Success("Opened ${action.appName}")
                    else {
                        val found = appDiscoveryService.findApp(action.appName)
                        if (found != null && appDiscoveryService.launchApp(found.packageName))
                            ActionResult.Success("Opened ${found.appName}")
                        else ActionResult.Failure("Could not find ${action.appName}")
                    }
                }
                is TaskAction.Search -> {
                    val app = appDiscoveryService.findApp(action.app)
                    if (app != null) {
                        appDiscoveryService.launchAppWithSearch(app.packageName, action.query)
                        ActionResult.Success("Searching for \"${action.query}\" in ${app.appName}")
                    } else ActionResult.Failure("Could not find ${action.app}")
                }
                is TaskAction.MakeCall -> {
                    val contact = contactsManager.findBestMatch(action.contact)
                    val number = contact?.phoneNumbers?.firstOrNull() ?: action.number
                    if (number.isBlank()) return ActionResult.Failure("Couldn't find a number for ${action.contact}")
                    contactsManager.makeCall(number)
                    ActionResult.Success("Calling ${contact?.name ?: action.contact}")
                }
                is TaskAction.SetAlarm -> {
                    setAlarm(action.hour, action.minute, action.label)
                    ActionResult.Success("Alarm set for ${action.hour}:${String.format("%02d", action.minute)}")
                }
                is TaskAction.SetTimer -> {
                    setTimer(action.minutes)
                    ActionResult.Success("Timer set for ${action.minutes} minute${if (action.minutes != 1) "s" else ""}")
                }
                is TaskAction.VolumeUp -> {
                    deviceControl.increaseVolume()
                    ActionResult.Success("Volume increased")
                }
                is TaskAction.VolumeDown -> {
                    deviceControl.decreaseVolume()
                    ActionResult.Success("Volume decreased")
                }
                is TaskAction.VolumeMute -> {
                    deviceControl.muteVolume()
                    ActionResult.Success("Volume muted")
                }
                is TaskAction.FlashlightOn -> {
                    deviceControl.turnFlashlightOn()
                    ActionResult.Success("Flashlight on")
                }
                is TaskAction.FlashlightOff -> {
                    deviceControl.turnFlashlightOff()
                    ActionResult.Success("Flashlight off")
                }
                is TaskAction.FlashlightToggle -> {
                    val on = deviceControl.toggleFlashlight()
                    ActionResult.Success(if (on) "Flashlight on" else "Flashlight off")
                }
                is TaskAction.GetWeather -> {
                    val result = weatherService.getWeather(action.location)
                    result.fold(
                        onSuccess = { ActionResult.Success(weatherService.formatWeatherResponse(it)) },
                        onFailure = { ActionResult.Failure("I couldn't get the weather right now. Try again in a moment.") }
                    )
                }
                is TaskAction.GetBattery -> {
                    ActionResult.Success(deviceControl.formatBatteryResponse())
                }
                is TaskAction.OpenSettings -> {
                    when (action.type.lowercase()) {
                        "wifi" -> deviceControl.openWifiSettings()
                        "bluetooth" -> deviceControl.openBluetoothSettings()
                        "data" -> deviceControl.openMobileDataSettings()
                    }
                    ActionResult.Success("Opening settings")
                }
                is TaskAction.WhatsAppMessage -> {
                    val contact = contactsManager.findBestMatch(action.contact)
                    val number = contact?.phoneNumbers?.firstOrNull() ?: action.phoneNumber
                    if (number.isBlank()) return ActionResult.Failure("Couldn't find ${action.contact}'s number")
                    ActionResult.RequiresHandling(action)
                }
                is TaskAction.Speak -> {
                    ttsManager.speak(action.text)
                    ActionResult.Success("")
                }
                else -> ActionResult.RequiresHandling(action)
            }
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "Action failed")
        }
    }

    private fun setAlarm(hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun setTimer(minutes: Int) {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

enum class UserIntent {
    CALL, MESSAGE, LAUNCH_APP, SEARCH, SET_ALARM, SET_TIMER, SET_REMINDER,
    WEATHER, NEWS, CONVERSATION, VOLUME, FLASHLIGHT, BATTERY, DEVICE_SETTINGS
}

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Failure(val error: String) : ActionResult()
    data class RequiresHandling(val action: TaskAction) : ActionResult()
}
