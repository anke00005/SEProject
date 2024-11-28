package util

/** Interface used by the network simulator to transmit messages.
 * Must be implemented by all ChatServers and ChatClients.
 *
 * @tparam MessageTy the types of messages the client can handle.
 */
interface NetworkClient<MessageTy> {
  var networkAddress: Int

  /**
   * Handle a message.
   *
   * @param message The received message.
   * @return whether the message was handled successfully or not.
   */
  fun handleMessage(message: MessageTy): Boolean
}

/** This class simulates a network that can be used by clients and servers to
 * communicate with each other.
 *
 * This is used instead of real network communication to simplify
 * implementation and testing of this assignment.
 */
class NetworkSimulator<MessageTy> {
  private var idCounter = 0;
  private val clients = mutableMapOf<Int, NetworkClient<MessageTy>>()

  /** Register a client with the network.
   * The returned id acts as a network address.
   */
  fun register(client: NetworkClient<MessageTy>): Int {
    val clientId = idCounter++
    client.networkAddress = clientId
    clients[clientId] = client
    return clientId
  }

  fun sendMessage(receiver: Int, message: MessageTy) {
    clients[receiver]?.handleMessage(message)
      ?: throw IllegalArgumentException("Unknown receiver: $receiver")
  }
}
