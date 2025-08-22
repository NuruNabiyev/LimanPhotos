/**
 * DEBUG SCRIPT: Clear All Preferences
 * 
 * This file provides a simple way to call the clearAllPreferences function.
 * You can use this for debugging by calling the function from your SettingsViewModel.
 * 
 * USAGE:
 * 1. In your app, navigate to Settings
 * 2. Call: settingsViewModel.handleAction(SettingsAction.ClearAllPreferences)
 * 
 * OR if you want to call it directly:
 * 
 * import com.limanphotos.limandoc.presentation.settings.SettingsAction
 * 
 * // In your code:
 * settingsViewModel.handleAction(SettingsAction.ClearAllPreferences)
 * 
 * WHAT IT CLEARS:
 * - All folder selections
 * - Onboarding completion status
 * - All cached AI analyses
 * - Search index
 * - All Java Preferences stored by the app
 * 
 * NOTE: You may need to restart the app after clearing for complete reset.
 */

fun clearAllPreferencesDebug() {
    println("🧹 DEBUG: Use SettingsAction.ClearAllPreferences to clear all data")
    println("🔧 Call: settingsViewModel.handleAction(SettingsAction.ClearAllPreferences)")
}