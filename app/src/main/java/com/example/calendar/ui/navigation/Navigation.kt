package com.example.calendar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.calendar.ui.calendar.CalendarScreen
import com.example.calendar.ui.event.EventDetailScreen
import com.example.calendar.ui.event.EventEditScreen
import com.example.calendar.ui.settings.SettingsScreen
import com.example.calendar.viewmodel.CalendarViewModel

/**
 * 导航路由常量
 */
object Routes {
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val EVENT_DETAIL = "event/{eventId}"
    const val EVENT_EDIT = "event/edit?eventId={eventId}&year={year}&month={month}&day={day}"
    const val EVENT_NEW = "event/new?year={year}&month={month}&day={day}"
    
    fun eventDetail(eventId: String) = "event/$eventId"
    
    fun eventEdit(eventId: String) = "event/edit?eventId=$eventId"
    
    fun eventNew(year: Int? = null, month: Int? = null, day: Int? = null): String {
        val params = mutableListOf<String>()
        if (year != null) params.add("year=$year")
        if (month != null) params.add("month=$month")
        if (day != null) params.add("day=$day")
        return if (params.isEmpty()) "event/new" else "event/new?${params.joinToString("&")}"
    }
}

/**
 * 应用导航图
 */
@Composable
fun CalendarNavHost(
    navController: NavHostController = rememberNavController(),
    calendarViewModel: CalendarViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CALENDAR
    ) {
        // 日历主屏幕
        composable(Routes.CALENDAR) {
            CalendarScreen(
                viewModel = calendarViewModel,
                onEventClick = { eventId ->
                    navController.navigate(Routes.eventDetail(eventId))
                },
                onAddEventClick = { year, month, day ->
                    navController.navigate(Routes.eventNew(year, month, day))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        
        // 设置页面
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // 日程详情页
        composable(
            route = Routes.EVENT_DETAIL,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            EventDetailScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.eventEdit(eventId)) },
                onDeleted = { navController.popBackStack() }
            )
        }
        
        // 新建日程页
        composable(
            route = "event/new?year={year}&month={month}&day={day}",
            arguments = listOf(
                navArgument("year") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("month") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("day") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getString("year")?.toIntOrNull()
            val month = backStackEntry.arguments?.getString("month")?.toIntOrNull()
            val day = backStackEntry.arguments?.getString("day")?.toIntOrNull()
            
            val initialDate = if (year != null && month != null && day != null) {
                Triple(year, month, day)
            } else null
            
            EventEditScreen(
                eventId = null,
                initialDate = initialDate,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        
        // 编辑日程页
        composable(
            route = "event/edit?eventId={eventId}",
            arguments = listOf(
                navArgument("eventId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            
            EventEditScreen(
                eventId = eventId,
                initialDate = null,
                onBack = { navController.popBackStack() },
                onSaved = { 
                    // 保存后返回两级 (跳过详情页)
                    navController.popBackStack()
                    navController.popBackStack()
                }
            )
        }
    }
}
