

sourceGenerators in Compile +=  Def.task {
  val dir = (sourceManaged in Compile).value
  val projectVersion = version.value
  val projectName = name.value
  val file = dir / "fr" / "janalyse" / "primesui" / "MetaInfo.scala"
  val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val buildate = sdf.format(new java.util.Date())
  IO.write(file,
  """package fr.janalyse.primesui
    |object MetaInfo {
    |  val version="%s"
    |  val project="%s"
    |  val buildate="%s"
    |  val appcode="primesui"
    |}
    |""".stripMargin.format(projectVersion, projectName, buildate) )
  Seq(file)
}.taskValue

