
import util.NetworkClient
import util.NetworkSimulator
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

class MessageFactoryImpl : MessageFactory {
  override fun connectionMessage(sender: Int): Message {
    TODO("Not yet implemented")
  }

  override fun textMessage(sender: Int, message: String): Message {
    TODO("Not yet implemented")
  }

  override fun coloredTextMessage(sender: Int, message: String, color: String): Message {
    TODO("Not yet implemented")
  }

  override fun authenticationMessage(sender: Int, user: String, password: String): Message {
    TODO("Not yet implemented")
  }
}

abstract class MessageFactoryDecorator(private val messageFactory: MessageFactory): MessageFactory{
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
class EncryptingMessageFactory(private val  messageFactory: MessageFactory, private val encryption: EncryptionMethod) : MessageFactoryDecorator(messageFactory){
  override fun connectionMessage(sender: Int): Message {
    TODO("Not yet implemented")
  }

  override fun textMessage(sender: Int, message: String): Message {
    TODO("Not yet implemented")
  }

  override fun authenticationMessage(sender: Int, user: String, password: String): Message {
    TODO("Not yet implemented")
  }

}

class AuthenticatingMessageFactory(private val messageFactory: MessageFactory):MessageFactoryDecorator(messageFactory){

  override fun authenticationMessage(sender: Int, user: String, password: String): Message {
    TODO("Not yet implemented")
  }

}

class ColoringMessageFactory(private val messageFactory: MessageFactory ): MessageFactoryDecorator(messageFactory){
  override fun connectionMessage(sender: Int): Message {
    TODO("Not yet implemented")
  }

  override fun textMessage(sender: Int, message: String): Message {
    TODO("Not yet implemented")
  }

  override fun coloredTextMessage(sender: Int, message: String, color: String): Message {
    TODO("Not yet implemented")
  }

  override fun authenticationMessage(sender: Int, user: String, password: String): Message {
    TODO("Not yet implemented")
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

open class ChatServerImpl(override val messageFactory: MessageFactory, val network : NetworkSimulator<Message>) : ChatServer {
  override var networkAddress: Int = 0

  override fun handleMessage(message: Message): Boolean {

  }

}

abstract class ChatServerDecorator(private val chatServer: ChatServer): ChatServer{
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
  val chatServer: ChatServer): ChatServerDecorator(chatServer){

}

class EncryptingServer(val chatServer: ChatServer, val encryption: EncryptionMethod): ChatServerDecorator(chatServer){

}

class AuthenticatingServer(val chatServer: ChatServer, clients: MutableMap<String,String>): ChatServerDecorator(chatServer){

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

abstract class ChatClientDecorator(private val chatClient:ChatClient): ChatClient{
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

    override var networkAddress: Int
        get() = chatClient.networkAddress
        set(value) {
            chatClient.networkAddress = value
        }
}

class LoggingClient(private val chatClient: ChatClient): ChatClientDecorator(chatClient){

}

class EncryptingClient(private val chatClient: ChatClient, private val encryption:EncryptionMethod): ChatClientDecorator(chatClient){

}

class AuthenticatingClient(private val chatClient: ChatClient): ChatClientDecorator(chatClient){

}

class ColoringClient(private val chatClient: ChatClient): ChatClientDecorator(chatClient){

}



class ChatClientImpl(messageFactory: MessageFactory, network: NetworkSimulator<Message>) : ChatClient {
  override val view: View
    get() = TODO("Not yet implemented")
  override val messageFactory: MessageFactory
    get() = TODO("Not yet implemented")

  override fun handleMessage(message: Message): Boolean {
    TODO("Not yet implemented")
  }

  override fun connect(serverId: Int) {
    TODO("Not yet implemented")
  }

  override fun send(message: String) {
    TODO("Not yet implemented")
  }

  override fun send(message: String, color: String) {
    TODO("Not yet implemented")
  }

  override fun authenticate(username: String, password: String) {
    TODO("Not yet implemented")
  }

  override var networkAddress: Int
    get() = TODO("Not yet implemented")
    set(value) {}
}


// TODO: implement task b)
