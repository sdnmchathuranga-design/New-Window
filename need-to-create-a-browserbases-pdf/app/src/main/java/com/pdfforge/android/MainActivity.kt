package com.pdfforge.android

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : Activity() {
    private val mergeRequest = 1001
    private val splitRequest = 1002
    private val createMergedRequest = 1003
    private val createRangeRequest = 1004
    private val createZipRequest = 1005

    private val mergeUris = mutableListOf<Uri>()
    private var splitUri: Uri? = null
    private var pendingOutput: ByteArray? = null

    private lateinit var mergeList: TextView
    private lateinit var splitFile: TextView
    private lateinit var rangeInput: EditText
    private lateinit var splitMode: RadioGroup
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return

        when (requestCode) {
            mergeRequest -> {
                val clip = data.clipData
                mergeUris.clear()
                if (clip != null) {
                    for (index in 0 until clip.itemCount) {
                        mergeUris.add(clip.getItemAt(index).uri)
                    }
                } else {
                    data.data?.let(mergeUris::add)
                }
                renderMergeList()
            }
            splitRequest -> {
                splitUri = data.data
                splitFile.text = splitUri?.let { "Selected: ${displayName(it)}" } ?: "No PDF selected"
                setStatus("")
            }
            createMergedRequest, createRangeRequest, createZipRequest -> {
                val bytes = pendingOutput ?: return
                data.data?.let { writeBytes(it, bytes) }
                pendingOutput = null
            }
        }
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
            setBackgroundColor(0xFFF5F7F5.toInt())
        }

        root.addView(text("Mahesh PDF Editor", 34f, true))
        root.addView(text("Merge PDFs, split page ranges, or export every page separately.", 16f, false))
        root.addView(section("Merge PDFs"))
        root.addView(button("Choose PDFs") { chooseMergePdfs() })
        mergeList = text("No PDFs selected", 14f, false)
        root.addView(mergeList)
        root.addView(button("Merge and Save") { mergeSelectedPdfs() })

        root.addView(section("Split Pages"))
        root.addView(button("Choose One PDF") { chooseSplitPdf() })
        splitFile = text("No PDF selected", 14f, false)
        root.addView(splitFile)

        rangeInput = EditText(this).apply {
            hint = "Pages: 1-3, 5, 8-10"
            setSingleLine(true)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        root.addView(rangeInput, blockParams())

        splitMode = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            addView(RadioButton(context).apply {
                id = 1
                text = "Save selected pages as one PDF"
                isChecked = true
            })
            addView(RadioButton(context).apply {
                id = 2
                text = "Save every page separately as ZIP"
            })
        }
        root.addView(splitMode, blockParams())
        root.addView(button("Split and Save") { splitSelectedPdf() })

        status = text("", 14f, true)
        root.addView(status)
        scroll.addView(root)
        return scroll
    }

    private fun chooseMergePdfs() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, mergeRequest)
    }

    private fun chooseSplitPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, splitRequest)
    }

    private fun mergeSelectedPdfs() {
        if (mergeUris.size < 2) {
            setStatus("Choose at least two PDFs to merge.")
            return
        }

        runCatching {
            val output = ByteArrayOutputStream()
            val merger = PDFMergerUtility()
            PDDocument().use { merged ->
                mergeUris.forEach { uri ->
                    contentResolver.openInputStream(uri)?.use { input ->
                        PDDocument.load(input).use { source ->
                            merger.appendDocument(merged, source)
                        }
                    }
                }
                merged.save(output)
            }
            output.toByteArray()
        }.onSuccess {
            pendingOutput = it
            createDocument("merged.pdf", "application/pdf", createMergedRequest)
        }.onFailure {
            setStatus("Could not merge these PDFs. Try unlocked PDF files.")
        }
    }

    private fun splitSelectedPdf() {
        val sourceUri = splitUri
        if (sourceUri == null) {
            setStatus("Choose a PDF to split.")
            return
        }

        runCatching {
            contentResolver.openInputStream(sourceUri)?.use { input ->
                PDDocument.load(input).use { document ->
                    if (splitMode.checkedRadioButtonId == 2) {
                        buildZip(document)
                    } else {
                        val pages = parsePageRanges(rangeInput.text.toString(), document.numberOfPages)
                        buildPdf(document, pages)
                    }
                }
            } ?: error("Could not open PDF.")
        }.onSuccess {
            pendingOutput = it
            if (splitMode.checkedRadioButtonId == 2) {
                createDocument("split-pages.zip", "application/zip", createZipRequest)
            } else {
                createDocument("selected-pages.pdf", "application/pdf", createRangeRequest)
            }
        }.onFailure {
            setStatus(it.message ?: "Could not split this PDF.")
        }
    }

    private fun buildPdf(source: PDDocument, pageIndexes: List<Int>): ByteArray {
        val outputBytes = ByteArrayOutputStream()
        PDDocument().use { output ->
            pageIndexes.forEach { output.importPage(source.getPage(it)) }
            output.save(outputBytes)
        }
        return outputBytes.toByteArray()
    }

    private fun buildZip(source: PDDocument): ByteArray {
        val zipBytes = ByteArrayOutputStream()
        ZipOutputStream(zipBytes).use { zip ->
            for (index in 0 until source.numberOfPages) {
                zip.putNextEntry(ZipEntry("page-${index + 1}.pdf"))
                zip.write(buildPdf(source, listOf(index)))
                zip.closeEntry()
            }
        }
        return zipBytes.toByteArray()
    }

    private fun parsePageRanges(value: String, pageCount: Int): List<Int> {
        val pages = mutableListOf<Int>()
        val seen = mutableSetOf<Int>()
        val chunks = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        require(chunks.isNotEmpty()) { "Enter pages to extract, like 1-3, 5." }

        chunks.forEach { chunk ->
            val parts = chunk.split("-").map { it.trim() }
            require(parts.size in 1..2 && parts.all { it.toIntOrNull() != null }) {
                "$chunk is not a valid page range."
            }

            val start = parts[0].toInt()
            val end = if (parts.size == 2) parts[1].toInt() else start
            require(start in 1..pageCount && end in 1..pageCount && start <= end) {
                "$chunk is outside this PDF's $pageCount pages."
            }

            for (page in start..end) {
                val index = page - 1
                if (seen.add(index)) pages.add(index)
            }
        }

        return pages
    }

    private fun createDocument(fileName: String, mimeType: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        startActivityForResult(intent, requestCode)
    }

    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        runCatching {
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        }.onSuccess {
            setStatus("Saved successfully.")
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }.onFailure {
            setStatus("Could not save the output file.")
        }
    }

    private fun renderMergeList() {
        mergeList.text = if (mergeUris.isEmpty()) {
            "No PDFs selected"
        } else {
            mergeUris.mapIndexed { index, uri -> "${index + 1}. ${displayName(uri)}" }.joinToString("\n")
        }
        setStatus("")
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return uri.lastPathSegment ?: "PDF file"
    }

    private fun text(value: String, size: Float, bold: Boolean): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(0xFF1E2528.toInt())
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, dp(6))
        }
    }

    private fun section(value: String): TextView {
        return text(value, 22f, true).apply {
            setPadding(0, dp(28), 0, dp(8))
        }
    }

    private fun button(value: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = value
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF0D7C66.toInt())
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
    }

    private fun blockParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dp(8), 0, dp(8))
        }
    }

    private fun setStatus(message: String) {
        status.text = message
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
