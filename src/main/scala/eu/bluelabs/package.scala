package eu

package object bluelabs {
  object Log {
    def success(text: String) = println(
      s"[${Console.GREEN}SUCCESS${Console.RESET}] $text"
    )

    def warn(text: String) = println(
      s"[${Console.YELLOW}WARN${Console.RESET}] $text"
    )

    def error(text: String) = println(
      s"[${Console.RED}ERROR${Console.RESET}] $text"
    )

    def info(text: String) = println(
      s"[${Console.BLUE}INFO${Console.RESET}] $text"
    )

  }

  /**
    * utility function for safely executing side effect functions
    * with no special error handling required other then logging the error
    *
    * @param code
    */
  def tryOrLog[T](code: => T): Unit = {
    try code
    catch {
      case err: Throwable => Log.error(err.getMessage)
    }
  }
}
