package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.example.data.model.CargoEntity
import com.example.data.model.TruckEntity
import com.example.ui.components.AiAssistantDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiLogisticsViewModel
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarTabScreen(
    viewModel: BmkViewModel,
    aiViewModel: AiLogisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val allTrucks by viewModel.allTrucks.collectAsState()
    val allCargo by viewModel.allCargo.collectAsState()
    val truckSearchQuery by viewModel.truckSearchQuery.collectAsState()
    val truckSearchResults by viewModel.truckSearchResults.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var showAiDialog by remember { mutableStateOf(false) }
    var showAddTruckDialog by remember { mutableStateOf(false) }
    var selectedTruckDetails by remember { mutableStateOf<TruckEntity?>(null) }

    // Form fields for Add Car
    var truckNumber by remember { mutableStateOf("3N-776445 (12-Wheeler)") }
    var ownerName by remember { mutableStateOf("ဦးဝင်းအောင်") }
    var driverName by remember { mutableStateOf("ဦးစိုးမြင့်") }
    var driverPhone by remember { mutableStateOf("0945009988") }
    var priceStr by remember { mutableStateOf("1500000") }
    var advanceStr by remember { mutableStateOf("200000") }
    var dispatchDate by remember { mutableStateOf("2026-10-26") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (settingsState.language == "my") "ကား / ကားသမား စာရင်း (Car & Fleet)" else "Truck Fleet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "၁၂ ဘီး ကားများနှင့် ငှာမ်းခဲ့သော စာရင်းများ",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiDialog = true },
                        modifier = Modifier.testTag("car_action_open_ai")
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
                onClick = { showAddTruckDialog = true },
                containerColor = BmkOrange,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("ကားအသစ် ပေါင်းထည့်မည် (Add Car)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("fab_add_truck")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight)
        ) {
            // Search Bar for Rented / Dispatched Trucks
            Surface(
                color = CardBackground,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = truckSearchQuery,
                    onValueChange = { viewModel.setTruckSearchQuery(it) },
                    placeholder = { Text("ကားနံပါတ်၊ ပိုင်ရှင် သို့မဟုတ် ယာဉ်မောင်း ရှာရန်...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BmkNavy) },
                    trailingIcon = {
                        if (truckSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setTruckSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .testTag("search_truck_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BmkNavy,
                        unfocusedBorderColor = CardBorderColor
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "ငှာမ်းခဲ့သော ကားစာရင်းများ (${truckSearchResults.size} စီး) - [နံပါတ် သို့မဟုတ် အမည်ကို နှိပ်၍ အသေးစိတ်ကြည့်ပါ]",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                items(truckSearchResults) { truck ->
                    CompactTruckRowItem(
                        truck = truck,
                        currencyFormat = currencyFormat,
                        onClick = { selectedTruckDetails = truck }
                    )
                }
            }
        }
    }

    // Truck Detail Modal Dialog when tapped (Includes 1..N Numbered Loaded Cargo List)
    selectedTruckDetails?.let { truck ->
        val loadedCargos = remember(allCargo, truck) {
            allCargo.filter { it.truckId == truck.id || it.truckNumber == truck.truckNumber }.sortedBy { it.sequenceOrder }
        }

        AlertDialog(
            onDismissRequest = { selectedTruckDetails = null },
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = truck.truckNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BmkNavy
                    )

                    // Truck Status Badge with distinct colors
                    val statusText = when (truck.status) {
                        "EMPTY" -> "ကုန်မရှိ"
                        "LOADING" -> "ကုန်တင်ပြီး"
                        "IN_TRANSIT" -> "စထွက်သွား"
                        else -> "ကလေးရောက် ကုန်ချပြီး"
                    }
                    val statusBg = when (truck.status) {
                        "EMPTY" -> SurfaceLight
                        "LOADING" -> StatusBlueBg
                        "IN_TRANSIT" -> StatusOrangeBg
                        else -> StatusGreenBg
                    }
                    val statusColor = when (truck.status) {
                        "EMPTY" -> TextMuted
                        "LOADING" -> StatusBlueText
                        "IN_TRANSIT" -> StatusOrangeText
                        else -> StatusGreenText
                    }

                    Surface(color = statusBg, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (truck.ownerName.isNotBlank()) {
                        Text("• ကားပိုင်ရှင် အမည်: ${truck.ownerName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                    }
                    Text("• မောင်းသူ/ကားသမား: ${truck.driverName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text("• ဖုန်းနံပါတ်: ${truck.driverPhone}", fontSize = 12.sp, color = BmkOrange, fontWeight = FontWeight.Bold)
                    Text("• ကားခ: ${currencyFormat.format(truck.mandalayToKalayFreightPrice)} ကျပ်", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BmkNavy)
                    Text("• ကြိုထုတ်ငွေ: ${currencyFormat.format(truck.advancePayment)} ကျပ်", fontSize = 12.sp, color = BmkOrange)
                    Text("• ထွက်ခွာမည့် နေ့ရက်: ${truck.dispatchDate}", fontSize = 12.sp, color = TextSecondary)
                    if (truck.notes.isNotBlank()) Text("• အထွေထွေ မှတ်ချက်: ${truck.notes}", fontSize = 11.sp, color = TextMuted)

                    Divider(color = CardBorderColor)

                    Text("ကားပေါ်ပါ ကုန်ပစ္စည်းများ စာရင်း (${loadedCargos.size} မျိုး):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BmkNavy)

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 220.dp)) {
                        items(loadedCargos.size) { idx ->
                            val item = loadedCargos[idx]
                            Surface(color = SurfaceLight, shape = RoundedCornerShape(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = BmkNavy, shape = RoundedCornerShape(4.dp)) {
                                        Text("#${idx + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.cargoName.ifBlank { item.itemDescription }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        Text("ပို့သူ: ${item.senderName} (${item.quantity} ${item.weightUnit})", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Text("${item.weightViss} ပိဿာ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BmkNavy)
                                }
                            }
                        }
                    }

                    Divider(color = CardBorderColor)

                    Text("အခြေအနေ ပြောင်းရန်:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "EMPTY" to "ကုန်မရှိ",
                            "LOADING" to "ကုန်တင်ပြီး",
                            "IN_TRANSIT" to "စထွက်သွား",
                            "KALAY_ARRIVED" to "ကလေးရောက် ကုန်ချပြီး"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = truck.status == code,
                                onClick = { viewModel.updateTruckStatus(truck.id, code) },
                                label = { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BmkNavy,
                                    selectedLabelColor = Color.White,
                                    containerColor = SurfaceLight,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${truck.driverPhone}"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ကားသမား ဖုန်းခေါ်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTruckDetails = null }) {
                    Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        )
    }

    // Add Car Dialog with full fields
    if (showAddTruckDialog) {
        AlertDialog(
            onDismissRequest = { showAddTruckDialog = false },
            containerColor = Color.White,
            title = { Text("ငှာမ်းခဲ့သော ကားသစ် စာရင်းသွင်းရန် (Add Car)", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = truckNumber,
                        onValueChange = { truckNumber = it },
                        label = { Text("ကားနံပါတ် (Car Number e.g. 3N-776445)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_truck_number"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("ကားပိုင်ရှင် အမည် (Owner Name)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_owner_name"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = driverName,
                        onValueChange = { driverName = it },
                        label = { Text("မောင်းသူ / ကားသမား အမည် (Driver Name)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_driver_name"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = driverPhone,
                        onValueChange = { driverPhone = it },
                        label = { Text("ဖုန်းနံပါတ်") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("input_driver_phone"),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("ကားခ (ကျပ်)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = advanceStr,
                            onValueChange = { advanceStr = it },
                            label = { Text("ကြိုထုတ်ငွေ (ကျပ်)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = dispatchDate,
                        onValueChange = { dispatchDate = it },
                        label = { Text("ထွက်ခွာမည့် နေ့ရက်") },
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
                        val price = priceStr.toDoubleOrNull() ?: 1500000.0
                        val advance = advanceStr.toDoubleOrNull() ?: 0.0
                        viewModel.registerTruck(
                            truckNumber = truckNumber,
                            ownerName = ownerName,
                            driverName = driverName,
                            driverPhone = driverPhone,
                            mandalayToKalayFreightPrice = price,
                            advancePayment = advance,
                            dispatchDate = dispatchDate,
                            notes = notes,
                            onSuccess = {
                                showAddTruckDialog = false
                                Toast.makeText(context, "ကားသစ် အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange)
                ) {
                    Text("Save (သိမ်းဆည်းမည်)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTruckDialog = false }) {
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

// Compact Truck List Item showing ONLY [ ကားနံပါတ်, ပိုင်ရှင် အမည် or မောင်းသူ အမည် ]
@Composable
fun CompactTruckRowItem(
    truck: TruckEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val statusBg = when (truck.status) {
        "EMPTY" -> SurfaceLight
        "LOADING" -> StatusBlueBg
        "IN_TRANSIT" -> StatusOrangeBg
        else -> StatusGreenBg
    }
    val statusColor = when (truck.status) {
        "EMPTY" -> TextMuted
        "LOADING" -> StatusBlueText
        "IN_TRANSIT" -> StatusOrangeText
        else -> StatusGreenText
    }
    val statusLabel = when (truck.status) {
        "EMPTY" -> "ကုန်မရှိ"
        "LOADING" -> "ကုန်တင်ပြီး"
        "IN_TRANSIT" -> "စထွက်သွား"
        else -> "ကလေးရောက် ကုန်ချပြီး"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("truck_row_${truck.id}"),
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
                Text(
                    text = truck.truckNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BmkNavy
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ပိုင်ရှင်/မောင်းသူ: ${truck.ownerName.ifBlank { truck.driverName }} (${truck.driverPhone})",
                    fontSize = 12.sp,
                    color = BmkOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("အသေးစိတ်", fontSize = 10.sp, color = BmkNavy, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
