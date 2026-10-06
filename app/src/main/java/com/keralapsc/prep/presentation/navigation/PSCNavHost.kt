package com.keralapsc.prep.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.keralapsc.prep.PSCApplication
import com.keralapsc.prep.presentation.home.HomeScreen
import com.keralapsc.prep.presentation.home.HomeViewModel
import com.keralapsc.prep.presentation.mockexam.MockExamLobbyViewModel
import com.keralapsc.prep.presentation.mockexam.MockExamScreen
import com.keralapsc.prep.presentation.mockexam.result.MockExamResultScreen
import com.keralapsc.prep.presentation.mockexam.result.MockExamResultViewModel
import com.keralapsc.prep.presentation.mockexam.review.MockExamReviewScreen
import com.keralapsc.prep.presentation.mockexam.review.MockExamReviewViewModel
import com.keralapsc.prep.presentation.mockexam.runner.MockExamRunnerScreen
import com.keralapsc.prep.presentation.mockexam.runner.MockExamViewModel
import com.keralapsc.prep.presentation.settings.SettingsScreen
import com.keralapsc.prep.presentation.settings.SettingsViewModel
import com.keralapsc.prep.presentation.attribution.AttributionScreen
import com.keralapsc.prep.presentation.attribution.AttributionViewModel
import com.keralapsc.prep.presentation.practice.PracticeScreen
import com.keralapsc.prep.presentation.practice.PracticeViewModel
import com.keralapsc.prep.presentation.profile.ProfileScreen
import com.keralapsc.prep.presentation.progress.ProgressScreen
import com.keralapsc.prep.presentation.progress.ProgressViewModel
import com.keralapsc.prep.presentation.quiz.runner.QuizRunnerScreen
import com.keralapsc.prep.presentation.quiz.runner.QuizViewModel
import com.keralapsc.prep.presentation.quiz.summary.QuizSummaryScreen
import com.keralapsc.prep.presentation.quiz.summary.QuizSummaryViewModel
import com.keralapsc.prep.presentation.topic.TopicDetailScreen
import com.keralapsc.prep.presentation.topic.TopicDetailViewModel
import com.keralapsc.prep.presentation.content.ContentPacksScreen
import com.keralapsc.prep.presentation.content.ContentPacksViewModel
import com.keralapsc.prep.presentation.viewmodel.ViewModelFactory

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.keralapsc.prep.presentation.error.DatabaseErrorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PSCApp(
    navController: NavHostController = rememberNavController(),
    databaseError: Throwable? = null
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as? PSCApplication)?.container
    val activeDbError = databaseError ?: appContainer?.databaseInitializationError?.collectAsState()?.value

    if (activeDbError != null) {
        DatabaseErrorScreen(
            error = activeDbError,
            onRetry = {
                appContainer?.let { container ->
                    (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                        container.initializeDatabase()
                    }
                }
            }
        )
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentTopLevelDestination: TopLevelDestination? = when {
        currentDestination?.hasRoute<Screen.Home>() == true -> TopLevelDestination.HOME
        currentDestination?.hasRoute<Screen.Practice>() == true -> TopLevelDestination.PRACTICE
        currentDestination?.hasRoute<Screen.MockExam>() == true -> TopLevelDestination.MOCK_EXAM
        currentDestination?.hasRoute<Screen.Progress>() == true -> TopLevelDestination.PROGRESS
        currentDestination?.hasRoute<Screen.Profile>() == true -> TopLevelDestination.PROFILE
        else -> null
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentTopLevelDestination != null) {
                PSCBottomNavBar(
                    currentRoute = currentTopLevelDestination.route,
                    onNavigateToDestination = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        PSCNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun PSCNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as PSCApplication).container

    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = modifier
    ) {
        // Top-level destinations
        composable<Screen.Home> {
            val viewModel: HomeViewModel = viewModel(
                factory = ViewModelFactory(appContainer = appContainer)
            )
            HomeScreen(
                viewModel = viewModel,
                onStartDailyQuiz = {
                    navController.navigate(Screen.QuizRunner(quizType = "DAILY"))
                },
                onTopicSelected = { topicId ->
                    navController.navigate(Screen.QuizRunner(quizType = "TOPIC", referenceId = topicId))
                },
                onContinuePractice = { topicId, levelId, postId ->
                    navController.navigate(
                        Screen.QuizRunner(
                            quizType = "TOPIC",
                            referenceId = topicId,
                            levelId = levelId,
                            postId = postId
                        )
                    )
                },
                onReviewNowClick = { levelId, postId ->
                    navController.navigate(
                        Screen.QuizRunner(
                            quizType = "SPACED_REPETITION",
                            levelId = levelId,
                            postId = postId
                        )
                    )
                }
            )
        }

        composable<Screen.Practice> {
            val viewModel: PracticeViewModel = viewModel(
                factory = ViewModelFactory(appContainer = appContainer)
            )
            PracticeScreen(
                viewModel = viewModel,
                onTopicSelected = { topicId, topicTitle ->
                    navController.navigate(Screen.TopicDetail(topicId = topicId, topicTitle = topicTitle))
                },
                onStartTopicQuiz = { topicId, levelId, postId ->
                    navController.navigate(
                        Screen.QuizRunner(
                            quizType = "TOPIC",
                            referenceId = topicId,
                            levelId = levelId,
                            postId = postId
                        )
                    )
                },
                onStartAdaptivePractice = { levelId, postId ->
                    navController.navigate(
                        Screen.QuizRunner(
                            quizType = "SPACED_REPETITION",
                            levelId = levelId,
                            postId = postId
                        )
                    )
                }
            )
        }

        composable<Screen.TopicDetail> { backStackEntry ->
            val route: Screen.TopicDetail = backStackEntry.toRoute()
            val viewModel: TopicDetailViewModel = viewModel(
                factory = ViewModelFactory(
                    appContainer = appContainer,
                    topicId = route.topicId,
                    topicTitle = route.topicTitle
                )
            )
            TopicDetailScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onStartQuiz = { topicId, levelId, postId ->
                    navController.navigate(
                        Screen.QuizRunner(
                            quizType = "TOPIC",
                            referenceId = topicId,
                            levelId = levelId,
                            postId = postId
                        )
                    )
                }
            )
        }

        composable<Screen.MockExam> {
            val viewModel: MockExamLobbyViewModel = viewModel(
                factory = ViewModelFactory(appContainer = appContainer)
            )
            MockExamScreen(
                viewModel = viewModel,
                onStartMockExam = { levelId, postId, questionCount ->
                    navController.navigate(
                        Screen.MockExamRunner(
                            attemptId = null,
                            levelId = levelId,
                            postId = postId,
                            questionCount = questionCount
                        )
                    )
                },
                onResumeMockExam = { attemptId ->
                    navController.navigate(
                        Screen.MockExamRunner(
                            attemptId = attemptId
                        )
                    )
                },
                onReviewMockExam = { attemptId ->
                    navController.navigate(
                        Screen.MockExamReview(
                            attemptId = attemptId
                        )
                    )
                }
            )
        }

        composable<Screen.Progress> {
            val viewModel: ProgressViewModel = viewModel(
                factory = ViewModelFactory(appContainer = appContainer)
            )
            ProgressScreen(
                viewModel = viewModel,
                onNavigateToPractice = {
                    navController.navigate(Screen.Practice) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onStartTopicQuiz = { topicId, levelId, postId ->
                    navController.navigate(
                        Screen.QuizRunner(
                            quizType = "TOPIC",
                            referenceId = topicId,
                            levelId = levelId,
                            postId = postId
                        )
                    )
                }
            )
        }

        composable<Screen.Profile> {
            ProfileScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings)
                },
                onNavigateToAttribution = {
                    navController.navigate(Screen.PSCAttribution)
                },
                onNavigateToContentPacks = {
                    navController.navigate(Screen.ContentPacks)
                }
            )
        }

        // Fullscreen Quiz Runner
        composable<Screen.QuizRunner> { backStackEntry ->
            val route: Screen.QuizRunner = backStackEntry.toRoute()
            val viewModel: QuizViewModel = viewModel(
                factory = ViewModelFactory(
                    appContainer = appContainer,
                    quizType = route.quizType,
                    referenceId = route.referenceId,
                    levelId = route.levelId,
                    postId = route.postId
                )
            )
            QuizRunnerScreen(
                viewModel = viewModel,
                onNavigateToSummary = { attemptId ->
                    navController.navigate(Screen.QuizSummary(attemptId = attemptId)) {
                        popUpTo<Screen.QuizRunner> {
                            inclusive = true
                        }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Fullscreen Quiz Summary
        composable<Screen.QuizSummary> { backStackEntry ->
            val route: Screen.QuizSummary = backStackEntry.toRoute()
            val viewModel: QuizSummaryViewModel = viewModel(
                factory = ViewModelFactory(
                    appContainer = appContainer,
                    attemptId = route.attemptId
                )
            )
            QuizSummaryScreen(
                viewModel = viewModel,
                onReturnToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                        }
                    }
                },
                onPracticeAgain = {
                    navController.navigate(Screen.QuizRunner(quizType = "DAILY")) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                        }
                    }
                }
            )
        }

        // Fullscreen Mock Exam Runner
        composable<Screen.MockExamRunner> { backStackEntry ->
            val route: Screen.MockExamRunner = backStackEntry.toRoute()
            val viewModel: MockExamViewModel = viewModel(
                factory = ViewModelFactory(
                    appContainer = appContainer,
                    savedStateHandle = backStackEntry.savedStateHandle,
                    attemptId = route.attemptId,
                    levelId = route.levelId,
                    postId = route.postId,
                    questionCount = route.questionCount
                )
            )
            MockExamRunnerScreen(
                viewModel = viewModel,
                onNavigateToResult = { attemptId ->
                    navController.navigate(Screen.MockExamResult(attemptId = attemptId)) {
                        popUpTo<Screen.MockExamRunner> {
                            inclusive = true
                        }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Fullscreen Mock Exam Result
        composable<Screen.MockExamResult> { backStackEntry ->
            val route: Screen.MockExamResult = backStackEntry.toRoute()
            val viewModel: MockExamResultViewModel = viewModel(
                factory = ViewModelFactory(
                    appContainer = appContainer,
                    savedStateHandle = backStackEntry.savedStateHandle,
                    attemptId = route.attemptId
                )
            )
            MockExamResultScreen(
                viewModel = viewModel,
                onReviewAnswersClick = { attemptId ->
                    navController.navigate(Screen.MockExamReview(attemptId = attemptId))
                },
                onBackToMocksClick = {
                    navController.navigate(Screen.MockExam) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                        }
                    }
                }
            )
        }

        // Fullscreen Mock Exam Review
        composable<Screen.MockExamReview> { backStackEntry ->
            val route: Screen.MockExamReview = backStackEntry.toRoute()
            val viewModel: MockExamReviewViewModel = viewModel(
                factory = ViewModelFactory(
                    appContainer = appContainer,
                    savedStateHandle = backStackEntry.savedStateHandle,
                    attemptId = route.attemptId
                )
            )
            MockExamReviewScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.Settings> {
            val viewModel: SettingsViewModel = viewModel(
                factory = ViewModelFactory(appContainer = appContainer)
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTargetSelection = {
                    navController.navigate(Screen.Practice)
                },
                onNavigateToContentPacks = {
                    navController.navigate(Screen.ContentPacks)
                }
            )
        }

        composable<Screen.PSCAttribution> {
            val viewModel: AttributionViewModel = viewModel(
                factory = ViewModelFactory(appContainer = appContainer)
            )
            AttributionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.ContentPacks> {
            val viewModel: ContentPacksViewModel = viewModel(
                factory = ViewModelFactory(appContainer = appContainer)
            )
            ContentPacksScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
