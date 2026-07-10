package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM class_schedule ORDER BY dayOfWeek, timeStart ASC")
    fun getAllClasses(): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedule WHERE dayOfWeek = :day ORDER BY timeStart ASC")
    fun getClassesForDay(day: String): Flow<List<ClassSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classSchedule: ClassSchedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ClassSchedule>)

    @Update
    suspend fun updateClass(classSchedule: ClassSchedule)

    @Delete
    suspend fun deleteClass(classSchedule: ClassSchedule)

    @Query("DELETE FROM class_schedule")
    suspend fun clearAllClasses()

    // Study Log Queries
    @Query("SELECT * FROM study_logs ORDER BY date DESC")
    fun getAllStudyLogs(): Flow<List<StudyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLog(studyLog: StudyLog)

    @Query("DELETE FROM study_logs WHERE id = :id")
    suspend fun deleteStudyLog(id: Int)

    // Exam Routine Queries
    @Query("SELECT * FROM exam_schedule ORDER BY date, timeStart ASC")
    fun getAllExams(): Flow<List<ExamSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(examSchedule: ExamSchedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamSchedule>)

    @Update
    suspend fun updateExam(examSchedule: ExamSchedule)

    @Delete
    suspend fun deleteExam(examSchedule: ExamSchedule)

    @Query("DELETE FROM exam_schedule")
    suspend fun clearAllExams()
}
