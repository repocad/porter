package com.siigna.module.porter.PDF


import java.io.OutputStream
import java.awt.Color
import com.siigna.app.model.Drawing
import com.siigna.util.geom.TransformationMatrix
import com.itextpdf.text.{PageSize, Document}
import com.itextpdf.text.pdf.{BaseFont, PdfContentByte, PdfWriter}
import com.siigna.app.Siigna
import com.siigna.util.geom.Vector2D
import com.siigna._
import com.siigna.app.model.shape.PolylineShape
import com.siigna.app.model.shape.CircleShape
import com.siigna.app.model.shape.TextShape
import com.siigna.app.model.shape.ArcShape
import com.siigna.app.model.shape.RectangleShape
import com.siigna.app.model.shape.LineShape

/**
 * Exports the current drawing in PDF format.
 * For help and other examples; see http://itextpdf.com/book/examples.php
 */

class PDFExporter extends (OutputStream => Unit) {
  var mm = 72/25.4

  //get the paper size and orientaion
  lazy val orientation = {
    println("pagesize: "+pageSize._1)
    if (pageSize._1 == 0) {
      if(pageSize._2) PageSize.A0.rotate()
      else PageSize.A0
    }
    else if (pageSize._1 == 1) {
      if(pageSize._2) PageSize.A1.rotate()
      else PageSize.A1
    }
    else if (pageSize._1 == 2) {
      if(pageSize._2) {
        PageSize.A2.rotate()
      }
      else {
        PageSize.A2
      }
    }
    else if (pageSize._1 == 3) {
      if(pageSize._2) PageSize.A3.rotate()
      else PageSize.A3
    }
    //default: A4
    else {
      if(pageSize._2) PageSize.A4.rotate()
      else PageSize.A4
    }
  }

  def apply(out : OutputStream) {
    println("orientation in apply;: "+orientation)
    val document = new Document(orientation)
    val writer = PdfWriter.getInstance(document, out)
    document.open()

    // The basic font. Initialized here to make sure the document has been opened
    lazy val bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED)

    val canvas = writer.getDirectContent
    writeHeader(canvas, bf)

    shapesEvaluation

    document.close()
    def shapesEvaluation = {
      if(Drawing.shapes.size != 0) {
        val shapes = Drawing.shapes.map (t => (t._2))

        shapes.foreach(f = s => {
          //println("\t" + s)
          s match {
            case c: CircleShape => {
              //println(c.attributes.toString())
              canvas.saveState()
              val center = rePos(c.center)
              val reposRadius = c.radius * (72 / 25.4) / Siigna.paperScale
              canvas.setLineWidth(width(c.attributes))
              canvas.circle(center.x.toFloat, center.y.toFloat, reposRadius.toFloat)
              val col = color(c.attributes)
              canvas.setRGBColorStroke(col.getRed, col.getGreen, col.getBlue)
              canvas.stroke()
              canvas.restoreState()
            }
            case l: LineShape => {
              canvas.saveState()
              val p1 = rePos(l.p1)
              val p2 = rePos(l.p2)
              //print(s"width for line $l: ${width(l.attributes)}")
              canvas.setLineWidth(width(l.attributes))
              canvas.moveTo(p1.x.toFloat, p1.y.toFloat)
              canvas.lineTo(p2.x.toFloat, p2.y.toFloat)

              val col = color(l.attributes)
              canvas.setRGBColorStroke(col.getRed, col.getGreen, col.getBlue)
              canvas.stroke()
              canvas.restoreState()
              // line(rePos(l.p1),rePos(l.p2),l.attributes)
            }
            //takes care of both the open and the closed polylineshapes
            case o: PolylineShape => {
              val s = rePos(o.startPoint)
              //println(s"PolyLine start x:$s.x y:$s.y")
              canvas.saveState()
              canvas.moveTo(s.x.toFloat, s.y.toFloat)
              for (i <- o.innerShapes) {
                val p = rePos(i.point)
                //print(s" end: $p")
                canvas.lineTo(p.x.toFloat, p.y.toFloat)
              }
              if (o.isInstanceOf[PolylineShape.PolylineShapeClosed]) {
                canvas.lineTo(s.x.toFloat, s.y.toFloat)
              }
              canvas.setLineWidth(width(o.attributes))
              val col = color(o.attributes)
              canvas.setRGBColorStroke(col.getRed, col.getGreen, col.getBlue)
              canvas.stroke()
              canvas.restoreState()
            }
            case t: TextShape => {

              val content = t.text
              val pos = rePos(t.position)
              val size = (t.fontSize*10).toInt
              writeText(canvas, bf, content, size, pos.x.toFloat, pos.y.toFloat)
            }
            case a: ArcShape => {
              val circle = Circle2D(a.center, a.radius)
              val box = circle.boundary
              val start = rePos(Vector2D(box.xMin, box.yMin))
              val end = rePos(Vector2D(box.xMax, box.yMax))
              val x1 = start.x.toFloat
              val y1 = start.y.toFloat
              val x2 = end.x.toFloat
              val y2 = end.y.toFloat
              val angle = a.geometry.startAngle.toFloat
              val endAngle = a.geometry.angle.toFloat
              canvas.saveState()
              canvas.arc(x1, y1, x2, y2, angle, endAngle)
              canvas.setLineWidth(width(a.attributes))
              val col = color(a.attributes)
              canvas.setRGBColorStroke(col.getRed, col.getGreen, col.getBlue)
              canvas.stroke()
              canvas.restoreState()
            }
            case r: RectangleShape => {

              r.geometry.segments
              for (seg <- r.geometry.segments) {
                canvas.saveState()
                val p1 = rePos(seg.p1)
                val p2 = rePos(seg.p2)
                canvas.moveTo(p1.x.toFloat, p1.y.toFloat)
                canvas.lineTo(p2.x.toFloat, p2.y.toFloat)
                canvas.setLineWidth(width(r.attributes))
                val col = color(r.attributes)
                canvas.setRGBColorStroke(col.getRed, col.getGreen, col.getBlue)
                canvas.stroke()
                canvas.restoreState()
              }
            }
            case e => println("no match on shapes: " + shapes)
          }
        }
        )
      } else {
        println("no lines in the drawing")
        None
      }

    }
    //CLEAR THE DOCUMENT
    //document = null
    //writer = null
    //canvas = null
  }
  def writeText(canvas:PdfContentByte, bf : BaseFont, text:String, size:Int, x:Float, y:Float){
    canvas.saveState()
    canvas.beginText()
    canvas.moveText(x, y)
    canvas.setFontAndSize(bf, size)
    canvas.showText(text)
    canvas.endText()
    canvas.restoreState()
  }
  def extension = "pdf"

  //parse the drawing content from drwing coordinates to the coordinate system of the PDF
  def rePos(v : Vector2D) = {
    //find the bounding box of the drawing
    Drawing.calculateBoundary() //get the current boundary
    val box = Drawing.calculateBoundary().center
    val landscape = pageSize._2
    //transform all objects so that the center point of the bounding box matches that of the PDF.
    val paperCenter = {
      val cX = orientation.getWidth/2
      val cY = orientation.getHeight/2
      Vector2D(cX, cY)

      //if (landscape) Vector2D(420.5, 298.5) else Vector2D(298.5,420.5)
    }
    //move the center of the shapes to 0,0, then scale them to millimeters. (PDF is in inches per default)
    val t = TransformationMatrix(-box,1) //move to 0,0
    val t2 = TransformationMatrix(Vector2D(0,0),mm / Siigna.paperScale) //scale to mm and divide by the paper scale.
    val t3 = TransformationMatrix(paperCenter,1) // move the shapes to the center of the paper
    val transformed = v.transform(t)  //perform the first transformation
    val transformed2 = transformed.transform(t2) //perform the second.
    val transformed3 = transformed2.transform(t3) //perform the third.
    transformed3
  }
  //get the current paper size
  def pageSize : (Int, Boolean) = {
    val isLandscape : Boolean = Drawing.boundary.width > Drawing.boundary.height
    val a = Siigna.double("printFormatMin").get.toInt

    //A3
    if(a == 296) {
      if(isLandscape) (3, true) else (3, false)
    }
    //A2
    else if(a == 420) {
      if(isLandscape) (2, true) else (2, false)
    }
    //A1
    else if(a == 593) {
      if(isLandscape) (1, true) else (1, false)
    }
    //A0
    else if(a == 840) {
      if(isLandscape) (0, true) else (0, false)
    }
    //A4
    else {
      if(isLandscape) (4, true) else (4, false)
    }
  }
  //NOTE by OEP: multiplied with 2.54 to get the width in inches.
  def width(a : Attributes):Float= a.double("StrokeWidth").getOrElse(0.2d).toFloat*2.54f

  //add a drawing header
  def writeHeader (canvas : PdfContentByte, bf : BaseFont) {
    val scaleText = "SCALE: 1: "+ Siigna.paperScale
    val infoText ="created @ www.siigna.com - free online 2D CAD drawing and library"
    val yPos = 18
    val xPosScale=(orientation.getWidth/1.2).toFloat
    val xPosInfo =orientation.getWidth/20
    val textSize = 8

    writeText(canvas,bf, scaleText,textSize,xPosScale,yPos)
    writeText(canvas,bf, infoText,textSize,xPosInfo,yPos)
  }
  //add colors to exported shapes
  def color(a : Attributes):Color ={
    a.color("Color").getOrElse(Color.BLACK)
  }

}
