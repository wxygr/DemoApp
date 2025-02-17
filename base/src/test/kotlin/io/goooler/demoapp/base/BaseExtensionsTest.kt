package io.goooler.demoapp.base

import io.goooler.demoapp.base.util.times
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class BaseExtensionsTest {

  @Test
  fun `String times(Int)`() {
    assertEquals("1" * 3, "111")
    assertEquals("1" * 0, "")
    assertFailsWith<IllegalArgumentException> {
      "1" * -1
    }.message.let {
      assertEquals(it, "Param num should >= 0")
    }
  }
}
