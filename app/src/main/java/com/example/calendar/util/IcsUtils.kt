package com.example.calendar.util

import com.example.calendar.data.entity.CalendarEvent
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * ICS (iCalendar RFC5545) 导入导出工具类
 * 手动解析实现，不依赖 ical4j 以避免兼容性问题
 */
object IcsUtils {
    
    private const val PRODID = "-//SmartCalendar//CN"
    private const val VERSION = "2.0"
    
    private val dateTimeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    private val dateTimeFormatLocal = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    
    /**
     * 将日程列表导出为 ICS 格式字符串
     */
    fun exportToIcs(events: List<CalendarEvent>): String {
        val sb = StringBuilder()
        
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:$VERSION")
        sb.appendLine("PRODID:$PRODID")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")
        
        events.forEach { event ->
            sb.appendLine("BEGIN:VEVENT")
            sb.appendLine("UID:${event.id}")
            sb.appendLine("DTSTAMP:${formatDateTime(System.currentTimeMillis())}")
            
            if (event.isAllDay) {
                sb.appendLine("DTSTART;VALUE=DATE:${formatDate(event.startTime)}")
                sb.appendLine("DTEND;VALUE=DATE:${formatDate(event.endTime + 86400000)}") // 全天事件结束日期需要+1天
            } else {
                sb.appendLine("DTSTART:${formatDateTime(event.startTime)}")
                sb.appendLine("DTEND:${formatDateTime(event.endTime)}")
            }
            
            sb.appendLine("SUMMARY:${escapeText(event.title)}")
            
            event.description?.let {
                if (it.isNotBlank()) {
                    sb.appendLine("DESCRIPTION:${escapeText(it)}")
                }
            }
            
            event.location?.let {
                if (it.isNotBlank()) {
                    sb.appendLine("LOCATION:${escapeText(it)}")
                }
            }
            
            // 添加提醒
            event.reminder?.let { minutes ->
                if (minutes >= 0) {
                    sb.appendLine("BEGIN:VALARM")
                    sb.appendLine("TRIGGER:-PT${minutes}M")
                    sb.appendLine("ACTION:DISPLAY")
                    sb.appendLine("DESCRIPTION:日程提醒")
                    sb.appendLine("END:VALARM")
                }
            }
            
            sb.appendLine("END:VEVENT")
        }
        
        sb.appendLine("END:VCALENDAR")
        
        return sb.toString()
    }
    
    /**
     * 从 ICS 字符串解析日程列表
     */
    fun parseFromIcs(icsContent: String, subscriptionUrl: String? = null): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val lines = unfoldLines(icsContent)
        
        var inEvent = false
        var inAlarm = false
        var currentEvent = mutableMapOf<String, String>()
        var reminderMinutes: Int? = null
        
        for (line in lines) {
            val trimmedLine = line.trim()
            
            when {
                trimmedLine == "BEGIN:VEVENT" -> {
                    inEvent = true
                    currentEvent = mutableMapOf()
                    reminderMinutes = null
                }
                trimmedLine == "END:VEVENT" -> {
                    if (inEvent) {
                        val event = parseEventMap(currentEvent, reminderMinutes, subscriptionUrl)
                        if (event != null) {
                            events.add(event)
                        }
                    }
                    inEvent = false
                }
                trimmedLine == "BEGIN:VALARM" -> {
                    inAlarm = true
                }
                trimmedLine == "END:VALARM" -> {
                    inAlarm = false
                }
                inAlarm && trimmedLine.startsWith("TRIGGER:") -> {
                    reminderMinutes = parseTrigger(trimmedLine.substringAfter("TRIGGER:"))
                }
                inEvent && !inAlarm -> {
                    val colonIndex = trimmedLine.indexOf(':')
                    if (colonIndex > 0) {
                        val key = trimmedLine.substring(0, colonIndex)
                        val value = trimmedLine.substring(colonIndex + 1)
                        currentEvent[key] = value
                    }
                }
            }
        }
        
        return events
    }
    
    /**
     * 从 InputStream 解析日程列表
     */
    fun parseFromInputStream(inputStream: InputStream, subscriptionUrl: String? = null): List<CalendarEvent> {
        val content = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
        return parseFromIcs(content, subscriptionUrl)
    }
    
    /**
     * 展开 ICS 折叠行 (RFC5545 规定每行不超过75字符，续行以空格开头)
     */
    private fun unfoldLines(content: String): List<String> {
        val result = mutableListOf<String>()
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        val sb = StringBuilder()
        
        for (line in lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                // 续行
                sb.append(line.substring(1))
            } else {
                if (sb.isNotEmpty()) {
                    result.add(sb.toString())
                }
                sb.clear()
                sb.append(line)
            }
        }
        if (sb.isNotEmpty()) {
            result.add(sb.toString())
        }
        
        return result
    }
    
    /**
     * 解析事件 Map 为 CalendarEvent
     */
    private fun parseEventMap(
        map: Map<String, String>,
        reminderMinutes: Int?,
        subscriptionUrl: String?
    ): CalendarEvent? {
        val summary = map.entries.find { it.key.startsWith("SUMMARY") }?.value?.let { unescapeText(it) }
            ?: return null
        
        val uid = map["UID"] ?: UUID.randomUUID().toString()
        
        // 解析开始时间
        val dtStartEntry = map.entries.find { it.key.startsWith("DTSTART") }
        val startTime = dtStartEntry?.let { parseDateTime(it.key, it.value) } ?: return null
        
        // 解析结束时间
        val dtEndEntry = map.entries.find { it.key.startsWith("DTEND") }
        val endTime = dtEndEntry?.let { parseDateTime(it.key, it.value) } 
            ?: (startTime + 3600000) // 默认1小时
        
        // 检查是否全天事件
        val isAllDay = dtStartEntry.key.contains("VALUE=DATE") && !dtStartEntry.key.contains("VALUE=DATE-TIME")
        
        val description = map.entries.find { it.key.startsWith("DESCRIPTION") }?.value?.let { unescapeText(it) }
        val location = map.entries.find { it.key.startsWith("LOCATION") }?.value?.let { unescapeText(it) }
        
        return CalendarEvent(
            id = uid,
            title = summary,
            description = description,
            location = location,
            startTime = startTime,
            endTime = if (isAllDay) endTime - 86400000 else endTime, // 全天事件结束日期需要-1天
            isAllDay = isAllDay,
            reminder = reminderMinutes,
            color = null,
            isFromSubscription = subscriptionUrl != null,
            subscriptionUrl = subscriptionUrl
        )
    }
    
    /**
     * 解析日期时间
     */
    private fun parseDateTime(key: String, value: String): Long? {
        return try {
            val cleanValue = value.trim()
            
            when {
                // 全天事件 (只有日期)
                key.contains("VALUE=DATE") && !key.contains("VALUE=DATE-TIME") -> {
                    dateFormat.parse(cleanValue)?.time
                }
                // UTC 时间
                cleanValue.endsWith("Z") -> {
                    dateTimeFormat.parse(cleanValue)?.time
                }
                // 本地时间或带时区
                else -> {
                    // 尝试解析为 yyyyMMdd'T'HHmmss
                    dateTimeFormatLocal.parse(cleanValue.replace("Z", ""))?.time
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 解析提醒触发器
     */
    private fun parseTrigger(trigger: String): Int? {
        return try {
            // 格式: -PT15M, -P1D, -PT1H 等
            val isNegative = trigger.startsWith("-")
            val value = trigger.removePrefix("-").removePrefix("+")
            
            var minutes = 0
            
            if (value.startsWith("PT")) {
                // 时间间隔: PT15M, PT1H30M
                val timeStr = value.substring(2)
                
                val hourMatch = Regex("(\\d+)H").find(timeStr)
                val minuteMatch = Regex("(\\d+)M").find(timeStr)
                val secondMatch = Regex("(\\d+)S").find(timeStr)
                
                hourMatch?.groupValues?.get(1)?.toIntOrNull()?.let { minutes += it * 60 }
                minuteMatch?.groupValues?.get(1)?.toIntOrNull()?.let { minutes += it }
                secondMatch?.groupValues?.get(1)?.toIntOrNull()?.let { minutes += it / 60 }
            } else if (value.startsWith("P")) {
                // 日期间隔: P1D
                val dayMatch = Regex("(\\d+)D").find(value)
                dayMatch?.groupValues?.get(1)?.toIntOrNull()?.let { minutes += it * 24 * 60 }
            }
            
            if (minutes > 0) minutes else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 格式化日期时间为 ICS 格式
     */
    private fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }
    
    /**
     * 格式化日期为 ICS 格式
     */
    private fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * 转义 ICS 文本中的特殊字符
     */
    private fun escapeText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }
    
    /**
     * 反转义 ICS 文本
     */
    private fun unescapeText(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }
    
    /**
     * 验证 ICS 内容是否有效
     */
    fun isValidIcs(content: String): Boolean {
        return content.contains("BEGIN:VCALENDAR") && content.contains("END:VCALENDAR")
    }
}
