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

package com.siigna.module.porter.PDF

import com.siigna.module.porter.PDF.contents.OBJsections._
import sun.misc.BASE64Encoder

//TODO: implement lines, rectangles, arcs, and circles generation as done in https://github.com/MrRio/jsPDF/blob/master/jspdf.js
//adapted from the jsPDF project.
class PDF {

  var defaultPageFormat = "a4"

  var k = 1.0 // Scale factor

  def pageFormats = { // Size in mm of various paper formats
    val a3 = (841.89,1190.55)
    val a4 = (595.28, 841.89)
    val a5 = (420.94, 595.28)
    val letter = (612, 792)
    val legal = (612, 1008)
  }

  var textColor = "0 g"
  var unit = "mm" // Default to mm for units
  var fontSize = 12 // Default font size
  var pageFontSize = 12

  // Initilisation
  if (unit == "pt") {
    k = 1
  } else if(unit == "mm") {
    k = 72/25.4
  } else if(unit == "cm") {
    k = 72/2.54
  } else if(unit == "in") {
    k = 72
  }

  //kører ikke!
  def endDocument = {
    println("running endDocument")
    state = 1
    //putHeader
    putPages

    putResources
    //Info
    newObject
    out("<<")
    putInfo
    out(">>")
    out("endobj")

    //Catalog
    newObject
    out("<<")
    putCatalog
    out(">>")
    out("endobj")

    //Cross-ref
    var o = buffer.length
    out("xref")
    out("0 " + (objectNumber + 1))
    out("0000000000 65535 f ")
    for (i <- 0 to objectNumber) {
      out(format("%010d 00000 n ", offsets(i)))
    }
    //Trailer
    out("trailer")
    out("<<")
    putTrailer
    out(">>")
    out("startxref")
    //out(o) //type should be string?
    out("o")
    out("%%EOF")
    state = 3
  }

  val beginPage = {
    page += 1
    // Do dimension stuff
    state = 2
    //pages(page) = ""
    //pages.update(page, _)

    // TODO: Hardcoded at A4 and portrait
    pageHeight = 841 / k
    pageWidth = 596 / k
  }

  var _addPage = {
    beginPage
    // Set line width
    out(format("%.2f w", (lineWidth * k)))

    //TODO:set font.
    // 12 is the font size
    pageFontSize = fontSize
    out("BT /F1 " + fontSize + ".00 Tf ET")
  }

  // Add the first page automatically
  _addPage


  def addPage = {
    _addPage
  }

  //add text to the PDF document: (x : position, y : position, text string)
  def text(x : Int, y : Int, text : String) = {
    println("running text")
    // need page height
    if(pageFontSize != fontSize) {
      out("BT /F1 " + fontSize + ".00 Tf ET")
      pageFontSize = fontSize
    }
    var str = format("BT %.2f %.2f Td (%s) Tj ET", x * k, (pageHeight - y) * k, pdfEscape(text))
    println("text: got string: "+str)
    out(str)
  }

  //ADD IMAGES HERE
  //addImage: function(imageData, format, x, y, w, h) {

  def output(inputType : Option[String]) = {
    endDocument
    println("endDocument: "+endDocument)
    if(inputType == None) {
      buffer
    }
    if(inputType == Some("datauri")) {
      var encodedBuffer =new BASE64Encoder().encode(buffer.toString.getBytes())
      var document = "c:/siigna;base64," + encodedBuffer
      document
    }
    //TODO: Add different output options
  }
  def setFontSize(size : Int) =  {
    fontSize = size
  }
}

