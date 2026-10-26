package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PrintTemplateEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmkViewModel
import com.example.util.ReceiptBitmapGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintTemplateEditorDialog(
    viewModel: BmkViewModel,
    initialTemplateId: Int = 1,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val templates by viewModel.allTemplates.collectAsState()

    var activeTabId by remember { mutableStateOf(initialTemplateId) }
    val currentTemplate = templates.find { it.id == activeTabId } ?: templates.firstOrNull()

    // Form states
    var templateName by remember(currentTemplate) { mutableStateOf(currentTemplate?.templateName ?: "") }
    var paperWidthMm by remember(currentTemplate) { mutableStateOf(currentTemplate?.paperWidthMm ?: 80) }
    var headerSalutation by remember(currentTemplate) { mutableStateOf(currentTemplate?.headerSalutation ?: "ဗုဒ္ဓံ ဓမ္မံ သံဃံ") }
    var gateName by remember(currentTemplate) { mutableStateOf(currentTemplate?.gateName ?: "ဘုန်းမောင်ခန့်") }
    var routeTitle by remember(currentTemplate) { mutableStateOf(currentTemplate?.routeTitle ?: "မန္တလေး <=> ကလေး ကုန်စည်ပို့ဆောင်ရေး") }
    var mandalayAddress by remember(currentTemplate) { mutableStateOf(currentTemplate?.mandalayAddress ?: "စွယ်တော်ကားကွင်း၊ မန္တလေးမြို့") }
    var mandalayPhones by remember(currentTemplate) { mutableStateOf(currentTemplate?.mandalayPhones ?: "09-987769641, 09-449362369") }
    var kalayAddress by remember(currentTemplate) { mutableStateOf(currentTemplate?.kalayAddress ?: "နယ်မြေ(၄)၊ တောင်ဖီလာရပ်ကွက်၊ ကလေးမြို့") }
    var kalayPhones by remember(currentTemplate) { mutableStateOf(currentTemplate?.kalayPhones ?: "09-987769642, 09-449362352") }
    var hasTableGrid by remember(currentTemplate) { mutableStateOf(currentTemplate?.hasTableGrid ?: true) }
    var tableColumns by remember(currentTemplate) { mutableStateOf(currentTemplate?.tableColumns ?: "စဉ်, အမည်, အကြောင်းအရာ, ဦးရေ, နှုန်း, သင့်ငွေကျပ်, လ/သ, မှတ်ချက်") }
    var tableColumnWidths by remember(currentTemplate) { mutableStateOf(currentTemplate?.tableColumnWidths ?: "8, 18, 24, 10, 10, 14, 8, 8") }
    var tableRowCount by remember(currentTemplate) { mutableStateOf(currentTemplate?.tableRowCount ?: 8) }
    var footerNote by remember(currentTemplate) { mutableStateOf(currentTemplate?.footerNote ?: "အားပေးမှုကို အထူးကျေးဇူးတင်ရှိပါသည်") }
    var showSignatureLine by remember(currentTemplate) { mutableStateOf(currentTemplate?.showSignatureLine ?: true) }
    var signatureLabel by remember(currentTemplate) { mutableStateOf(currentTemplate?.signatureLabel ?: "လက်မှတ် ...................") }
    var voucherNumberPosition by remember(currentTemplate) { mutableStateOf(currentTemplate?.voucherNumberPosition ?: "TOP_RIGHT") }
    var showQrBarcode by remember(currentTemplate) { mutableStateOf(currentTemplate?.showQrBarcode ?: true) }

    // Live generated mock entity for dynamic preview
    val draftTemplate = remember(
        activeTabId, templateName, paperWidthMm, headerSalutation, gateName, routeTitle,
        mandalayAddress, mandalayPhones, kalayAddress, kalayPhones, hasTableGrid,
        tableColumns, tableColumnWidths, tableRowCount, footerNote, showSignatureLine,
        signatureLabel, voucherNumberPosition, showQrBarcode
    ) {
        PrintTemplateEntity(
            id = activeTabId,
            templateName = templateName,
            paperWidthMm = paperWidthMm,
            headerSalutation = headerSalutation,
            gateName = gateName,
            routeTitle = routeTitle,
            mandalayAddress = mandalayAddress,
            mandalayPhones = mandalayPhones,
            kalayAddress = kalayAddress,
            kalayPhones = kalayPhones,
            hasTableGrid = hasTableGrid,
            tableColumns = tableColumns,
            tableColumnWidths = tableColumnWidths,
            tableRowCount = tableRowCount,
            footerNote = footerNote,
            showSignatureLine = showSignatureLine,
            signatureLabel = signatureLabel,
            voucherNumberPosition = voucherNumberPosition,
            showQrBarcode = showQrBarcode
        )
    }

    val previewBitmap: Bitmap? = remember(draftTemplate) {
        ReceiptBitmapGenerator.generateReceiptBitmap(
            template = draftTemplate,
            voucherNumber = "0100",
            carNumber = "MDY 7D-1212 (12-Wheeler)",
            driverName = "ဦးကိုကို (09-45001122)",
            customerName = "ဒေါ်အေးအေး (စိန်ရွှေဆိုင်)",
            customerPhone = "09-450012345",
            customerAddress = "ကလေးမြို့ တောင်ဖီလာရပ်ကွက်",
            itemDescription = "စက်ပစ္စည်း အပိုပစ္စည်း (၃ ပုံး)",
            quantity = 3,
            weightViss = 50.0,
            freightFee = 60000.0,
            laborFee = 2500.0,
            totalAmount = 62500.0,
            paymentStatus = "PAID",
            remarks = "အစမ်း နမူနာ ဘောက်ချာ"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
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
                        Icon(Icons.Default.Tune, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Print Template ၇ မျိုး စိတ်ကြိုက် ပြုပြင်သိမ်းဆည်းရန်", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy)
                            Text("Header, Title, Phones, ဇယားကွက် Rows/Columns, Footer, Voucher No စိတ်ကြိုက် သတ်မှတ်နိုင်သည်", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 6.dp), color = CardBorderColor)

                // 7 Template Tabs
                ScrollableTabRow(
                    selectedTabIndex = (activeTabId - 1).coerceIn(0, 6),
                    edgePadding = 0.dp,
                    containerColor = SurfaceLight,
                    contentColor = BmkNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..7).forEach { tabId ->
                        val isSelected = activeTabId == tabId
                        val tmpl = templates.find { it.id == tabId }
                        Tab(
                            selected = isSelected,
                            onClick = { activeTabId = tabId },
                            text = {
                                Text(
                                    "ပုံစံ $tabId ${if (tmpl != null) "(${tmpl.paperWidthMm}mm)" else ""}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isSelected) BmkNavy else TextSecondary
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Split Layout: Left Editor, Right Live Preview
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left Editor Panel
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Template Title & Paper Size
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            label = { Text("ပုံစံ အမည် (Template Name)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8))
                        )

                        // Paper Size Selector
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight), border = BorderStroke(1.dp, CardBorderColor)) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("စက္ကူ အကျယ် Paper Size ရွေးချယ်ရန်:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(58, 80, 100).forEach { size ->
                                        val isSelected = paperWidthMm == size
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { paperWidthMm = size },
                                            label = { Text("${size} mm", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isSelected) Color.White else TextPrimary) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF1E3A8A),
                                                selectedLabelColor = Color.White,
                                                containerColor = SurfaceLight,
                                                labelColor = TextPrimary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Header & Title
                        OutlinedTextField(
                            value = headerSalutation,
                            onValueChange = { headerSalutation = it },
                            label = { Text("ထိပ်စီး စာသား (Salutation e.g. ဗုဒ္ဓံ ဓမ္မံ သံဃံ)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8))
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = gateName,
                                onValueChange = { gateName = it },
                                label = { Text("ဂိတ် အမည် (Gate Name)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8))
                            )
                            OutlinedTextField(
                                value = routeTitle,
                                onValueChange = { routeTitle = it },
                                label = { Text("ခရီးစဉ် ခေါင်းစဉ် (Route)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8))
                            )
                        }

                        // Phones & Addresses
                        OutlinedTextField(
                            value = mandalayPhones,
                            onValueChange = { mandalayPhones = it },
                            label = { Text("မန္တလေး ဖုန်းနံပါတ်များ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8))
                        )

                        OutlinedTextField(
                            value = kalayPhones,
                            onValueChange = { kalayPhones = it },
                            label = { Text("ကလေး ဖုန်းနံပါတ်များ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8))
                        )

                        // Table Grid Settings (User requirement: row, column, column widths)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = BorderStroke(1.dp, CardBorderColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Print Area တွင် ဇယားကွက် ပါဝင်မှု:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        Text("Row / Column စိတ်ကြိုက် သတ်မှတ်မည်", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Switch(
                                        checked = hasTableGrid,
                                        onCheckedChange = { hasTableGrid = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BmkNavy)
                                    )
                                }

                                if (hasTableGrid) {
                                    OutlinedTextField(
                                        value = tableColumns,
                                        onValueChange = { tableColumns = it },
                                        label = { Text("ကော်လံ အမည်များ (ကော်မာ ခံ၍ ရေးပါ)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8))
                                    )

                                    OutlinedTextField(
                                        value = tableColumnWidths,
                                        onValueChange = { tableColumnWidths = it },
                                        label = { Text("ကော်လံ အကျယ် အချိုးအစား (Column Widths)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ဇယားကွက် အတန်း အရေအတွက် (Rows): $tableRowCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Slider(
                                            value = tableRowCount.toFloat(),
                                            onValueChange = { tableRowCount = it.toInt() },
                                            valueRange = 3f..12f,
                                            steps = 8,
                                            modifier = Modifier.width(160.dp),
                                            colors = SliderDefaults.colors(thumbColor = BmkOrange, activeTrackColor = BmkOrange)
                                        )
                                    }
                                }
                            }
                        }

                        // Footer Note & Signature Line
                        OutlinedTextField(
                            value = footerNote,
                            onValueChange = { footerNote = it },
                            label = { Text("Footer စာသား (ဥပမာ - အားပေးမှုကို အထူးကျေးဇူးတင်ရှိပါသည်)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BmkNavy, unfocusedBorderColor = Color(0xFF94A3B8))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("လက်မှတ် ထိုးရန် နေရာ (Signature line):", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Switch(
                                checked = showSignatureLine,
                                onCheckedChange = { showSignatureLine = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BmkNavy)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ဘောင်ချာ အမှတ် (Voucher No) ရိုက်ထုတ်ရန်:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Switch(
                                checked = showQrBarcode,
                                onCheckedChange = { showQrBarcode = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BmkNavy)
                            )
                        }
                    }

                    // Right Live Preview Panel
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Live Preview (ပြောင်းလဲမှု အစမ်းမြင်ကွင်း):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = "Template Live Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorderColor)

                // Save Template Button
                Button(
                    onClick = {
                        viewModel.updatePrintTemplate(draftTemplate) {
                            Toast.makeText(context, "${draftTemplate.templateName} အား အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_print_template_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ယခု ပုံစံ $activeTabId အား အပြီးအပိုင် သိမ်းဆည်းမည် (Save Template)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}
