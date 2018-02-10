import sys.process._
import java.io.File
import java.util.Date
import java.text.SimpleDateFormat

object BlueWake {
  def checkArgs(args: Array[String]) {
    if (args.size != 2) {
      println("I need:")
      println("MAC address of the phone that wakes the PC")
      println("MAC address of the pc")
      System.exit(1)
    }
    if (!("whoami".!! contains "root")) {
      println("Run me as root.")
      System.exit(1)
    }
  }
  def main(args: Array[String]) {
    checkArgs(args);
    val phoneMac = args(0)
    val desktopMac = args(1)
    var state : String = "willWake"
    while (true) {
      println(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
      println(s"Checking whether $phoneMac is around")
      val available = (s"hcitool info $phoneMac" #> new File("/dev/null")).! == 0
      if (available) {
        println("Found!")
      } else {
        println("Not found :(")
      }

      (state, available) match {
        case ("willWake", true) => {
          println(s"Waking $desktopMac")
          (s"wakeonlan $desktopMac" #> new File("/dev/null")).!
          state = "leaveHouseBeforeWaking"
          println("Woken up, waiting for you to leave the house before another wake.")
        }
        case ("willWake", false) => {
          println("Not waking, waiting for you to come home!")
        }
        case ("leaveHouseBeforeWaking", true) => {
          println("Still at home, not touching.")
        }
        case ("leaveHouseBeforeWaking", false) => {
          println("Seems like nobody is home, will wake when you're back")
          state = "willWake"
        }
      }
      println("Sleepin for 5s.")
      Thread.sleep(5000)
      println("Back to loopin")
      println("")
    }
  }
}
