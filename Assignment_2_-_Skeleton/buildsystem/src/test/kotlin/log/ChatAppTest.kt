import util.NetworkSimulator
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

  private fun ChatServer.assertLastLoggedMessage(message: String) {
    assertEquals(message, this.logger.lastLoggedMessage)
  }

  private fun ChatClient.assertLastLoggedMessage(message: String) {
    assertEquals(message, this.logger.lastLoggedMessage)
  }

  // ==============================================================================
  // Logging tests
  // ==============================================================================
  @Test
  fun testLogging() {
    val network = NetworkSimulator<Message>()
    val server = ChatServer(network)
    val client = ChatClient(network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    server.assertLastLoggedMessage("New client: ${client.networkAddress}")

    client.send("Hello world!")
    server.assertLastLoggedMessage("Broadcasting message from sender ${client.networkAddress}")
    client.assertLastLoggedMessage("Received message from sender ${client.networkAddress}")
    client.assertLastMessage(client.networkAddress, "Hello world!")
  }
}