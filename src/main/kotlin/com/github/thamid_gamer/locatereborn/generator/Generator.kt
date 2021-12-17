package com.github.thamid_gamer.locatereborn.generator

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.github.thamid_gamer.locatereborn.generator.data.Course
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter
import kotlin.math.ceil

/**
 * Generates BCA Schoology course information
 */
fun generate() {
    BufferedReader(InputStreamReader(System.`in`)).use { input ->
        PrintWriter(Path("studentdata.csv").bufferedWriter()).use { pw ->
            while (true) {
                if (!queryData(input, pw)) {
                    println("Invalid credentials!")
                }
                else {
                    break
                }
            }
        }
    }
}

/**
 * Queries a username and password from the user and attempts to log in
 *
 * @param input Input reader of stdin
 * @param output Outputs to studentdata.csv
 * @return Whether the query was successful
 */
private fun queryData(input: BufferedReader, output: PrintWriter): Boolean {
    println("Please enter your username.")
    val username = input.readLine()
    println("Please enter your password.")
    val password = input.readLine()

    login(username, password)?.let {
        return printCourseData(output, it.cookies(), listOf(2233228305, 2232950152))
    }

    return false
}

/**
 * Logs into Schoology
 *
 * @param username Self-explanatory
 * @param password Self-explanatory
 * @return The result of the login
 */
private fun login(username: String, password: String): Connection.Response? = Jsoup
        .connect("https://bca.schoology.com/login/ldap?&school=11897239")
        .method(Connection.Method.POST)
        .data(mapOf(
            "mail" to username,
            "pass" to password,
            "school_nid" to "11897239",
            "form_build_id" to "489d854-djh1fsa2DkrmcVrso_nzX105TaBTc4arsSHbbof2FZI",
            "form_id" to "s_user_login_form"
        ))
        .execute()

/**
 * Prints out all relevant data to studentdata.csv and coursedata.csv
 *
 * @param out Outputs to studentdata.csv
 * @param cookies Cookies associated with the current login
 * @param ids The ids of the groups to fetch students from
 * @return Whether the query was successful
 */
private fun printCourseData(out: PrintWriter, cookies: Map<String, String>, ids: Collection<Long>): Boolean {
    val studentLists = mutableListOf<Element>()
    val courseStudentMap = mutableMapOf<String, String>()
    val usedStudents = mutableSetOf<String>()

    for (id in ids) {
        val doc = Jsoup.connect("https://bca.schoology.com/group/$id/members").cookies(cookies).get()
        val pages = doc.getElementsByClass("total")
        if (pages.isNotEmpty()) {
            for (x in 1..ceil(pages[0].text().toDouble() / 30).toInt()) {
                try {
                    buildStudentList(cookies, id, x)
                        ?.getElementsByAttributeValue("role", "presentation")
                        ?.first()
                        ?.child(0)
                        ?.let {
                            studentLists.add(it)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            return false
        }
    }

    for (studentList in studentLists) {
        printStudentCourses(studentList, cookies, out, courseStudentMap, usedStudents)
    }
    printCourseMembers(courseStudentMap)

    return true
}

/**
 * Builds the current list of students on a page
 *
 * @param cookies Cookies associated with the current login
 * @param id The id of the group to fetch students from
 * @param page The current page number
 * @return The list of students
 */
private fun buildStudentList(cookies: Map<String, String>, id: Long, page: Int): Document? {
    val request = Jsoup
        .connect("https://bca.schoology.com/enrollments/edit/members/group/$id/ajax")
        .method(Connection.Method.GET)
        .cookies(cookies)
        .ignoreHttpErrors(true)
        .data(mapOf("ss" to "", "p" to page.toString()))

    val response = request.execute()

    return when (response.statusCode()) {
        200 -> response.parse()
        429 -> {
            val timeout = response.header("Retry-After").toLong()
            Thread.sleep(timeout + 250)

            request.get()
        }
        else -> null
    }
}

/**
 * Outputs a page of students' courses to studentdata.csv
 *
 * @param studentList The list of students to iterate over
 * @param cookies Cookies associated with the current login
 * @param out Outputs to studentdata.csv
 * @param courseStudentMap A map of courses and a comma separated list of its students
 */
private fun printStudentCourses(
    studentList: Element,
    cookies: Map<String, String>,
    out: PrintWriter,
    courseStudentMap: MutableMap<String, String>,
    usedStudents: MutableSet<String>
) {
    for (student in studentList.children()) {
        val courseLink = student.child(1).child(0)
        val id = courseLink.attr("href").substring(6)
        val name = courseLink.text()
        if (id !in usedStudents) {
            getCourseInfo(id, cookies)?.getElementsByClass("my-courses-item-list")?.first()?.let {
                out.println("$id,$name")
                out.println("Period, Monday, Tuesday, Wednesday, Thursday, Friday")

                val courses = mutableListOf<Course>()
                addCourseInfo(id, it, courseStudentMap, courses)
                printPeriods(out, courses)

                out.println()

                usedStudents.add(id)
            }
        }
    }
}

/**
 * Adds course info to a student's courses
 *
 * @param id The student's ID
 * @param courseList A list of their courses as a DOM element
 * @param courseStudentMap A map of courses and a comma separated list of its students
 * @param courses A list of the student's courses
 */
private fun addCourseInfo(
    id: String,
    courseList: Element,
    courseStudentMap: MutableMap<String, String>,
    courses: MutableCollection<Course>
) {
    for (course in courseList.children()) {
        val courseRef = course.child(0).child(1).child(0)
        val courseId = courseRef.attr("href").substring(8)
        val courseName = courseRef.ownText()

        val components = courseName.split(":").toMutableList()
        val name = components[0]

        while (true) {
            var periods: Array<Int>
            var days: Array<Boolean>
            try {
                val courseScheduleData = components[1].trim()
                val periodData = courseScheduleData.substringBefore("(")
                val dayData = courseScheduleData.substringAfter("(").substringBefore(")")

                periods = parsePeriods(periodData)
                days = parseDays(dayData)
                components[1] = components[1]
                    .substringAfter(")")
                    .substringAfter(",", "")
            } catch (e: IndexOutOfBoundsException) {
                break
            } catch (e: NumberFormatException) {
                break
            }

            if (courseStudentMap[courseId] == null) {
                courseStudentMap[courseId] = ",\"$courseName\",$id"
            } else {
                courseStudentMap[courseId] += ",$id"
            }

            courses.add(Course(courseId, name, periods, days))
        }
    }
}

/**
 * Parses a list of mod boundaries
 *
 * @param periodData Data pertaining to the period
 * @return An array of mods during which the course takes place
 */
private fun parsePeriods(periodData: String): Array<Int> {
    return arrayOf(periodData.toInt())
}

/**
 * Parses a list of day boundaries
 *
 * @param dayData Data pertaining to the days
 * @return An array of booleans indicating on which days the course is held
 */
private fun parseDays(dayData: String): Array<Boolean> {
    val dateString = "ABCDE"
    val days = arrayOf(false, false, false, false, false)

    for (day in dayData.split(",")) {
        if (day.length == 1) {
            days[dateString.indexOf(day)] = true
        } else {
            val dayInterval = day.split("-")

            for (x in dateString.indexOf(dayInterval[0])..dateString.indexOf(dayInterval[1])) {
                days[x] = true
            }
        }
    }

    return days
}

/**
 * Prints out a student's schedule to studentdata.csv
 *
 * @param out Outputs to studentdata.csv
 * @param courses The student's courses
 */
private fun printPeriods(out: PrintWriter, courses: Collection<Course>) {
    for (x in 1..9) {
        out.print("$x,")

        val appendCourses = arrayOf("", "", "", "", "")
        for (course in courses) {
            if (course.periods.contains(x)) {
                for (y in 0..4) {
                    if (course.days[y]) {
                        appendCourses[y] = course.id
                    }
                }
            }
        }

        out.println(appendCourses.joinToString(","))
    }
}

/**
 * Gets a course's info
 *
 * @param id The student's ID
 * @param cookies Cookies associated with the current login
 * @return The course's information
 */
private fun getCourseInfo(id: String, cookies: Map<String, String>): Document? {
    val request = Jsoup
        .connect("https://bca.schoology.com/user/$id/courses/list")
        .method(Connection.Method.GET)
        .cookies(cookies)
        .ignoreHttpErrors(true)
        .data("destination", "${id.substring(1)}/info")
        .header("X-Drupal-Render-Mode", "json/popups")

    val response = request.execute()
    return when (response.statusCode()) {
        200, 429 -> parseCourseInfoResponse(request, response)
        else -> null
    }
}

/**
 * Parses the result of a course info request regarding a 200 or 429 HTML response
 *
 * @param request The connection on which the request was made
 * @param response The response of the original request
 * @return A parsed document containing course info
 */
private fun parseCourseInfoResponse(request: Connection, response: Connection.Response): Document? {
    try {
        return Jsoup.parse(
            Gson().fromJson(
                when (response.statusCode()) {
                    200 -> response.body()
                    429 -> {
                        Thread.sleep(250 + response.header("Retry-After").toLong())

                        request.execute().body()
                    }
                    else -> null
                }, JsonObject::class.java
            ).get("content").asString
        )
    } catch (e : Exception) {
        println(response.body())
        return null
    }
}

/**
 * Prints out a list of courses and their students to coursedata.csv
 *
 * @param courseStudentMap A map of courses and a comma separated list of its students
 */
private fun printCourseMembers(courseStudentMap: Map<String, String>) {
    PrintWriter(Path("coursedata.csv").bufferedWriter()).use {
        for (course in courseStudentMap) {
            it.println("${course.key}${course.value}")
        }
    }
}
