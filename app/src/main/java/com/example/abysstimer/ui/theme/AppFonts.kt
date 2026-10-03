package com.example.abysstimer.ui.theme

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.example.abysstimer.R

/**
 * アプリ内フォント管理ファイル (Font Management)
 */

// 1. Comfortaa (コンフォータ)
val ComfortaaFontFamily = FontFamily(
    Font(R.font.comfortaa, FontWeight.Normal)
)

// 2. M PLUS Rounded 1c (エムプラス ラウンド 1c)
val MPlusRounded1cFontFamily = FontFamily(
    Font(R.font.m_plus_rounded_1c, FontWeight.Normal)
)

// コロン「:」専用フォント (丸みと上下中央のバランスが綺麗なコロン用: Comfortaa)
val ColonFontFamily: FontFamily = ComfortaaFontFamily

/**
 * 数字・文字はデフォルトフォントを維持し、コロン「:」のみ指定のバランスフォントで描画する AnnotatedString 変換関数
 */
fun String.toColonStyledAnnotatedString(
    colonFont: FontFamily = ColonFontFamily
): AnnotatedString {
    if (!this.contains(':')) return AnnotatedString(this)
    return buildAnnotatedString {
        for (char in this@toColonStyledAnnotatedString) {
            if (char == ':') {
                withStyle(SpanStyle(fontFamily = colonFont)) {
                    append(char)
                }
            } else {
                append(char)
            }
        }
    }
}

