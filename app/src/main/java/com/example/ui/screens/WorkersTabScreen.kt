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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.WorkerLaborEntity
import com.example.ui.components.AiAssistantDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiLogisticsViewModel
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

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
    var selectedWageTab by remember { mutableIntStateOf(0) } // 0 = M-wages (Mandalay), 1 = K-wages (Kalay)
    val wageTabs = listOf("M-wages (မန္တလေးဂိတ်)", "K-wages (ကလေးဂိတ်)")

    var selectedLaborDetails by remember { mutableStateOf<WorkerLaborEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Form fields
    var workerName by remember { mutableStateOf("") }
    var vissHandledStr by remember { mutableStateOf("500.0") }
    var ratePerVissStr by remember { mutableStateOf("25.0") }
    var shuttleLaborStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val currentBranchFilter = if (selectedWageTab == 0) "MANDALAY" else "KALAY"
    val filteredRecords = remember(workerLaborRecords, currentBranchFilter) {
        workerLaborRecords.filter { it.gateBranch == currentBranchFilter }
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
                            text = "မန္တလေး တင်ခ (25Ks/viss) နှင့် ကလေး ချခ (25Ks/viss)",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BmkOrange,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("အလုပ်သမားခ စာရင်းသွင်းမည်", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("fab_add_worker_labor")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight)
        ) {
            // Sub-tabs: M-wages vs K-wages
            TabRow(
                selectedTabIndex = selectedWageTab,
                containerColor = CardBackground,
                contentColor = BmkNavy
            ) {
                wageTabs.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = selectedWageTab == index,
                        onClick = { selectedWageTab = index },
                        text = {
                            Text(
                                text = tabTitle,
                                fontWeight = if (selectedWageTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selectedWageTab == index) BmkNavy else TextSecondary
                            )
                        }
                    )
                }
            }

            // Summary Card
            Surface(
                color = CardBackground,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedWageTab == 0) "Mandalay Gate M-wages စုစုပေါင်း" else "Kalay Gate K-wages စုစုပေါင်း",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = "${currencyFormat.format(if (selectedWageTab == 0) totalMdyLaborPaid else totalKlyLaborPaid)} ကျပ်",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BmkOrange
                        )
                    }

                    Surface(
                        color = if (selectedWageTab == 0) StatusBlueBg else StatusGreenBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (selectedWageTab == 0) "25 ကျပ်/ပိဿာ တင်ခ" else "25 ကျပ်/ပိဿာ ချခ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedWageTab == 0) StatusBlueText else StatusGreenText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "${if (selectedWageTab == 0) "M-wages" else "K-wages"} စာရင်းများ (${filteredRecords.size} ခု) - [အမည်ကို နှိပ်၍ အသေးစိတ်ကြည့်ပါ]",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(filteredRecords) { record ->
                    CompactWorkerRowItem(
                        record = record,
                        currencyFormat = currencyFormat,
                        onClick = { selectedLaborDetails = record }
                    )
                }
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
                    text = if (record.gateBranch == "MANDALAY") "မန္တလေးဂိတ် M-wages စာရင်း" else "ကလေးမြို့ဂိတ် K-wages စာရင်း",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BmkNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• အလုပ်သမား / အဖွဲ့ အမည်: ${record.workerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
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

    // Add Worker Labor Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color.White,
            title = { Text("အလုပ်သမားခ စာရင်းသွင်းရန်", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ဂိတ် ရွေးချယ်ပါ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isMdy = selectedWageTab == 0
                        FilterChip(
                            selected = isMdy,
                            onClick = { selectedWageTab = 0 },
                            label = { Text("M-wages (မန္တလေး)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isMdy) Color.White else TextPrimary) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1E3A8A), selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        val isKly = selectedWageTab == 1
                        FilterChip(
                            selected = isKly,
                            onClick = { selectedWageTab = 1 },
                            label = { Text("K-wages (ကလေး)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isKly) Color.White else TextPrimary) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1E3A8A), selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = workerName,
                        onValueChange = { workerName = it },
                        label = { Text("အလုပ်သမား / အဖွဲ့ အမည်") },
                        modifier = Modifier.fillMaxWidth().testTag("input_worker_name"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = vissHandledStr,
                        onValueChange = { vissHandledStr = it },
                        label = { Text("မနိန်းကုန် ပိဿာ (viss)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ratePerVissStr,
                        onValueChange = { ratePerVissStr = it },
                        label = { Text("၁ ပိဿာ လုပ်အားခ (ကျပ်/ပိဿာ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = shuttleLaborStr,
                        onValueChange = { shuttleLaborStr = it },
                        label = { Text("ခေါက်တို အလုပ်သမားခ (ကျပ်)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("အထွေထွေ မှတ်ချက်") },
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
                            gateBranch = currentBranchFilter,
                            workerName = workerName,
                            date = "2026-10-26",
                            vissHandled = viss,
                            laborRatePerViss = rate,
                            shuttleLaborAmount = shuttle,
                            notes = notes,
                            onSuccess = {
                                showAddDialog = false
                                Toast.makeText(context, "အလုပ်သမားခ အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange)
                ) {
                    Text("Save (သိမ်းဆည်းမည်)", fontWeight = FontWeight.Bold, color = Color.White)
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
fun CompactWorkerRowItem(
    record: WorkerLaborEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("worker_row_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                            text = if (record.gateBranch == "MANDALAY") "M-wages" else "K-wages",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.gateBranch == "MANDALAY") StatusBlueText else StatusGreenText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "မနိန်းကုန်: ${record.vissHandled} ပိဿာ (${record.laborRatePerViss} ကျပ်/ပိဿာ)",
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
