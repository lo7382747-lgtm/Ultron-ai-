package com.example.ai

import com.example.ai.live.LiveSessionManager
import com.example.ai.text.TextAIEngine
import com.example.ai.tools.ToolManager

class AIEngine(
    val liveVoiceEngine: LiveSessionManager,
    val textAIEngine: TextAIEngine,
    val toolEngine: ToolManager
)
