package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.WorkerLaborEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiAssistantDialog
import com.example.ui.viewmodel.AiLogisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkersTabScreen(
    viewModel: BmkViewModel,
    aiViewModel: AiLogisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val workerLaborRecords by viewModel.workerLaborRecords.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var showAiDialog by remember { mutableStateOf(false) }
    var selectedBranchFilter by remember { mutableStateOf("ALL") }
    var selectedLaborDetails by remember { mutableStateOf<WorkerLaborEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Form
    var gateBranch by remember { mutableStateOf("MANDALAY") }
    var workerName by remember { mutableStateOf("") }
    var vissHandledStr by remember { mutableStateOf("500.0") }
    var ratePerVissStr by remember { mutableStateOf("25.0") }
    var shuttleLaborStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val filteredRecords = remember(workerLaborRecords, selectedBranchFilter) {
        if (selectedBranchFilter == "ALL") workerLaborRecords
        else workerLaborRecords.filter { it.gateBranch == selectedBranchFilter }
    }

    val totalMdyLaborPaid = workerLaborRecords.filter { it.gateBranch == "MANDALAY" }.sumOf { it.totalLaborPaid }
    val totalKlyLaborPaid = workerLaborRecords.filter { it.gateBranch == "KALAY" }.sumOf { it.totalLaborPaid }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (settingsState.language == "my") "အလုပ်သမားခ စာရင်း (Workers Labor)" else "Workers & Labor Payouts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (settingsState.language == "my") "မန္တလေး နှင့် ကလေးမြို့ ဂိတ် အလုပ်သမားခများ" else "Mandalay Loading (25Ks/viss) & Kalay Unloading (25Ks/viss)",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiDialog = true },
                        modifier = Modifier.testTag("workers_action_open_ai")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "BMK AI Smart Assistant",
                            tint = BmkOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BmkNavy)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BmkOrange,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("အလုပ်သမားခ ထည့်မည်", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                modifier = Modifier.testTag("fab_add_worker_labor")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Filter chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "အားလုံး", "MANDALAY" to "မန္တလေးဂိတ်", "KALAY" to "ကလေးဂိတ်").forEach { (code, label) ->
                        FilterChip(
                            selected = selectedBranchFilter == code,
                            onClick = { selectedBranchFilter = code },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BmkNavy,
                                selectedLabelColor = Color.White,
                                containerColor = CardBackground,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedBranchFilter == code,
                                borderColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "အလုပ်သမား စာရင်းများ - [အမည်ကို နှိပ်၍ အသေးစိတ်ကြည့်ပါ]",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(filteredRecords) { record ->
                CompactWorkerLaborListItem(
                    record = record,
                    currencyFormat = currencyFormat,
                    onClick = { selectedLaborDetails = record }
                )
            }
        }
    }

    // Labor Detail Modal Dialog
    selectedLaborDetails?.let { record ->
        AlertDialog(
            onDismissRequest = { selectedLaborDetails = null },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (record.gateBranch == "MANDALAY") "မန္တလေးဂိတ် (၁၂ ဘီး တင်ခ)" else "ကလေးမြို့ဂိတ် (၁၂ ဘီး ချခ)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BmkNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• အလုပ်သမား/အဖွဲ့: ${record.workerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("• နေ့ရက်: ${record.date}", fontSize = 12.sp, color = TextSecondary)
                    Text("• မနိန်းကုန် ပိဿာ: ${record.vissHandled} ပိဿာ", fontSize = 12.sp, color = TextPrimary)
                    Text("• ၁ ပိဿာ လုပ်အားခ: ${record.laborRatePerViss} ကျပ်/ပိဿာ", fontSize = 12.sp, color = TextPrimary)
                    if (record.shuttleLaborAmount > 0) {
                        Text("• ခေါက်တို အလုပ်သမားခ: ${currencyFormat.format(record.shuttleLaborAmount)} ကျပ်", fontSize = 12.sp, color = BmkOrange)
                    }
                    Text("• စုစုပေါင်း ရှင်းပြီးငွေ: ${currencyFormat.format(record.totalLaborPaid)} ကျပ်", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BmkOrange)
                    if (record.notes.isNotBlank()) Text("• မှတ်ချက်: ${record.notes}", fontSize = 11.sp, color = TextMuted)
                }
            },
            confirmButton = {
                Button(onClick = { selectedLaborDetails = null }, colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)) {
                    Text("ပိတ်မည်", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            title = { Text("အလုပ်သမားခ ထည့်ရန်", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = gateBranch == "MANDALAY",
                            onClick = { gateBranch = "MANDALAY" },
                            label = { Text("မန္တလေးဂိတ်", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = gateBranch == "KALAY",
                            onClick = { gateBranch = "KALAY" },
                            label = { Text("ကလေးဂိတ်", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = workerName,
                        onValueChange = { workerName = it },
                        label = { Text("အလုပ်သမား / အဖွဲ့ အမည်") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vissHandledStr,
                        onValueChange = { vissHandledStr = it },
                        label = { Text("မနိန်းကုန် ပိဿာ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ratePerVissStr,
                        onValueChange = { ratePerVissStr = it },
                        label = { Text("၁ ပိဿာ အလုပ်သမားခ (ကျပ်)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = shuttleLaborStr,
                        onValueChange = { shuttleLaborStr = it },
                        label = { Text("ခေါက်တို အလုပ်သမားခ (ကျပ်)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("မှတ်ချက်") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val viss = vissHandledStr.toDoubleOrNull() ?: 0.0
                        val rate = ratePerVissStr.toDoubleOrNull() ?: 25.0
                        val shuttle = shuttleLaborStr.toDoubleOrNull() ?: 0.0
                        viewModel.addWorkerLabor(
                            gateBranch = gateBranch,
                            workerName = workerName,
                            date = "2026-10-25",
                            vissHandled = viss,
                            laborRatePerViss = rate,
                            shuttleLaborAmount = shuttle,
                            notes = notes,
                            onSuccess = {
                                showAddDialog = false
                                Toast.makeText(context, "အလုပ်သမားခ စာရင်းသွင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange)
                ) {
                    Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("မလုပ်တော့ပါ", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAiDialog) {
        AiAssistantDialog(
            aiViewModel = aiViewModel,
            onDismiss = { showAiDialog = false }
        )
    }
}

@Composable
fun CompactWorkerLaborListItem(
    record: WorkerLaborEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("worker_item_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.workerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BmkNavy
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (record.gateBranch == "MANDALAY") StatusBlueBg else StatusGreenBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (record.gateBranch == "MANDALAY") "မန္တလေး" else "ကလေး",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.gateBranch == "MANDALAY") StatusBlueText else StatusGreenText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ကုန်ပိဿာ: ${record.vissHandled} ပိဿာ (${record.laborRatePerViss} ကျပ်/ပိဿာ)",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currencyFormat.format(record.totalLaborPaid)} ကျပ်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BmkOrange
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("အသေးစိတ်", fontSize = 10.sp, color = BmkNavy, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
