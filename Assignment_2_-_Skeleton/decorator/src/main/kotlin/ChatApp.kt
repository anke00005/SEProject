import util.Logger
import util.NetworkClient
import util.NetworkSimulator
import util.View
import kotlin.properties.Delegates

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
    override fun toString(): String = "[$sender] $message"
}

data class ConnectionMessage(override val sender: Int) : Message {
    override fun encrypt(encryption: EncryptionMethod): Message {
        return this
    }

    override fun decrypt(encryption: EncryptionMethod): Message {
        return this
    }
    override fun toString(): String = "[$sender] Connecting to server."

}

data class AuthenticationMessage(override val sender: Int, val username: String, val password: String) : Message {
    override fun encrypt(encryption: EncryptionMethod): Message {
        return AuthenticationMessage(sender, encryption.encrypt(username), encryption.encrypt(password))
    }

    override fun decrypt(encryption: EncryptionMethod): Message {
        return AuthenticationMessage(sender, encryption.decrypt(username), encryption.decrypt(password))
    }
    override fun toString(): String = "[$sender] u=$username p=$password"
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

class MessageFactoryImpl : MessageFactory {
    override fun connectionMessage(sender: Int): Message {
        return connectionMessage(sender)
    }

    override fun textMessage(sender: Int, message: String): Message {
        return TextMessage(sender, message,null)
    }

    override fun coloredTextMessage(sender: Int, message: String, color: String): Message {
        //Might need to throw configuration error
        return TextMessage(sender, message, color)
    }

    override fun authenticationMessage(sender: Int, user: String, password: String): Message {
        //Might need to throw configuration error
        return AuthenticationMessage(sender, user, password)
    }
}

abstract class MessageFactoryDecorator(private val messageFactory: MessageFactory) : MessageFactory {
    override fun connectionMessage(sender: Int): Message {
        return messageFactory.connectionMessage(sender)
    }

    override fun textMessage(sender: Int, message: String): Message {
        return messageFactory.textMessage(sender, message)
    }

    override fun coloredTextMessage(sender: Int, message: String, color: String): Message {
        return messageFactory.coloredTextMessage(sender, message, color)
    }

    override fun authenticationMessage(sender: Int, user: String, password: String): Message {
        return messageFactory.authenticationMessage(sender, user, password)
    }


}

class EncryptingMessageFactory(private val messageFactory: MessageFactory, private val encryption: EncryptionMethod) :
    MessageFactoryDecorator(messageFactory) {
    override fun connectionMessage(sender: Int): Message {
        return messageFactory.connectionMessage(sender).encrypt(encryption)
    }

    override fun textMessage(sender: Int, message: String): Message {
        return messageFactory.textMessage(sender, message).encrypt(encryption)
    }

    override fun coloredTextMessage(sender: Int, message: String, color: String): Message {
        return messageFactory.coloredTextMessage(sender, message, color).encrypt(encryption)
    }

    override fun authenticationMessage(sender: Int, user: String, password: String): Message {
        return messageFactory.authenticationMessage(sender, user, password).encrypt(encryption)
    }
}

class AuthenticatingMessageFactory(private val messageFactory: MessageFactory) :
    MessageFactoryDecorator(messageFactory) {

    override fun authenticationMessage(sender: Int, user: String, password: String): Message {
        return messageFactory.authenticationMessage(sender, user, password)
    }

}

class ColoringMessageFactory(private val messageFactory: MessageFactory) : MessageFactoryDecorator(messageFactory) {
    override fun connectionMessage(sender: Int): Message {
        throw ConfigurationError()
    }

    override fun textMessage(sender: Int, message: String): Message {
        throw ConfigurationError()
    }

    override fun coloredTextMessage(sender: Int, message: String, color: String): Message {
        return messageFactory.coloredTextMessage(sender, message, color)
    }

    override fun authenticationMessage(sender: Int, user: String, password: String): Message {
        throw ConfigurationError()
    }

}


// TODO: implement task b)

//==============================================================================
// Server
//==============================================================================
interface ChatServer : NetworkClient<Message> {
    override fun handleMessage(message: Message): Boolean
    val messageFactory: MessageFactory
}

open class ChatServerImpl(override val messageFactory: MessageFactory, val network: NetworkSimulator<Message>) :
    ChatServer {
    override var networkAddress by Delegates.notNull<Int>()

    override fun handleMessage(message: Message): Boolean {
        val sender = message.sender
        network.sendMessage(sender, message)
        return true
    }

}

abstract class ChatServerDecorator(private val chatServer: ChatServer) : ChatServer {
    override val messageFactory: MessageFactory
        get() = chatServer.messageFactory

    override fun handleMessage(message: Message): Boolean {
        return chatServer.handleMessage(message)
    }

    override var networkAddress: Int
        get() = chatServer.networkAddress
        set(value) {
            chatServer.networkAddress = value
        }
}

class LoggingServer(
    val chatServer: ChatServer
) : ChatServerDecorator(chatServer) {
    val logger = Logger()
}

class EncryptingServer(val chatServer: ChatServer, val encryption: EncryptionMethod) : ChatServerDecorator(chatServer) {
    override fun handleMessage(message: Message): Boolean {
        val decryptedMessage = message.decrypt(encryption)
        return chatServer.handleMessage(decryptedMessage)
    }
}

class AuthenticatingServer(val chatServer: ChatServer, clients: Map<String, String>) :
    ChatServerDecorator(chatServer) {
    private val registeredUsers: Map<String, String> = HashMap()
    private val unauthenticatedClients: MutableSet<Int> = mutableSetOf()

    override fun handleMessage(message: Message): Boolean {
        if (message is AuthenticationMessage) {
            val sender = message.sender
            val username = message.username
            val password = message.password
            if (registeredUsers[username] == password){
                unauthenticatedClients.remove(sender)
            }
        }
        return true
    }

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

abstract class ChatClientDecorator(private val chatClient: ChatClient) : ChatClient {
    override val view: View
        get() = chatClient.view
    override val messageFactory: MessageFactory
        get() = chatClient.messageFactory

    override fun handleMessage(message: Message): Boolean {
        return chatClient.handleMessage(message)
    }

    override fun connect(serverId: Int) {
        chatClient.connect(serverId)
    }

    override fun send(message: String) {
        chatClient.send(message)
    }

    override fun send(message: String, color: String) {
        chatClient.send(message, color)
    }

    override fun authenticate(username: String, password: String) {
        chatClient.authenticate(username, password)
    }

    override var networkAddress by Delegates.notNull<Int>()

}

class LoggingClient(private val chatClient: ChatClient) : ChatClientDecorator(chatClient) {
    val logger = Logger()

    override fun handleMessage(message: Message) : Boolean{
        logger.log("Received message from sender ${message.sender}")
        return chatClient.handleMessage(message)
    }
}

class EncryptingClient(private val chatClient: ChatClient, private val encryption: EncryptionMethod) :
    ChatClientDecorator(chatClient) {
    override fun handleMessage(message: Message): Boolean {
        val decryptedMessage = message.decrypt(encryption)
        return chatClient.handleMessage(decryptedMessage)
    }
}

class AuthenticatingClient(private val chatClient: ChatClient) : ChatClientDecorator(chatClient) {
    val isAuthenticated = false
    override fun authenticate(username: String, password: String) {
        if (!isAuthenticated){
            chatClient.authenticate(username, password)
        }
    }

}

class ColoringClient(private val chatClient: ChatClient) : ChatClientDecorator(chatClient) {

}


class ChatClientImpl(override val messageFactory: MessageFactory, val network: NetworkSimulator<Message>) : ChatClient {
    override val view: View = View()
    val logger = Logger()
    val serverId :Int? = null
    val authenticated = false
    override var networkAddress by Delegates.notNull<Int>()

    override fun handleMessage(message: Message): Boolean {
        TODO("Not yet implemented")
    }

    override fun connect(serverId: Int) {
        network.sendMessage(serverId, messageFactory.connectionMessage(networkAddress))

    }

    override fun send(message: String) {
        val textMessage = messageFactory.textMessage(networkAddress, message)
        network.sendMessage(serverId!!, textMessage)
    }

    override fun send(message: String, color: String) {
        val textMessage = messageFactory.coloredTextMessage(networkAddress, message, color)
        network.sendMessage(serverId!!, textMessage)
    }

    override fun authenticate(username: String, password: String) {
        val authMessage = messageFactory.authenticationMessage(networkAddress, username, password)
        network.sendMessage(serverId!!, authMessage)
    }

}


// TODO: implement task b)
