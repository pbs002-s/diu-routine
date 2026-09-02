package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

object DiuRoutineDataManager {

    /**
     * Converts a 12-hour AM/PM time string like "1:00 PM" or "08:30 AM" to 24-hour "HH:mm".
     */
    fun convertTo24HourTime(timeStr: String?): String {
        if (timeStr.isNullOrBlank()) return "08:30"
        val clean = timeStr.trim().uppercase(Locale.US)
        return try {
            if (clean.contains("AM") || clean.contains("PM")) {
                val parser = SimpleDateFormat("h:mm a", Locale.US)
                val formatter = SimpleDateFormat("HH:mm", Locale.US)
                val date = parser.parse(clean)
                if (date != null) formatter.format(date) else clean
            } else if (clean.matches(Regex("^\\d{1,2}:\\d{2}$"))) {
                val parts = clean.split(":")
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1]
                String.format(Locale.US, "%02d:%s", h, m)
            } else {
                clean
            }
        } catch (e: Exception) {
            clean
        }
    }

    /**
     * Maps a list of DiuRoutineItem from the API directly into Room ClassSchedule entities.
     */
    fun mapRoutineItemsToClassSchedules(
        items: List<DiuRoutineItem>,
        defaultDept: String,
        defaultSection: String
    ): List<ClassSchedule> {
        return items.mapNotNull { item ->
            val weekday = item.weekday?.trim()?.replaceFirstChar { it.uppercase() } ?: return@mapNotNull null
            val subjectCode = item.courseCode?.trim()?.replace("-", " ") ?: "COURSE"
            val subjectName = item.courseTitle?.trim() ?: "Academic Course"
            val teacherCode = item.teacherInitials?.trim() ?: item.teacherName?.trim() ?: "TBA"
            val roomNo = item.room?.trim() ?: "TBA"
            val timeStart = convertTo24HourTime(item.startTime)
            val timeEnd = convertTo24HourTime(item.endTime)
            val dept = item.department?.trim()?.ifEmpty { null } ?: defaultDept
            val sec = item.section?.trim()?.ifEmpty { null } ?: defaultSection

            ClassSchedule(
                dayOfWeek = weekday,
                subjectCode = subjectCode,
                subjectName = subjectName,
                teacherCode = teacherCode,
                timeStart = timeStart,
                timeEnd = timeEnd,
                roomNo = roomNo,
                department = dept,
                section = sec,
                notificationEnabled = true,
                isCompleted = false
            )
        }
    }

    /**
     * Reads a bundled JSON file from app assets.
     */
    private fun readAssetJson(context: Context, filename: String): String? {
        return try {
            val inputStream: InputStream = context.assets.open(filename)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Loads list of departments from bundled offline assets.
     */
    fun getOfflineDepartments(context: Context): List<DiuDepartment> {
        val jsonStr = readAssetJson(context, "departments.json") ?: return emptyList()
        val list = mutableListOf<DiuDepartment>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    DiuDepartment(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        code = obj.optString("code", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Loads list of batches for a department from bundled offline assets.
     */
    fun getOfflineBatches(context: Context, deptCode: String): List<String> {
        val jsonStr = readAssetJson(context, "batches.json") ?: return emptyList()
        val set = linkedSetOf<String>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val deptObj = obj.optJSONObject("department")
                val dCode = deptObj?.optString("code") ?: obj.optString("departmentCode", "")
                if (dCode.equals(deptCode, ignoreCase = true) || deptCode.isEmpty()) {
                    val bName = obj.optString("name", "").trim()
                    if (bName.isNotEmpty()) {
                        set.add(bName)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return set.toList().sortedDescending()
    }

    /**
     * Loads list of sections for a department and batch from bundled offline assets.
     */
    fun getOfflineSections(context: Context, deptCode: String, batchName: String): List<String> {
        val jsonStr = readAssetJson(context, "sections.json") ?: return emptyList()
        val set = linkedSetOf<String>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val deptObj = obj.optJSONObject("department")
                val batchObj = obj.optJSONObject("batch")
                val dCode = deptObj?.optString("code") ?: ""
                val bName = batchObj?.optString("name") ?: ""

                if (dCode.equals(deptCode, ignoreCase = true) && bName.equals(batchName, ignoreCase = true)) {
                    val sName = obj.optString("name", "").trim()
                    if (sName.isNotEmpty()) {
                        set.add(sName)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return set.toList().sorted()
    }

    /**
     * Loads routine entries directly from the bundled offline routines_by_section.json
     * matching tag e.g. "CSE_68_D".
     */
    fun getOfflineRoutineForSection(
        context: Context,
        deptCode: String,
        batchName: String,
        sectionName: String
    ): List<ClassSchedule> {
        val jsonStr = readAssetJson(context, "routines_by_section.json") ?: return emptyList()
        val tag = "${deptCode.uppercase(Locale.US)}_${batchName}_${sectionName.uppercase(Locale.US)}"
        val results = mutableListOf<ClassSchedule>()

        try {
            val rootObj = JSONObject(jsonStr)
            var array: JSONArray? = null
            val candidateTags = listOf(
                tag,
                "${deptCode.uppercase(Locale.US)} ${batchName}_${sectionName.uppercase(Locale.US)}",
                "${deptCode.uppercase(Locale.US)}_${sectionName.uppercase(Locale.US)}",
                sectionName.uppercase(Locale.US)
            )

            for (t in candidateTags) {
                if (rootObj.has(t)) {
                    array = rootObj.getJSONArray(t)
                    break
                }
            }

            if (array == null) {
                // Try looking for tag ignoring case or underscores
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val normalizedK = k.replace(" ", "_").uppercase(Locale.US)
                    val normalizedTag = tag.replace(" ", "_").uppercase(Locale.US)
                    if (normalizedK == normalizedTag || k.equals(tag, ignoreCase = true)) {
                        array = rootObj.getJSONArray(k)
                        break
                    }
                }
            }

            if (array != null) {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val weekday = obj.optString("weekday", "Sunday")
                    val courseCode = obj.optString("courseCode", "COURSE").replace("-", " ")
                    val courseTitle = obj.optString("courseTitle", "Academic Course")
                    val teacherInitials = obj.optString("teacherInitials", "").ifEmpty {
                        obj.optString("teacherName", "TBA")
                    }
                    val room = obj.optString("room", "TBA")
                    val startTime = convertTo24HourTime(obj.optString("startTime", "08:30"))
                    val endTime = convertTo24HourTime(obj.optString("endTime", "10:00"))

                    results.add(
                        ClassSchedule(
                            dayOfWeek = weekday,
                            subjectCode = courseCode,
                            subjectName = courseTitle,
                            teacherCode = teacherInitials,
                            timeStart = startTime,
                            timeEnd = endTime,
                            roomNo = room,
                            department = deptCode,
                            section = sectionName,
                            notificationEnabled = true,
                            isCompleted = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }
}
