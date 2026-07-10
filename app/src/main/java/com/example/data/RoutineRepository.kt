package com.example.data

import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val routineDao: RoutineDao) {

    val allClasses: Flow<List<ClassSchedule>> = routineDao.getAllClasses()
    
    val allStudyLogs: Flow<List<StudyLog>> = routineDao.getAllStudyLogs()

    fun getClassesForDay(day: String): Flow<List<ClassSchedule>> {
        return routineDao.getClassesForDay(day)
    }

    suspend fun insertClass(classSchedule: ClassSchedule) {
        routineDao.insertClass(classSchedule)
    }

    suspend fun insertClasses(classes: List<ClassSchedule>) {
        routineDao.insertClasses(classes)
    }

    suspend fun updateClass(classSchedule: ClassSchedule) {
        routineDao.updateClass(classSchedule)
    }

    suspend fun deleteClass(classSchedule: ClassSchedule) {
        routineDao.deleteClass(classSchedule)
    }

    suspend fun clearAllClasses() {
        routineDao.clearAllClasses()
    }

    suspend fun insertStudyLog(studyLog: StudyLog) {
        routineDao.insertStudyLog(studyLog)
    }

    suspend fun deleteStudyLog(id: Int) {
        routineDao.deleteStudyLog(id)
    }

    // Exam Routine Methods
    val allExams: Flow<List<ExamSchedule>> = routineDao.getAllExams()

    suspend fun insertExam(examSchedule: ExamSchedule) {
        routineDao.insertExam(examSchedule)
    }

    suspend fun insertExams(exams: List<ExamSchedule>) {
        routineDao.insertExams(exams)
    }

    suspend fun updateExam(examSchedule: ExamSchedule) {
        routineDao.updateExam(examSchedule)
    }

    suspend fun deleteExam(examSchedule: ExamSchedule) {
        routineDao.deleteExam(examSchedule)
    }

    suspend fun clearAllExams() {
        routineDao.clearAllExams()
    }
}
