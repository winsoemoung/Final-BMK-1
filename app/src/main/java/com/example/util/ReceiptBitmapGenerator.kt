package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.data.model.CargoEntity
import com.example.data.model.PrintTemplateEntity
import com.example.data.model.PrintedVoucherEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptBitmapGenerator {

    /**
     * Generates a high-contrast monochrome Bitmap tailored for thermal printing
     * based on a PrintTemplateEntity and receipt information.
     */
    fun generateReceiptBitmap(
        template: PrintTemplateEntity,
        voucherNumber: String,
        issueDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        carNumber: String = "",
        driverName: String = "",
        customerName: String = "",
        customerPhone: String = "",
        customerAddress: String = "",
        itemDescription: String = "",
        quantity: Int = 1,
        weightViss: Double = 0.0,
        freightFee: Double = 0.0,
        laborFee: Double = 0.0,
        totalAmount: Double = 0.0,
        paymentStatus: String = "PAID",
        customRows: List<List<String>> = emptyList(),
        remarks: String = ""
    ): Bitmap {
        val currencyFormat = NumberFormat.getNumberInstance(Locale.US)

        // Determine canvas pixel width based on paper mm (203 DPI standard)
        val canvasWidth = when (template.paperWidthMm) {
            58 -> 384
            80 -> 576
            100 -> 800
            else -> (template.paperWidthMm * 7.2f).toInt().coerceIn(384, 832)
        }

        // We estimate height dynamically then draw
        val scale = canvasWidth / 576f
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.DEFAULT
        }
        val paintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.DEFAULT_BOLD
        }
        val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 2f * scale
            style = Paint.Style.STROKE
        }
        val paintThinLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            strokeWidth = 1f * scale
            style = Paint.Style.STROKE
        }

        // Calculate approximate canvas height
        val estimatedHeight = (880 * scale).toInt().coerceAtLeast(600)
        val bitmap = Bitmap.createBitmap(canvasWidth, estimatedHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val padding = 16f * scale
        var currentY = 24f * scale

        // 1. Top Salutation (e.g. ဗုဒ္ဓံ ဓမ္မံ သံဃံ)
        if (template.headerSalutation.isNotBlank()) {
            paintText.textSize = 14f * scale
            paintText.textAlign = Paint.Align.CENTER
            canvas.drawText(template.headerSalutation, canvasWidth / 2f, currentY, paintText)
            currentY += 22f * scale
        }

        // 2. Gate Name (Large Bold Title e.g. ဘုန်းမောင်ခန့်)
        paintBold.textSize = 24f * scale
        paintBold.textAlign = Paint.Align.CENTER
        canvas.drawText(template.gateName, canvasWidth / 2f, currentY + 4f * scale, paintBold)
        currentY += 30f * scale

        // 3. Route Subtitle (e.g. မန္တလေး <=> ကလေး ကုန်စည်ပို့ဆောင်ရေး)
        if (template.routeTitle.isNotBlank()) {
            paintBold.textSize = 15f * scale
            paintBold.textAlign = Paint.Align.CENTER
            canvas.drawText(template.routeTitle, canvasWidth / 2f, currentY, paintBold)
            currentY += 24f * scale
        }

        // 4. Branch Info (Mandalay & Kalay Gate Phones & Addresses)
        paintText.textSize = 11f * scale
        paintText.textAlign = Paint.Align.CENTER
        if (template.mandalayAddress.isNotBlank() || template.mandalayPhones.isNotBlank()) {
            val mdyText = "${template.mandalayAddress} | Ph: ${template.mandalayPhones}"
            canvas.drawText(mdyText, canvasWidth / 2f, currentY, paintText)
            currentY += 16f * scale
        }
        if (template.kalayAddress.isNotBlank() || template.kalayPhones.isNotBlank()) {
            val klyText = "${template.kalayAddress} | Ph: ${template.kalayPhones}"
            canvas.drawText(klyText, canvasWidth / 2f, currentY, paintText)
            currentY += 18f * scale
        }

        // Top Divider Line
        currentY += 4f * scale
        canvas.drawLine(padding, currentY, canvasWidth - padding, currentY, paintLine)
        currentY += 16f * scale

        // 5. Voucher Number & Meta Section
        paintText.textAlign = Paint.Align.LEFT
        paintBold.textAlign = Paint.Align.LEFT

        // Voucher Number display
        val voucherLabel = "ဘောင်ချာ အမှတ် (Voucher No): "
        paintBold.textSize = 14f * scale
        canvas.drawText(voucherLabel, padding, currentY, paintBold)
        val vLabelWidth = paintBold.measureText(voucherLabel)

        // Large prominent Voucher Number
        paintBold.textSize = 18f * scale
        paintBold.color = Color.BLACK
        canvas.drawText(voucherNumber, padding + vLabelWidth, currentY, paintBold)

        // Date right-aligned
        paintText.textSize = 12f * scale
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText("နေ့စွဲ: $issueDate", canvasWidth - padding, currentY, paintText)
        currentY += 22f * scale

        // Metadata: Car number, Driver, Customer
        paintText.textAlign = Paint.Align.LEFT
        if (carNumber.isNotBlank() || driverName.isNotBlank()) {
            val carText = "ယာဉ်အမှတ်: ${if (carNumber.isNotBlank()) carNumber else "-"}  |  ယာဉ်မောင်း: ${if (driverName.isNotBlank()) driverName else "-"}"
            paintText.textSize = 12f * scale
            canvas.drawText(carText, padding, currentY, paintText)
            currentY += 18f * scale
        }

        if (customerName.isNotBlank()) {
            val custText = "အမည်: $customerName  ${if (customerPhone.isNotBlank()) "($customerPhone)" else ""}"
            paintText.textSize = 12f * scale
            canvas.drawText(custText, padding, currentY, paintText)
            currentY += 18f * scale
        }

        if (customerAddress.isNotBlank()) {
            paintText.textSize = 11f * scale
            canvas.drawText("လိပ်စာ: $customerAddress", padding, currentY, paintText)
            currentY += 18f * scale
        }

        currentY += 6f * scale

        // 6. Print Area: Table Grid (if enabled)
        if (template.hasTableGrid) {
            val rawColumns = template.tableColumns.split(",").map { it.trim() }
            val rawWidths = template.tableColumnWidths.split(",").mapNotNull { it.trim().toFloatOrNull() }
            val columnCount = rawColumns.size

            // Compute actual column pixel widths
            val totalWeight = if (rawWidths.size == columnCount) rawWidths.sum() else columnCount * 10f
            val tableWidth = canvasWidth - (padding * 2)
            val colWidths = (0 until columnCount).map { i ->
                val w = if (i < rawWidths.size) rawWidths[i] else 10f
                (w / totalWeight) * tableWidth
            }

            val tableTop = currentY
            val headerRowHeight = 26f * scale
            val dataRowHeight = 24f * scale
            val numRows = template.tableRowCount.coerceIn(3, 12)

            // Draw table outer rectangle
            val tableHeight = headerRowHeight + (numRows * dataRowHeight)
            canvas.drawRect(padding, tableTop, padding + tableWidth, tableTop + tableHeight, paintLine)

            // Draw header background line
            canvas.drawLine(padding, tableTop + headerRowHeight, padding + tableWidth, tableTop + headerRowHeight, paintLine)

            // Draw header column names and vertical divider lines
            var colX = padding
            paintBold.textSize = 11f * scale
            paintBold.textAlign = Paint.Align.CENTER

            for (i in 0 until columnCount) {
                val colW = colWidths[i]
                val headerText = rawColumns[i]
                val centerX = colX + (colW / 2f)
                canvas.drawText(headerText, centerX, tableTop + (headerRowHeight * 0.65f), paintBold)

                if (i > 0) {
                    canvas.drawLine(colX, tableTop, colX, tableTop + tableHeight, paintLine)
                }
                colX += colW
            }

            // Fill rows
            paintText.textSize = 10f * scale
            paintText.textAlign = Paint.Align.CENTER

            for (r in 0 until numRows) {
                val rowTop = tableTop + headerRowHeight + (r * dataRowHeight)
                val rowBottom = rowTop + dataRowHeight
                // Horizontal row divider line
                canvas.drawLine(padding, rowBottom, padding + tableWidth, rowBottom, paintThinLine)

                val rowData = if (r < customRows.size) {
                    customRows[r]
                } else if (r == 0 && itemDescription.isNotBlank()) {
                    // Default row with active cargo info
                    buildDefaultRowCells(rawColumns, voucherNumber, customerName, itemDescription, quantity, weightViss, freightFee, laborFee, totalAmount, paymentStatus)
                } else {
                    emptyList()
                }

                var cellX = padding
                for (c in 0 until columnCount) {
                    val colW = colWidths[c]
                    if (c < rowData.size) {
                        val cellText = rowData[c]
                        val centerX = cellX + (colW / 2f)
                        canvas.drawText(cellText, centerX, rowTop + (dataRowHeight * 0.68f), paintText)
                    }
                    cellX += colW
                }
            }

            currentY = tableTop + tableHeight + 14f * scale
        } else {
            // Non-table list view
            canvas.drawLine(padding, currentY, canvasWidth - padding, currentY, paintLine)
            currentY += 16f * scale
            paintText.textSize = 12f * scale
            paintText.textAlign = Paint.Align.LEFT

            if (itemDescription.isNotBlank()) {
                canvas.drawText("ပစ္စည်း အမျိုးအမည်: $itemDescription", padding, currentY, paintText)
                currentY += 18f * scale
            }
            if (quantity > 0) {
                canvas.drawText("အရေအတွက်: $quantity  |  အလေးချိန်: $weightViss ပိဿာ", padding, currentY, paintText)
                currentY += 18f * scale
            }
            canvas.drawLine(padding, currentY, canvasWidth - padding, currentY, paintThinLine)
            currentY += 14f * scale
        }

        // 7. Totals Summary Block
        val totalBoxTop = currentY
        val totalBoxWidth = (canvasWidth - padding * 2) * 0.55f
        val totalBoxLeft = canvasWidth - padding - totalBoxWidth

        paintBold.textSize = 12f * scale
        paintText.textSize = 12f * scale
        paintText.textAlign = Paint.Align.LEFT
        paintBold.textAlign = Paint.Align.RIGHT

        // Freight Fee row
        canvas.drawText("တန်ဆာခ နှုန်း:", totalBoxLeft, currentY + 12f * scale, paintText)
        canvas.drawText("${currencyFormat.format(freightFee)} ကျပ်", canvasWidth - padding, currentY + 12f * scale, paintBold)
        currentY += 18f * scale

        // Labor Fee row
        if (laborFee > 0) {
            canvas.drawText("အလုပ်သမားခ:", totalBoxLeft, currentY + 12f * scale, paintText)
            canvas.drawText("${currencyFormat.format(laborFee)} ကျပ်", canvasWidth - padding, currentY + 12f * scale, paintBold)
            currentY += 18f * scale
        }

        // Grand Total row
        canvas.drawLine(totalBoxLeft, currentY + 4f * scale, canvasWidth - padding, currentY + 4f * scale, paintLine)
        currentY += 16f * scale

        paintBold.textSize = 14f * scale
        canvas.drawText("စုစုပေါင်းကျသင့်ငွေ:", totalBoxLeft, currentY + 6f * scale, paintBold)
        canvas.drawText("${currencyFormat.format(totalAmount)} ကျပ်", canvasWidth - padding, currentY + 6f * scale, paintBold)

        // Payment status badge
        val statusText = if (paymentStatus == "PAID") "[ ရှင်းပြီး - PAID ]" else "[ လ/ခ ပို့ခပေးရန် - COLLECT ]"
        paintBold.textSize = 11f * scale
        paintBold.textAlign = Paint.Align.LEFT
        canvas.drawText(statusText, padding, currentY + 6f * scale, paintBold)

        currentY += 30f * scale

        // 8. Footer Section
        canvas.drawLine(padding, currentY, canvasWidth - padding, currentY, paintThinLine)
        currentY += 18f * scale

        // Footer Note (e.g. အားပေးမှုကို အထူးကျေးဇူးတင်ရှိပါသည်)
        if (template.footerNote.isNotBlank()) {
            paintText.textSize = 12f * scale
            paintText.textAlign = Paint.Align.CENTER
            canvas.drawText(template.footerNote, canvasWidth / 2f, currentY, paintText)
            currentY += 22f * scale
        }

        // Signature Line
        if (template.showSignatureLine) {
            paintText.textSize = 12f * scale
            paintText.textAlign = Paint.Align.RIGHT
            val sigText = template.signatureLabel.ifBlank { "လက်မှတ် ..................." }
            canvas.drawText(sigText, canvasWidth - padding - 20f * scale, currentY + 8f * scale, paintText)
            currentY += 30f * scale
        }

        // Bottom Voucher Barcode / Unique ID tag
        if (template.showQrBarcode) {
            paintText.textSize = 9f * scale
            paintText.textAlign = Paint.Align.CENTER
            canvas.drawText("* $voucherNumber * (BMK EXPRESS CAR GATE SYSTEM)", canvasWidth / 2f, currentY, paintText)
            currentY += 14f * scale
        }

        return bitmap
    }

    private fun buildDefaultRowCells(
        columns: List<String>,
        voucherNo: String,
        customer: String,
        item: String,
        qty: Int,
        viss: Double,
        freight: Double,
        labor: Double,
        total: Double,
        paymentStatus: String
    ): List<String> {
        val currencyFormat = NumberFormat.getNumberInstance(Locale.US)
        return columns.map { col ->
            when {
                col.contains("စဉ်") -> "၁"
                col.contains("အမည်") && !col.contains("အမျိုး") -> customer.ifBlank { "ဦးမောင်မောင်" }
                col.contains("အမျိုးအမည်") || col.contains("အကြောင်းအရာ") -> item.ifBlank { "လူသုံးကုန် ပစ္စည်း" }
                col.contains("ဦးရေ") -> "$qty"
                col.contains("ပိဿာ") -> "${viss.toInt()}"
                col.contains("နှုန်း") -> "${currencyFormat.format(if (viss > 0) freight / viss else 1200.0)}"
                col.contains("သင့်ငွေ") || col.contains("ကျပ်") -> currencyFormat.format(freight)
                col.contains("လ/သ") || col.contains("လ/ခ") -> if (paymentStatus == "PAID") "ရှင်း" else "လ/ခ"
                col.contains("မှတ်ချက်") -> "-"
                else -> ""
            }
        }
    }

    fun fromCargo(
        template: PrintTemplateEntity,
        cargo: CargoEntity,
        voucherNumber: String
    ): Bitmap {
        return generateReceiptBitmap(
            template = template,
            voucherNumber = voucherNumber,
            issueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(cargo.createdTimestamp)),
            carNumber = cargo.truckNumber,
            driverName = cargo.driverName,
            customerName = cargo.receiverName.ifBlank { cargo.senderName },
            customerPhone = cargo.receiverPhone.ifBlank { cargo.senderPhone },
            customerAddress = cargo.receiverShopAddress.ifBlank { cargo.senderAddress },
            itemDescription = cargo.cargoName.ifBlank { cargo.itemDescription },
            quantity = cargo.quantity,
            weightViss = cargo.weightViss,
            freightFee = cargo.freightFee,
            laborFee = cargo.mdyLoadingLaborFee + cargo.klyUnloadingLaborFee + cargo.shuttleLaborFee,
            totalAmount = cargo.totalAmount,
            paymentStatus = cargo.paymentStatus,
            remarks = cargo.remarks
        )
    }

    fun fromPrintedVoucher(
        template: PrintTemplateEntity,
        voucher: PrintedVoucherEntity
    ): Bitmap {
        return generateReceiptBitmap(
            template = template,
            voucherNumber = voucher.voucherNumber,
            issueDate = voucher.issueDate,
            carNumber = voucher.carNumber,
            driverName = voucher.driverName,
            customerName = voucher.customerName,
            customerPhone = voucher.customerPhone,
            customerAddress = voucher.customerAddress,
            itemDescription = voucher.itemDescription,
            quantity = voucher.quantity,
            weightViss = voucher.weightViss,
            freightFee = voucher.freightFee,
            laborFee = voucher.laborFee,
            totalAmount = voucher.totalAmount,
            paymentStatus = voucher.paymentStatus,
            remarks = voucher.remarks
        )
    }
}
