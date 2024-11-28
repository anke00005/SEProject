import util.Logger
import util.NetworkClient
import util.NetworkSimulator
import util.View
import kotlin.properties.Delegates

/**
 * Interface for encryption methods.
 */
sealed interface EncryptionMethod {
  fun encrypt(str: String): String
  fun decrypt(str: String): String
}

/**
 * ROT13 "encryption".
 */
data object ROT13 : EncryptionMethod {
  // This allows writing "<Int> + <Char>" in the encrypt function below.
  operator fun Int.plus(c: Char): Int = this + c.code

  override fun encrypt(str: String): String =
    str.map { c ->
      when {
        (c >= 'A') && (c <= 'Z') ->
          ((c - 'A' + 13) % 26 + 'A').toChar()

        (c >= 'a') && (c <= 'z') ->
          ((c - 'a' + 13) % 26 + 'a').toChar()

        else -> c
      }
    }.joinToString("")

  override fun decrypt(str: String): String = encrypt(str)
}

/**
 * String-reversal "encryption".
 */
data object REVERSE : EncryptionMethod {
  override fun encrypt(str: String): String = str.reversed()

  override fun decrypt(str: String): String = encrypt(str)
}

//==============================================================================
// Messages
//==============================================================================
sealed interface Message {
  val sender: Int

  fun encrypt(encryption: EncryptionMethod): Message
  fun decrypt(encryption: EncryptionMethod): Message
}

data class ConnectionMessage(override val sender: Int) : Message {
  override fun encrypt(encryption: EncryptionMethod): ConnectionMessage = this
  override fun decrypt(encryption: EncryptionMethod): ConnectionMessage = this

  override fun toString(): String = "[$sender] Connecting to server."
}

data class AuthenticationMessage(
  override val sender: Int,
  val username: String,
  val password: String
) : Message {
  override fun encrypt(encryption: EncryptionMethod): AuthenticationMessage =
    AuthenticationMessage(
      sender,
      encryption.encrypt(username),
      encryption.encrypt(password)
    )

  override fun decrypt(encryption: EncryptionMethod): AuthenticationMessage =
    AuthenticationMessage(
      sender,
      encryption.decrypt(username),
      encryption.decrypt(password)
    )

  override fun toString(): String = "[$sender] u=$username p=$password"
}

data class TextMessage(
  override val sender: Int,
  val message: String,
  val color: String? = null
) : Message {
  override fun encrypt(encryption: EncryptionMethod): TextMessage =
    TextMessage(sender, encryption.encrypt(message), color)

  override fun decrypt(encryption: EncryptionMethod): TextMessage =
    TextMessage(sender, encryption.decrypt(message), color)

  override fun toString(): String = "[$sender] $message"
}

//==============================================================================
// Server
//==============================================================================
class ChatServer(
  private val network: NetworkSimulator<Message>,
  private val registeredUsers: Map<String, String> = HashMap()
) : NetworkClient<Message> {
  override var networkAddress by Delegates.notNull<Int>()
  val logger = Logger()

  private val clients: MutableSet<Int> = mutableSetOf()
  private val unauthenticatedClients: MutableSet<Int> = mutableSetOf()

#ifdef encryption
#ifdef ROT13
  private val encryption = ROT13
#endif
#ifdef REVERSE
  private val encryption = REVERSE
#endif
#endif

  override fun handleMessage(message: Message): Boolean {
    return when (message) {
      is ConnectionMessage -> connect(message)
      is AuthenticationMessage -> authenticate(message)
      is TextMessage -> broadcast(message)
    }
  }

  private fun connect(message: ConnectionMessage): Boolean {
#ifdef logging
      logger.log("New client: ${message.sender}")
#endif

#ifdef authentication
      unauthenticatedClients += message.sender
#else
      clients += message.sender
#endif

    sendMessage(message.sender, ConnectionMessage(networkAddress))
    return true
  }

  private fun broadcast(message: TextMessage): Boolean {
    val sender = message.sender

#ifdef authentication
      if (!isAuthenticated(sender)) {
#ifdef logging
          logger.log("Rejected message from unauthenticated client: ${message.sender}")
#endif

        sendMessage(
          sender,
          TextMessage(
            networkAddress,
            "You must authenticate before sending messages."
          )
        )
        return false
      }
#endif

#ifdef logging
      logger.log("Broadcasting message from sender ${message.sender}")
#endif

    clients.forEach { network.sendMessage(it, message) }
    return true
  }

#ifdef authentication
  private fun authenticate(message: AuthenticationMessage): Boolean {
    val (sender, username, password) =
#ifdef encryption
      message.decrypt(encryption)
#else
      message
#endif

    if (registeredUsers[username] == password) {
#ifdef logging
        logger.log("Successfully authenticated client: $sender")
#endif

      unauthenticatedClients.remove(sender)
      clients.add(sender)
      return true
    } else {
#ifdef logging
        logger.log("Failed to authenticate client: $sender")
#endif

      sendMessage(sender, TextMessage(networkAddress, "Authentication failed."))
      return false
    }
  }
#endif

  private fun isAuthenticated(clientId: Int): Boolean =
    clients.contains(clientId)

  private fun sendMessage(
    clientId: Int,
    message: Message
  ) {
#ifdef encryption
      network.sendMessage(clientId, message.encrypt(encryption))
#else
      network.sendMessage(clientId, message)
#endif
  }
}

//==============================================================================
// Client
//==============================================================================
class ChatClient(
  private val network: NetworkSimulator<Message>
) : NetworkClient<Message> {
  val view: View = View()
  val logger: Logger = Logger()
  override var networkAddress by Delegates.notNull<Int>()

  private var serverId: Int? = null
  private var isAuthenticated = false

#ifdef encryption
#ifdef ROT13
  private val encryption = ROT13
#endif
#ifdef REVERSE
  private val encryption = REVERSE
#endif
#endif

  override fun handleMessage(message: Message): Boolean {
#ifdef logging
      logger.log("Received message from sender ${message.sender}")
#endif

    val decryptedMessage =
#ifdef encryption
      message.decrypt(encryption)
#else
      message
#endif

    when (decryptedMessage) {
      is ConnectionMessage -> serverId = decryptedMessage.sender
#ifdef authentication
      is AuthenticationMessage ->
          if (decryptedMessage.sender == serverId) {
            isAuthenticated = true
          }
#endif
      is TextMessage -> displayMessage(decryptedMessage)
    }
    return true
  }

  private fun displayMessage(message: TextMessage) {
#ifdef color
      if (message.color != null) {
        view.printMessage(message.sender, message.message, message.color)
      } else {
        view.printMessage(message.sender, message.message)
      }
#else
      view.printMessage(message.sender, message.message)
#endif
  }

  fun connect(serverId: Int) {
    network.sendMessage(serverId, ConnectionMessage(networkAddress))
  }

  fun send(message: String) {
    val textMessage = TextMessage(networkAddress, message)

#ifdef logging
      logger.log("Sending message: $textMessage")
#endif

    sendMessage(textMessage)
  }

#ifdef color
  fun send(message: String, color: String) {
    val textMessage = TextMessage(networkAddress, message, color)

#ifdef logging
      logger.log("Sending message: $textMessage")
#endif

    sendMessage(textMessage)
  }
#endif

#ifdef authentication
  fun authenticate(username: String, password: String) {
    if (!isAuthenticated) {
      val authenticationMessage =
        AuthenticationMessage(networkAddress, username, password)

#ifdef logging
        logger.log(
          "Sending authentication request: $authenticationMessage"
        )
#endif

      sendMessage(authenticationMessage)
    }
  }
#endif

  private fun sendMessage(
    message: Message
  ) {
    val serverId = requireNotNull(serverId)

#ifdef encryption
      network.sendMessage(serverId, message.encrypt(encryption))
#else
      network.sendMessage(serverId, message)
#endif
  }
}

