package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import com.example.data.model.CargoEntity
import com.example.data.model.TruckEntity
import com.example.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiAssistantDialog
import com.example.ui.viewmodel.AiLogisticsViewModel
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandalayTabScreen(
    viewModel: BmkViewModel,
    aiViewModel: AiLogisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val allCargo by viewModel.allCargo.collectAsState()
    val allTrucks by viewModel.allTrucks.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var showAiDialog by remember { mutableStateOf(false) }
    var showIntakeForm by remember { mutableStateOf(false) }
    var selectedCargoDetails by remember { mutableStateOf<CargoEntity?>(null) }

    // Form fields
    var cargoDensityType by remember { mutableStateOf("HEAVY") } // "HEAVY" (ကုန်မာ) or "BULK" (ကုန်ပွ)
    var cargoName by remember { mutableStateOf("") }
    
    // 3. တစ်ခု အလေးချိန် + (ပိဿာ or Kg)
    var unitWeightStr by remember { mutableStateOf("3.2") }
    var weightTypeUnit by remember { mutableStateOf("ပိဿာ") } // "ပိဿာ" or "Kg"
    
    // 4. အရေအတွက် + ယူနစ် (ဖာ, ခု, အိတ်, ပုံး, ထုပ်, etc)
    var quantityStr by remember { mutableStateOf("50") }
    var selectedPackageUnit by remember { mutableStateOf("ဖာ") }
    val packageUnitsList = listOf("ဖာ", "ခု", "အိတ်", "ပုံး", "ထုပ်", "စီး")
    var isPackageUnitDropdownExpanded by remember { mutableStateOf(false) }
    
    // 5. အသေးစိတ် ဖော်ပြချက်
    var itemDescription by remember { mutableStateOf("") }
    
    // 6. ပို့သူ အချက်အလက် (မန္တလေးဂိတ်)
    var senderName by remember { mutableStateOf("") }
    var senderPhone by remember { mutableStateOf("") }
    var senderAddress by remember { mutableStateOf("") }
    
    // 7. ခေါက်တို သွားတိုက်ရန် အမှန်ခြစ်
    var isShuttlePickup by remember { mutableStateOf(false) }
    var shuttleFeeStr by remember { mutableStateOf("5000") }
    var shuttleLaborFeeStr by remember { mutableStateOf("2000") }
    
    // 8. Kalaymyo မှ လက်ခံသူ
    var receiverName by remember { mutableStateOf("") }
    var receiverPhone by remember { mutableStateOf("") }
    var receiverShopAddress by remember { mutableStateOf("") }
    
    // 9. Car Tab မှ Car တစ်စီး ရွေးချယ်မှု
    var selectedTruck by remember { mutableStateOf<TruckEntity?>(null) }
    var isTruckDropdownExpanded by remember { mutableStateOf(false) }
    
    var paymentStatus by remember { mutableStateOf("PAID") }
    var remarks by remember { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var issuedTrackingNo by remember { mutableStateOf("") }

    // Dynamic Total Weight Calculation = [ အရေအတွက် x တစ်ခု အလေးချိန် ]
    val unitWeight = unitWeightStr.toDoubleOrNull() ?: 0.0
    val quantity = quantityStr.toIntOrNull() ?: 1
    
    val totalWeightInEnteredUnit = unitWeight * quantity
    
    val totalCalculatedViss = if (weightTypeUnit == "Kg") {
        viewModel.convertKgToViss(totalWeightInEnteredUnit)
    } else {
        totalWeightInEnteredUnit
    }
    val totalCalculatedKg = if (weightTypeUnit == "Kg") {
        totalWeightInEnteredUnit
    } else {
        viewModel.convertVissToKg(totalWeightInEnteredUnit)
    }

    val freightFee = totalCalculatedViss * settingsState.freightRatePerViss
    val mdyLoadingFee = totalCalculatedViss * settingsState.mdyLoadingRatePerViss
    val klyUnloadingFee = totalCalculatedViss * settingsState.klyUnloadingRatePerViss
    val shuttleFee = if (isShuttlePickup) (shuttleFeeStr.toDoubleOrNull() ?: 0.0) else 0.0
    val shuttleLaborFee = if (isShuttlePickup) (shuttleLaborFeeStr.toDoubleOrNull() ?: 0.0) else 0.0

    val totalCalculatedAmount = freightFee + mdyLoadingFee + klyUnloadingFee + shuttleFee + shuttleLaborFee

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (settingsState.language == "my") "မန္တလေးဂိတ် (Mandalay Gate)" else "Mandalay Gate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (settingsState.language == "my") "ကုန်လက်ခံခြင်း နှင့် ခေါက်တို ပို့ဆောင်မှု" else "Cargo Intake & Shuttle Service",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiDialog = true },
                        modifier = Modifier.testTag("action_open_ai")
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
                onClick = { showIntakeForm = true },
                containerColor = BmkOrange,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (settingsState.language == "my") "ကုန်လက်ခံမည်" else "Receive Cargo", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                modifier = Modifier.testTag("fab_receive_cargo")
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
            item {
                Text(
                    text = if (settingsState.language == "my") "မန္တလေးဂိတ် ကုန်ပစ္စည်းများ စာရင်း (${allCargo.size} ထုပ်) - [အမည်ကို နှိပ်၍ အသေးစိတ်ကြည့်ပါ]" else "Mandalay Cargos (${allCargo.size}) - [Tap name for details]",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Compact List View
            items(allCargo) { cargo ->
                CompactMandalayCargoListItem(
                    cargo = cargo,
                    currencyFormat = currencyFormat,
                    onClick = { selectedCargoDetails = cargo }
                )
            }
        }
    }

    // Detail Modal Dialog when item is tapped
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
                    Text(
                        text = cargo.trackingNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BmkNavy
                    )
                    Surface(
                        color = if (cargo.isShuttlePickup) StatusOrangeBg else StatusBlueBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (cargo.isShuttlePickup) "ခေါက်တို" else "ဂိတ်လာပို့",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cargo.isShuttlePickup) StatusOrangeText else StatusBlueText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ကုန်ပစ္စည်း အမည်: ${cargo.cargoName.ifBlank { cargo.itemDescription }}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text("• ကုန်အမျိုးအစား: ${if (cargo.cargoDensityType == "HEAVY") "ကုန်မာ (Heavy Cargo)" else "ကုန်ပွ (Bulk Cargo)"}", fontSize = 12.sp, color = TextSecondary)
                    Text("• အရေအတွက် & ယူနစ်: ${cargo.quantity} ${cargo.weightUnit}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("• တစ်ခု အလေးချိန်: ${cargo.unitWeight} ${cargo.weightUnit}", fontSize = 12.sp, color = TextSecondary)
                    Text("• စုစုပေါင်း အလေးချိန်: ${cargo.weightViss} ပိဿာ / ${String.format("%.1f", cargo.weightKg)} Kg", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                    Divider(color = CardBorderColor)
                    Text("• ပို့သူ: ${cargo.senderName} (${cargo.senderPhone})", fontSize = 12.sp, color = TextPrimary)
                    if (cargo.senderAddress.isNotBlank()) Text("  မန္တလေး လိပ်စာ: ${cargo.senderAddress}", fontSize = 11.sp, color = TextSecondary)
                    Text("• ကလေးမြို့ လက်ခံသူ: ${cargo.receiverName} (${cargo.receiverPhone})", fontSize = 12.sp, color = TextPrimary)
                    Text("  ကလေးမြို့ ဆိုင်လိပ်စာ: ${cargo.receiverShopAddress}", fontSize = 11.sp, color = TextSecondary)
                    Divider(color = CardBorderColor)
                    Text("• သတ်မှတ် ၁၂ ဘီး ကား: ${cargo.truckNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BmkNavy)
                    Text("• တန်ဆာခ: ${currencyFormat.format(cargo.freightFee)} ကျပ်", fontSize = 12.sp)
                    Text("• မန္တလေး ၁၂ ဘီး တင်ခ: ${currencyFormat.format(cargo.mdyLoadingLaborFee)} ကျပ်", fontSize = 12.sp)
                    if (cargo.isShuttlePickup) {
                        Text("• ခေါက်တို ကြေး + အလုပ်သမားခ: ${currencyFormat.format(cargo.shuttleFee + cargo.shuttleLaborFee)} ကျပ်", fontSize = 12.sp, color = BmkOrange)
                    }
                    Text("• စုစုပေါင်း ကျသင့်ငွေ: ${currencyFormat.format(cargo.totalAmount)} ကျပ် (${cargo.paymentStatus})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BmkOrange)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cargo.senderPhone}"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ပို့သူ ဖုန်းခေါ်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCargoDetails = null }) {
                    Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        )
    }

    // Refactored Intake Form Dialog matching exact 10 items
    if (showIntakeForm) {
        AlertDialog(
            onDismissRequest = { showIntakeForm = false },
            containerColor = Color.White,
            title = { Text("မန္တလေးဂိတ် ကုန်လက်ခံလွှာ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BmkNavy) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAiDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BmkNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ai_autofill_intake_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BmkOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✨ Gemini AI ဖြင့် စာကြောင်းမှ စာရင်းအလိုအလျောက်ဖြည့်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // 1. ကုန်မာ / ကုန်ပွ
                    item {
                        Text("၁။ ကုန်အမျိုးအစား:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { cargoDensityType = "HEAVY" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (cargoDensityType == "HEAVY") BmkNavy else SurfaceLight,
                                    contentColor = if (cargoDensityType == "HEAVY") Color.White else TextPrimary
                                ),
                                border = BorderStroke(1.dp, if (cargoDensityType == "HEAVY") BmkNavy else Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f)
                            ) { Text("ကုန်မာ (Heavy)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                            Button(
                                onClick = { cargoDensityType = "BULK" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (cargoDensityType == "BULK") BmkOrange else SurfaceLight,
                                    contentColor = if (cargoDensityType == "BULK") Color.White else TextPrimary
                                ),
                                border = BorderStroke(1.dp, if (cargoDensityType == "BULK") BmkOrange else Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f)
                            ) { Text("ကုန်ပွ (Bulk)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                    }

                    // 2. ကုန်ပစ္စည်း အမည်
                    item {
                        OutlinedTextField(
                            value = cargoName,
                            onValueChange = { cargoName = it },
                            label = { Text("၂။ ကုန်ပစ္စည်း အမည် (e.g. ကြွေပြား)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_cargo_name"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                    }

                    // 3. တစ်ခု အလေးချိန် + (ပိဿာ or Kg)
                    item {
                        Column {
                            Text("၃။ တစ်ခု အလေးချိန် (Weight per item):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = unitWeightStr,
                                    onValueChange = { unitWeightStr = it },
                                    label = { Text("တစ်ခု အလေးချိန် (e.g. 3.2)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.3f).testTag("input_unit_weight"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                                )

                                // Radio/Segmented Selection for "ပိဿာ" or "Kg"
                                Row(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .background(SurfaceLight, RoundedCornerShape(8.dp))
                                        .padding(2.dp)
                                ) {
                                    FilterChip(
                                        selected = weightTypeUnit == "ပိဿာ",
                                        onClick = { weightTypeUnit = "ပိဿာ" },
                                        label = { Text("ပိဿာ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BmkNavy,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color.Transparent,
                                            labelColor = TextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = weightTypeUnit == "Kg",
                                        onClick = { weightTypeUnit = "Kg" },
                                        label = { Text("Kg", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BmkNavy,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color.Transparent,
                                            labelColor = TextPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Customer လာအပ်သော အရေအတွက် + ဘေးတွင် ယူနစ် (ဖာ, ခု, အိတ်, ပုံး, etc)
                    item {
                        Column {
                            Text("၄။ Customer လာအပ်သော အရေအတွက် နှင့် ယူနစ်:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = quantityStr,
                                    onValueChange = { quantityStr = it },
                                    label = { Text("အရေအတွက် (e.g. 50)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.2f).testTag("input_cargo_quantity"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = isPackageUnitDropdownExpanded,
                                    onExpandedChange = { isPackageUnitDropdownExpanded = !isPackageUnitDropdownExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedPackageUnit,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("ယူနစ် (e.g. ဖာ)") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPackageUnitDropdownExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isPackageUnitDropdownExpanded,
                                        onDismissRequest = { isPackageUnitDropdownExpanded = false }
                                    ) {
                                        packageUnitsList.forEach { unitItem ->
                                            DropdownMenuItem(
                                                text = { Text(unitItem, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    selectedPackageUnit = unitItem
                                                    isPackageUnitDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. အသေးစိတ် ဖော်ပြချက် ၏ အထက်တွင် စုစုပေါင်း အလေးချိန် ဖော်ပြချက်
                    item {
                        Surface(
                            color = BmkNavy.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BmkNavy.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "စုစုပေါင်း အလေးချိန် = [ အရေအတွက် $quantity x တစ်ခု အလေးချိန် $unitWeight $weightTypeUnit ]",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "စုစုပေါင်း = ${String.format("%.2f", totalCalculatedViss)} ပိဿာ (${String.format("%.2f", totalCalculatedKg)} Kg)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BmkNavy
                                )
                            }
                        }
                    }

                    // ၅။ အသေးစိတ် ဖော်ပြချက်
                    item {
                        OutlinedTextField(
                            value = itemDescription,
                            onValueChange = { itemDescription = it },
                            label = { Text("၅။ ကုန်ပစ္စည်း အသေးစိတ် ဖော်ပြချက်") },
                            modifier = Modifier.fillMaxWidth().testTag("input_item_description"),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                    }

                    // ၆။ ပို့သူ (မန္တလေးဂိတ်) - အမည်၊ ဖုန်းနံပါတ်၊ Mandalay Address
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("၆။ ပို့သူ (မန္တလေးဂိတ်):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            OutlinedTextField(
                                value = senderName,
                                onValueChange = { senderName = it },
                                label = { Text("အမည်") },
                                modifier = Modifier.fillMaxWidth().testTag("input_sender_name"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                            OutlinedTextField(
                                value = senderPhone,
                                onValueChange = { senderPhone = it },
                                label = { Text("ဖုန်းနံပါတ်") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth().testTag("input_sender_phone"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                            OutlinedTextField(
                                value = senderAddress,
                                onValueChange = { senderAddress = it },
                                label = { Text("မန္တလေး လိပ်စာ (Mandalay Address)") },
                                modifier = Modifier.fillMaxWidth().testTag("input_sender_address"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                        }
                    }

                    // ၇။ ခေါက်တို သွားတိုက်ရန် အမှန်ခြစ်
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusOrangeBg),
                            border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isShuttlePickup,
                                        onCheckedChange = { isShuttlePickup = it },
                                        colors = CheckboxDefaults.colors(checkedColor = BmkOrange)
                                    )
                                    Text("၇။ ဂိတ် မပို့နိုင်ရင် ခေါက်တို သွားတိုက်ရန်", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusOrangeText)
                                }
                                AnimatedVisibility(visible = isShuttlePickup) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                        OutlinedTextField(
                                            value = shuttleFeeStr,
                                            onValueChange = { shuttleFeeStr = it },
                                            label = { Text("ခေါက်တို ကြေး (ကျပ်)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = shuttleLaborFeeStr,
                                            onValueChange = { shuttleLaborFeeStr = it },
                                            label = { Text("အလုပ်သမားခ (ကျပ်)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ၈။ Kalaymyo မှ လက်ခံသူ
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("၈။ Kalaymyo မှ လက်ခံသူ:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            OutlinedTextField(
                                value = receiverName,
                                onValueChange = { receiverName = it },
                                label = { Text("အမည်") },
                                modifier = Modifier.fillMaxWidth().testTag("input_receiver_name"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                            OutlinedTextField(
                                value = receiverPhone,
                                onValueChange = { receiverPhone = it },
                                label = { Text("ဖုန်းနံပါတ်") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth().testTag("input_receiver_phone"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                            OutlinedTextField(
                                value = receiverShopAddress,
                                onValueChange = { receiverShopAddress = it },
                                label = { Text("ကလေးမြို့ ပို့ရမည့် ဆိုင် / လိပ်စာ") },
                                modifier = Modifier.fillMaxWidth().testTag("input_receiver_shop_address"),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                        }
                    }

                    // ၉။ Car Tab မှ Car တစ်စီး ရွေးချယ်မှု
                    item {
                        Column {
                            Text("၉။ Car Tab မှ Car တစ်စီး ရွေးချယ်ရန် (Optional):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = isTruckDropdownExpanded,
                                onExpandedChange = { isTruckDropdownExpanded = !isTruckDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedTruck?.truckNumber ?: "၁၂ ဘီး ကား ရွေးချယ်ရန်",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("သတ်မှတ် ၁၂ ဘီး ကား") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTruckDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8), focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                                )
                                ExposedDropdownMenu(
                                    expanded = isTruckDropdownExpanded,
                                    onDismissRequest = { isTruckDropdownExpanded = false }
                                ) {
                                    allTrucks.forEach { truck ->
                                        DropdownMenuItem(
                                            text = { Text("${truck.truckNumber} (${truck.driverName})", fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                selectedTruck = truck
                                                isTruckDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Total Calculation Summary Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ကျသင့်ငွေ တွက်ချက်မှု ချုပ်:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                                Text("• တန်ဆာခ (${String.format("%.1f", totalCalculatedViss)} ပိဿာ x ${settingsState.freightRatePerViss.toInt()} ကျပ်) = ${currencyFormat.format(freightFee)} ကျပ်", fontSize = 12.sp, color = TextPrimary)
                                Text("• မန္တလေး ၁၂ ဘီး တင်ခ = ${currencyFormat.format(mdyLoadingFee)} ကျပ်", fontSize = 12.sp, color = TextPrimary)
                                Text("• ကလေး ၁၂ ဘီး ချခ = ${currencyFormat.format(klyUnloadingFee)} ကျပ်", fontSize = 12.sp, color = TextPrimary)
                                if (isShuttlePickup) {
                                    Text("• ခေါက်တို ကြေး + အလုပ်သမားခ = ${currencyFormat.format(shuttleFee + shuttleLaborFee)} ကျပ်", fontSize = 12.sp, color = BmkOrange, fontWeight = FontWeight.Bold)
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFCBD5E1))
                                Text(
                                    text = "စုစုပေါင်း ကျသင့်ငွေ = ${currencyFormat.format(totalCalculatedAmount)} ကျပ်",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BmkOrange
                                )
                            }
                        }
                    }
                }
            },
            // ၁၀။ Cancel သို့မဟုတ် စာရင်းသွင်းမည်
            confirmButton = {
                Button(
                    onClick = {
                        if (cargoName.isBlank() || senderName.isBlank() || receiverName.isBlank() || senderPhone.isBlank()) {
                            Toast.makeText(context, "ကျေးဇူးပြု၍ ကုန်အမည်၊ ပို့သူ နှင့် လက်ခံသူ ဖြည့်သွင်းပါ", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.registerCargoIntake(
                                cargoDensityType = cargoDensityType,
                                cargoName = cargoName,
                                unitWeight = unitWeight,
                                weightUnit = selectedPackageUnit,
                                quantity = quantity,
                                itemDescription = itemDescription.ifBlank { cargoName },
                                senderName = senderName,
                                senderPhone = senderPhone,
                                senderAddress = senderAddress,
                                isShuttlePickup = isShuttlePickup,
                                shuttleFee = shuttleFee,
                                shuttleLaborFee = shuttleLaborFee,
                                receiverName = receiverName,
                                receiverPhone = receiverPhone,
                                receiverShopAddress = receiverShopAddress.ifBlank { "ကလေးမြို့ ဂိတ်တွင် လာထုတ်မည်" },
                                assignedTruck = selectedTruck,
                                paymentStatus = paymentStatus,
                                remarks = remarks,
                                onSuccess = { trackingNo ->
                                    issuedTrackingNo = trackingNo
                                    showIntakeForm = false
                                    showSuccessDialog = true
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                    modifier = Modifier.testTag("confirm_register_btn")
                ) {
                    Text("စာရင်းသွင်းမည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showIntakeForm = false },
                    modifier = Modifier.testTag("cancel_register_btn")
                ) {
                    Text("Cancel (မလုပ်တော့ပါ)", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            containerColor = Color.White,
            title = { Text("ကုန်ပစ္စည်း စာရင်းသွင်းပြီးပါပြီ!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BmkNavy) },
            text = { Text("ဘောက်ချာ နံပါတ်: $issuedTrackingNo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkOrange) },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)) {
                    Text("အတည်ပြုပါသည်", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAiDialog) {
        AiAssistantDialog(
            aiViewModel = aiViewModel,
            onDismiss = { showAiDialog = false },
            onAutoFillCargo = { parsed ->
                showIntakeForm = true
                cargoDensityType = parsed.cargoType
                cargoName = parsed.cargoName
                quantityStr = parsed.quantity.toString()
                selectedPackageUnit = parsed.packageUnit.ifBlank { "ဖာ" }
                unitWeightStr = parsed.unitWeight.toString()
                if (parsed.senderName.isNotBlank()) senderName = parsed.senderName
                if (parsed.senderPhone.isNotBlank()) senderPhone = parsed.senderPhone
                if (parsed.description.isNotBlank()) itemDescription = parsed.description
            }
        )
    }
}

@Composable
fun CompactMandalayCargoListItem(
    cargo: CargoEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("cargo_list_item_${cargo.trackingNumber}"),
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
                        text = cargo.cargoName.ifBlank { cargo.itemDescription },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BmkNavy
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (cargo.cargoDensityType == "HEAVY") BmkNavy.copy(alpha = 0.12f) else StatusOrangeBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (cargo.cargoDensityType == "HEAVY") "ကုန်မာ" else "ကုန်ပွ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cargo.cargoDensityType == "HEAVY") BmkNavy else StatusOrangeText,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "အရေအတွက်: ${cargo.quantity} ${cargo.weightUnit} (${cargo.weightViss} ပိဿာ)",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Text(
                    text = "ပို့သူ: ${cargo.senderName} (${cargo.senderPhone})",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currencyFormat.format(cargo.totalAmount)} ကျပ်",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BmkOrange
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "အသေးစိတ်ကြည့်ပါ",
                        fontSize = 10.sp,
                        color = BmkNavy,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = BmkNavy,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
