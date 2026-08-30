package com.aricneto.twistytimer.puzzle

import org.worldcubeassociation.tnoodle.puzzle.CubePuzzle

/**
 * The TNoodle library only provides default functions up to 4x4 cube puzzles.
 * This class extends the CubePuzzle class by modifying the constructor to allow
 * a custom-sized cube.
 */
class NbyNCubePuzzle(size: Int) : CubePuzzle(size)
