package com.example.fajrapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajrapp.ui.components.GlassContainer
import com.example.fajrapp.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.res.stringResource
import com.example.fajrapp.R

@Composable
fun LanguageSelectionScreen(
    viewModel: SettingsViewModel,
    hazeState: HazeState,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassContainer(
                cornerRadius = 14.dp,
                hazeState = hazeState,
                modifier = Modifier.size(48.dp)
            ) {
                 Box(modifier = Modifier.fillMaxSize().clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                 }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            


            Text(
                text = stringResource(R.string.settings_language),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(x = (-24).dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
        }

        // Language List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.availableLanguages) { language ->
                val isSelected = language.code == uiState.selectedLanguage.code
                
                GlassContainer(
                    cornerRadius = 20.dp,
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { 
                                viewModel.setLanguage(language)
                            }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.flagEmoji,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        
                        Text(
                            text = language.nativeName,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
