import util.NetworkSimulator
import util.View
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatAppTest {
  private fun ChatClient.assertLastMessage(
    expectedSender: Int,
    expectedText: String
  ) {
    val actualMessage = view.lastDisplayedMessage
    assertEquals("[$expectedSender] $expectedText", actualMessage)
  }

  private fun ChatClient.assertLastMessage(
    expectedSender: Int,
    expectedText: String,
    expectedColor: String
  ) {
    val actualMessage = view.lastDisplayedMessage
    assertEquals(
      "${View.RESET}$expectedColor[$expectedSender] $expectedText${View.RESET}",
      actualMessage
    )
  }

  // ==============================================================================
  // Color tests
  // ==============================================================================
  @Test
  fun testDisplaysColoredMessage() {
    val network = NetworkSimulator<Message>()
    val server = ChatServer(network)
    val client = ChatClient(network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!", View.RED)
    client.assertLastMessage(client.networkAddress, "HelloWorld!", View.RED)
  }
}