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

package com.siigna.module

import com.siigna._

/**
 * a class for an Input-output dialog, if it is needed
 */

class ModuleInit extends Module {
  def stateMap = Map(
    'Start -> {
      case MouseDown(p, MouseButtonLeft, _) :: tail => {
        Siigna display "opening dxf import"
        Start('DXFImport, "com.siigna.module.porter.DXF")
      }
      case MouseDown(p, MouseButtonRight, _) :: tail => {
        Siigna display ("testing pdf export")
        Start('PDFExport, "com.siigna.module.porter.PDF")
      }
      case _ =>
    }
  )
}
