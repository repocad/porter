/*
 * Copyright (c) 2012. Siigna is released under the creative common license by-nc-sa. You are free
 * to Share — to copy, distribute and transmit the work,
 * to Remix — to adapt the work
 *
 * Under the following conditions:
 * Attribution —  You must attribute the work to http://siigna.com in the manner specified by the author or licensor (but not in any way that suggests that they endorse you or your use of the work).
 * Noncommercial — You may not use this work for commercial purposes.
 * Share Alike — If you alter, transform, or build upon this work, you may distribute the resulting work only under the same or similar license to this one.
 */

package com.siigna.module.porter

import java.awt.{FileDialog, Frame}
import java.io.{FileOutputStream, BufferedWriter, FileWriter}
import com.siigna._
import com.siigna.module.porter.PDF._
import io.Codec

class Export {

  // The exporters that can export a given extension (string)
  protected var exporters : Map[String, Exporter] = Map()

  private var frameIsLoaded: Boolean = false

  def exporter(extension : String) = {
    try {
      val frame = new Frame()
      val dialog = new FileDialog(frame, "Export to file", FileDialog.SAVE)
      dialog.setVisible(true)

      val directory = dialog.getDirectory
      val filename = dialog.getFile

      if (extension == "pdf" ) {
        // Fetch the output stream
        val output = new FileOutputStream(directory + filename)
        val PDFdoc = new PDF // instantiate the PDF class

        val contents = PDFdoc.output(Some("datauri"))
        output.write(Codec.toUTF8(contents))

        // Flush and close
        output.flush()
        output.close()
      }

      dialog.dispose()
      frame.dispose()

      Siigna display "Export successful."

    } catch {
      case e => Siigna display "Export cancelled."
    }
  }


  /**
   * Adds a hook that uses the file-extension in the given Exporter to match known file types, so they
   * can be exported using the apply-method in the Exporter itself. Overrides any hooks defined on that
   * given extension.
   * @param exporter  The exporter containing the file extension and the algorithms to export a given file-type.
   */
  def addExporter(exporter : Exporter) {
    exporters = exporters + (exporter.extension.toLowerCase -> exporter)
  }

}