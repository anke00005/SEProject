package util

/**
 * Simple logger class that allows easy access to logged messages for testing.
 */
class Logger {
  private val log = mutableListOf<String>()

  fun log(message: String) {
    log += message
  }

  val lastLoggedMessage: String
    get() = log.lastOrNull() ?: ""
}