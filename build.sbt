val projDeps = chisel.dependencies(Seq(
  ("edu.berkeley.cs" %% "firrtl" % "1.0.+", "firrtl"),
  ("edu.berkeley.cs" %% "chisel3" % "3.1.+", "chisel3"),
  ("edu.berkeley.cs" %% "chisel-iotesters" % "1.2.+", "chisel-iotesters")
))

val dependentProjects = projDeps.projects

lazy val rself = (project in file("."))
  .settings(
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.11",
    publishLocal := {},
    publish := {},
    packagedArtifacts := Map.empty,
    libraryDependencies ++= projDeps.libraries
  )
  .dependsOn(dependentProjects.map(classpathDependency(_)): _*)
  .aggregate(dependentProjects: _*)
