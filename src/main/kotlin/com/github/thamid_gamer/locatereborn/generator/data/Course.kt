package com.github.thamid_gamer.locatereborn.generator.data

/**
 * A BCA course
 *
 * @constructor Creates a BCA course object representation
 * @param id The id of the course
 * @param name The name of the course
 * @param periods The periods during which the course takes place
 * @param days The days during which the course takes place
 */
class Course(val id: String, val name: String, val periods: Array<Int>, val days: Array<Boolean>) {

}
