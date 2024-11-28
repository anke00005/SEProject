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

  //==============================================================================
  // Basic Tests
  //==============================================================================
  @Test
  fun testDisplaysSentMessage() {
    val network = NetworkSimulator<Message>()
    val server = ChatServer(network)
    val client1 = ChatClient(network)
    val client2 = ChatClient(network)

    network.register(server)
    network.register(client1)
    network.register(client2)

    client1.connect(server.networkAddress)
    client2.connect(server.networkAddress)
    client1.send("HelloWorld!")
    client1.assertLastMessage(client1.networkAddress, "HelloWorld!")
    client2.assertLastMessage(client1.networkAddress, "HelloWorld!")
  }
}