package fr.janalyse.primesui

import scala.util.{Try, Success}

trait SysInfo {

  private def fromProps:Map[String,String] = {
    import collection.JavaConversions._
    val res = List(
      "^os[.].*",
      "^java[.]vm[.].*",
      "^java[.]vendor[.].*",
      "^java[.]runtime[.].*",
      "^java[.]version$",
      "^java[.]class[.]version$",
      "^user[.]timezone$",
      "^user[.]country$",
      "^user[.]language$").map(_.r)
    val props = System.getProperties.toMap
    props.filter {
      case (k, v) =>
        res.exists(_.findFirstIn(k).isDefined)
    }
  }

  private def fromJmx:Map[String,String] = {
    import java.lang.management.ManagementFactory
    import javax.management.ObjectName
    val mbs = ManagementFactory.getPlatformMBeanServer
    def get(on: ObjectName, attr: String, toKey: String): Option[Tuple2[String, String]] = {
      Try(mbs.getAttribute(on, attr)) match {
        case Success(value)=> Some(toKey -> value.toString) 
        case _ => None
      }
    }
    val os = ManagementFactory.getOperatingSystemMXBean()
    val osObjectName = new javax.management.ObjectName("java.lang:type=OperatingSystem")
    val rt = ManagementFactory.getRuntimeMXBean()
    Map(
      "extra.sysinfo" -> "enabled",
      "os.availableProcessors" -> java.lang.Runtime.getRuntime.availableProcessors.toString()) ++
      get(osObjectName, "TotalPhysicalMemorySize", "os.totalMemory") ++
      get(osObjectName, "TotalSwapSpaceSize", "os.totalSwap") ++
      get(osObjectName, "MaxFileDescriptorCount", "user.maxFileDescriptorCount")
  }

  private def fromExternalProcesses:Map[String,String] = {
    import scala.sys.process._
    def exec(command:String, toKey:String):Option[Tuple2[String,String]] = {
      Try(command.!!) match {
        case Success(value) => Some(toKey -> value.trim)
        case _ => None
      }
    }
    Map.empty[String,String] ++ exec("uname -a", "os.uname")
  }
  
  
  val sysinfoProps = fromProps ++ fromJmx ++ fromExternalProcesses
}
