package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.util.BackupManager
import com.example.util.BackupStats
import com.example.util.GoogleAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiAssistantDialog
import com.example.ui.components.CargoPrintDialog
import com.example.ui.components.PrintTemplateEditorDialog
import com.example.ui.components.VoucherSearchReprintDialog
import com.example.ui.viewmodel.AiLogisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabScreen(
    viewModel: BmkViewModel,
    aiViewModel: AiLogisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settingsState by viewModel.settingsState.collectAsState()
    val templates by viewModel.allTemplates.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val coroutineScope = rememberCoroutineScope()

    var showAiDialog by remember { mutableStateOf(false) }
    var showTemplateEditorDialog by remember { mutableStateOf(false) }
    var showVoucherSearchDialog by remember { mutableStateOf(false) }
    var showTestPrintDialog by remember { mutableStateOf(false) }

    // Backup & Google Drive Sync State
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var isSyncingCloud by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var restoreStatsDialog by remember { mutableStateOf<BackupStats?>(null) }

    // Document Launchers for Local File Backup & Restore
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isRestoring = true
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                    if (jsonString.isNotBlank()) {
                        val stats = BackupManager.restoreFromJson(context, jsonString)
                        restoreStatsDialog = stats
                        Toast.makeText(context, "ဒေတာ အောင်မြင်စွာ ပြန်လည်တင်သွင်းပြီးပါပြီ", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "ဖိုင်တွင် အချက်အလက် မရှိပါ", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "အချက်အလက် ပြန်တင်ရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isRestoring = false
                }
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    isBackingUp = true
                    val jsonStr = BackupManager.generateBackupJson(context)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(jsonStr.toByteArray())
                    }
                    Toast.makeText(context, "ဖိုင်သို့ Backup အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Backup သိမ်းဆည်းရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isBackingUp = false
                }
            }
        }
    }

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
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isBurmese = settingsState.language == "my"
                            Surface(
                                onClick = { viewModel.updateSettings(language = "my") },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isBurmese) Color(0xFF1E3A8A) else Color.Transparent,
                                shadowElevation = if (isBurmese) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f).testTag("lang_burmese")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                                ) {
                                    if (isBurmese) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = "မြန်မာဘာသာ (Burmese)",
                                        fontWeight = if (isBurmese) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isBurmese) Color.White else TextPrimary
                                    )
                                }
                            }

                            val isEnglish = settingsState.language == "en"
                            Surface(
                                onClick = { viewModel.updateSettings(language = "en") },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isEnglish) Color(0xFF1E3A8A) else Color.Transparent,
                                shadowElevation = if (isEnglish) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f).testTag("lang_english")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                                ) {
                                    if (isEnglish) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = "English Language",
                                        fontWeight = if (isEnglish) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isEnglish) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bluetooth Thermal Printer & 7 Print Templates Card
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
                            Icon(Icons.Default.Print, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Bluetooth Thermal Printer & Print Settings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "80mm / 58mm / 100mm Paper Sizes • 7 Print Templates",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = "ဘုန်းမောင်ခန့် ကုန်စည်ပို့ဆောင်ရေး အတွက် Bluetooth Thermal Printer (80mm, 58mm, 100mm) စက္ကူ အရွယ်အစားများဖြင့် ဘောက်ချာ print ထုတ်နိုင်ပါသည်။ Header, Gate Name, ဇယားကွက် Rows/Columns, Footer စာသားများအား Template ၇ မျိုး ခွဲ၍ စိတ်ကြိုက် သတ်မှတ်သိမ်းဆည်းနိုင်ပြီး ဘောင်ချာ နံပါတ် အတိုင်း ရှာဖွေ၍ မူရင်း print page အား ပြန်လည် ထုတ်ယူနိုင်ပါသည်။",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )

                        // 3 Quick Action Buttons
                        Button(
                            onClick = { showTemplateEditorDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BmkNavy),
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("open_template_editor_btn")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚙️ Print Template ၇ မျိုး ပြင်ဆင်သိမ်းဆည်းရန်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = { showVoucherSearchDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("open_voucher_search_btn")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔍 ဘောင်ချာ နံပါတ်ဖြင့် မူရင်း Print စာရွက် ရှာဖွေ / Reprint ပြုလုပ်ရန်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { showTestPrintDialog = true },
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("open_test_print_btn")
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🖨️ Bluetooth Thermal စမ်းသပ် Print ထုတ်ကြည့်မည် (Test Print)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BmkNavy)
                        }

                        Divider(color = CardBorderColor, modifier = Modifier.padding(vertical = 4.dp))

                        // Current 7 Templates List Summary
                        Text("သိမ်းဆည်းထားသော Print Templates (၇) မျိုး စာရင်း:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)

                        templates.forEach { tmpl ->
                            Surface(
                                color = SurfaceLight,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CardBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tmpl.templateName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        Text(
                                            "စက္ကူ: ${tmpl.paperWidthMm}mm  |  ဇယား: ${if (tmpl.hasTableGrid) "ပါရှိ (${tmpl.tableRowCount} တန်း)" else "စာသားသီးသန့်"}  |  ထိပ်စီး: ${tmpl.headerSalutation}",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { showTemplateEditorDialog = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BmkNavy, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
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
                                    Surface(color = Color(0xFF1E3A8A).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", convertedViss)} ပိဿာ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1E3A8A),
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
                                            text = "${String.format(Locale.US, "%.2f", convertedKg)} Kg",
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

            // Backup & Google Drive Cloud Sync Card
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ဒေတာ သိမ်းဆည်း/ပြန်တင်ခြင်း (Backup & Restore)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Google Drive • Cloud Sync • Local JSON Export/Import",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = "ဂိတ်ရှိ ကုန်စည်မှတ်တမ်းများ၊ ကားသမားစာရင်း၊ Print Voucher များ မပျောက်ပျက်စေရန် Google Drive သို့မဟုတ် Cloud ပေါ်တွင် လုံခြုံစွာ သိမ်းဆည်းထားနိုင်ပါသည်။ အင်တာနက် ရရှိပါက Google Account ဖြင့် တိုက်ရိုက် ချိတ်ဆက်နိုင်ပါသည်။",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )

                        // Google Account Connection Status
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentUser != null) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (currentUser != null) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (currentUser != null) Icons.Default.CheckCircle else Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = if (currentUser != null) Color(0xFF047857) else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (currentUser != null) (currentUser?.displayName ?: currentUser?.email ?: "Google Account") else "Google Account မချိတ်ဆက်ရသေးပါ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (currentUser != null) Color(0xFF065F46) else TextPrimary
                                        )
                                        Text(
                                            text = if (currentUser != null) "Google Drive & Cloud Sync ချိတ်ဆက်ထားသည်" else "အင်တာနက် ဖွင့်၍ Google အကောင့်ဖြင့် ဝင်ပါ",
                                            fontSize = 11.sp,
                                            color = if (currentUser != null) Color(0xFF047857) else TextSecondary
                                        )
                                    }
                                }

                                if (currentUser == null) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val result = GoogleAuthHelper.signInWithGoogle(context)
                                                result.onSuccess { name ->
                                                    currentUser = FirebaseAuth.getInstance().currentUser
                                                    Toast.makeText(context, "$name ဖြင့် Google Account ချိတ်ဆက်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, "Google Sign-In: ${err.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Log in", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            GoogleAuthHelper.signOut()
                                            currentUser = null
                                            Toast.makeText(context, "Google Account မှ ထွက်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Sign Out", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        // Direct Google Drive Backup Button
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        isBackingUp = true
                                        val uri = BackupManager.exportBackupFile(context)
                                        BackupManager.shareOrSaveToDrive(context, uri)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Backup ဖိုင် ပြုလုပ်ရာတွင် အမှား: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isBackingUp = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("backup_to_drive_btn"),
                            enabled = !isBackingUp && !isRestoring
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Backup ဖိုင် ထုတ်ယူနေပါသည်...", fontSize = 12.sp, color = Color.White)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google Drive သို့ တိုက်ရိုက် Backup သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }

                        // Cloud Sync & Restore Row (Firestore Cloud Storage)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSyncingCloud = true
                                        val result = BackupManager.syncBackupToCloud(context)
                                        result.onSuccess { msg ->
                                            backupStatusMessage = msg
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Cloud Sync အမှား: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                        isSyncingCloud = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("cloud_sync_backup_btn"),
                                enabled = !isSyncingCloud && !isRestoring
                            ) {
                                if (isSyncingCloud) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Syncing...", fontSize = 11.sp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cloud သို့ Backup", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isRestoring = true
                                        val result = BackupManager.restoreBackupFromCloud(context)
                                        result.onSuccess { stats ->
                                            restoreStatsDialog = stats
                                            Toast.makeText(context, "Cloud မှ အချက်အလက်များ အောင်မြင်စွာ ပြန်တင်ပြီးပါပြီ", Toast.LENGTH_LONG).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, "Cloud Restore အမှား: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                        isRestoring = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
                                modifier = Modifier.weight(1f).testTag("cloud_restore_btn"),
                                enabled = !isRestoring && !isSyncingCloud
                            ) {
                                if (isRestoring) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF1E3A8A), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restoring...", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cloud မှ ပြန်တင်", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E3A8A))
                                }
                            }
                        }

                        Divider(color = CardBorderColor)

                        // Local File JSON Export / Import
                        Text(
                            text = "ဖိုင်ဖြင့် ဒေါင်းလုဒ် သိမ်းဆည်းရန် / ပြန်တင်ရန် (Local File):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val dateCompact = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                    createDocumentLauncher.launch("bmk_backup_$dateCompact.json")
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f).testTag("export_json_file_btn")
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON ဖိုင် သိမ်းမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            OutlinedButton(
                                onClick = {
                                    openDocumentLauncher.launch("*/*")
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f).testTag("import_json_file_btn")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ဖိုင်မှ ပြန်လည်တင်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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

    if (showTemplateEditorDialog) {
        PrintTemplateEditorDialog(
            viewModel = viewModel,
            onDismiss = { showTemplateEditorDialog = false }
        )
    }

    if (showVoucherSearchDialog) {
        VoucherSearchReprintDialog(
            viewModel = viewModel,
            onDismiss = { showVoucherSearchDialog = false }
        )
    }

    if (showTestPrintDialog) {
        CargoPrintDialog(
            cargo = null,
            truck = null,
            viewModel = viewModel,
            onDismiss = { showTestPrintDialog = false }
        )
    }

    restoreStatsDialog?.let { stats ->
        AlertDialog(
            onDismissRequest = { restoreStatsDialog = null },
            containerColor = Color.White,
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    text = "ဒေတာ အောင်မြင်စွာ ပြန်လည်တင်သွင်းပြီးပါပြီ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Backup ပြုလုပ်ခဲ့သည့် ရက်စွဲ: ${stats.backupDate}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Divider(color = CardBorderColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ကုန်ပစ္စည်း စာရင်း (Cargos):", fontSize = 12.sp, color = TextPrimary)
                        Text("${stats.cargoCount} ခု", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ကားသမား / ယာဉ်စာရင်း (Trucks):", fontSize = 12.sp, color = TextPrimary)
                        Text("${stats.truckCount} စီး", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Print Templates:", fontSize = 12.sp, color = TextPrimary)
                        Text("${stats.templateCount} ခု", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ရိုက်ပြီးသော ဘောက်ချာများ (Vouchers):", fontSize = 12.sp, color = TextPrimary)
                        Text("${stats.voucherCount} စောင်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("လျော်ကြေးမှတ်တမ်းများ (Claims):", fontSize = 12.sp, color = TextPrimary)
                        Text("${stats.claimCount} ခု", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("အလုပ်သမားခ စာရင်းများ (Labor):", fontSize = 12.sp, color = TextPrimary)
                        Text("${stats.laborCount} ခု", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E3A8A))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { restoreStatsDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("ကောင်းပါပြီ (OK)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
