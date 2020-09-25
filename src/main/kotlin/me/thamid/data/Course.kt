package me.thamid.data

/**
 * Parses a list of mod boundaries
 *
 * @param courseScheduleData Data pertaining to the course
 * @return An array of mods during which the course takes place
 */
private fun parseMods(courseScheduleData: List<String>): Array<Int> {
    val modInterval = courseScheduleData[0].split("-")
    val modList = if (modInterval.size == 1) listOf(modInterval[0].toInt()) else (modInterval[0].toInt()..modInterval[1].toInt()).toList()

    return modList.toTypedArray()
}

/**
 * Parses a list of day boundaries
 *
 * @param courseScheduleData Data pertaining to the course
 * @return An array of booleans indicating on which days the course is held
 */
private fun parseDays(courseScheduleData: List<String>): Array<Boolean> {
    val dateString = "ABCDE"
    val days = arrayOf(false, false, false, false, false)

    val dateIntervals = courseScheduleData[1]
    for (day in dateIntervals.split(",")) {
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
 * @param courseText The text to parse to create a course
 */
class Course(courseText: String) {

    /**
     * The name of the course
     */
    val name: String

    /**
     * The mods during which the course takes place
     */
    var mods: Array<Int>

    /**
     * The days during which the course takes place
     */
    var days: Array<Boolean>

    init {
        val components = courseText.split(":")
        name = components[0]

        try {
            val courseScheduleData = components[1].trim().substring(0, components[1].length - 2).split("(")

            mods = parseMods(courseScheduleData)
            days = parseDays(courseScheduleData)
        } catch (e: IndexOutOfBoundsException) {
            mods = emptyArray()
            days = arrayOf(false, false, false, false, false)
        } catch (e: NumberFormatException) {
            mods = emptyArray()
            days = arrayOf(false, false, false, false, false)
        }
    }

}