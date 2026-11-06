package com.voyagerfiles.desktop

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberDesktopPointingDevice(): Boolean {
    val manager = LocalContext.current.getSystemService(Context.INPUT_SERVICE) as InputManager
    fun connected() = manager.inputDeviceIds.any { id ->
        manager.getInputDevice(id)?.let { device ->
            !device.isVirtual && (device.supportsSource(InputDevice.SOURCE_MOUSE) ||
                device.supportsSource(InputDevice.SOURCE_TOUCHPAD))
        } == true
    }
    var pointing by remember(manager) { mutableStateOf(connected()) }
    DisposableEffect(manager) {
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) { pointing = connected() }
            override fun onInputDeviceRemoved(deviceId: Int) { pointing = connected() }
            override fun onInputDeviceChanged(deviceId: Int) { pointing = connected() }
        }
        manager.registerInputDeviceListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
        pointing = connected()
        onDispose { manager.unregisterInputDeviceListener(listener) }
    }
    return pointing
}
