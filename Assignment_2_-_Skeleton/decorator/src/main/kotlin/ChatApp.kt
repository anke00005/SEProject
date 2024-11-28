import util.NetworkClient
import util.View

/**
 * Exception that is thrown when functionality is accessed that is not
 * available for the current configuration.
 */
class ConfigurationError : RuntimeException()


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
/**
 * Interface for all message types.
 */
sealed interface Message {
  val sender: Int

  fun encrypt(encryption: EncryptionMethod): Message
  fun decrypt(encryption: EncryptionMethod): Message
}

// TODO: implement task b)

/**
 * Factory for creating messages.
 *
 * You will need this to correctly implement encryption.
 * An instance of message factory is passed to the base server and client
 * implementations.
 */
interface MessageFactory {
  fun connectionMessage(sender: Int): Message
  fun textMessage(sender: Int, message: String): Message
  fun coloredTextMessage(sender: Int, message: String, color: String): Message
  fun authenticationMessage(
    sender: Int,
    user: String,
    password: String
  ): Message
}

// TODO: implement task b)

//==============================================================================
// Server
//==============================================================================
interface ChatServer : NetworkClient<Message> {
  override fun handleMessage(message: Message): Boolean
  val messageFactory: MessageFactory
}

// TODO: implement task b)

//==============================================================================
// Client
//==============================================================================
interface ChatClient : NetworkClient<Message> {
  val view: View
  val messageFactory: MessageFactory
  override fun handleMessage(message: Message): Boolean

  fun connect(serverId: Int)
  fun send(message: String)
  fun send(message: String, color: String)
  fun authenticate(username: String, password: String)
}

// TODO: implement task b)
