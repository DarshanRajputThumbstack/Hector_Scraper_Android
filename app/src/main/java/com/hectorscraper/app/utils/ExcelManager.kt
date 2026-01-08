package com.hectorscraper.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ExcelManager {

    private const val FILE_NAME = "instamart_data.xlsx"
    private const val SHEET_NAME = "data"

    fun getFile(context: Context): File {
        return File(context.getExternalFilesDir(null), FILE_NAME)
    }

    fun createExcelIfNotExists(context: Context) {
        val file = getFile(context)
        if (file.exists()) return

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(SHEET_NAME)

        val headers = listOf(
            "date", "scrape_timestamp", "store_id", "merchant_id", "product_id",
            "sku_name", "brand", "sku_url", "grammage", "l0_category",
            "l1_category", "l2_category","dark_store_name", "dark_store_address", "city", "locality", "pincode",
            "plus_code", "availability_flag", "inventory", "mrp", "sp",
            "discount_pct", "eta_identifier", "merchant_type", "ranking","time"
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, h ->
            headerRow.createCell(index).setCellValue(h)
        }

        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
    }


    fun addRow(context: Context, values: List<Any>) {
        val file = getFile(context)
        val workbook = XSSFWorkbook(FileInputStream(file))
        val sheet = workbook.getSheet(SHEET_NAME)

        val nextRow = sheet.lastRowNum + 1
        val row = sheet.createRow(nextRow)

        values.forEachIndexed { index, value ->
            row.createCell(index).setCellValue(value.toString())
        }

        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
    }


    fun exportExcel(context: Context) {
        val file = getFile(context)
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Export Excel"))
    }
}