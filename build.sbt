
sourceGenerators in Compile <+= 
 (sourceManaged in Compile, version, name) map {
  (dir, version, projectname) =>
  val file = dir / "fr" / "janalyse" / "primesui" / "MetaInfo.scala"
  val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val buildate = sdf.format(new java.util.Date())
  IO.write(file,
  """package fr.janalyse.primesui
    |object MetaInfo { 
    |  val version="%s"
    |  val project="%s"
    |  val buildate="%s"
    |}
    |""".stripMargin.format(version, projectname, buildate) )
  Seq(file)
}
