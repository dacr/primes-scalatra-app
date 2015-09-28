
sourceGenerators in Compile <+= 
 (sourceManaged in Compile, version, name) map {
  (dir, version, projectname) =>
  val file = dir / "fr" / "janalyse" / "primesui" / "MetaInfo.scala"
  IO.write(file,
  """package fr.janalyse.primesui
    |object MetaInfo { 
    |  val version="%s"
    |  val project="%s"
    |}
    |""".stripMargin.format(version, projectname) )
  Seq(file)
}
