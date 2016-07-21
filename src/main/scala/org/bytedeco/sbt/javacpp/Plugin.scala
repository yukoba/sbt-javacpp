package org.bytedeco.sbt.javacpp

import scala.language.postfixOps
import sbt._
import sbt.Keys._

object Plugin extends AutoPlugin {

  override def projectSettings: Seq[Setting[_]] = {
    import autoImport._
    Seq(
      autoCompilerPlugins := true,
      classpathTypes += "maven-plugin",
      javaCppPlatform := Platform.current,
      javaCppVersion := Versions.javaCppVersion,
      javaCppPresetLibs := Seq.empty,
      libraryDependencies <+= javaCppVersion { resolvedJavaCppVersion =>
        "org.bytedeco" % "javacpp" % resolvedJavaCppVersion
      },
      javaCppPresetDependencies
    )
  }

  object Versions {
    val javaCppVersion = "1.2.2"
  }

  object autoImport {
    val javaCppPlatform = SettingKey[Seq[String]]("javaCppPlatform", """The platform that you want to compile for (defaults to the platform of the current computer). You can also set this via the "sbt.javacpp.platform" System Property """)
    val javaCppVersion = SettingKey[String]("javaCppVersion", s"Version of Java CPP that you want to use, defaults to ${Versions.javaCppVersion}")
    val javaCppPresetLibs = SettingKey[Seq[(String, String)]]("javaCppPresetLibs", "List of additional JavaCPP presets that you would wish to bind lazily, defaults to an empty list")
  }

  override def requires: Plugins = plugins.JvmPlugin

  override def trigger: PluginTrigger = allRequirements

  private def javaCppPresetDependencies: Def.Setting[Seq[ModuleID]] = {
    import autoImport._
    libraryDependencies <++= (javaCppPlatform, javaCppVersion, javaCppPresetLibs) {
      (resolvedJavaCppPlatforms, resolvedJavaCppVersion, resolvedJavaCppPresetLibs) =>
        val majorMinorJavaCppVersion = majorMinorOnly(resolvedJavaCppVersion)
        resolvedJavaCppPresetLibs.flatMap {
          case (libName, libVersion) => {
            val generic = "org.bytedeco.javacpp-presets" % libName % s"$libVersion-$majorMinorJavaCppVersion" classifier ""
            val platformSpecific = resolvedJavaCppPlatforms.map { platform =>
              "org.bytedeco.javacpp-presets" % libName % s"$libVersion-$majorMinorJavaCppVersion" classifier platform
            }
            generic +: platformSpecific
          }
        }
    }
  }

  /**
   * Given a version string, simply drops the patch level and returns the major-minor version only
   * @param version
   */
  private def majorMinorOnly(version: String): String = {
    version.split('.').take(2).mkString(".")
  }

}