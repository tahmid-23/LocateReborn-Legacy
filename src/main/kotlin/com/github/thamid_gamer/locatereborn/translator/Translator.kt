package com.github.thamid_gamer.locatereborn.translator

import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.io.path.writeText

/**
 * Translates the IDs in coursedata.csv and student.csv to the actual course/student names
 */
fun translate() {
    val courseData = Path("coursedata.csv").readLines().toMutableList()
    val studentData = Path("studentdata.csv").readLines().toMutableList()

    val courseIdMap = createCourseIdMap(courseData)
    val studentIdMap = createStudentIdMap(studentData)

    replaceStudentIds(courseData, studentIdMap)
    replaceCourseIds(studentData, courseIdMap)

    printTranslatedData(courseData, studentData)
}

/**
 * Creates a map of course IDs to course names
 *
 * @param courseData Lines of coursedata.csv
 * @return A map of course IDs to course names
 */
private fun createCourseIdMap(courseData: MutableList<String>): Map<String, String> {
    val courseIdMap = mutableMapOf<String, String>()

    for (x in courseData.indices) {
        val components = courseData[x].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()) // Regex by https://stackoverflow.com/questions/18893390/splitting-on-comma-outside-quotes accepted answer
        courseIdMap[components[0]] = "${components[1].split(":")[0]}\""
        courseData[x] = components.drop(1).joinToString(",")
    }

    return courseIdMap
}

/**
 * Creates a map of student IDs to student names
 *
 * @param studentData Lines of coursedata.csv
 * @return A map of student IDs to student names
 */
private fun createStudentIdMap(studentData: MutableList<String>): Map<String, String> {
    val studentIdMap = mutableMapOf<String, String>()

    for (x in 0 until (studentData.size + 1) / 12) {
        val components = studentData[12 * x].split(",")
        studentIdMap[components[0]] = components[1]
        studentData[12 * x] = components[1]
    }

    return studentIdMap
}

/**
 * Replaces student IDs with student names
 *
 * @param courseData Lines of new course data
 * @param studentIdMap A map of student IDs to student names
 */
private fun replaceStudentIds(courseData: MutableList<String>, studentIdMap: Map<String, String>) {
    for (x in 0 until courseData.size) {
        val components = courseData[x].split(",").toMutableList()
        for (y in 1 until components.size) {
            studentIdMap[components[y]]?.let {
                components[y] = it
            }
        }
        courseData[x] = components.joinToString(",")
    }
}

/**
 * Replaces course IDs with course names
 *
 * @param studentData Lines of new student data
 * @param courseIdMap A map of course IDs to course names
 */
private fun replaceCourseIds(studentData: MutableList<String>, courseIdMap: Map<String, String>) {
    for (x in 0 until (studentData.size + 1) / 12) {
        for (y in 0 until 12) {
            when (y) {
                0, 1 -> continue
                else -> {
                    val components = studentData[12 * x + y].split(",").toMutableList()

                    for (z in 1 until components.size) {
                        courseIdMap[components[z]]?.let {
                            components[z] = it
                        }
                    }
                    studentData[12 * x + y] = components.joinToString(",")
                }
            }
        }
    }
}

/**
 * Prints out translated data
 *
 * @param courseData The new course data
 * @param studentData The new student data
 */
private fun printTranslatedData(courseData: List<String>, studentData: List<String>) {
    Path("translated-coursedata.csv").writeText(courseData.joinToString(System.lineSeparator()))
    Path("translated-studentdata.csv").writeText(studentData.joinToString(System.lineSeparator()))
}
