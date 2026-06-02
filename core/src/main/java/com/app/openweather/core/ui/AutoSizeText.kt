package com.app.openweather.core.ui

import android.util.Log
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    val initialFontSize = if (fontSize.isUnspecified) style.fontSize else fontSize
    var scaledTextStyle by remember(text) { mutableStateOf(style.copy(fontSize = initialFontSize)) }
    var readyToDraw by remember(text) { mutableStateOf(false) }
    
    val minFontSize = 8.sp

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        fontSize = scaledTextStyle.fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = { textLayoutResult ->
            if (readyToDraw) return@Text
            
            if (textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight) {
                val nextSize = scaledTextStyle.fontSize * 0.9f
                if (nextSize >= minFontSize) {
                    scaledTextStyle = scaledTextStyle.copy(fontSize = nextSize)
                } else {
                    // Reached minimum size, just show it (it will be ellipsized)
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        },
        style = style
    )
}
