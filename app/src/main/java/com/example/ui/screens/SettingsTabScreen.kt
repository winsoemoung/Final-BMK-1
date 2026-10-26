package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiAssistantDialog
import com.example.ui.viewmodel.AiLogisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabScreen(
    viewModel: BmkViewModel,
    aiViewModel: AiLogisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settingsState by viewModel.settingsState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var showAiDialog by remember { mutableStateOf(false) }

    // Rate fields
    var freightRateInput by remember(settingsState) { mutableStateOf(settingsState.freightRatePerViss.toInt().toString()) }
    var mdyLoadingInput by remember(settingsState) { mutableStateOf(settingsState.mdyLoadingRatePerViss.toInt().toString()) }
    var klyUnloadingInput by remember(settingsState) { mutableStateOf(settingsState.klyUnloadingRatePerViss.toInt().toString()) }

    // Converter Tool States
    var kgInput by remember { mutableStateOf("10.0") }
    var vissInput by remember { mutableStateOf("6.12") }

    val convertedViss = (kgInput.toDoubleOrNull() ?: 0.0) / 1.63293
    val convertedKg = (vissInput.toDoubleOrNull() ?: 0.0) * 1.63293

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (settingsState.language == "my") "ဆက်တင်များ (Settings)" else "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gemini AI Smart Assistant Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BmkNavy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BmkOrange, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BMK Gemini AI Smart Assistant", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                        Text(
                            text = "မန္တလေး - ကလေး တန်ဆာခ မေးမြန်းခြင်း၊ စာကြောင်းမှ စာရင်းခွဲခြားခြင်း နှင့် Viber ဘောက်ချာ ရေးသားခြင်းများကို AI မှ ကူညီဆောင်ရွက်ပေးပါမည်။",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = { showAiDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                            modifier = Modifier.fillMaxWidth().testTag("open_ai_assistant_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✨ AI အကူကို ဖွင့်မည် (Open AI Assistant)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // Language Selection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = BmkNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (settingsState.language == "my") "ဘာသာစကား ရွေးချယ်ရန် (Language)" else "Language Selection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = settingsState.language == "my",
                                onClick = { viewModel.updateSettings(language = "my") },
                                label = { Text("မြန်မာဘာသာ (Burmese)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                leadingIcon = {
                                    if (settingsState.language == "my") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BmkNavy,
                                    selectedLabelColor = Color.White,
                                    containerColor = SurfaceLight,
                                    labelColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("lang_burmese")
                            )

                            FilterChip(
                                selected = settingsState.language == "en",
                                onClick = { viewModel.updateSettings(language = "en") },
                                label = { Text("English Language", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                leadingIcon = {
                                    if (settingsState.language == "en") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BmkNavy,
                                    selectedLabelColor = Color.White,
                                    containerColor = SurfaceLight,
                                    labelColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("lang_english")
                            )
                        }
                    }
                }
            }

            // Daily Rate Customization Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PriceChange, contentDescription = null, tint = BmkOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (settingsState.language == "my") "နေ့စဉ် ပြောင်းလဲနိုင်သော နှုန်းထားများ သတ်မှတ်ရန်" else "Daily Dynamic Rates Customizer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (settingsState.language == "my") "တစ်ပိဿာ ဈေးနှုန်းသည် နေ့စဉ် ပြောင်းလဲနေသောကြောင့် ဤနေရာတွင် စိတ်ကြိုက် ပြင်ဆင် သတ်မှတ်နိုင်ပါသည်။" else "Freight & labor rates change daily. Customize today's default rates below.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = freightRateInput,
                            onValueChange = { freightRateInput = it },
                            label = { Text(if (settingsState.language == "my") "ယနေ့ ၁ ပိဿာ တန်ဆာခ (ကျပ်) - default 1200" else "Today's Freight Rate per Viss (Ks)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("input_custom_freight_rate"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = mdyLoadingInput,
                                onValueChange = { mdyLoadingInput = it },
                                label = { Text("မန္တလေး ၁၂ ဘီး တင်ခ (ကျပ်/ပိဿာ)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = klyUnloadingInput,
                                onValueChange = { klyUnloadingInput = it },
                                label = { Text("ကလေး ၁၂ ဘီး ချခ (ကျပ်/ပိဿာ)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                        }

                        Button(
                            onClick = {
                                val freight = freightRateInput.toDoubleOrNull() ?: 1200.0
                                val mdy = mdyLoadingInput.toDoubleOrNull() ?: 25.0
                                val kly = klyUnloadingInput.toDoubleOrNull() ?: 25.0

                                viewModel.updateSettings(
                                    freightRatePerViss = freight,
                                    mdyLoadingRatePerViss = mdy,
                                    klyUnloadingRatePerViss = kly
                                )
                                Toast.makeText(context, "ယနေ့ နှုန်းထားများ အောင်မြင်စွာ ပြောင်းလဲပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_custom_rates_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (settingsState.language == "my") "နှုန်းထားများ သိမ်းဆည်းမည်" else "Save Rates Settings", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Weight Converter Calculator Tool (Kg <-> Viss)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = BmkNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (settingsState.language == "my") "အလေးချိန် တွက်ချက်စက် (Kg ↔ ပိဿာ Converter)" else "Weight Converter (Kg ↔ Viss)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "၁ ပိဿာ = ၁.၆၃၂၉၃ ကီလိုဂရမ် (1 Viss = 1.63293 Kg)",
                            fontSize = 12.sp,
                            color = BmkNavy,
                            fontWeight = FontWeight.Bold
                        )

                        // Kg to Viss Section
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), border = BorderStroke(1.dp, Color(0xFFCBD5E1))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ကီလိုဂရမ် (Kg) မှ ပိဿာ သို့ ပြောင်းရန်:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = kgInput,
                                        onValueChange = { kgInput = it },
                                        label = { Text("ကီလိုဂရမ် (Kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                                    )
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = BmkOrange)
                                    Surface(color = BmkNavy.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${String.format("%.2f", convertedViss)} ပိဿာ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = BmkNavy,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Viss to Kg Section
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), border = BorderStroke(1.dp, Color(0xFFCBD5E1))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ပိဿာ မှ ကီလိုဂရမ် (Kg) သို့ ပြောင်းရန်:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = vissInput,
                                        onValueChange = { vissInput = it },
                                        label = { Text("ပိဿာ (Viss)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                                    )
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = BmkNavy)
                                    Surface(color = BmkOrange.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${String.format("%.2f", convertedKg)} Kg",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = BmkOrange,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // App Version Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BMK EXPRESS CAR GATE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BmkNavy
                        )
                        Text(text = "မန္တလေး - ကလေး အမြန်ကုန်ပို့ဆောင်ရေး", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "Version 2.0 (High Contrast High-Legibility UI)", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }
    }

    if (showAiDialog) {
        AiAssistantDialog(
            aiViewModel = aiViewModel,
            onDismiss = { showAiDialog = false }
        )
    }
}
