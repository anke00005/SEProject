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

  private fun LoggingServer.assertLastLoggedMessage(message: String) {
    assertEquals(message, this.logger.lastLoggedMessage)
  }

  private fun LoggingClient.assertLastLoggedMessage(message: String) {
    assertEquals(message, this.logger.lastLoggedMessage)
  }

  //==============================================================================
  // Decorator order
  //==============================================================================
  @Test
  fun testDecoratorOrder() {
    val network = NetworkSimulator<Message>()
    val messageFactory = EncryptingMessageFactory(
      AuthenticatingMessageFactory(ColoringMessageFactory(MessageFactoryImpl())),
      ROT13
    )
    val server = LoggingServer(
      EncryptingServer(
        AuthenticatingServer(
          ChatServerImpl(messageFactory, network),
          mutableMapOf("user2" to "securePassword")
        ),
        ROT13
      )
    )
    val client = LoggingClient(
      EncryptingClient(
        AuthenticatingClient(
          ColoringClient(ChatClientImpl(messageFactory, network))
        ),
        ROT13
      )
    )

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
  }


  @Test
  fun testDisplaysSentMessage() {
    val network = NetworkSimulator<Message>()
    val messageFactory = MessageFactoryImpl()
    val server = ChatServerImpl(messageFactory, network)
    val client1 = ChatClientImpl(messageFactory, network)
    val client2 = ChatClientImpl(messageFactory, network)

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
    val messageFactory = MessageFactoryImpl()
    val server1 = ChatServerImpl(messageFactory, network)
    val server2 = ChatServerImpl(messageFactory, network)
    val client1 = ChatClientImpl(messageFactory, network)
    val client2 = ChatClientImpl(messageFactory, network)

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
    val messageFactory = AuthenticatingMessageFactory(MessageFactoryImpl())
    val server =
      AuthenticatingServer(
        ChatServerImpl(messageFactory, network),
        mutableMapOf("user2" to "securePassword")
      )
    val client = AuthenticatingClient(ChatClientImpl(messageFactory, network))

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
    val messageFactory = AuthenticatingMessageFactory(MessageFactoryImpl())
    val server = AuthenticatingServer(
      ChatServerImpl(messageFactory, network),
      mutableMapOf("user2" to "securePassword")
    )
    val client = AuthenticatingClient(ChatClientImpl(messageFactory, network))

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.authenticate("user1", "securePassword")
    client.assertLastMessage(server.networkAddress, "Authentication failed.")
  }

  @Test
  fun testSameUserMultipleClients() {
    val network = NetworkSimulator<Message>()
    val messageFactory = AuthenticatingMessageFactory(MessageFactoryImpl())
    val server = AuthenticatingServer(
      ChatServerImpl(messageFactory, network),
      mutableMapOf("user2" to "securePassword")
    )
    val client1 = AuthenticatingClient(ChatClientImpl(messageFactory, network))
    val client2 = AuthenticatingClient(ChatClientImpl(messageFactory, network))

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
    val messageFactory = AuthenticatingMessageFactory(MessageFactoryImpl())
    val server = AuthenticatingServer(
      ChatServerImpl(messageFactory, network),
      mutableMapOf("user2" to "securePassword")
    )
    val client1 = AuthenticatingClient(ChatClientImpl(messageFactory, network))
    val client2 = AuthenticatingClient(ChatClientImpl(messageFactory, network))

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
    val messageFactory = AuthenticatingMessageFactory(MessageFactoryImpl())
    val server = AuthenticatingServer(
      ChatServerImpl(messageFactory, network),
      mutableMapOf("user1" to "password", "user2" to "securePassword")
    )
    val client1 = AuthenticatingClient(ChatClientImpl(messageFactory, network))
    val client2 = AuthenticatingClient(ChatClientImpl(messageFactory, network))

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
    val messageFactory = ColoringMessageFactory(MessageFactoryImpl())
    val server = ChatServerImpl(messageFactory, network)
    val client = ColoringClient(ChatClientImpl(messageFactory, network))

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!", View.RED)
    client.assertLastMessage(client.networkAddress, "HelloWorld!", View.RED)
  }

  @Test
  fun testServerSendsUncoloredMessages() {
    val network = NetworkSimulator<Message>()
    val messageFactory = ColoringMessageFactory(MessageFactoryImpl())
    val server = AuthenticatingServer(ChatServerImpl(messageFactory, network))
    val client = AuthenticatingClient(
      ColoringClient(
        ChatClientImpl(
          messageFactory,
          network
        )
      )
    )

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
    val messageFactory = ColoringMessageFactory(MessageFactoryImpl())
    val server = ChatServerImpl(messageFactory, network)
    val client = ColoringClient(ChatClientImpl(messageFactory, network))

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!")
    client.assertLastMessage(client.networkAddress, "HelloWorld!")
  }

  @Test
  fun testNoColorSupportThrowsOnColoredMessage() {
    val network = NetworkSimulator<Message>()
    val messageFactory = MessageFactoryImpl()
    val server = ChatServerImpl(messageFactory, network)
    val client = ChatClientImpl(messageFactory, network)

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
    val messageFactory = EncryptingMessageFactory(MessageFactoryImpl(), REVERSE)
    val server =
      EncryptingServer(ChatServerImpl(messageFactory, network), REVERSE)
    val client =
      EncryptingClient(ChatClientImpl(messageFactory, network), REVERSE)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!")
    client.assertLastMessage(client.networkAddress, "HelloWorld!")
  }

  @Test
  fun testDisplaysDecryptedMessageRot13() {
    val network = NetworkSimulator<Message>()
    val messageFactory = EncryptingMessageFactory(MessageFactoryImpl(), ROT13)
    val server =
      EncryptingServer(ChatServerImpl(messageFactory, network), ROT13)
    val client =
      EncryptingClient(ChatClientImpl(messageFactory, network), ROT13)

    network.register(server)
    network.register(client)

    client.connect(server.networkAddress)
    client.send("HelloWorld!")
    client.assertLastMessage(client.networkAddress, "HelloWorld!")
  }

  @Test
  fun testEncryptionWorksWithAuthentication() {
    val network = NetworkSimulator<Message>()
    val messageFactory = EncryptingMessageFactory(
      AuthenticatingMessageFactory(MessageFactoryImpl()),
      ROT13
    )
    val server = EncryptingServer(
      AuthenticatingServer(
        ChatServerImpl(messageFactory, network),
        mapOf("user" to "CorrectHorseBatteryStaple")
      ), ROT13
    )
    val client =
      EncryptingClient(
        AuthenticatingClient(
          ChatClientImpl(
            messageFactory,
            network
          )
        ), ROT13
      )

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
    val messageFactory = EncryptingMessageFactory(
      ColoringMessageFactory(MessageFactoryImpl()),
      ROT13
    )
    val server =
      EncryptingServer(ChatServerImpl(messageFactory, network), ROT13)
    val client =
      EncryptingClient(
        ColoringClient(ChatClientImpl(messageFactory, network)),
        ROT13
      )

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
    val messageFactory = AuthenticatingMessageFactory(MessageFactoryImpl())
    val server = LoggingServer(
      AuthenticatingServer(
        ChatServerImpl(messageFactory, network),
        mutableMapOf("user2" to "securePassword")
      )
    )
    val client = LoggingClient(
      AuthenticatingClient(
        ChatClientImpl(
          messageFactory,
          network
        )
      )
    )

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
    val messageFactory = EncryptingMessageFactory(
      AuthenticatingMessageFactory(MessageFactoryImpl()),
      REVERSE
    )
    val server = LoggingServer(
      EncryptingServer(
        AuthenticatingServer(
          ChatServerImpl(messageFactory, network),
          mutableMapOf("user2" to "securePassword")
        ), REVERSE
      )
    )
    val client = LoggingClient(
      EncryptingClient(
        AuthenticatingClient(
          ChatClientImpl(messageFactory, network)
        ),
        REVERSE
      )
    )

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
    val messageFactory = ColoringMessageFactory(MessageFactoryImpl())
    val server = LoggingServer(ChatServerImpl(messageFactory, network))
    val client =
      LoggingClient(ColoringClient(ChatClientImpl(messageFactory, network)))

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
    val messageFactory = EncryptingMessageFactory(
      AuthenticatingMessageFactory(ColoringMessageFactory(MessageFactoryImpl())),
      REVERSE
    )
    val server = LoggingServer(
      EncryptingServer(
        AuthenticatingServer(
          ChatServerImpl(messageFactory, network),
          mutableMapOf("user2" to "securePassword")
        ),
        REVERSE
      )
    )
    val client = LoggingClient(
      EncryptingClient(
        AuthenticatingClient(
          ColoringClient(ChatClientImpl(messageFactory, network))
        ),
        REVERSE
      )
    )

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