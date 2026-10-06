package com.keralapsc.prep.presentation.quiz.runner.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import com.keralapsc.prep.R
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keralapsc.prep.domain.model.question.QuestionOption
import com.keralapsc.prep.domain.model.quiz.AnswerEvaluation
import com.keralapsc.prep.domain.model.quiz.EvaluationStatus
import com.keralapsc.prep.presentation.theme.ForestGreenDark
import com.keralapsc.prep.presentation.theme.ForestGreenPrimary
import com.keralapsc.prep.presentation.theme.GoldAccent

/**
 * Dynamic option card supporting arbitrary option counts (A, B, C, D, ...).
 * Renders distinct visual styles for Unselected, Selected, Correct, Incorrect, and Cancelled states.
 */
@Composable
fun OptionCard(
    option: QuestionOption,
    isSelected: Boolean,
    evaluation: AnswerEvaluation?,
    isAnswerRevealed: Boolean,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isGenuineCorrect = evaluation != null && option.id == evaluation.correctOptionId
    val isCancelled = evaluation?.status == EvaluationStatus.CANCELLED_AWARDED
    val isUserSelection = isSelected

    // Determine colors based on evaluation state
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isAnswerRevealed -> if (isSelected) ForestGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
            isCancelled && isUserSelection -> GoldAccent.copy(alpha = 0.15f)
            isGenuineCorrect -> Color(0xFFE8F5E9) // SuccessGreenContainer
            isUserSelection && evaluation?.isCredited == false -> Color(0xFFFFEBEE) // ErrorRedContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "optionBgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !isAnswerRevealed -> if (isSelected) ForestGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            isCancelled && isUserSelection -> GoldAccent
            isGenuineCorrect -> Color(0xFF2E7D32) // SuccessGreen
            isUserSelection && evaluation?.isCredited == false -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        },
        label = "optionBorderColor"
    )

    val contentAlpha = if (isAnswerRevealed && !isGenuineCorrect && !isUserSelection && !isCancelled) 0.55f else 1f

    val accessibilityDesc = when {
        isAnswerRevealed && isGenuineCorrect -> stringResource(
            R.string.cd_option_correct,
            option.optionIdentifier,
            option.text
        )
        isAnswerRevealed && isUserSelection && evaluation?.isCredited == false -> stringResource(
            R.string.cd_option_incorrect,
            option.optionIdentifier,
            option.text
        )
        isAnswerRevealed && isCancelled -> stringResource(
            R.string.cd_option_cancelled,
            option.optionIdentifier,
            option.text
        )
        isSelected -> stringResource(
            R.string.cd_option_selected,
            option.optionIdentifier,
            option.text
        )
        else -> stringResource(
            R.string.cd_option_unselected,
            option.optionIdentifier,
            option.text
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .alpha(contentAlpha)
            .clickable(enabled = !isAnswerRevealed) { onOptionSelected(option.id) }
            .semantics { contentDescription = accessibilityDesc },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected || (isAnswerRevealed && isGenuineCorrect)) 2.dp else 1.dp, borderColor),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Letter Indicator Badge
            Surface(
                shape = CircleShape,
                color = when {
                    !isAnswerRevealed -> if (isSelected) ForestGreenPrimary else MaterialTheme.colorScheme.surfaceVariant
                    isCancelled && isUserSelection -> GoldAccent
                    isGenuineCorrect -> Color(0xFF2E7D32)
                    isUserSelection && evaluation?.isCredited == false -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        isAnswerRevealed && isGenuineCorrect -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        isAnswerRevealed && isUserSelection && evaluation?.isCredited == false -> {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        isAnswerRevealed && isCancelled && isUserSelection -> {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ForestGreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = option.optionIdentifier,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected && !isAnswerRevealed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Option Text
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
