package tasks

import contributors.MockGithubService
import contributors.expectedResults
import contributors.testRequestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
class Request4SuspendKtTest {
    @Test
    fun testSuspend() = runBlockingTest {
        val startTime = System.currentTimeMillis()
        val result = loadContributorsSuspend(MockGithubService, testRequestData)
        Assert.assertEquals("Wrong result for 'loadContributorsSuspend'", expectedResults.users, result)
        val totalTime = System.currentTimeMillis() - startTime
        Assert.assertEquals(
            "The calls run consequently, so the total virtual time should be 4000 ms: " +
                    "1000 for repos request plus (1000 + 1200 + 800) = 3000 for sequential contributors requests)",
            expectedResults.timeFromStart, totalTime
        )
//        Assert.assertTrue(
//            "The calls run consequently, so the total time should be around 4000 ms: " +
//                    "1000 for repos request plus (1000 + 1200 + 800) = 3000 for sequential contributors requests)",
//            totalTime in expectedResults.timeFromStart..(expectedResults.timeFromStart + 500)
//        )
    }
}