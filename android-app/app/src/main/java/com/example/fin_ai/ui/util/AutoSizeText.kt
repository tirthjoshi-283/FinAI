package com.example.fin_ai.ui.util

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Single-line text that shrinks until it fits its width — for amounts like ₹10,00,965.36
 * that must never wrap mid-number on narrow screens or with large system fonts.
 * It isn't drawn until the size has settled, so there's no visible flicker.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    minFontSize: TextUnit = 11.sp,
    style: TextStyle = LocalTextStyle.current,
    /** What the size is computed for; pass the final value when [text] animates (count-ups). */
    sizingKey: Any? = text,
) {
    val base = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize
    var size by remember(sizingKey, base) { mutableStateOf(base) }
    var ready by remember(sizingKey, base) { mutableStateOf(false) }
    Text(
        text = text,
        color = color,
        fontSize = size,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        style = style.copy(lineHeight = TextUnit.Unspecified),
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && size.value > minFontSize.value) {
                size = (size.value * 0.92f).coerceAtLeast(minFontSize.value).sp
            } else {
                ready = true
            }
        }
    )
}
