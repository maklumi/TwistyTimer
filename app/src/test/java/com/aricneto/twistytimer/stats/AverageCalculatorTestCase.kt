package com.aricneto.twistytimer.stats

import com.aricneto.twistytimer.stats.AverageCalculator.AverageOfN
import com.aricneto.twistytimer.stats.AverageCalculator.Companion.DNF
import com.aricneto.twistytimer.stats.AverageCalculator.Companion.UNKNOWN
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Random

/**
 * Tests the [AverageCalculator] class. Averages are tests for 1, 3 and 5 solves. This covers
 * the trivial case (1), the case where a simple arithmetic mean is used (3) and the case where a
 * truncated arithmetic mean is used (5). Values above 5 should be no different. Each average is
 * tested for both DNF handling modes (automatic disqualification or not).
 *
 * @author damo
 */
class AverageCalculatorTestCase {
    @Test
    @Throws(Exception::class)
    fun testCreateOne() {
        val ac = AverageCalculator(1, 10)

        assertEquals(1, ac.n.toLong())
        assertEquals(0, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(UNKNOWN, ac.currentAverage)
        assertEquals(UNKNOWN, ac.bestAverage)

        assertEquals(UNKNOWN, ac.bestTime)
        assertEquals(UNKNOWN, ac.worstTime)
        assertEquals(UNKNOWN, ac.totalTime)
        assertEquals(UNKNOWN, ac.meanTime)
        assertEquals(UNKNOWN, ac.standardDeviation)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateThree() {
        val ac = AverageCalculator(3, 0)

        assertEquals(3, ac.n.toLong())
        assertEquals(0, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(UNKNOWN, ac.currentAverage)
        assertEquals(UNKNOWN, ac.bestAverage)

        assertEquals(UNKNOWN, ac.bestTime)
        assertEquals(UNKNOWN, ac.worstTime)
        assertEquals(UNKNOWN, ac.totalTime)
        assertEquals(UNKNOWN, ac.meanTime)
        assertEquals(UNKNOWN, ac.standardDeviation)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateFive() {
        val ac = AverageCalculator(5, 20)

        assertEquals(5, ac.n.toLong())
        assertEquals(0, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(UNKNOWN, ac.currentAverage)
        assertEquals(UNKNOWN, ac.bestAverage)

        assertEquals(UNKNOWN, ac.bestTime)
        assertEquals(UNKNOWN, ac.worstTime)
        assertEquals(UNKNOWN, ac.totalTime)
        assertEquals(UNKNOWN, ac.meanTime)
        assertEquals(UNKNOWN, ac.standardDeviation)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateFailure() {
        try {
            AverageCalculator(0, 0)
            fail("Expected an exception when 'n' is zero.")
        } catch (ignore: IllegalArgumentException) {
            // This is expected.
        } catch (e: Exception) {
            fail("Unexpected exception type: $e")
        }

        try {
            AverageCalculator(-1, 0)
            fail("Expected an exception when 'n' is negative.")
        } catch (ignore: IllegalArgumentException) {
            // This is expected.
        } catch (e: Exception) {
            fail("Unexpected exception type: $e")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testAddTime() {
        val ac = AverageCalculator(5, 20)

        // Initial state is already checked in other test methods.
        // Just test that the counters, sums, best, worst, etc. are updated.
        ac.addTime(DNF)
        assertEquals(1, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())
        assertEquals(UNKNOWN, ac.bestTime)
        assertEquals(UNKNOWN, ac.worstTime)
        assertEquals(UNKNOWN, ac.totalTime)
        assertEquals(UNKNOWN, ac.meanTime)
        assertEquals(UNKNOWN, ac.standardDeviation)

        ac.addTime(500)
        assertEquals(2, ac.numSolves.toLong())
        assertEquals(500, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(500, ac.totalTime)
        assertEquals(500, ac.meanTime)
        assertEquals(UNKNOWN, ac.standardDeviation)

        ac.addTime(300)
        assertEquals(3, ac.numSolves.toLong())
        assertEquals(300, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(800, ac.totalTime)
        assertEquals(400, ac.meanTime)
        assertEquals(UNKNOWN, ac.standardDeviation)

        // Standard deviation should only be calculated once valid (non-DNF) solves > 2
        ac.addTime(1000)
        assertEquals(4, ac.numSolves.toLong())
        assertEquals(300, ac.bestTime)
        assertEquals(1000, ac.worstTime)
        assertEquals(1800, ac.totalTime)
        assertEquals(600, ac.meanTime)
        assertEquals(360, ac.standardDeviation)

        ac.addTime(DNF)
        assertEquals(5, ac.numSolves.toLong())
        assertEquals(300, ac.bestTime)
        assertEquals(1000, ac.worstTime)
        assertEquals(1800, ac.totalTime)
        assertEquals(600, ac.meanTime)
        assertEquals(360, ac.standardDeviation)
    }

    @Test
    @Throws(Exception::class)
    fun testAddTimes() {
        val ac = AverageCalculator(5, 20)

        // Initial state is already checked in other test methods.
        // Just test that the counters, sums, best, worst, etc. are updated.
        ac.addTimes(*((null as LongArray?) ?: longArrayOf()))
        assertEquals("Null array of times should be ignored.", 0, ac.numSolves.toLong())

        ac.addTimes()
        assertEquals("Empty array of times should be ignored.", 0, ac.numSolves.toLong())

        ac.addTimes(DNF, 500, 300, DNF)
        assertEquals(4, ac.numSolves.toLong())
        assertEquals(2, ac.numDNFSolves.toLong())
        assertEquals(300, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(800, ac.totalTime)
        assertEquals(400, ac.meanTime)
        assertEquals(UNKNOWN, ac.standardDeviation)
    }

    /**
     * Tests the trivial edge case where "n" is one. DNFs cause disqualification of the average,
     * but that should make no difference when "n" is one.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    @Throws(Exception::class)
    fun testAverageOfOneDisqualifyDNFs() {
        val ac = AverageCalculator(1, 0)

        // Initial state is already checked in other test methods.
        ac.addTime(500)

        assertEquals(1, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(500, ac.currentAverage)
        assertEquals(500, ac.bestAverage)

        assertEquals(500, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(500, ac.totalTime)
        assertEquals(500, ac.meanTime)

        ac.addTime(300)

        assertEquals(2, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(300, ac.currentAverage)
        assertEquals(300, ac.bestAverage)

        assertEquals(300, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(800, ac.totalTime)
        assertEquals(400, ac.meanTime)

        ac.addTime(DNF)
        assertEquals(3, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())

        assertEquals(DNF, ac.currentAverage)
        assertEquals(300, ac.bestAverage)

        assertEquals(300, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(800, ac.totalTime)
        assertEquals(400, ac.meanTime)

        ac.addTime(1000)
        assertEquals(4, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())

        assertEquals(1000, ac.currentAverage)
        assertEquals(300, ac.bestAverage)

        assertEquals(300, ac.bestTime)
        assertEquals(1000, ac.worstTime)
        assertEquals(1800, ac.totalTime)
        assertEquals(600, ac.meanTime)
    }

    /**
     * Tests the trivial edge case where "n" is one. DNFs do not cause disqualification of the
     * average, but that should make no difference when "n" is one.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    @Throws(Exception::class)
    fun testAverageOfOneAllowDNFs() {
        val ac = AverageCalculator(1, 0)

        // Initial state is already checked in other test methods.
        ac.addTime(500)

        assertEquals(1, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(500, ac.currentAverage)
        assertEquals(500, ac.bestAverage)

        assertEquals(500, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(500, ac.totalTime)
        assertEquals(500, ac.meanTime)

        ac.addTime(300)

        assertEquals(2, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(300, ac.currentAverage)
        assertEquals(300, ac.bestAverage)

        assertEquals(300, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(800, ac.totalTime)
        assertEquals(400, ac.meanTime)

        ac.addTime(DNF)
        assertEquals(3, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())

        assertEquals(DNF, ac.currentAverage)
        assertEquals(300, ac.bestAverage)

        assertEquals(300, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(800, ac.totalTime)
        assertEquals(400, ac.meanTime)

        ac.addTime(1000)
        assertEquals(4, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())

        assertEquals(1000, ac.currentAverage)
        assertEquals(300, ac.bestAverage)

        assertEquals(300, ac.bestTime)
        assertEquals(1000, ac.worstTime)
        assertEquals(1800, ac.totalTime)
        assertEquals(600, ac.meanTime)
    }

    /**
     * Tests the calculation of the average of three. For an average of three, a truncated mean
     * should not be calculated. Any DNF should cause disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    @Throws(Exception::class)
    fun testAverageOfThreeDisqualifyDNFs() {
        val ac = AverageCalculator(3, 0)

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150)

        assertEquals(3, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(300, ac.currentAverage)
        assertEquals(300, ac.bestAverage)

        assertEquals(150, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(900, ac.totalTime)
        assertEquals(300, ac.meanTime)
        assertEquals(180, ac.standardDeviation)

        ac.addTimes(DNF, 800)

        assertEquals(5, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())

        assertEquals(DNF, ac.currentAverage)
        assertEquals(300, ac.bestAverage)

        assertEquals(150, ac.bestTime)
        assertEquals(800, ac.worstTime)
        assertEquals(1700, ac.totalTime)
        assertEquals(425, ac.meanTime) // 1700 / 4 non-DNF solves.
        assertEquals(290, ac.standardDeviation)

        ac.addTimes(100)

        assertEquals(6, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())

        assertEquals(DNF, ac.currentAverage)
        assertEquals(300, ac.bestAverage)

        assertEquals(100, ac.bestTime)
        assertEquals(800, ac.worstTime)
        assertEquals(1800, ac.totalTime)
        assertEquals(360, ac.meanTime) // 1800 / 5 non-DNF solves.
        assertEquals(290, ac.standardDeviation)

        // Third non-DNF time in a row should push change the current average to a non-DNF average.
        ac.addTimes(900)

        assertEquals(7, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())

        assertEquals(600, ac.currentAverage) // Last three were 800, 100, 900.
        assertEquals(300, ac.bestAverage)

        assertEquals(100, ac.bestTime)
        assertEquals(900, ac.worstTime)
        assertEquals(2700, ac.totalTime)
        assertEquals(450, ac.meanTime) // 2700 / 6 non-DNF solves.
        assertEquals(340, ac.standardDeviation)

        ac.addTimes(DNF)

        assertEquals(8, ac.numSolves.toLong())
        assertEquals(2, ac.numDNFSolves.toLong())

        assertEquals(DNF, ac.currentAverage) // Last three were 100, 900, DNF.
        assertEquals(300, ac.bestAverage)

        assertEquals(100, ac.bestTime)
        assertEquals(900, ac.worstTime)
        assertEquals(2700, ac.totalTime)
        assertEquals(450, ac.meanTime) // 2700 / 6 non-DNF solves.
        assertEquals(340, ac.standardDeviation)

        // Set a new record for the average time.
        ac.addTimes(90, 210, 300)

        assertEquals(11, ac.numSolves.toLong())
        assertEquals(2, ac.numDNFSolves.toLong())

        assertEquals(200, ac.currentAverage)
        assertEquals(200, ac.bestAverage)

        assertEquals(90, ac.bestTime)
        assertEquals(900, ac.worstTime)
        assertEquals(3300, ac.totalTime)
        assertEquals(366, ac.meanTime) // 3300 / 9 non-DNF solves. 366.6666... is truncated.
        assertEquals(301, ac.standardDeviation)
    }

    /**
     * Tests the calculation of the average of five. For an average of five, a truncated mean
     * should be calculated. Any DNF should cause disqualification of the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    @Throws(Exception::class)
    fun testAverageOfFiveDisqualifyDNFs() {
        val ac = AverageCalculator(5, 20)

        // Initial state is already checked in other test methods.
        ac.addTimes(500, 250, 150, 400, 200)

        assertEquals(5, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(283, ac.currentAverage) // (250+400+200) / 3. Exclude 150, 500.
        assertEquals(283, ac.bestAverage)

        assertEquals(150, ac.bestTime)
        assertEquals(500, ac.worstTime)
        assertEquals(1500, ac.totalTime)
        assertEquals(300, ac.meanTime)

        // One DNF should be tolerated and treated as the worst time when calculating the average.
        // (It is not the worst time reported, though, as that is always a non-DNF time.)
        ac.addTimes(DNF, 800) // Current: 150, 400, 200, DNF, 800

        assertEquals(7, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())

        assertEquals(466, ac.currentAverage) // (400+200+800) / 3. Exclude 150, DNF.
        assertEquals(283, ac.bestAverage)

        assertEquals(150, ac.bestTime)
        assertEquals(800, ac.worstTime)
        assertEquals(2300, ac.totalTime)
        assertEquals(383, ac.meanTime) // 2300 / 6 non-DNF solves.

        ac.addTimes(300) // Current: 400, 200, DNF, 800, 300

        assertEquals(8, ac.numSolves.toLong())
        assertEquals(1, ac.numDNFSolves.toLong())

        assertEquals(500, ac.currentAverage) // (400+800+300) / 3. Exclude 200, DNF.
        assertEquals(283, ac.bestAverage)

        assertEquals(150, ac.bestTime)
        assertEquals(800, ac.worstTime)
        assertEquals(2600, ac.totalTime)
        assertEquals(371, ac.meanTime) // 2600 / 7 non-DNF solves.

        // Second DNF in "current" 5 times. Result should be disqualified.
        ac.addTimes(DNF) // Current: 200, DNF, 800, 300, DNF

        assertEquals(9, ac.numSolves.toLong())
        assertEquals(2, ac.numDNFSolves.toLong())

        assertEquals(DNF, ac.currentAverage) // More than one DNF.
        assertEquals(283, ac.bestAverage)

        assertEquals(150, ac.bestTime)
        assertEquals(800, ac.worstTime)
        assertEquals(2600, ac.totalTime)
        assertEquals(371, ac.meanTime) // 2600 / 7 non-DNF solves.

        // Test the "reset()" method, too.
        ac.reset()

        assertEquals(5, ac.n.toLong()) // Should not be changed by a reset.
        assertEquals(0, ac.numSolves.toLong())
        assertEquals(0, ac.numDNFSolves.toLong())

        assertEquals(UNKNOWN, ac.currentAverage)
        assertEquals(UNKNOWN, ac.bestAverage)

        assertEquals(UNKNOWN, ac.bestTime)
        assertEquals(UNKNOWN, ac.worstTime)
        assertEquals(UNKNOWN, ac.totalTime)
        assertEquals(UNKNOWN, ac.meanTime)
    }

    /**
     * Tests the [AverageOfN] class to ensure it presents the times in the correct order
     * and identifies the best and worst times correctly. This test covers the case where best
     * and worst times should not be identified because the value of "N" is low. DNFs disqualify
     * the average.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    @Throws(Exception::class)
    fun testAverageOfNDetailsForThreeDisqualifyDNFs() {
        val ac = AverageCalculator(3, 0)

        // Add less than the minimum required number of times. Average cannot be calculated.
        ac.addTimes(500, 250)
        var aoN = ac.averageOfN

        assertTrue(aoN.times.isEmpty())
        assertEquals(UNKNOWN, aoN.average)
        assertEquals(-1, aoN.bestTimeIndex.toLong())
        assertEquals(-1, aoN.worstTimeIndex.toLong())

        // Complete the first three times. Average can now be calculated.
        ac.addTime(150)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(500, 250, 150), aoN.times) // mNext == 3
        assertEquals(300, aoN.average)
        assertEquals(-1, aoN.bestTimeIndex.toLong())  // No elimination of best time for N=3.
        assertEquals(-1, aoN.worstTimeIndex.toLong()) // No elimination of worst time for N=3.

        // 1 DNF disqualifies the result.
        ac.addTime(DNF)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(250, 150, DNF), aoN.times) // mNext == 1
        assertEquals(DNF, aoN.average)
        assertEquals(-1, aoN.bestTimeIndex.toLong())  // No elimination of best time for N=3.
        assertEquals(-1, aoN.worstTimeIndex.toLong()) // No elimination of worst time for N=3.

        // 2 DNFs disqualify the result.
        ac.addTime(DNF)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(150, DNF, DNF), aoN.times) // mNext == 2
        assertEquals(DNF, aoN.average)
        assertEquals(-1, aoN.bestTimeIndex.toLong())  // No elimination of best time for N=3.
        assertEquals(-1, aoN.worstTimeIndex.toLong()) // No elimination of worst time for N=3.

        // 3 DNFs disqualify the result.
        ac.addTime(DNF)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(DNF, DNF, DNF), aoN.times) // mNext == 3
        assertEquals(DNF, aoN.average)
        assertEquals(-1, aoN.bestTimeIndex.toLong())  // No elimination of best time for N=3.
        assertEquals(-1, aoN.worstTimeIndex.toLong()) // No elimination of worst time for N=3.

        // No DNFs and the result is valid again.
        ac.addTimes(100, 200, 600)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(100, 200, 600), aoN.times) // mNext == 3
        assertEquals(300, aoN.average)
        assertEquals(-1, aoN.bestTimeIndex.toLong())  // No elimination of best time for N=3.
        assertEquals(-1, aoN.worstTimeIndex.toLong()) // No elimination of worst time for N=3.
    }


    /**
     * Tests the [AverageOfN] class to ensure it presents the times in the correct order
     * and identifies the best and worst times correctly. This test covers the case where best
     * and worst times must be identified because the value of "N" is high enough to trigger the
     * calculation of a truncated mean. More than one DNF disqualifies the average. The "best" and
     * worst times are identified in the same manner as when DNFs do not cause disqualifications,
     * even where the average is disqualified.
     *
     * @throws Exception If the test fails to run.
     */
    @Test
    @Throws(Exception::class)
    fun testAverageOfNDetailsForFiveDisqualifyDNFs() {
        val ac = AverageCalculator(5, 20)

        // Add less than the minimum required number of times. Average cannot be calculated.
        ac.addTimes(500, 150, 250, 600)
        var aoN = ac.averageOfN

        assertTrue(aoN.times.isEmpty())
        assertEquals(UNKNOWN, aoN.average)
        assertEquals(-1, aoN.bestTimeIndex.toLong())
        assertEquals(-1, aoN.worstTimeIndex.toLong())

        // Complete the first five times. Average can now be calculated.
        ac.addTime(350)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(500, 150, 250, 600, 350), aoN.times) // mNext == 5
        assertEquals(366, aoN.average) // Mean of 500+250+350. 150 and 600 are eliminated.
        assertEquals(1, aoN.bestTimeIndex.toLong())  // 150
        assertEquals(3, aoN.worstTimeIndex.toLong()) // 600

        // 1 DNF does not disqualify the result. DNF becomes the "worst" time.
        ac.addTime(DNF)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(150, 250, 600, 350, DNF), aoN.times) // mNext == 1
        assertEquals(400, aoN.average) // Mean of 250+600+350. 150 and DNF are eliminated.
        assertEquals(0, aoN.bestTimeIndex.toLong())  // 150
        assertEquals(4, aoN.worstTimeIndex.toLong()) // DNF

        // 2 DNFs disqualify the result.
        ac.addTime(DNF)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(250, 600, 350, DNF, DNF), aoN.times) // mNext == 2
        assertEquals(DNF, aoN.average)
        assertEquals(0, aoN.bestTimeIndex.toLong())  // 250
        assertEquals(3, aoN.worstTimeIndex.toLong()) // First DNF

        // 3 DNFs disqualify the result.
        ac.addTime(DNF)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(600, 350, DNF, DNF, DNF), aoN.times) // mNext == 3
        assertEquals(DNF, aoN.average)
        assertEquals(1, aoN.bestTimeIndex.toLong())  // 350
        assertEquals(2, aoN.worstTimeIndex.toLong()) // First DNF

        // 4 DNFs disqualify the result, and no best time will be identified.
        ac.addTime(DNF)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(350, DNF, DNF, DNF, DNF), aoN.times) // mNext == 4
        assertEquals(DNF, aoN.average)
        assertEquals(-1, aoN.bestTimeIndex.toLong()) // No identification of the only non-DNF time.
        assertEquals(1, aoN.worstTimeIndex.toLong()) // First DNF

        // 5 DNFs disqualify the result. No eliminations.
        ac.addTime(DNF)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(DNF, DNF, DNF, DNF, DNF), aoN.times) // mNext == 5
        assertEquals(DNF, aoN.average) // Average is disqualified.
        assertEquals(-1, aoN.bestTimeIndex.toLong())
        assertEquals(-1, aoN.worstTimeIndex.toLong())

        // Where all times are the same, the best and worst eliminations must not be the at the
        // same index. Expect the best to identified first and the worst second.
        ac.addTimes(100, 100, 100, 100, 100)
        aoN = ac.averageOfN

        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(100, 100, 100, 100, 100), aoN.times)
        assertEquals(100, aoN.average)
        assertEquals(0, aoN.bestTimeIndex.toLong())  // First time is "best".
        assertEquals(1, aoN.worstTimeIndex.toLong()) // Next time is "worst".
    }

    @Test
    @Throws(Exception::class)
    fun testAssortedAverageOfHundredCalculations() {
        val ac = AverageCalculator(100, 10)

        // Add less than the minimum required number of times. Average cannot be calculated.
        ac.addTimes(4106, 6118, 7594, 5829, 4544, 8091, 3661, 7461, 9127, 4649, 6289, 5559, 4911, 2778, 3690, 7496, 6042, 4077, 6107, 9062, 8412, 7027, 2096, 8129, 7225, 7774, 9106, 8608, 2910, 3268, 6670, 9233, 5427, 4260, 2510, 4902, 2842, 1386, 9144, 2883, 6522, 1609, 3463, 7368, 2041, 8208, 5573, 2489, 3846, 9254, 4228, 6624, 6334, 2105, 7787, 2415, 7456, 8613, 3591, 7811, 2873, 4597, 7600, 3831, 1202, 8955, 3150, 1163, 7869, 4891, 3445, 4593, 1901, 8897, 9896, 8528, 4485, 9777, 5749, 8382, 3508, 1614, 6138, 2029, 4201, 4872, 7465, 8650, 8114, 6320, 7545, 9130, 3409, 1914, 7372, 4515, 5001, 3146, 7509)
        var aoN = ac.averageOfN

        assertTrue(aoN.times.isEmpty())
        assertEquals(UNKNOWN, aoN.average)
        assertEquals(-1, aoN.bestTimeIndex.toLong())
        assertEquals(-1, aoN.worstTimeIndex.toLong())

        ac.addTime(1007)
        aoN = ac.averageOfN
        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(4106, 6118, 7594, 5829, 4544, 8091, 3661, 7461, 9127, 4649, 6289, 5559, 4911, 2778, 3690, 7496, 6042, 4077, 6107, 9062, 8412, 7027, 2096, 8129, 7225, 7774, 9106, 8608, 2910, 3268, 6670, 9233, 5427, 4260, 2510, 4902, 2842, 1386, 9144, 2883, 6522, 1609, 3463, 7368, 2041, 8208, 5573, 2489, 3846, 9254, 4228, 6624, 6334, 2105, 7787, 2415, 7456, 8613, 3591, 7811, 2873, 4597, 7600, 3831, 1202, 8955, 3150, 1163, 7869, 4891, 3445, 4593, 1901, 8897, 9896, 8528, 4485, 9777, 5749, 8382, 3508, 1614, 6138, 2029, 4201, 4872, 7465, 8650, 8114, 6320, 7545, 9130, 3409, 1914, 7372, 4515, 5001, 3146, 7509, 1007), aoN.times) // mNext == 5
        assertEquals(5562, aoN.average)

        ac.addTime(9874) // Will eject 4106
        aoN = ac.averageOfN
        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(6118, 7594, 5829, 4544, 8091, 3661, 7461, 9127, 4649, 6289, 5559, 4911, 2778, 3690, 7496, 6042, 4077, 6107, 9062, 8412, 7027, 2096, 8129, 7225, 7774, 9106, 8608, 2910, 3268, 6670, 9233, 5427, 4260, 2510, 4902, 2842, 1386, 9144, 2883, 6522, 1609, 3463, 7368, 2041, 8208, 5573, 2489, 3846, 9254, 4228, 6624, 6334, 2105, 7787, 2415, 7456, 8613, 3591, 7811, 2873, 4597, 7600, 3831, 1202, 8955, 3150, 1163, 7869, 4891, 3445, 4593, 1901, 8897, 9896, 8528, 4485, 9777, 5749, 8382, 3508, 1614, 6138, 2029, 4201, 4872, 7465, 8650, 8114, 6320, 7545, 9130, 3409, 1914, 7372, 4515, 5001, 3146, 7509, 1007, 9874), aoN.times) // mNext == 5
        assertEquals(15866, aoN.getmLowerTrimSum())
        assertEquals(93603, aoN.getmUpperTrimSum())
        assertEquals(559351 - (aoN.getmLowerTrimSum() + aoN.getmUpperTrimSum()), aoN.getmMiddleTrimSum())
        assertEquals(5623, aoN.average)

        ac.addTimes(1678, 6298, 6) // Will eject 6118, 7594, 5829
        aoN = ac.averageOfN
        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertArrayEquals(longArrayOf(4544, 8091, 3661, 7461, 9127, 4649, 6289, 5559, 4911, 2778, 3690, 7496, 6042, 4077, 6107, 9062, 8412, 7027, 2096, 8129, 7225, 7774, 9106, 8608, 2910, 3268, 6670, 9233, 5427, 4260, 2510, 4902, 2842, 1386, 9144, 2883, 6522, 1609, 3463, 7368, 2041, 8208, 5573, 2489, 3846, 9254, 4228, 6624, 6334, 2105, 7787, 2415, 7456, 8613, 3591, 7811, 2873, 4597, 7600, 3831, 1202, 8955, 3150, 1163, 7869, 4891, 3445, 4593, 1901, 8897, 9896, 8528, 4485, 9777, 5749, 8382, 3508, 1614, 6138, 2029, 4201, 4872, 7465, 8650, 8114, 6320, 7545, 9130, 3409, 1914, 7372, 4515, 5001, 3146, 7509, 1007, 9874, 1678, 6298, 6), aoN.times) // mNext == 5
        assertEquals(13480, aoN.getmLowerTrimSum())
        assertEquals(93603, aoN.getmUpperTrimSum())
        assertEquals(547792 - (aoN.getmLowerTrimSum() + aoN.getmUpperTrimSum()), aoN.getmMiddleTrimSum())
        assertEquals(5508, aoN.average)

    }


    @Test
    @Throws(Exception::class)
    fun testLargeAverage() {
        val ac = AverageCalculator(50, 5)

        ac.addTimes(89950, 95540, 95990, 72580, 74560, 92800, 92420, 83900, 98010, 89740, 95070, 82480, 99060, 81910, 88290, 72620, 115280, 96510, 79570, 79860, 65980, 79430, 96970, 89840, 85730, 74930, 77310, 91310, 91990, 97730, 74350, 66290, 64820, 78960, 73680, 86090, 95390, 75620, 86390, 79930, 89150, 88090, 86570, 73630, 99780, 91050, 88750, 89740, 84670, 92950, 86830, 78630, 81930, 86170, 79480, 87630, 79190, 90680, 77230, 80220, 77070, 79360, 83350, 100290, 103240, 80990, 84190, 75990, 86490, 77310, 87960, 72250, 84340, 82670, 92400, 97220, 85430, 87780, 85710, 94650, 94970, 80740, 89290, 75110, 95410, 111380, 96660, 74710, 73920, 90590, 95820, 103260, 92030, 87790, 95400, 99080, 80910, 90120, 74520, 89840, 96060, 74730, 66320, 88930, 73740, 84870, 95960, 105230, 80370, 80960, 77450, 103350, 86730, 106070, 85510, 72120, 106750, 84940, 120410, 97030, 83840, 94900, 108510, 87870, 71520, 82570, 88600, 101390, 86790, 84490, 93170, 93940, 102440, 99150, 81370, 85580, 87860, 94980, 98780, 81850, 82610, 78670, 84810, 89350, 119210, 76550, 89270, 98520, 72340, 99700, 83060, 70070, 120210, 78450, 74580, 84860, 88730, 84120, 100840, 98040, 88520, 106250, 95910, 90040, 92360, 83390, 88580, 81240, 70700, 103160, 94160, 107270, 82590, 79360, 101450, 92420, 114950, 83970, 95780, 102550, 98690, 73930, 74890, 85190, 83980, 72290, 102640, 77430, 104500, 130680, 93820, 89570, 102470, 93500, 90470, 113360, 93550, 99450, 155980, 121440, 138660, 113600, 86400, 96320, 101420, 106970, 116600, 109140, 120990, 144260, 84500, 92430, 115610, 104720, 116010, 170760, 106910, 118350, 115150, 123530, 94250, 116800, 83410, 90030, 119140, 86440, 171490, 176300, 99300, 113650, 123400, 123400, 110880, 124790, 127890, 125120, 109420, 119890, 157070, 108740, 144950, 130470, 127060, 103270, 102450, 124820, 92750, 99990, 104990, 123780, 128360, 95250, 112700, 99530, 98620, 116720, 150670, 107740, 101990, 144910, 118340, 134440, 112190, 103280, 121440, 114720, 134100, 106880, 113970, 113160, 104740, 73880, 95690, 85970, 100150, 102480, 96730, 67030, 84900, 86000, 71500, 88150, 99320, 92850, 79970, 103730, 104490, 77180, 106040, 115300, 142720, 88490, 77750, 89450, 77590, 170660, 80350, 88340, 88030, 102580, 97660, 88600, 73960, 84560, 84880, 84840, 74140, 98020, 81770, 95600)
        val aoN = ac.averageOfN
        assertEquals(ac.n.toLong(), aoN.times.size.toLong())
        assertEquals(83675, ac.bestAverage)

        ac.addTimes(DNF, DNF, DNF) // The DNF threshold is 5% of N. With 3 DNFs, we should still be able to calculate a valid average
        assertEquals(3, ac.numDNFSolves.toLong())
        assertEquals(102410, ac.currentAverage)

        ac.addTime(DNF) // 6 DNFs should disqualify the average.
        assertEquals(4, ac.numDNFSolves.toLong())
        assertEquals(DNF, ac.currentAverage)

    }

    @Test
    @Throws(Exception::class)
    fun testAoFiveOverflow() {
        val ac = AverageCalculator(5, 5)
        var aoN: AverageOfN

        ac.addTimes(*Random(1).longs(3000000, 299995, 300000).toArray())
        assertTrue(ac.bestAverage >= 250000)

        ac.addTimes(8, 10, 4, 5, 6, 3)
        assertTrue(ac.bestAverage > 0)
    }

    /**
     * Tests all possible tree swap operations
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testTreeSwap() {
        val ac = AverageCalculator(12, 20)
        var aoN: AverageOfN

        ac.addTimes(10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000, 100000, 110000, 120000)
        aoN = ac.averageOfN
        assertEquals(65000, aoN.average)

        // Eject lower, add lower
        ac.addTime(10000)
        aoN = ac.averageOfN
        assertEquals(65000, aoN.average)

        // Eject lower, add middle
        ac.addTime(60000)
        aoN = ac.averageOfN
        assertEquals(68333, aoN.average)

        // Eject lower, add top
        ac.addTime(120000)
        aoN = ac.averageOfN
        assertEquals(76666, aoN.average)


        val ac2 = AverageCalculator(12, 20)

        ac2.addTimes(120000, 110000, 100000, 90000, 80000, 70000, 60000, 50000, 40000, 30000, 20000, 10000)
        aoN = ac2.averageOfN
        assertEquals(65000, aoN.average)

        // Eject upper, add lower
        ac2.addTimes(10000)
        aoN = ac2.averageOfN
        assertEquals(55000, aoN.average)

        // Eject upper, add middle
        ac2.addTimes(60000)
        aoN = ac2.averageOfN
        assertEquals(51666, aoN.average)

        // Eject upper, add upper
        ac2.addTimes(120000)
        aoN = ac2.averageOfN
        assertEquals(51666, aoN.average)

        val ac3 = AverageCalculator(12, 20)

        ac3.addTimes(90000, 80000, 70000, 60000, 50000, 40000, 30000, 20000, 10000, 120000, 110000, 100000)
        aoN = ac3.averageOfN
        assertEquals(65000, aoN.average)

        // Eject middle, add lower
        ac3.addTimes(10000)
        aoN = ac3.averageOfN
        assertEquals(55000, aoN.average)

        // Eject middle, add middle
        ac3.addTimes(60000)
        aoN = ac3.averageOfN
        assertEquals(51666, aoN.average)

        // Eject middle, add upper
        ac3.addTimes(120000)
        aoN = ac3.averageOfN
        assertEquals(56666, aoN.average)
    }

    private var rand = Random(0)
    private var mLargeTestTimes = rand.longs(1000000, 25000, 30000).toArray()

    /**
     * Used to test the efficiency of the algorithm only.
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testVeryLargeAverage() {
        val ac = AverageCalculator(1000, 5)

        ac.addTimes(*mLargeTestTimes)
    }

}
