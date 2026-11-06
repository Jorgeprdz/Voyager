package com.voyagerfiles.desktop

enum class InterfaceMode { AUTOMATIC, MOBILE, DESKTOP }
object DesktopInteractionPolicy {
    fun useDesktop(preference: InterfaceMode, pointingDevice: Boolean, desktopWindow: Boolean): Boolean =
        when (preference) {
            InterfaceMode.AUTOMATIC -> pointingDevice || desktopWindow
            InterfaceMode.MOBILE -> false
            InterfaceMode.DESKTOP -> true
        }
}
