package com.app.openweather.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.app.openweather.R
import com.app.openweather.core.ui.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings), color = AppColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AppColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.BgDark)
            )
        },
        containerColor = AppColors.BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.label_language),
                color = AppColors.AccentBlue,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LanguageOption(
                label = stringResource(R.string.lang_system),
                isSelected = AppCompatDelegate.getApplicationLocales().isEmpty,
                onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
            )
            
            LanguageOption(
                label = stringResource(R.string.lang_en),
                isSelected = AppCompatDelegate.getApplicationLocales().toLanguageTags() == "en",
                onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
            )
            
            LanguageOption(
                label = stringResource(R.string.lang_zh),
                isSelected = AppCompatDelegate.getApplicationLocales().toLanguageTags() == "zh-TW",
                onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-TW")) }
            )
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AppColors.TextPrimary, fontSize = 16.sp)
        RadioButton(
            selected = isSelected,
            onClick = null, // Handled by row clickable
            colors = RadioButtonDefaults.colors(selectedColor = AppColors.AccentBlue)
        )
    }
}
