package com.keralapsc.prep.presentation.mockexam.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.keralapsc.prep.domain.model.mockexam.MockQuestionStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.keralapsc.prep.R
import com.keralapsc.prep.presentation.theme.PSCTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockQuestionPaletteBottomSheet(
    totalQuestions: Int,
    currentIndex: Int,
    questionStatuses: List<MockQuestionStatus>,
    answeredCount: Int,
    unansweredCount: Int,
    markedCount: Int,
    onQuestionSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("mock_palette_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PSCTokens.spacing.medium)
                .padding(bottom = PSCTokens.spacing.large)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question Palette ($totalQuestions)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("mock_palette_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close palette"
                    )
                }
            }

            Spacer(modifier = Modifier.height(PSCTokens.spacing.small))

            // Legend / Counts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(PSCTokens.spacing.small))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(PSCTokens.spacing.small),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatusLegendItem(label = "Answered", count = answeredCount, color = MaterialTheme.colorScheme.primaryContainer)
                StatusLegendItem(label = "Unanswered", count = unansweredCount, color = MaterialTheme.colorScheme.surfaceVariant)
                StatusLegendItem(label = "Marked", count = markedCount, color = MaterialTheme.colorScheme.tertiaryContainer)
            }

            Spacer(modifier = Modifier.height(PSCTokens.spacing.medium))

            // 0..(questionCount - 1) dynamic grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                verticalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(PSCTokens.spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .testTag("mock_palette_grid")
            ) {
                itemsIndexed(questionStatuses) { index, status ->
                    val isCurrent = (index == currentIndex)
                    QuestionTile(
                        index = index,
                        status = status,
                        isCurrent = isCurrent,
                        onClick = {
                            onQuestionSelected(index)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLegendItem(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuestionTile(
    index: Int,
    status: MockQuestionStatus,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when (status) {
        MockQuestionStatus.ANSWERED -> MaterialTheme.colorScheme.primaryContainer
        MockQuestionStatus.ANSWERED_AND_MARKED -> MaterialTheme.colorScheme.primaryContainer
        MockQuestionStatus.MARKED_FOR_REVIEW -> MaterialTheme.colorScheme.tertiaryContainer
        MockQuestionStatus.UNANSWERED -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (status) {
        MockQuestionStatus.ANSWERED -> MaterialTheme.colorScheme.onPrimaryContainer
        MockQuestionStatus.ANSWERED_AND_MARKED -> MaterialTheme.colorScheme.onPrimaryContainer
        MockQuestionStatus.MARKED_FOR_REVIEW -> MaterialTheme.colorScheme.onTertiaryContainer
        MockQuestionStatus.UNANSWERED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isMarked = (status == MockQuestionStatus.MARKED_FOR_REVIEW || status == MockQuestionStatus.ANSWERED_AND_MARKED)

    val stateText = when (status) {
        MockQuestionStatus.ANSWERED -> stringResource(R.string.cd_palette_state_answered)
        MockQuestionStatus.ANSWERED_AND_MARKED -> "${stringResource(R.string.cd_palette_state_answered)}, ${stringResource(R.string.cd_palette_state_marked)}"
        MockQuestionStatus.MARKED_FOR_REVIEW -> stringResource(R.string.cd_palette_state_marked)
        MockQuestionStatus.UNANSWERED -> stringResource(R.string.cd_palette_state_unanswered)
    }
    val tileContentDesc = stringResource(R.string.cd_palette_item, index + 1, stateText)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(
                if (isCurrent) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = tileContentDesc }
            .testTag("palette_tile_$index"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )

        if (isMarked) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .padding(1.dp)
            )
        }
    }
}
