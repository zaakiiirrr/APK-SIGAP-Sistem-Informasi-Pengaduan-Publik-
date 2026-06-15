package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Complaint

@Composable
fun KecamatanBarChart(
    complaints: List<Complaint>,
    modifier: Modifier = Modifier
) {
    // Standard sub-districts list in Bantul
    val districts = listOf("Sewon", "Kasihan", "Banguntapan", "Piyungan", "Bantul Kota", "Imogiri")
    val data = districts.map { dist ->
        val count = complaints.count { it.location.contains(dist, ignoreCase = true) }
        dist to count
    }

    val maxVal = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Volume Pengaduan Per Kecamatan",
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingLeft = 30.dp.toPx()
            val paddingBottom = 24.dp.toPx()

            val chartWidth = width - paddingLeft
            val chartHeight = height - paddingBottom

            // Draw Y-Axis grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = chartHeight - (i * (chartHeight / gridLines))
                val labelVal = (i * (maxVal.toFloat() / gridLines)).toInt()
                
                // Draw grid line
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw Y-Axis label
                drawContext.canvas.nativeCanvas.drawText(
                    labelVal.toString(),
                    10.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                    }
                )
            }

            // Draw Bars
            val barCount = data.size
            val barSpacing = chartWidth / barCount
            val barWidth = barSpacing * 0.55f

            data.forEachIndexed { index, (name, valCount) ->
                val barHeight = (valCount.toFloat() / maxVal) * chartHeight
                val x = paddingLeft + (index * barSpacing) + (barSpacing - barWidth) / 2
                val y = chartHeight - barHeight

                // Draw solid bar column
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Value text at top of bar
                if (valCount > 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        valCount.toString(),
                        x + (barWidth / 2) - 4.dp.toPx(),
                        y - 6.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = textColor.hashCode()
                            textSize = 11.sp.toPx()
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }

                // X-Axis text
                val shortName = if (name.length > 8) name.substring(0, 6) + ".." else name
                drawContext.canvas.nativeCanvas.drawText(
                    shortName,
                    x + (barWidth / 2),
                    height - 6.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 9.sp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryTrendLineChart(
    complaints: List<Complaint>,
    modifier: Modifier = Modifier
) {
    // Generate trend of complaints over the last 5 days
    val categories = listOf("Road", "Trash", "Flood", "Air Quality", "Other")
    val translatedCats = mapOf(
        "Road" to "Jalan",
        "Trash" to "Sampah",
        "Flood" to "Banjir",
        "Air Quality" to "Udara",
        "Other" to "Lainnya"
    )

    // Calculate aggregated items per category
    val data = categories.map { cat ->
        val count = complaints.count { it.category == cat }
        translatedCats[cat]!! to count
    }

    val maxVal = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val lineColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tren Frekuensi Sektoral Keluhan",
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingLeft = 30.dp.toPx()
            val paddingBottom = 24.dp.toPx()

            val chartWidth = width - paddingLeft
            val chartHeight = height - paddingBottom

            // Draw Grid
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = chartHeight - (i * (chartHeight / gridLines))
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Label on Y axis
                val labelVal = (i * (maxVal.toFloat() / gridLines)).toInt()
                drawContext.canvas.nativeCanvas.drawText(
                    labelVal.toString(),
                    10.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                    }
                )
            }

            // Draw Line
            val pointsCount = data.size
            val spacing = chartWidth / (pointsCount - 1).coerceAtLeast(1)
            val path = Path()

            data.forEachIndexed { index, (_, count) ->
                val x = paddingLeft + (index * spacing)
                val barHeight = (count.toFloat() / maxVal) * chartHeight
                val y = chartHeight - barHeight

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw line stroke
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw node circles and label names
            data.forEachIndexed { index, (catName, count) ->
                val x = paddingLeft + (index * spacing)
                val barHeight = (count.toFloat() / maxVal) * chartHeight
                val y = chartHeight - barHeight

                // Small center point dot
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )

                // Value above the point
                drawContext.canvas.nativeCanvas.drawText(
                    count.toString(),
                    x,
                    y - 8.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )

                // Axis label
                drawContext.canvas.nativeCanvas.drawText(
                    catName,
                    x,
                    height - 6.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 9.sp.toPx()
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}
