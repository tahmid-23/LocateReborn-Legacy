package com.github.thamid_gamer.locatereborn.generator.data

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
 * A BCA course
 *
 * @constructor Creates a BCA course object representation
 * @param id The id of the course
 * @param courseText The text to parse to create a course
 */
class Course(val id: String, courseText: String) {

    /**
     * The name of the course
     */
    val name: String

    /**
     * The periods during which the course takes place
     */
    val periods: Array<Int>

    /**
     * The days during which the course takes place
     */
    val days: Array<Boolean>

    init {
        val components = courseText.split(":")
        name = components[0]

        var periods: Array<Int>
        var days: Array<Boolean>
        try {
            val courseScheduleData = components[1].trim()
            val periodData = courseScheduleData.substringBefore("(")
            val dayData = courseScheduleData.substringAfter("(").substringBefore(")")

            periods = parsePeriods(periodData)
            days = parseDays(dayData)
        } catch (e: IndexOutOfBoundsException) {
            periods = emptyArray()
            days = arrayOf(false, false, false, false, false)
        } catch (e: NumberFormatException) {
            periods = emptyArray()
            days = arrayOf(false, false, false, false, false)
        }

        this.periods = periods
        this.days = days
    }

}
