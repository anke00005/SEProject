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

data class TextMessage(override val sender: Int, val message: String, val color: String?) : Message {
    override fun encrypt(encryption: EncryptionMethod): Message {
        return TextMessage(sender, encryption.encrypt(message), color)
    }

    override fun decrypt(encryption: EncryptionMethod): Message {
        return TextMessage(sender, encryption.decrypt(message), color)
    }

}

data class ConnectionMessage(override val sender: Int) : Message {
    override fun encrypt(encryption: EncryptionMethod): Message {
        return this
    }

    override fun decrypt(encryption: EncryptionMethod): Message {
        return this
    }

}

data class AuthenticationMessage(override val sender: Int, val username: String, val password: String) : Message {
    override fun encrypt(encryption: EncryptionMethod): Message {
        return AuthenticationMessage(sender, encryption.encrypt(username), encryption.encrypt(password))
    }

    override fun decrypt(encryption: EncryptionMethod): Message {
        return AuthenticationMessage(sender, encryption.decrypt(username), encryption.decrypt(password))
    }
}

//==============================================================================
// Server
//==============================================================================
class ChatServer(
    private val config: ChatConfig,
    private val network: NetworkSimulator<Message>,
    private val registeredUsers: Map<String, String> = HashMap()
) : NetworkClient<Message> {
    override var networkAddress by Delegates.notNull<Int>()
    private val clients: MutableSet<Int> = mutableSetOf()
    private val unauthenticatedClients: MutableSet<Int> = mutableSetOf()
    val logger = Logger()

    override fun handleMessage(message: Message): Boolean {
        return when (message) {
            is ConnectionMessage -> connect(message)
            is AuthenticationMessage -> authentication(message)
            is TextMessage -> broadcast(message)
        }
    }

    private fun connect(message: ConnectionMessage): Boolean {
        if (config.logging) {
            logger.log("New client: ${message.sender}")
        }
        if (config.authentication) {
            unauthenticatedClients.add(message.sender)
        } else
            clients.add(message.sender)
        sendMessage(message.sender, ConnectionMessage(networkAddress))
        return true
    }

    private fun broadcast(message: TextMessage): Boolean {
        val sender = message.sender
        if (config.authentication) {
            if (!isAuthenticated(sender)) {
                if (config.logging) {
                    logger.log("Rejected message from unauthenticated client: ${message.sender}")
                }
                sendMessage(sender, TextMessage(networkAddress, "You must authenticate before sending messages.", null))
                return false
            }
        }
        if (config.logging) {
            logger.log("Broadcasting message from sender ${message.sender}")
        }
        clients.forEach { network.sendMessage(it, message) }
        return true
    }

    private fun authentication(message: AuthenticationMessage): Boolean {
        if (!config.authentication) {
            return false
        }
        val decryptedMessage =
            if (config.encryption != null) message.decrypt(config.encryption) as AuthenticationMessage else message
        val sender = decryptedMessage.sender
        val username = decryptedMessage.username
        val password = decryptedMessage.password
        if (registeredUsers[username] == password) {
            if (config.logging) {
                logger.log("Successfully authenticated client: $sender")
            }
            unauthenticatedClients.remove(sender)
            clients.add(sender)
            return true
        } else {
            if (config.logging) {
                logger.log("Failed to authenticate client: $sender")
            }
            sendMessage(sender, TextMessage(networkAddress, "Authentication failed.", null))
            return false
        }
    }

    private fun sendMessage(clientId: Int, message: Message) {
        if (config.encryption != null) {
            network.sendMessage(clientId, message.encrypt(config.encryption))
        } else {
            network.sendMessage(clientId, message)
        }
    }

    private fun isAuthenticated(clientID: Int): Boolean {
        return clients.contains(clientID)
    }
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
    private var authenticated = false
    private var serverId: Int? = null
    private val encryption = config.encryption

    override fun handleMessage(message: Message): Boolean {
        if (config.logging) {
            logger.log("Received message from sender ${message.sender}")
        }
        val decryptedMessage = if (encryption != null) message.decrypt(encryption) else message
        if (config.authentication && decryptedMessage is AuthenticationMessage) {
            if (serverId != null) {
                if (decryptedMessage == (serverId ?: -1111)) {
                    authenticated = true
                }
            }

        }
        when (decryptedMessage) {
            is ConnectionMessage -> {
                if (serverId == null) {
                    serverId = decryptedMessage.sender
                }
            }

            is TextMessage -> displayMessage(decryptedMessage)
            is AuthenticationMessage -> throw ConfigurationError()
        }
        return true
    }

    private fun displayMessage(message: TextMessage) {
        if (config.color) {
            if (message.color != null) {
                view.printMessage(message.sender, message.message, message.color)
            } else {
                view.printMessage(message.sender, message.message)
            }
        } else
            view.printMessage(message.sender, message.message)
    }

    fun connect(server: Int) {
        network.sendMessage(server, ConnectionMessage(networkAddress))
    }

    private fun sendMessage(message: Message) {
        val serverId = requireNotNull(serverId)

        if (config.encryption != null) {
            network.sendMessage(serverId, message.encrypt(config.encryption))
        } else {
            network.sendMessage(serverId, message)
        }
    }

    fun send(message: String) {
        val textMessage = TextMessage(networkAddress, message, null)
        if (config.logging) {
            logger.log("Sending message: $textMessage")
        }
        sendMessage(textMessage)
    }

    fun send(message: String, color: String) {
        if (config.color) {
            val textMessage = TextMessage(networkAddress, message, color)
            if (config.logging) {
                logger.log("Sending message: $textMessage")
            }
            sendMessage(textMessage)
        }else
            throw ConfigurationError()
    }

    fun authenticate(username: String, password: String){
        if (config.authentication){
            if (!authenticated){
                val authenticationMessage = AuthenticationMessage(networkAddress, username, password)
                if (config.logging){
                    logger.log("Sending authentication request: $authenticationMessage")
                }
                sendMessage(authenticationMessage)
            }
        }
    }
}

