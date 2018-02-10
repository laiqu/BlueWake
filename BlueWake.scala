import sys.process._
import java.io.File
import java.util.Date
import java.text.SimpleDateFormat

object BlueWake {
  def checkArgs(args: Array[String]) {
    if (args.size != 3) {
      println("I need:")
      println("MAC address of the bt device that wakes the PC")
      println("MAC address of the wifi device that wakes the PC")
      println("MAC address of the pc")
      println("Note: I only wake when bt & wifi devices are available at the same time.")
      System.exit(1)
    }
    if (!("whoami".!! contains "root")) {
      println("Run me as root.")
      System.exit(1)
    }
  }

  def checkBluetoothPresence(btMac: String) : Boolean = {
    return (s"hcitool info $btMac" #> new File("/dev/null")).! == 0
  }

  def checkWiFiPresence(wifiMac: String) : Boolean = {
    return (s"sudo nmap -sP 192.168.1.0/24").!! contains wifiMac;
  }

  def main(args: Array[String]) {
    checkArgs(args);
    val btMac = args(0)
    val wifiMac = args(1)
    val desktopMac = args(2)
    var state : String = "willWake"
    while (true) {
      println(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
      println(s"Checking whether $btMac (bluetooth) and $wifiMac (wifi) are available.")
      val available = Map(
        "BlueTooth" -> checkBluetoothPresence(btMac),
        "WiFi" ->  checkWiFiPresence(wifiMac)
      )
      available map {
              case (device, availability) => println(
              s"  $device is" + (if (availability) " " else " not ") + "available")
      }
      val requiredDevicesArePresent = available.values reduce (_ && _)
      val atLeastOneDeviceIsPresent = available.values reduce (_ || _)

      (state, requiredDevicesArePresent, atLeastOneDeviceIsPresent) match {
        case ("willWake", true, _) => {
          println(s"Waking $desktopMac")
          (s"wakeonlan $desktopMac" #> new File("/dev/null")).!
          state = "leaveHouseBeforeWaking"
          println("Woken up, waiting for you to leave the house before another wake.")
        }
        case ("willWake", false, _) => {
          println("Not waking, waiting for you to come home!")
        }
        case ("leaveHouseBeforeWaking", _, true) => {
          println("Still at home, not touching.")
        }
        case ("leaveHouseBeforeWaking", _, false) => {
          println("Seems like nobody is home, will wake when you're back")
          state = "willWake"
        }
        case (_, _, _) => {
          // Just to kill the warning, should not happen?
          println("wat?")
        }
      }
      println("Sleepin for 5s.")
      Thread.sleep(5000)
      println("Back to loopin")
      println("")
    }
  }
}
