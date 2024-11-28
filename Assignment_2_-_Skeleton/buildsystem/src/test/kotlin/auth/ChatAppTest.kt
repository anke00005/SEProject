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

  @Test
  fun testAuthenticatedUserMessageAccept() {
    val network = NetworkSimulator<Message>()
    val server = ChatServer(network, mutableMapOf("user2" to "securePassword"))
    val client = ChatClient(network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.authenticate("user2", "securePassword")
    client.send("HelloWorld!")
    client.assertLastMessage(client.networkAddress, "HelloWorld!")
  }
}