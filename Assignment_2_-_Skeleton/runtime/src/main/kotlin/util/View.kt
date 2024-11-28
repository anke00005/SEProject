package util

/**
 * Simple view class that simulates a console-based user interface and allows
 * easy access to printed messages for testing purposes.
 */
class View {
  private val history = mutableListOf<String>()


  fun printMessage(sender: Int, message: String) {
    history += "[$sender] $message"
  }

  fun printMessage(sender: Int, message: String, color: String) {
    history += "$RESET$color[$sender] $message$RESET"
  }

  val lastDisplayedMessage: String
    get() = history.lastOrNull() ?: ""

  companion object {
    const val RESET  = "\u001b[0m"
    const val BLACK  = "\u001b[30m"
    const val RED    = "\u001b[31m"
    const val GREEN  = "\u001b[32m"
    const val YELLOW = "\u001b[33m"
    const val BLUE   = "\u001b[34m"
  }
}