package com.example.ui.components

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.SystemPrintHelper
import com.example.data.model.CargoEntity
import com.example.data.model.PrintTemplateEntity
import com.example.data.model.PrintedVoucherEntity
import com.example.data.model.TruckEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmkViewModel
import com.example.util.BluetoothPrinterManager
import com.example.util.PrinterConnectionState
import com.example.util.ReceiptBitmapGenerator
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Dialog for printing any cargo or truck waybill with template selection,
 * live preview, and Bluetooth transmission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CargoPrintDialog(
    cargo: CargoEntity? = null,
    truck: TruckEntity? = null,
    cargosOnTruck: List<CargoEntity> = emptyList(),
    viewModel: BmkViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val templates by viewModel.allTemplates.collectAsState()

    val printerManager = remember { BluetoothPrinterManager(context) }
    val connectionState by printerManager.connectionState.collectAsState()
    val pairedDevices by printerManager.pairedDevices.collectAsState()

    var selectedTemplateIndex by remember { mutableStateOf(0) }
    val currentTemplate = templates.getOrNull(selectedTemplateIndex) ?: templates.firstOrNull()

    // Voucher Number
    var voucherNumberInput by remember {
        mutableStateOf(viewModel.generateNextVoucherNumber())
    }

    // Refresh paired devices on launch
    LaunchedEffect(Unit) {
        printerManager.refreshPairedDevices()
    }

    // Generated receipt bitmap for preview and printing
    val receiptBitmap: Bitmap? = remember(currentTemplate, voucherNumberInput, cargo, truck) {
        if (currentTemplate != null) {
            if (cargo != null) {
                ReceiptBitmapGenerator.fromCargo(currentTemplate, cargo, voucherNumberInput)
            } else if (truck != null) {
                // Generate truck waybill
                val customRows = cargosOnTruck.mapIndexed { index, c ->
                    listOf(
                        "${index + 1}",
                        c.receiverName.ifBlank { c.senderName },
                        c.cargoName.ifBlank { c.itemDescription },
                        "${c.quantity}",
                        "${c.weightViss.toInt()}",
                        "${c.freightFee.toInt()}",
                        if (c.paymentStatus == "PAID") "ရှင်း" else "လ/ခ",
                        "-"
                    )
                }
                ReceiptBitmapGenerator.generateReceiptBitmap(
                    template = currentTemplate,
                    voucherNumber = voucherNumberInput,
                    carNumber = truck.truckNumber,
                    driverName = truck.driverName,
                    customerName = "ယာဉ်တင် ကုန်စည် စာရင်းချုပ်",
                    itemDescription = "ကုန်ပစ္စည်း အုပ်စုပေါင်း (${truck.totalCargosCount}) ခု",
                    quantity = truck.totalCargosCount,
                    weightViss = truck.totalVissLoaded,
                    freightFee = truck.mandalayToKalayFreightPrice,
                    laborFee = truck.totalVissLoaded * 25.0,
                    totalAmount = truck.mandalayToKalayFreightPrice,
                    paymentStatus = "PAID",
                    customRows = customRows,
                    remarks = truck.notes
                )
            } else {
                null
            }
        } else {
            null
        }
    }

    var isPrinting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("ဘောက်ချာ Print ထုတ်ရန် (Print Voucher)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy)
                            Text("Thermal 58mm / 80mm / 100mm Bluetooth Printer", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorderColor)

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left side: Settings & Printer Connection
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Voucher Number Input Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = BorderStroke(1.dp, CardBorderColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("ဘောင်ချာ အမှတ် (Voucher Number):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = voucherNumberInput,
                                        onValueChange = { voucherNumberInput = it },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BmkNavy,
                                            unfocusedBorderColor = Color(0xFF94A3B8)
                                        )
                                    )
                                    Button(
                                        onClick = { voucherNumberInput = viewModel.generateNextVoucherNumber() },
                                        colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)
                                    ) {
                                        Text("အသစ်ယူ", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Template Selector (1 to 7)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = BorderStroke(1.dp, CardBorderColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Print ပုံစံ ရွေးချယ်ရန် (Templates 1 - 7):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)

                                templates.forEachIndexed { index, tmpl ->
                                    val isSelected = index == selectedTemplateIndex
                                    Surface(
                                        color = if (isSelected) BmkNavy else Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isSelected) BmkNavy else CardBorderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedTemplateIndex = index }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    tmpl.templateName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) Color.White else TextPrimary
                                                )
                                                Text(
                                                    "${tmpl.paperWidthMm}mm  |  ${if (tmpl.hasTableGrid) "ဇယားကွက် ပါရှိ" else "စာသားသီးသန့်"}",
                                                    fontSize = 10.sp,
                                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BmkOrange, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bluetooth Printer Device Selection
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = BorderStroke(1.dp, CardBorderColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bluetooth Thermal ပရင်တာ:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                    }
                                    IconButton(
                                        onClick = { printerManager.refreshPairedDevices() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BmkNavy)
                                    }
                                }

                                // Status badge
                                when (val state = connectionState) {
                                    is PrinterConnectionState.Connected -> {
                                        Surface(
                                            color = SuccessGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, SuccessGreen)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("ချိတ်ဆက်ထားသည်: ${state.deviceName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                            }
                                        }
                                    }
                                    is PrinterConnectionState.Connecting -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BmkOrange)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("ချိတ်ဆက်နေသည်...", fontSize = 11.sp, color = BmkOrange)
                                        }
                                    }
                                    is PrinterConnectionState.Error -> {
                                        Text(state.message, fontSize = 11.sp, color = ErrorRed)
                                    }
                                    PrinterConnectionState.Disconnected -> {
                                        Text("Bluetooth ပရင်တာ ရွေးချယ်ပြီး ချိတ်ဆက်ပါ", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }

                                if (pairedDevices.isEmpty()) {
                                    Text("ဖုန်း Bluetooth Settings တွင် Thermal Printer အား အရင် Paired ပြုလုပ်ထားပါ", fontSize = 11.sp, color = TextMuted)
                                } else {
                                    pairedDevices.forEach { device ->
                                        val deviceName = try { device.name ?: "Unknown Printer" } catch (e: SecurityException) { "Printer" }
                                        Surface(
                                            color = Color.White,
                                            border = BorderStroke(1.dp, CardBorderColor),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(deviceName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                                                    Text(device.address, fontSize = 9.sp, color = TextMuted)
                                                }
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            printerManager.connectToDevice(device)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = BmkNavy),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("ချိတ်ဆက်မည်", fontSize = 10.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right side: Live Preview of Receipt
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Print Preview (စက္ကူ ပုံစံ အစမ်းမြင်ကွင်း):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (receiptBitmap != null) {
                                Image(
                                    bitmap = receiptBitmap.asImageBitmap(),
                                    contentDescription = "Receipt Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            } else {
                                Text("Preview ပြုလုပ်နေပါသည်...", modifier = Modifier.padding(16.dp), fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorderColor)

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // System Print / Save as PDF Button
                    OutlinedButton(
                        onClick = {
                            if (receiptBitmap != null) {
                                try {
                                    SystemPrintHelper.printBitmap(context, "BMK-Voucher-$voucherNumberInput", receiptBitmap)
                                    // Record to printed voucher database
                                    saveVoucherRecord(viewModel, currentTemplate, voucherNumberInput, cargo, truck)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Print Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF / System Print", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BmkNavy)
                    }

                    // Primary Bluetooth Print Button
                    Button(
                        onClick = {
                            if (receiptBitmap != null) {
                                isPrinting = true
                                coroutineScope.launch {
                                    val success = printerManager.printBitmap(receiptBitmap)
                                    isPrinting = false
                                    if (success) {
                                        Toast.makeText(context, "ဘောင်ချာ နံပါတ် $voucherNumberInput အား အောင်မြင်စွာ Print ထုတ်ပြီးပါပြီ", Toast.LENGTH_LONG).show()
                                        // Save to Printed Vouchers table for future search & reprint!
                                        saveVoucherRecord(viewModel, currentTemplate, voucherNumberInput, cargo, truck)
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Bluetooth Printer သို့ ပို့၍ မရသေးပါ။ ပရင်တာ ချိတ်ဆက်ထားခြင်း ရှိမရှိ စစ်ဆေးပါ", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                        enabled = !isPrinting,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("thermal_print_execute_btn")
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bluetooth ဖြင့် Print ထုတ်မည်", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun saveVoucherRecord(
    viewModel: BmkViewModel,
    template: PrintTemplateEntity?,
    voucherNumber: String,
    cargo: CargoEntity?,
    truck: TruckEntity?
) {
    if (cargo != null) {
        viewModel.savePrintedVoucher(
            voucherNumber = voucherNumber,
            templateId = template?.id ?: 1,
            templateName = template?.templateName ?: "ပုံစံ ၁",
            paperWidthMm = template?.paperWidthMm ?: 80,
            cargoId = cargo.id,
            truckId = cargo.truckId,
            customerName = cargo.receiverName.ifBlank { cargo.senderName },
            customerPhone = cargo.receiverPhone.ifBlank { cargo.senderPhone },
            customerAddress = cargo.receiverShopAddress.ifBlank { cargo.senderAddress },
            carNumber = cargo.truckNumber,
            driverName = cargo.driverName,
            itemDescription = cargo.cargoName.ifBlank { cargo.itemDescription },
            quantity = cargo.quantity,
            weightViss = cargo.weightViss,
            freightFee = cargo.freightFee,
            laborFee = cargo.mdyLoadingLaborFee + cargo.klyUnloadingLaborFee + cargo.shuttleLaborFee,
            totalAmount = cargo.totalAmount,
            paymentStatus = cargo.paymentStatus,
            remarks = cargo.remarks
        )
    } else if (truck != null) {
        viewModel.savePrintedVoucher(
            voucherNumber = voucherNumber,
            templateId = template?.id ?: 1,
            templateName = template?.templateName ?: "ပုံစံ ၁",
            paperWidthMm = template?.paperWidthMm ?: 80,
            cargoId = null,
            truckId = truck.id,
            customerName = "ယာဉ်တင် ကုန်စည် စာရင်းချုပ်",
            customerPhone = truck.driverPhone,
            customerAddress = "မန္တလေး <=> ကလေး ပြေးဆွဲမှု",
            carNumber = truck.truckNumber,
            driverName = truck.driverName,
            itemDescription = "ကုန်ပစ္စည်း အုပ်စု (${truck.totalCargosCount}) ခု",
            quantity = truck.totalCargosCount,
            weightViss = truck.totalVissLoaded,
            freightFee = truck.mandalayToKalayFreightPrice,
            laborFee = truck.totalVissLoaded * 25.0,
            totalAmount = truck.mandalayToKalayFreightPrice,
            paymentStatus = "PAID",
            remarks = truck.notes
        )
    }
}
