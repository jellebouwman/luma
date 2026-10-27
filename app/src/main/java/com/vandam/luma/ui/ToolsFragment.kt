package com.vandam.luma.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.vandam.luma.MainActivity
import com.vandam.luma.R
import com.vandam.luma.data.AndroidLauncherApp
import com.vandam.luma.data.HomeItemsManager
import com.vandam.luma.data.Prefs
import com.vandam.luma.data.Tool
import com.vandam.luma.helper.ManagedAppManager
import com.vandam.luma.ui.compose.SettingsScreen
import com.vandam.luma.ui.compose.ToggleTextButton

/**
 * Local replacement for the dashboard: choose which LightOS tools and installed Android apps
 * appear on the home screen. Writes the same preferences the dashboard sync used to write and
 * applies the layout through the same function.
 */
class ToolsFragment : Fragment() {
    private val prefs by lazy { Prefs.getInstance(requireContext()) }
    private val toolStates = mutableStateMapOf<Tool, Boolean>()
    private val appStates = mutableStateMapOf<String, Boolean>()
    private val installedApps = mutableStateOf<List<AndroidLauncherApp>>(emptyList())

    override fun onResume() {
        super.onResume()
        Tool.entries.forEach { tool -> toolStates[tool] = prefs.isToolEnabled(tool) }
        val apps = ManagedAppManager.getInstalledAndroidApps(requireContext())
        val enabledKeys = prefs.enabledAndroidApps.map { it.key }.toSet()
        apps.forEach { app -> appStates[app.key] = app.key in enabledKeys }
        installedApps.value = apps
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView(onSwipeBack = ::goBack) { Screen() }

    @Composable
    private fun Screen() {
        SettingsScreen(
            title = stringResource(R.string.tools_title),
            onBack = ::goBack,
        ) {
            Tool.entries.forEach { tool ->
                ToggleTextButton(
                    title = tool.defaultLabel(requireContext()),
                    checked = toolStates[tool] ?: false,
                    onValueChange = { enabled ->
                        toolStates[tool] = enabled
                        prefs.setToolEnabled(tool, enabled)
                        applyLayout()
                    },
                )
            }
            installedApps.value.forEach { app ->
                ToggleTextButton(
                    title = app.label,
                    checked = appStates[app.key] ?: false,
                    onValueChange = { enabled ->
                        appStates[app.key] = enabled
                        val others = prefs.enabledAndroidApps.filterNot { it.key == app.key }
                        prefs.enabledAndroidApps = if (enabled) others + app else others
                        applyLayout()
                    },
                )
            }
        }
    }

    private fun applyLayout() {
        HomeItemsManager.applyCurrentHomeLayout(requireContext(), prefs)
        (activity as? MainActivity)?.onToolSyncApplied()
    }
}
