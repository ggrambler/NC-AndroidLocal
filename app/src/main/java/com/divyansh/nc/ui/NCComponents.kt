package com.divyansh.nc.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divyansh.nc.data.BudgetMath
import kotlin.math.roundToInt

@Composable
fun NCCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NCSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, color = NCMuted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun MetricPill(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = NCSurfaceHigher), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = NCMuted, fontSize = 12.sp)
            Text(value, color = tint, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ConcentricMacroRings(calories: Int, budget: Int, protein: Double, proteinGoal: Double) {
    val calorieFraction = if (budget > 0) calories / budget.toFloat() else 0f
    val proteinFraction = if (proteinGoal > 0) (protein / proteinGoal).toFloat() else 0f
    val animatedCalories by animateFloatAsState(calorieFraction.coerceIn(0f, 1f), tween(900), label = "calorie ring")
    val animatedProtein by animateFloatAsState(proteinFraction.coerceIn(0f, 1f), tween(1100), label = "protein ring")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val outerRadius = size.minDimension / 2 - 12.dp.toPx()
                val innerRadius = outerRadius - 23.dp.toPx()
                fun ring(radius: Float, fraction: Float, strokeColor: Color, thickness: Float) {
                    val bounds = Size(radius * 2, radius * 2)
                    val origin = Offset(center.x - radius, center.y - radius)
                    drawArc(NCSurfaceHigher, -90f, 360f, false, origin, bounds, style = Stroke(thickness, cap = StrokeCap.Round))
                    drawArc(strokeColor, -90f, 360f * fraction, false, origin, bounds, style = Stroke(thickness, cap = StrokeCap.Round))
                }
                ring(outerRadius, animatedCalories, if (calories > budget) NCRed else NCPurple, 11.dp.toPx())
                ring(innerRadius, animatedProtein, NCGreen, 9.dp.toPx())
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${BudgetMath.completionPercent(calories, budget)}%", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Text("CALORIES", fontSize = 10.sp, color = NCMuted)
                Spacer(Modifier.height(5.dp))
                Text("${BudgetMath.completionPercent(protein, proteinGoal)}% protein", fontSize = 11.sp, color = NCGreen)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricPill("Calories", "$calories / $budget", NCPurple, Modifier.weight(1f))
            MetricPill("Protein", "${protein.roundToInt()} / ${proteinGoal.roundToInt()}g", NCGreen, Modifier.weight(1f))
        }
    }
}

@Composable
fun TextInput(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NCPurple,
            focusedLabelColor = NCPurple
        )
    )
}

@Composable
fun ConfirmDeleteDialog(label: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete $label?") },
        text = { Text("This cannot be undone after deletion.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = NCRed) } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}
