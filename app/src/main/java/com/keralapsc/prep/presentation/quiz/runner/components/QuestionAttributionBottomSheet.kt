package com.keralapsc.prep.presentation.quiz.runner.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keralapsc.prep.domain.model.quiz.QuizQuestion
import com.keralapsc.prep.presentation.theme.ForestGreenPrimary

/**
 * Modal bottom sheet presenting official Kerala PSC sourcing and attribution metadata.
 * Displays only metadata actually present; never fabricates placeholder fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionAttributionBottomSheet(
    question: QuizQuestion,
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Official Sourcing & Attribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Curated from official Kerala Public Service Commission publications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Organization
            if (!question.sourceOrganization.isNullOrBlank()) {
                AttributionRow(label = "Authority", value = question.sourceOrganization)
            }

            // Exam Title
            if (!question.examTitle.isNullOrBlank()) {
                AttributionRow(label = "Examination", value = question.examTitle)
            }

            // Question Paper Code
            if (!question.sourcePaperCode.isNullOrBlank()) {
                AttributionRow(label = "Question Paper Code", value = question.sourcePaperCode)
            }

            // Alpha Code
            if (!question.sourceAlphaCode.isNullOrBlank()) {
                AttributionRow(label = "Question Booklet Alpha Code", value = question.sourceAlphaCode)
            }

            // Medium
            if (!question.sourceMedium.isNullOrBlank()) {
                AttributionRow(label = "Medium of Examination", value = question.sourceMedium)
            }

            // Answer Key Status
            AttributionRow(label = "Answer Key Status", value = question.answerKeyStatus.name)
        }
    }
}

@Composable
private fun AttributionRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
