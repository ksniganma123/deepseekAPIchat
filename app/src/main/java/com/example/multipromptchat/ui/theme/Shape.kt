package com.example.multipromptchat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Generous rounding is the single strongest signal of the HarmonyOS look. */
val HarmonyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val CardShape = RoundedCornerShape(20.dp)
val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val PillShape = RoundedCornerShape(percent = 50)
