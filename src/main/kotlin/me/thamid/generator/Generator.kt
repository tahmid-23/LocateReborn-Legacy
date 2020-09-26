package me.thamid.generator

import com.google.gson.Gson
import com.google.gson.JsonObject
import me.thamid.generator.data.Course
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import kotlin.math.ceil

/**
 * Generates BCA Schoology course information
 */
fun generate() {
    val input = BufferedReader(InputStreamReader(System.`in`))

    File("studentdata.csv").printWriter().use {
        queryData(input, it)
        it.close()
    }
}

/**
 * Queries a username and password from the user and attempts to log in
 *
 * @param input Input reader of stdin
 * @param out Outputs to studentdata.csv
 */
private fun queryData(input: BufferedReader, out: PrintWriter) {
    println("Please enter your username.")
    val username = input.readLine()
    println("Please enter your password.")
    val password = input.readLine()

    login(username, password)?.let {
        printCourseData(input, out, it.cookies())
    }
}

/**
 * Logs into Schoology
 *
 * @param username Self-explanatory
 * @param password Self-explanatory
 * @return The result of the login
 */
private fun login(username: String, password: String): Connection.Response? {
    val connection = Jsoup.connect("https://bca.schoology.com/login/ldap?&school=11897239")
    connection.method(Connection.Method.POST)

    val params = mapOf("mail" to username, "pass" to password, "school_nid" to "11897239", "form_build_id" to "489d854-djh1fsa2DkrmcVrso_nzX105TaBTc4arsSHbbof2FZI", "form_id" to "s_user_login_form")
    connection.data(params)

    return connection.execute()
}

/**
 * Prints out all relevant data to studentdata.csv and coursedata.csv
 *
 * @param input Input reader of stdin
 * @param out Outputs to studentdata.csv
 * @param cookies Cookies associated with the current login
 */
private fun printCourseData(input: BufferedReader, out: PrintWriter, cookies: Map<String, String>) {
    val doc = Jsoup.connect("https://bca.schoology.com/group/2233228305/members").cookies(cookies).get()
    val pages = doc.getElementsByClass("total")

    if (pages.size > 0) {
        val courseStudentMap = mutableMapOf<String, String>()
        for (x in 1..ceil(pages[0].text().toDouble() / 30).toInt()) {

            buildStudentList(cookies, x)?.getElementsByAttributeValue("role", "presentation")?.first()?.child(0)?.let {
                printStudentCourses(it, cookies, out, courseStudentMap)
            }
        }
        printCourseMembers(courseStudentMap)
    } else {
        println("Invalid credentials!")
        queryData(input, out)
    }
}

/**
 * Builds the current list of students on a page
 *
 * @param cookies Cookies associated with the current login
 * @param page The current page number
 * @return The list of students
 */
private fun buildStudentList(cookies: Map<String, String>, page: Int): Document? {
    val request = Jsoup.connect("https://bca.schoology.com/enrollments/edit/members/group/2233228305/ajax")
            .method(Connection.Method.GET)
            .cookies(cookies)
            .ignoreHttpErrors(true)
            .data(mapOf("ss" to "", "p" to page.toString()))

    val response = request.execute()

    return when (response.statusCode()) {
        200 -> response.parse()
        429 -> {
            val timeout = response.header("Retry-After").toLong()
            Thread.sleep(1000 + timeout)

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
private fun printStudentCourses(studentList: Element, cookies: Map<String, String>, out: PrintWriter, courseStudentMap: MutableMap<String, String>) {
    for (student in studentList.children()) {
        val courseLink = student.child(1).child(0)
        val id = courseLink.attr("href").substring(6)
        getCourseInfo(id, cookies)?.getElementsByClass("my-courses-item-list")?.first()?.let {
            val name = courseLink.text()
            out.println("$id,$name")
            out.println("Mod, Monday, Tuesday, Wednesday, Thursday, Friday")

            val courses = mutableListOf<Course>()
            addCourseInfo(id, it, courseStudentMap, courses)
            printMods(out, courses)

            out.println()
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
private fun addCourseInfo(id: String, courseList: Element, courseStudentMap: MutableMap<String, String>, courses: MutableList<Course>) {
    for (course in courseList.children()) {
        val courseRef = course.child(0).child(1).child(0)
        val courseId = courseRef.attr("href").substring(8)
        val courseName = courseRef.ownText()
        val courseObject = Course(courseId, courseName)

        if (courseStudentMap[courseId] == null) courseStudentMap[courseId] = ",\"$courseName\",$id" else courseStudentMap[courseId] += ",$id"

        courses.add(courseObject)
    }
}

/**
 * Prints out a student's schedule to studentdata.csv
 *
 * @param out Outputs to studentdata.csv
 * @param courses The student's courses
 */
private fun printMods(out: PrintWriter, courses: MutableList<Course>) {
    for (x in 1..27) {
        out.print("$x,")

        val appendCourses = arrayOf("", "", "", "", "")
        for (course in courses) {
            if (course.mods.contains(x)) {
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
    val request = Jsoup.connect("https://bca.schoology.com/user/$id/courses/list")
            .method(Connection.Method.GET)
            .cookies(cookies)
            .ignoreHttpErrors(true)
            .data("destination", "${id.substring(1)}/info")
            .header("X-Drupal-Render-Mode", "json/popups")

    val response = request.execute()
    return if (response.statusCode() == 200 || response.statusCode() == 429) parseCourseInfoResponse(request, response) else null
}

/**
 * Parses the result of a course info request regarding a 200 or 429 HTML response
 *
 * @param request The connection on which the request was made
 * @param response The response of the original request
 * @return A parsed document containing course info
 */
private fun parseCourseInfoResponse(request: Connection, response: Connection.Response): Document? {
    return Jsoup.parse(Gson().fromJson(if (response.statusCode() == 200) {
        response.body()
    } else {
        Thread.sleep(1000 + response.header("Retry-After").toLong())

        request.execute().body()
    }, JsonObject::class.java).get("content").asString)
}

/**
 * Prints out a list of courses and their students to coursedata.csv
 *
 * @param courseStudentMap A map of courses and a comma separated list of its students
 */
private fun printCourseMembers(courseStudentMap: MutableMap<String, String>) {
    File("coursedata.csv").printWriter().use { it ->
        for (course in courseStudentMap) {
            it.println("${course.key}${course.value}")
        }
        it.close()
    }
}