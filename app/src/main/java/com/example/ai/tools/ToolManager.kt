package com.example.ai.tools

import android.content.Context
import com.example.device.apps.AppLauncher
import com.example.device.apps.AppLaunchResult
import com.example.device.settings.DeviceController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ToolExecutionResult(
    val toolName: String,
    val callId: String?,
    val output: String,
    val isSuccess: Boolean
)

class ToolManager(private val context: Context) {

    private val appLauncher = AppLauncher(context)
    private val deviceController = DeviceController(context)

    /**
     * Gemini Tool Declarations JSON structure compatible with Live WebSocket setup & REST
     */
    fun getToolsDeclarationJson(): JSONArray {
        val toolsArray = JSONArray()
        val toolsObj = JSONObject()
        val functionDeclarations = JSONArray()

        // 1. open_app
        functionDeclarations.put(JSONObject().apply {
            put("name", "open_app")
            put("description", "Opens an installed Android application like YouTube, Chrome, Maps, Settings, Camera, Calculator, Clock, Spotify, WhatsApp, or any other app.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("appName", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Name of the Android app to launch, e.g. YouTube, Camera, Maps, Chrome, Settings")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("appName") })
            })
        })

        // 2. get_device_time
        functionDeclarations.put(JSONObject().apply {
            put("name", "get_device_time")
            put("description", "Returns the current device time, full date, timezone, and battery percentage.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 3. search_web
        functionDeclarations.put(JSONObject().apply {
            put("name", "search_web")
            put("description", "Performs a web search on Google for the specified query and opens the results.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("query", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The query to search on the web")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("query") })
            })
        })

        // 4. open_website
        functionDeclarations.put(JSONObject().apply {
            put("name", "open_website")
            put("description", "Opens a website URL in the user's browser.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("url", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The URL or website domain, e.g. https://wikipedia.org")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("url") })
            })
        })

        // 5. control_device
        functionDeclarations.put(JSONObject().apply {
            put("name", "control_device")
            put("description", "Controls device hardware or navigates to quick device settings.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("action", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Action to perform: 'toggle_flashlight', 'turn_on_flashlight', 'turn_off_flashlight', 'volume_settings', 'wifi_settings', 'battery_settings'")
                })
                props.put("enabled", JSONObject().apply {
                    put("type", "BOOLEAN")
                    put("description", "Optional boolean state for toggleable features")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("action") })
            })
        })

        // 6. calculate
        functionDeclarations.put(JSONObject().apply {
            put("name", "calculate")
            put("description", "Evaluates a mathematical or computational expression.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("expression", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Mathematical expression to evaluate, e.g. (45 * 12) / 3")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("expression") })
            })
        })

        toolsObj.put("functionDeclarations", functionDeclarations)
        toolsArray.put(toolsObj)
        return toolsArray
    }

    suspend fun executeTool(name: String, args: JSONObject, callId: String?): ToolExecutionResult = withContext(Dispatchers.Main) {
        try {
            when (name) {
                "open_app" -> {
                    val appName = args.optString("appName", "")
                    val result = appLauncher.launchApp(appName)
                    val message = when (result) {
                        is AppLaunchResult.Success -> result.message
                        is AppLaunchResult.Fallback -> result.message
                        is AppLaunchResult.NotFound -> result.message
                    }
                    ToolExecutionResult(name, callId, message, true)
                }
                "get_device_time" -> {
                    val status = deviceController.getDeviceStatus()
                    ToolExecutionResult(name, callId, status, true)
                }
                "search_web" -> {
                    val query = args.optString("query", "")
                    val res = deviceController.searchWeb(query)
                    ToolExecutionResult(name, callId, res, true)
                }
                "open_website" -> {
                    val url = args.optString("url", "")
                    val res = deviceController.openWebsite(url)
                    ToolExecutionResult(name, callId, res, true)
                }
                "control_device" -> {
                    val action = args.optString("action", "")
                    val enabled = if (args.has("enabled")) args.optBoolean("enabled") else null
                    val output = when (action) {
                        "toggle_flashlight" -> deviceController.toggleFlashlight(enabled)
                        "turn_on_flashlight" -> deviceController.toggleFlashlight(true)
                        "turn_off_flashlight" -> deviceController.toggleFlashlight(false)
                        "volume_settings" -> deviceController.openVolumeSettings()
                        "wifi_settings" -> deviceController.openWifiSettings()
                        "battery_settings" -> deviceController.openBatterySettings()
                        else -> "Unknown device action: $action"
                    }
                    ToolExecutionResult(name, callId, output, true)
                }
                "calculate" -> {
                    val expression = args.optString("expression", "")
                    val computed = simpleEval(expression)
                    ToolExecutionResult(name, callId, "Calculation result for $expression = $computed", true)
                }
                else -> {
                    ToolExecutionResult(name, callId, "Tool '$name' not recognized.", false)
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(name, callId, "Tool execution error: ${e.message}", false)
        }
    }

    private fun simpleEval(expr: String): String {
        return try {
            val clean = expr.replace(" ", "")
            // basic calculation fallback
            val parts = clean.split("+", "-", "*", "/")
            if (parts.size == 2) {
                val a = parts[0].toDoubleOrNull() ?: return "Invalid number"
                val b = parts[1].toDoubleOrNull() ?: return "Invalid number"
                val res = when {
                    clean.contains("+") -> a + b
                    clean.contains("-") -> a - b
                    clean.contains("*") -> a * b
                    clean.contains("/") -> if (b != 0.0) a / b else "Division by zero"
                    else -> "Invalid"
                }
                res.toString()
            } else {
                "Expression received: $expr"
            }
        } catch (e: Exception) {
            "Error evaluating: ${e.message}"
        }
    }
}
