package com.javis.os.domain.model

data class TaskPlan(
    val goal: String,
    val steps: List<TaskStep>,
    val currentStep: Int = 0,
    val status: TaskStatus = TaskStatus.PENDING
)

data class TaskStep(
    val index: Int,
    val description: String,
    val action: TaskAction,
    val status: StepStatus = StepStatus.PENDING,
    val errorMessage: String? = null
)

enum class TaskStatus { PENDING, RUNNING, COMPLETED, FAILED }
enum class StepStatus { PENDING, RUNNING, COMPLETED, FAILED, SKIPPED }

sealed class TaskAction {
    data class LaunchApp(val packageName: String, val appName: String) : TaskAction()
    data class SendMessage(val contact: String, val message: String, val app: String = "whatsapp") : TaskAction()
    data class MakeCall(val contact: String, val number: String) : TaskAction()
    data class Search(val query: String, val app: String) : TaskAction()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : TaskAction()
    data class SetTimer(val minutes: Int) : TaskAction()
    data class SetReminder(val text: String, val timeMillis: Long) : TaskAction()
    data class Speak(val text: String) : TaskAction()
    data class AskConfirmation(val question: String) : TaskAction()
    data class WebSearch(val query: String) : TaskAction()
    data class GetWeather(val location: String = "auto") : TaskAction()
    data class OpenSettings(val type: String) : TaskAction()
    data class WhatsAppMessage(val contact: String, val phoneNumber: String, val message: String) : TaskAction()
    object GetBattery : TaskAction()
    object VolumeUp : TaskAction()
    object VolumeDown : TaskAction()
    object VolumeMute : TaskAction()
    object FlashlightOn : TaskAction()
    object FlashlightOff : TaskAction()
    object FlashlightToggle : TaskAction()
    object Unknown : TaskAction()
}
