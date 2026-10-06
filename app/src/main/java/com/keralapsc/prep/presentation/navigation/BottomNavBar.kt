package com.keralapsc.prep.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.keralapsc.prep.R

enum class TopLevelDestination(
    val route: Screen,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
    @StringRes val contentDescriptionRes: Int
) {
    HOME(
        route = Screen.Home,
        icon = Icons.Default.Home,
        labelRes = R.string.nav_home,
        contentDescriptionRes = R.string.cd_nav_home
    ),
    PRACTICE(
        route = Screen.Practice,
        icon = Icons.Default.MenuBook,
        labelRes = R.string.nav_practice,
        contentDescriptionRes = R.string.cd_nav_practice
    ),
    MOCK_EXAM(
        route = Screen.MockExam,
        icon = Icons.Default.Assignment,
        labelRes = R.string.nav_mock_exam,
        contentDescriptionRes = R.string.cd_nav_mock_exam
    ),
    PROGRESS(
        route = Screen.Progress,
        icon = Icons.Default.BarChart,
        labelRes = R.string.nav_progress,
        contentDescriptionRes = R.string.cd_nav_progress
    ),
    PROFILE(
        route = Screen.Profile,
        icon = Icons.Default.Person,
        labelRes = R.string.nav_profile,
        contentDescriptionRes = R.string.cd_nav_profile
    )
}

@Composable
fun PSCBottomNavBar(
    currentRoute: Screen?,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val isSelected = currentRoute == destination.route
            val labelText = stringResource(destination.labelRes)
            val descText = stringResource(destination.contentDescriptionRes)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = descText
                    )
                },
                label = {
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.semantics {
                    contentDescription = descText
                }
            )
        }
    }
}
