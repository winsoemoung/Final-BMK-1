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
import com.example.data.model.CompensationEntity
import com.example.ui.components.AiAssistantDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiLogisticsViewModel
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalaymyoTabScreen(
    viewModel: BmkViewModel,
    aiViewModel: AiLogisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val allCargo by viewModel.allCargo.collectAsState()
    val claims by viewModel.compensationClaims.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var showAiDialog by remember { mutableStateOf(false) }
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("၁၂ ဘီး ကားလိုက် လိုက်ပို့ခြင်း", "ဆိုင်အရောက် ကုန်ပစ္စည်း စာရင်း", "ပျက်စီးမှု လျော်ကြေး")

    var selectedCargoDetails by remember { mutableStateOf<CargoEntity?>(null) }
    var cargoToPrint by remember { mutableStateOf<CargoEntity?>(null) }
    var selectedClaimDetails by remember { mutableStateOf<CompensationEntity?>(null) }
    var showClaimDialog by remember { mutableStateOf(false) }

    // Delivery Status Update Dialog State
    var deliveryUpdateCargo by remember { mutableStateOf<CargoEntity?>(null) }
    var showPromisedDatePicker by remember { mutableStateOf(false) }
    var promisedDateInput by remember { mutableStateOf("2026-10-30") }

    // Collapsed Truck State: truckId -> collapsed/expanded boolean
    val collapsedTrucksMap = remember { mutableStateMapOf<Long, Boolean>() }
    // Truck Sort Mode: truckId -> SortMode ("SEQUENCE", "QUARTER", "STREET", "DENSITY", "AI_ROUTE")
    val truckSortModesMap = remember { mutableStateMapOf<Long, String>() }

    // Claim form
    var trackingNo by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var damageDescription by remember { mutableStateOf("") }
    var claimAmountStr by remember { mutableStateOf("25000") }
    var paidAmountStr by remember { mutableStateOf("25000") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (settingsState.language == "my") "ကလေးမြို့ဂိတ် (Kalaymyo Gate)" else "Kalaymyo Gate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "၁၂ ဘီး ကုန်ချခြင်း နှင့် ဆိုင်အရောက် ပို့ဆောင်မှု",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiDialog = true },
                        modifier = Modifier.testTag("kalay_action_open_ai")
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight)
        ) {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = CardBackground,
                contentColor = BmkNavy
            ) {
                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSubTab == index,
                        onClick = { selectedSubTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (selectedSubTab == index) BmkNavy else TextSecondary
                            )
                        }
                    )
                }
            }

            if (selectedSubTab == 0) {
                // 1. Truck Delivery Manifest (၁၂ ဘီး ကားလိုက် ကုန်ချ လိုက်ပို့ခြင်း)
                val trucksGrouped = remember(allCargo) {
                    allCargo.filter { it.truckNumber.isNotBlank() }.groupBy { it.truckId ?: 0L }
                }

                if (trucksGrouped.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ကလေးမြို့သို့ ရောက်ရှိလာသော ၁၂ ဘီး ကားများ မရှိသေးပါ", fontSize = 13.sp, color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(trucksGrouped.keys.toList()) { truckId ->
                            val rawTruckCargos = trucksGrouped[truckId] ?: emptyList()
                            val firstCargo = rawTruckCargos.firstOrNull()
                            val truckName = firstCargo?.truckNumber?.ifEmpty { "၁၂ ဘီး ယာဉ် #$truckId" } ?: "၁၂ ဘီး ယာဉ် #$truckId"
                            val isExpanded = collapsedTrucksMap[truckId] ?: true
                            val currentSortMode = truckSortModesMap[truckId] ?: "SEQUENCE"

                            // Sorted cargos based on selected sort mode
                            val sortedTruckCargos = remember(rawTruckCargos, currentSortMode) {
                                when (currentSortMode) {
                                    "QUARTER" -> rawTruckCargos.sortedBy { it.quarterName.ifBlank { it.receiverShopAddress } }
                                    "STREET" -> rawTruckCargos.sortedBy { it.streetName.ifBlank { it.receiverShopAddress } }
                                    "DENSITY" -> rawTruckCargos.sortedBy { it.cargoDensityType } // HEAVY vs BULK
                                    "AI_ROUTE" -> rawTruckCargos.sortedWith(
                                        compareBy<CargoEntity> { it.quarterName }
                                            .thenBy { it.streetName }
                                            .thenBy { it.cargoDensityType }
                                    )
                                    else -> rawTruckCargos.sortedBy { it.sequenceOrder }
                                }
                            }

                            // Calculate totals
                            val totalWeight = rawTruckCargos.sumOf { it.weightViss }
                            val totalAmt = rawTruckCargos.sumOf { it.totalAmount }
                            val paidAmt = rawTruckCargos.filter { it.deliveryStatus == "UNLOADED_PAID" || it.paymentStatus == "PAID" }.sumOf { it.totalAmount }
                            val creditAmt = rawTruckCargos.filter { it.deliveryStatus == "UNLOADED_CREDIT" || it.paymentStatus == "COLLECT" }.sumOf { it.totalAmount }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                border = BorderStroke(1.5.dp, BmkNavy)
                            ) {
                                Column {
                                    // Truck Header (Collapsible)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(BmkNavy)
                                            .clickable { collapsedTrucksMap[truckId] = !isExpanded }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = BmkOrange, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = truckName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "ယာဉ်မောင်း: ${firstCargo?.driverName ?: "-"} | ကုန်စည် (${rawTruckCargos.size}) မျိုး",
                                                    fontSize = 11.sp,
                                                    color = Color.White.copy(alpha = 0.85f)
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Toggle Expand",
                                            tint = Color.White
                                        )
                                    }

                                    if (isExpanded) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            // Smart Sorting Options Bar
                                            Text(
                                                text = "ဆိုင်အရောက် လိုက်ပို့ရန် အစဉ်လိုက် စီစဉ်မည်:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                listOf(
                                                    "SEQUENCE" to "နံပါတ်စဉ်",
                                                    "QUARTER" to "ရပ်ကွက်",
                                                    "STREET" to "လမ်း",
                                                    "DENSITY" to "ကုန်မာ/ပွ",
                                                    "AI_ROUTE" to "AI အစဉ်"
                                                ).forEach { (mode, label) ->
                                                    val isSelected = currentSortMode == mode
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = { truckSortModesMap[truckId] = mode },
                                                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else TextPrimary) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = BmkNavy,
                                                            selectedLabelColor = Color.White,
                                                            containerColor = SurfaceLight
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            Divider(color = CardBorderColor)

                                            // Color-coded Cargo List
                                            sortedTruckCargos.forEachIndexed { index, cargo ->
                                                val statusBgColor = when (cargo.deliveryStatus) {
                                                    "UNLOADED_PAID" -> Color(0xFFDCFCE7) // Emerald Green Light
                                                    "UNLOADED_CREDIT" -> Color(0xFFFEF3C7) // Amber Light
                                                    "UNLOADED_PENDING" -> Color(0xFFE0F2FE) // Sky Blue Light
                                                    "DAMAGED" -> Color(0xFFFEE2E2) // Red Light
                                                    else -> SurfaceLight // Default Neutral
                                                }

                                                val statusBorderColor = when (cargo.deliveryStatus) {
                                                    "UNLOADED_PAID" -> Color(0xFF059669)
                                                    "UNLOADED_CREDIT" -> Color(0xFFD97706)
                                                    "UNLOADED_PENDING" -> Color(0xFF0284C7)
                                                    "DAMAGED" -> Color(0xFFDC2626)
                                                    else -> CardBorderColor
                                                }

                                                val statusBadgeText = when (cargo.deliveryStatus) {
                                                    "UNLOADED_PAID" -> "ကုန်ချ ငွေရှင်းပြီး"
                                                    "UNLOADED_CREDIT" -> "ကုန်ချ - ငွေပေးမည့်ရက်: ${cargo.promisedPaymentDate.ifEmpty { "ရက်စွဲမရှိ" }}"
                                                    "UNLOADED_PENDING" -> "ကုန်ချပြီး ငွေမရှင်းရသေး"
                                                    "DAMAGED" -> "ကုန်ပစ္စည်း ပျက်စီး/လျော်ကြေး"
                                                    else -> "ကုန်ချဆဲ / မချရသေး"
                                                }

                                                val statusBadgeTextColor = when (cargo.deliveryStatus) {
                                                    "UNLOADED_PAID" -> Color(0xFF047857)
                                                    "UNLOADED_CREDIT" -> Color(0xFFB45309)
                                                    "UNLOADED_PENDING" -> Color(0xFF0369A1)
                                                    "DAMAGED" -> Color(0xFFB91C1C)
                                                    else -> TextSecondary
                                                }

                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { deliveryUpdateCargo = cargo },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = statusBgColor),
                                                    border = BorderStroke(1.dp, statusBorderColor)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Surface(color = BmkNavy, shape = RoundedCornerShape(4.dp)) {
                                                                    Text("#${index + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = cargo.receiverName,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 13.sp,
                                                                    color = BmkNavy
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text("(${cargo.receiverPhone})", fontSize = 11.sp, color = TextSecondary)
                                                            }
                                                            Text("ဆိုင်/လိပ်စာ: ${cargo.receiverShopAddress}", fontSize = 11.sp, color = TextPrimary)
                                                            Text("ကုန်: ${cargo.cargoName.ifBlank { cargo.itemDescription }} (${cargo.weightViss} ပိဿာ)", fontSize = 11.sp, color = TextSecondary)

                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Surface(
                                                                color = statusBorderColor.copy(alpha = 0.2f),
                                                                shape = RoundedCornerShape(4.dp),
                                                                border = BorderStroke(1.dp, statusBorderColor)
                                                            ) {
                                                                Text(
                                                                    text = statusBadgeText,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = statusBadgeTextColor,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(
                                                                text = "${currencyFormat.format(cargo.totalAmount)} ကျပ်",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = BmkOrange
                                                            )
                                                            IconButton(
                                                                onClick = { deliveryUpdateCargo = cargo },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.Edit, contentDescription = "Update Status", tint = BmkNavy, modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Divider(color = CardBorderColor)

                                            // Bottom Summary Card for Truck
                                            Surface(
                                                color = SurfaceLight,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, CardBorderColor)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("စုစုပေါင်း ပစ္စည်း: ${rawTruckCargos.size} မျိုး", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text("စုစုပေါင်း အလေးချိန်: $totalWeight ပိဿာ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("စုစုပေါင်း ငွေ: ${currencyFormat.format(totalAmt)} ကျပ်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BmkNavy)
                                                        Text("ရရှိပြီး: ${currencyFormat.format(paidAmt)} ကျပ်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("ရရန်ကျန်ငွေ (Credit): ${currencyFormat.format(creditAmt)} ကျပ်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BmkOrange)
                                                    }
                                                }
                                            }

                                            // Auto Archive / History Transfer Button
                                            Button(
                                                onClick = {
                                                    viewModel.autoArchiveTruckIfCompleted(truckId, rawTruckCargos)
                                                    Toast.makeText(context, "ကားနံပါတ် $truckName စာရင်း အားလုံးပြီးဆုံး၍ History သို့ ရွှေ့ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Archive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("ကုန်ချ ရှင်းပြီးပါက History သို့ အပြီးသိမ်းမည်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedSubTab == 1) {
                // Kalaymyo Receivers & Unloading
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "ကလေးမြို့ လက်ခံသူ / ဆိုင်များ စာရင်း (${allCargo.size} ထုပ်) - [အမည်ကို နှိပ်၍ အသေးစိတ်ကြည့်ပါ]",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    items(allCargo) { cargo ->
                        CompactKalayCargoListItem(
                            cargo = cargo,
                            currencyFormat = currencyFormat,
                            onClick = { selectedCargoDetails = cargo }
                        )
                    }
                }
            } else {
                // Compensation Claims
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ကုန်ပစ္စည်း ပျက်စီးမှု လျော်ကြေး စာရင်း",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Button(
                                    onClick = { showClaimDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("လျော်ကြေးသစ်", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        items(claims) { claim ->
                            CompactClaimListItem(
                                claim = claim,
                                currencyFormat = currencyFormat,
                                onClick = { selectedClaimDetails = claim }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delivery Status Update Dialog
    deliveryUpdateCargo?.let { cargo ->
        AlertDialog(
            onDismissRequest = { deliveryUpdateCargo = null },
            containerColor = Color.White,
            title = {
                Text(
                    text = "${cargo.receiverName} (${cargo.receiverShopAddress})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BmkNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ကုန်အမည်: ${cargo.cargoName.ifBlank { cargo.itemDescription }} (${cargo.weightViss} ပိဿာ)", fontSize = 12.sp, color = TextPrimary)
                    Text("ကျသင့်ငွေ: ${currencyFormat.format(cargo.totalAmount)} ကျပ်", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkOrange)

                    Divider(color = CardBorderColor)
                    Text("ကုန်ချ အခြေအနေ နှင့် ငွေပေးခြေမှု သတ်မှတ်ရန်:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // Option 1: Paid
                    Button(
                        onClick = {
                            viewModel.updateCargoDeliveryAndPayment(
                                cargoId = cargo.id,
                                deliveryStatus = "UNLOADED_PAID",
                                paymentStatus = "PAID",
                                promisedDate = ""
                            )
                            deliveryUpdateCargo = null
                            Toast.makeText(context, "ကုန်ချ၍ ငွေရှင်းပြီးကြောင်း မှတ်တမ်းတင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ကုန်ချ၍ ငွေပေးချေပြီး (Paid)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }

                    // Option 2: Promised Payment Date
                    Button(
                        onClick = {
                            showPromisedDatePicker = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("နောက်မှ ပေးမည် / ငွေပေးမည့်ရက် သတ်မှတ်မည်", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }

                    if (showPromisedDatePicker) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("ငွေလာယူရမည့် ရက်စွဲ (YYYY-MM-DD):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = promisedDateInput,
                                    onValueChange = { promisedDateInput = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        viewModel.updateCargoDeliveryAndPayment(
                                            cargoId = cargo.id,
                                            deliveryStatus = "UNLOADED_CREDIT",
                                            paymentStatus = "COLLECT",
                                            promisedDate = promisedDateInput
                                        )
                                        showPromisedDatePicker = false
                                        deliveryUpdateCargo = null
                                        Toast.makeText(context, "ငွေပေးမည့်ရက် $promisedDateInput အား မှတ်တမ်းတင်ပြီးပါပြီ (Notification Reminder တက်ပါမည်)", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BmkNavy),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ရက်စွဲ အတည်ပြုမည်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Option 3: Damaged
                    OutlinedButton(
                        onClick = {
                            viewModel.updateCargoDeliveryAndPayment(
                                cargoId = cargo.id,
                                deliveryStatus = "DAMAGED",
                                paymentStatus = cargo.paymentStatus,
                                promisedDate = ""
                            )
                            deliveryUpdateCargo = null
                            Toast.makeText(context, "ကုန်ပစ္စည်း ပျက်စီး/လျော်ကြေး အဖြစ် မှတ်တမ်းတင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ကုန်ပစ္စည်း ပျက်စီး / လျော်ကြေး", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFDC2626))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { cargoToPrint = cargo },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print ထုတ်မည်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { deliveryUpdateCargo = null; showPromisedDatePicker = false }) {
                    Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        )
    }

    // Cargo Detail Dialog
    selectedCargoDetails?.let { cargo ->
        AlertDialog(
            onDismissRequest = { selectedCargoDetails = null },
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = cargo.trackingNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy)
                    Surface(
                        color = if (cargo.status == "DELIVERED_TO_SHOP") StatusGreenBg else StatusAmberBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (cargo.status == "DELIVERED_TO_SHOP") "ဆိုင်အရောက် ပို့ပြီး" else "ကလေးဂိတ်ရောက်",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cargo.status == "DELIVERED_TO_SHOP") StatusGreenText else StatusAmberText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ကလေးမြို့ လက်ခံသူ: ${cargo.receiverName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text("• ဖုန်းနံပါတ်: ${cargo.receiverPhone}", fontSize = 12.sp, color = BmkOrange, fontWeight = FontWeight.Bold)
                    Text("• ပို့ရမည့် ဆိုင်လိပ်စာ: ${cargo.receiverShopAddress}", fontSize = 12.sp, color = TextPrimary)
                    Text("• ကုန်ပစ္စည်း: ${cargo.cargoName.ifBlank { cargo.itemDescription }} (${cargo.quantity} ${cargo.weightUnit})", fontSize = 12.sp, color = TextPrimary)
                    Text("• ကလေး ၁၂ ဘီး ကုန်ချခ: ${currencyFormat.format(cargo.klyUnloadingLaborFee)} ကျပ် (၂၅ ကျပ်/ပိဿာ)", fontSize = 12.sp, color = TextSecondary)
                    Text("• စုစုပေါင်း ကျသင့်ငွေ: ${currencyFormat.format(cargo.totalAmount)} ကျပ်", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BmkOrange)

                    Divider(color = CardBorderColor)

                    Text("ကလေးမြို့ ဂိတ်အခြေအနေ ပြောင်းရန်:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "KALAY_ARRIVED" to "ကလေးဂိတ်ရောက်",
                            "DELIVERED_TO_SHOP" to "ဆိုင်အရောက်ပို့ပြီး"
                        ).forEach { (code, label) ->
                            val isSelected = cargo.status == code
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateCargoStatus(cargo.id, code) },
                                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else TextPrimary) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1E3A8A),
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { cargoToPrint = cargo },
                        colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print ထုတ်မည်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cargo.receiverPhone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ဖုန်းခေါ်မည်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCargoDetails = null }) {
                    Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        )
    }

    // Cargo Print Dialog trigger
    cargoToPrint?.let { cargo ->
        com.example.ui.components.CargoPrintDialog(
            cargo = cargo,
            viewModel = viewModel,
            onDismiss = { cargoToPrint = null }
        )
    }

    // Claim Detail Dialog
    selectedClaimDetails?.let { claim ->
        AlertDialog(
            onDismissRequest = { selectedClaimDetails = null },
            containerColor = Color.White,
            title = { Text("ဘောက်ချာ: ${claim.trackingNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ဖောက်သည်: ${claim.customerName} (${claim.customerPhone})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text("• ပျက်စီးမှု/ဆုံးရှုံးမှု: ${claim.damageDescription}", fontSize = 12.sp, color = TextPrimary)
                    Text("• တောင်းဆိုငွေ: ${currencyFormat.format(claim.claimAmount)} ကျပ်", fontSize = 12.sp, color = TextSecondary)
                    Text("• ပေးလျော်ပြီးငွေ: ${currencyFormat.format(claim.paidAmount)} ကျပ်", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BmkOrange)
                    Text("• အခြေအနေ: ${if (claim.status == "PAID") "လျော်ကြေး ပေးပြီး" else "စိစစ်နေဆဲ"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BmkNavy)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val compensationCargo = CargoEntity(
                                id = claim.id,
                                trackingNumber = claim.trackingNumber.ifBlank { "DMG-${claim.id}" },
                                cargoName = "ပျက်စီးမှု လျော်ကြေး (" + (if (claim.status == "PAID") "ပေးလျော်ပြီး" else "စိစစ်ဆဲ") + ")",
                                itemDescription = claim.damageDescription,
                                senderName = "BMK EXPRESS Car Gate",
                                senderPhone = "09-987769641",
                                receiverName = claim.customerName,
                                receiverPhone = claim.customerPhone,
                                receiverShopAddress = "လျော်ကြေး ရက်စွဲ: ${claim.date}",
                                totalAmount = if (claim.paidAmount > 0) claim.paidAmount else claim.claimAmount,
                                freightFee = claim.claimAmount,
                                paymentStatus = if (claim.status == "PAID") "PAID" else "COLLECT",
                                remarks = "တောင်းဆိုငွေ: ${claim.claimAmount} ကျပ် | ပေးလျော်ငွေ: ${claim.paidAmount} ကျပ် | ${claim.notes}"
                            )
                            cargoToPrint = compensationCargo
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print Claim", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print ထုတ်မည်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { selectedClaimDetails = null },
                        colors = ButtonDefaults.buttonColors(containerColor = BmkNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ပိတ်မည်", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    if (showClaimDialog) {
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            containerColor = Color.White,
            title = { Text("ပျက်စီးမှု လျော်ကြေး စာရင်းသွင်းရန်", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = trackingNo,
                        onValueChange = { trackingNo = it },
                        label = { Text("ဘောက်ချာ နံပါတ်") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("ဖောက်သည် အမည်") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("ဖုန်းနံပါတ်") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = damageDescription,
                        onValueChange = { damageDescription = it },
                        label = { Text("ပျက်စီးမှု အသေးစိတ်") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = claimAmountStr,
                            onValueChange = { claimAmountStr = it },
                            label = { Text("တောင်းဆိုငွေ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paidAmountStr,
                            onValueChange = { paidAmountStr = it },
                            label = { Text("ပေးလျော်ငွေ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val claim = claimAmountStr.toDoubleOrNull() ?: 0.0
                        val paid = paidAmountStr.toDoubleOrNull() ?: 0.0
                        viewModel.submitCompensationClaim(
                            cargoId = 0,
                            trackingNumber = trackingNo,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            damageDescription = damageDescription,
                            claimAmount = claim,
                            paidAmount = paid,
                            date = "2026-10-26",
                            notes = notes,
                            onSuccess = {
                                showClaimDialog = false
                                Toast.makeText(context, "လျော်ကြေး စာရင်းသွင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange)
                ) {
                    Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClaimDialog = false }) {
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
fun CompactKalayCargoListItem(
    cargo: CargoEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("kalay_cargo_${cargo.trackingNumber}"),
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
                Text(
                    text = "ကလေးမြို့ လက်ခံသူ: ${cargo.receiverName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BmkNavy
                )
                Text(
                    text = "ဆိုင်/လိပ်စာ: ${cargo.receiverShopAddress}",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Text(
                    text = "ကုန်ပစ္စည်း: ${cargo.cargoName.ifBlank { cargo.itemDescription }}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (cargo.status == "DELIVERED_TO_SHOP") StatusGreenBg else StatusAmberBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (cargo.status == "DELIVERED_TO_SHOP") "ဆိုင်ရောက်ပြီး" else "ကလေးဆိုက်",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cargo.status == "DELIVERED_TO_SHOP") StatusGreenText else StatusAmberText,
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

@Composable
fun CompactClaimListItem(
    claim: CompensationEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("claim_item_${claim.id}"),
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
                Text(
                    text = "ဘောက်ချာ: ${claim.trackingNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BmkNavy
                )
                Text(
                    text = "ဖောက်သည်: ${claim.customerName} (${claim.customerPhone})",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currencyFormat.format(claim.paidAmount)} ကျပ်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BmkOrange
                )
                Surface(
                    color = if (claim.status == "PAID") StatusGreenBg else StatusAmberBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (claim.status == "PAID") "ပေးပြီး" else "စိစစ်ဆဲ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (claim.status == "PAID") StatusGreenText else StatusAmberText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
