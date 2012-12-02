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

package com.siigna.module.porter.DXF

import com.siigna.module.Module
import com.siigna._
import app.Siigna
import java.awt.{FileDialog, Frame, Color}
//import module.io.dxf._
import java.io.{FileInputStream, File}

import java.io.{FileInputStream, File, InputStream}
import org.kabeja.dxf.DXFDocument
import org.kabeja.dxf.DXFLayer
import org.kabeja.dxf.DXFLine
import org.kabeja.dxf.DXFPolyline
//import org.kabeja.dxf.DXFVertex
//import org.kabeja.dxf.DXFConstants
//import org.kabeja.dxf.helpers.Point
import java.awt.FileDialog
import com.siigna._
//import sun.security.provider.certpath.Vertex
import scala.Some

//import org.kabeja.parser.DXFParseException
import org.kabeja.parser.Parser
import org.kabeja.parser.DXFParser
import org.kabeja.parser.ParserBuilder

/**
 * An import module using Kabeja 0.4 DXF import JAR library.
 * Kabeja version 0.4 is released under the apache 2.0 license.
 * See the Kabeja 0.4 licence for more information.
 */
class DXFImport extends Module{
  lazy val anthracite = new Color(0.25f, 0.25f, 0.25f, 1.00f)
  val color = "Color" -> "#AAAAAA".color

  val frame = new Frame
  var fileLength: Int = 0

  //graphics to show the loading progress
  def loadBar(point: Int): Shape = PolylineShape(Rectangle2D(Vector2D(103, 297), Vector2D(point + 103, 303))).setAttribute("raster" -> anthracite)
  def loadFrame: Shape = PolylineShape(Rectangle2D(Vector2D(100, 294), Vector2D(500, 306))).setAttribute(color)

  private var startTime: Option[Long] = None

  def stateMap = Map(
    'Start -> {
      case _ => {

        try {
          //opens a file dialog
          val dialog = new FileDialog(frame)
          dialog.setVisible(true)

          val fileName = dialog.getFile
          val fileDir = dialog.getDirectory
          val file = new File(fileDir + fileName)

          // Can we import the file-type?
          val extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase
          if (extension == "dxf") {
            //these two lines are needed to draw a loading bar
            startTime = Some(System.currentTimeMillis())
            //TODO: find the correct scaling factor to make loading bar fit large DXF files.
            fileLength = file.length().toInt * 4

            // Import!
            //todo: when a paper stack is implemented in Siigna, then import each layer to its own paper.
            val readDXF = new DXFExtractor

            //todo: find all layers in the DXF file generically and import them.
            readDXF.read(file, "0")
            readDXF.read(file, "default")
            readDXF.read(file, "Default")


            Siigna display "Loading completed."
          } else Siigna display "please select a .dxf file"

        } catch {
          case e => {
            Siigna display "Import cancelled."
          }
        }
        End
      }
    }
  )
  override def paint(g: Graphics, t: TransformationMatrix) {
    g draw loadFrame
    if (fileLength > 0 && ((System.currentTimeMillis() - startTime.get) / (fileLength / 30)) < 394) {
      g draw loadBar(((System.currentTimeMillis() - startTime.get) / (fileLength / 30)).toInt)
    } else if (fileLength > 0 && ((System.currentTimeMillis() - startTime.get) / (fileLength / 30)) > 394)
      g draw loadBar(390)

  }
}

class DXFExtractor{
  import scala.collection.immutable.List

  var points : List[Vector2D] = List()

  //a function to read a DXF file and create the shapes in it.
  def read(file : File, layerid : String) = {
    val input : InputStream = new FileInputStream(file)
    val parser : Parser = ParserBuilder.createDefaultParser()

    try {
      parser.parse(input, DXFParser.DEFAULT_ENCODING)//parse

      val doc : DXFDocument = parser.getDocument  //get the document and the layer
      val layers = doc.getDXFLayerIterator //get the layers in the DXF file
      //TODO: extract the layer name and give to the doc.getDXFLayer method
      while(layers.hasNext) {
        val l = layers.next()
        val entityList = List()
        val layer : DXFLayer = doc.getDXFLayer(layerid)

        //get extractable objects:
        val lines = layer.getDXFEntities("LINE")
        val mLines = layer.getDXFEntities("MLINE")
        val polylines = layer.getDXFEntities("POLYLINE")
        val LwPolylines = layer.getDXFEntities("LWPOLYLINE")

        println("A: "+lines)
        println("B: "+polylines)

        if (lines != null) println(lines)
        if (mLines != null) println(mLines)
        if (polylines != null) println(polylines)
        if (LwPolylines != null) println(LwPolylines)

        //todo: collect all possible types in a list here:
        //val entities = List(lines, mLines, polylines, LwPolylines)
        val entities = polylines

        entities.toArray.collect {
          case p : DXFPolyline => {
            var size = p.getVertexCount
            for (i <- 0 until size) {
              var point = (p.getVertex(i).getPoint)
              var vector = Vector2D(point.getX,point.getY)
              if (vector.length != 0) points = points :+ vector
            }
            Create(PolylineShape(points))
            points = List()
          }
          case p : DXFLine => {
            var line = LineShape(Vector2D(p.getStartPoint.getX,p.getStartPoint.getY),Vector2D(p.getEndPoint.getX,p.getEndPoint.getY))
            Create(line)
          }
        }
      }


      //var vertex : DXFVertex = line.getVertex(2)
      //iterate over all vertex of the polyline
      //for (i <- line) {
      //var vertex = line.getVertex(i)
      //}
    } catch {
      case e => {
      input.close()
      println("found error: "+ e)
      Nil
      }
    }
    input.close()
  }
}