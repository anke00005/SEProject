import util.Logger
import util.NetworkClient
import util.NetworkSimulator
import util.View
import kotlin.properties.Delegates

/**
 * Configuration class.
 */
data class ChatConfig(
  val authentication: Boolean,
  val color: Boolean,
  val encryption: EncryptionMethod?,
  val logging: Boolean
)

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

// TODO: implement task a)

//==============================================================================
// Server
//==============================================================================
class ChatServer(
  private val config: ChatConfig,
  private val network: NetworkSimulator<Message>,
  private val registeredUsers: Map<String, String> = HashMap()
) : NetworkClient<Message> {
  override var networkAddress by Delegates.notNull<Int>()
  val logger = Logger()

  // TODO: implement task a)
}

//==============================================================================
// Client
//==============================================================================
class ChatClient(
  private val config: ChatConfig,
  private val network: NetworkSimulator<Message>
) : NetworkClient<Message> {
  val view: View = View()
  val logger: Logger = Logger()
  override var networkAddress by Delegates.notNull<Int>()

  // TODO: implement task a)
}

