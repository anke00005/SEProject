import org.junit.jupiter.api.assertThrows
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

  private fun ChatServer.assertLastLoggedMessage(message: String) {
    assertEquals(message, this.logger.lastLoggedMessage)
  }

  private fun ChatClient.assertLastLoggedMessage(message: String) {
    assertEquals(message, this.logger.lastLoggedMessage)
  }

  //==============================================================================
  // Basic Tests
  //==============================================================================
  @Test
  fun testDisplaysSentMessage() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(false, false, null, false)
    val server = ChatServer(config, network)
    val client1 = ChatClient(config, network)
    val client2 = ChatClient(config, network)

    network.register(server)
    network.register(client1)
    network.register(client2)

    client1.connect(server.networkAddress)
    client2.connect(server.networkAddress)
    client1.send("HelloWorld!")
    client1.assertLastMessage(client1.networkAddress, "HelloWorld!")
    client2.assertLastMessage(client1.networkAddress, "HelloWorld!")
  }

  @Test
  fun testMultipleServers() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(false, false, null, false)
    val server1 = ChatServer(config, network)
    val server2 = ChatServer(config, network)
    val client1 = ChatClient(config, network)
    val client2 = ChatClient(config, network)

    network.register(server1)
    network.register(server2)
    network.register(client1)
    network.register(client2)

    client1.connect(server1.networkAddress)
    client2.connect(server2.networkAddress)
    client1.send("HelloWorld!")
    client1.assertLastMessage(client1.networkAddress, "HelloWorld!")
    assertEquals("", client2.view.lastDisplayedMessage)

    client2.send("42")
    client1.assertLastMessage(client1.networkAddress, "HelloWorld!")
    client2.assertLastMessage(client2.networkAddress, "42")
  }

  // ==============================================================================
  // Authentication tests
  // ==============================================================================
  @Test
  fun testAuthenticatedUserMessageAccept() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, false, null, false)
    val server =
      ChatServer(config, network, mutableMapOf("user2" to "securePassword"))
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.authenticate("user2", "securePassword")
    client.send("HelloWorld!")
    client.assertLastMessage(client.networkAddress, "HelloWorld!")
  }

  @Test
  fun testUnregisteredUserAuthenticationFail() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, false, null, false)
    val server =
      ChatServer(config, network, mutableMapOf("user2" to "securePassword"))
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.authenticate("user1", "securePassword")
    client.assertLastMessage(server.networkAddress, "Authentication failed.")
  }

  @Test
  fun testSameUserMultipleClients() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, false, null, false)
    val server =
      ChatServer(config, network, mutableMapOf("user2" to "securePassword"))
    val client1 = ChatClient(config, network)
    val client2 = ChatClient(config, network)

    network.register(server)
    network.register(client1)
    network.register(client2)

    client1.connect(server.networkAddress)
    client2.connect(server.networkAddress)
    client1.authenticate("user2", "securePassword")
    client2.authenticate("user2", "securePassword")
    client1.send("HelloWorld!")
    client1.assertLastMessage(client1.networkAddress, "HelloWorld!")
    client2.assertLastMessage(client1.networkAddress, "HelloWorld!")
  }

  @Test
  fun testSendMessageOnlyToAuthenticatedClients() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, false, null, false)
    val server =
      ChatServer(config, network, mutableMapOf("user2" to "securePassword"))
    val client1 = ChatClient(config, network)
    val client2 = ChatClient(config, network)

    network.register(server)
    network.register(client1)
    network.register(client2)

    client1.connect(server.networkAddress)
    client2.connect(server.networkAddress)
    client1.authenticate("user2", "securePassword")
    client1.send("HelloWorld!")
    client1.assertLastMessage(client1.networkAddress, "HelloWorld!")
    assertEquals("", client2.view.lastDisplayedMessage)
  }

  @Test
  fun testMultipleUsers() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, false, null, false)
    val server = ChatServer(
      config,
      network,
      mutableMapOf("user1" to "password", "user2" to "securePassword")
    )
    val client1 = ChatClient(config, network)
    val client2 = ChatClient(config, network)

    network.register(server)
    network.register(client1)
    network.register(client2)

    client1.connect(server.networkAddress)
    client2.connect(server.networkAddress)
    client1.authenticate("user1", "password")
    client2.authenticate("user2", "securePassword")
    client1.send("HelloWorld!")
    client1.assertLastMessage(client1.networkAddress, "HelloWorld!")
    client2.assertLastMessage(client1.networkAddress, "HelloWorld!")
  }

  // ==============================================================================
  // Color tests
  // ==============================================================================
  @Test
  fun testDisplaysColoredMessage() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(false, true, null, false)
    val server = ChatServer(config, network)
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!", View.RED)
    client.assertLastMessage(client.networkAddress, "HelloWorld!", View.RED)
  }

  @Test
  fun testServerSendsUncoloredMessages() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, true, null, false)
    val server = ChatServer(config, network)
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!", View.YELLOW)
    client.assertLastMessage(
      server.networkAddress,
      "You must authenticate before sending messages."
    )
  }

  @Test
  fun testCanSendUncoloredMessages() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(false, true, null, false)
    val server = ChatServer(config, network)
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!")
    client.assertLastMessage(client.networkAddress, "HelloWorld!")
  }

  @Test
  fun testNoColorSupportThrowsOnColoredMessage() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(false, false, null, false)
    val server = ChatServer(config, network)
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    assertThrows<ConfigurationError> { client.send("HelloWorld!", View.BLUE) }
  }

  // ==============================================================================
  // Encryption tests
  // ==============================================================================
  @Test
  fun testDisplaysDecryptedMessageReverse() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(false, false, REVERSE, false)
    val server = ChatServer(config, network)
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!")
    client.assertLastMessage(client.networkAddress, "HelloWorld!")
  }

  @Test
  fun testDisplaysDecryptedMessageRot13() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(false, false, ROT13, false)
    val server = ChatServer(config, network)
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!")
    client.assertLastMessage(client.networkAddress, "HelloWorld!")
  }

  @Test
  fun testEncryptionWorksWithAuthentication() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, false, ROT13, false)
    val server =
      ChatServer(config, network, mapOf("user" to "CorrectHorseBatteryStaple"))
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.authenticate("user", "CorrectHorseBatteryStaple")
    client.send("HelloWorld!")
    client.assertLastMessage(client.networkAddress, "HelloWorld!")
  }

  @Test
  fun testEncryptionWorksWithColor() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(false, true, ROT13, false)
    val server = ChatServer(config, network)
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!", View.GREEN)
    client.assertLastMessage(client.networkAddress, "HelloWorld!", View.GREEN)
  }

  // ==============================================================================
  // Logging tests
  // ==============================================================================
  @Test
  fun testLogging() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, false, null, true)
    val server =
      ChatServer(config, network, mutableMapOf("user2" to "securePassword"))
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    server.assertLastLoggedMessage("New client: ${client.networkAddress}")

    client.send("Hello world!")
    server.assertLastLoggedMessage("Rejected message from unauthenticated client: ${client.networkAddress}")
    client.assertLastLoggedMessage("Received message from sender ${server.networkAddress}")
    client.assertLastMessage(
      server.networkAddress,
      "You must authenticate before sending messages."
    )

    client.authenticate("user1", "password")
    server.assertLastLoggedMessage("Failed to authenticate client: ${client.networkAddress}")
    client.assertLastMessage(server.networkAddress, "Authentication failed.")

    client.authenticate("user2", "securePassword")
    client.assertLastLoggedMessage("Sending authentication request: [${client.networkAddress}] u=user2 p=securePassword")
    server.assertLastLoggedMessage("Successfully authenticated client: ${client.networkAddress}")

    client.send("Hello world!")
    server.assertLastLoggedMessage("Broadcasting message from sender ${client.networkAddress}")
    client.assertLastLoggedMessage("Received message from sender ${client.networkAddress}")
    client.assertLastMessage(client.networkAddress, "Hello world!")
  }

  @Test
  fun testLoggingWithEncryption() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, false, REVERSE, true)
    val server =
      ChatServer(config, network, mutableMapOf("user2" to "securePassword"))
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    server.assertLastLoggedMessage("New client: ${client.networkAddress}")

    client.send("Hello world!")
    server.assertLastLoggedMessage("Rejected message from unauthenticated client: ${client.networkAddress}")
    client.assertLastLoggedMessage("Received message from sender ${server.networkAddress}")
    client.assertLastMessage(
      server.networkAddress,
      "You must authenticate before sending messages."
    )

    client.authenticate("user1", "password")
    server.assertLastLoggedMessage("Failed to authenticate client: ${client.networkAddress}")
    client.assertLastMessage(server.networkAddress, "Authentication failed.")

    client.authenticate("user2", "securePassword")
    client.assertLastLoggedMessage("Sending authentication request: [${client.networkAddress}] u=user2 p=securePassword")
    server.assertLastLoggedMessage("Successfully authenticated client: ${client.networkAddress}")

    client.send("Hello world!")
    server.assertLastLoggedMessage("Broadcasting message from sender ${client.networkAddress}")
    client.assertLastLoggedMessage("Received message from sender ${client.networkAddress}")
    client.assertLastMessage(client.networkAddress, "Hello world!")
  }

  @Test
  fun testLoggingWithColor() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(false, true, null, true)
    val server = ChatServer(config, network)
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    server.assertLastLoggedMessage("New client: ${client.networkAddress}")

    client.send("Hello world!", View.YELLOW)
    server.assertLastLoggedMessage("Broadcasting message from sender ${client.networkAddress}")
    client.assertLastLoggedMessage("Received message from sender ${client.networkAddress}")
    client.assertLastMessage(client.networkAddress, "Hello world!", View.YELLOW)
  }

  @Test
  fun testAllFeatures() {
    val network = NetworkSimulator<Message>()
    val config = ChatConfig(true, true, REVERSE, true)
    val server =
      ChatServer(config, network, mutableMapOf("user2" to "securePassword"))
    val client = ChatClient(config, network)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    server.assertLastLoggedMessage("New client: ${client.networkAddress}")

    client.send("Hello world!")
    server.assertLastLoggedMessage("Rejected message from unauthenticated client: ${client.networkAddress}")
    client.assertLastLoggedMessage("Received message from sender ${server.networkAddress}")
    client.assertLastMessage(
      server.networkAddress,
      "You must authenticate before sending messages."
    )

    client.authenticate("user1", "password")
    server.assertLastLoggedMessage("Failed to authenticate client: ${client.networkAddress}")
    client.assertLastMessage(server.networkAddress, "Authentication failed.")

    client.authenticate("user2", "securePassword")
    client.assertLastLoggedMessage("Sending authentication request: [${client.networkAddress}] u=user2 p=securePassword")
    server.assertLastLoggedMessage("Successfully authenticated client: ${client.networkAddress}")

    client.send("Hello world!", View.YELLOW)
    server.assertLastLoggedMessage("Broadcasting message from sender ${client.networkAddress}")
    client.assertLastLoggedMessage("Received message from sender ${client.networkAddress}")
    client.assertLastMessage(client.networkAddress, "Hello world!", View.YELLOW)
  }
}