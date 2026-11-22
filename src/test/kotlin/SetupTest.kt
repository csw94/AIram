import kotlin.test.Test
import kotlin.test.assertEquals

// Gradle + 테스트 환경이 제대로 동작하는지 확인하는 테스트
class SetupTest {
    @Test
    fun `테스트 환경이 동작한다`() {
        assertEquals(2, 1 + 1)
    }
}
