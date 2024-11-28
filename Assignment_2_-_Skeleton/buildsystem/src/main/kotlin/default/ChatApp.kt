import util.NetworkClient
import util.NetworkSimulator
import util.View
import kotlin.properties.Delegates

// Example skeleton for default configuration (all features are deselected)

//==============================================================================
// Server
//==============================================================================
class ChatServer(
  private val network: NetworkSimulator<Message>
) : NetworkClient<Message> {
  override var networkAddress by Delegates.notNull<Int>()

  // TODO: implement task c)
}

//==============================================================================
// Client
//==============================================================================
class ChatClient(
  private val network: NetworkSimulator<Message>
) : NetworkClient<Message> {
  val view: View = View()
  override var networkAddress by Delegates.notNull<Int>()

  // TODO: implement task c)
}

