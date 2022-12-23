package seamcarving

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*


val loadSaveImage = LoadSaveImage()

fun main(args: Array<String>) {
    val argsData = parseArgs(args)
    val picture = Picture(argsData.loadedImage)
    picture.replacePictureWithEnergy()
    loadSaveImage.saveImagePng(picture.imageIn, File(argsData.saveImagePath))
}

fun parseArgs(args: Array<String>): ParcedArgsData {
    var inArg = BufferedImage(1, 1, 1)
    var outArg = ""
    args.forEachIndexed { index, s ->
        when (s) {
            "-in" -> inArg = loadSaveImage.loadImage(args[index + 1])
            "-out" -> outArg = args[index + 1]
        }
    }
    return ParcedArgsData(inArg, outArg)
}

data class ParcedArgsData(val loadedImage: BufferedImage, val saveImagePath: String)
enum class RGB(val colorFunction: Color.() -> Int) { RED({ this.red }), GREEN({ this.green }), BLUE({ this.blue }) }

class Picture(val imageIn: BufferedImage) {
    private val height = imageIn.height
    private val width = imageIn.width
    private var maxPixelEnergy = 0.0
    private val energyMatrix = List(imageIn.width) { MutableList(imageIn.height) { 0.0 } }

    fun replacePictureWithEnergy() {

        imageIn.doForEveryPixel { posX, posY, _ ->
            val pixelEnergy = calculateEnergyPixel(posX, posY)
            maxPixelEnergy = max(pixelEnergy, maxPixelEnergy)
            energyMatrix[posX][posY] = pixelEnergy
        }
        imageIn.doForEveryPixel { posX, posY, _ ->
            val intensity = (255.0 * energyMatrix[posX][posY] / maxPixelEnergy).toInt()
            imageIn.setRGB(posX, posY, Color(intensity, intensity, intensity).rgb)
        }
    }
    private fun calculateEnergyPixel(posX: Int, posY: Int): Double {
        getEnergyPositions(posX, posY).apply {
            return calculateEnergyFormula(
                leftPixelX = energyPositionMapToColor("x1"),
                rightPixelX = energyPositionMapToColor("x2"),
                abovePixelY = energyPositionMapToColor("y1"),
                belowPixelY = energyPositionMapToColor("y2")
            )
        }
    }

    private fun Map<String, Pair<Int, Int>>.energyPositionMapToColor(coordKey: String): Color {
        try {
            return Color(imageIn.getRGB(this[coordKey]!!.first, this[coordKey]!!.second))
        } catch (e: Exception) {
            println(e.toString())
        }
        return Color(0)
    }
    private fun calculateEnergyFormula(
        leftPixelX: Color,
        rightPixelX: Color,
        abovePixelY: Color,
        belowPixelY: Color
    ): Double {
        val xGradient = gradientCalculation(leftPixelX, rightPixelX, RGB.RED, RGB.GREEN, RGB.BLUE)
        val yGradient = gradientCalculation(abovePixelY, belowPixelY, RGB.RED, RGB.GREEN, RGB.BLUE)
        return sqrt(xGradient + yGradient)
    }

    private fun gradientCalculation(firstPixel: Color, secondPixel: Color, vararg color: RGB): Double {
        var sum = 0.0
        color.forEach {
            sum += minusPixelToDoublePow(firstPixel, secondPixel, it)
        }
        return sum
    }

    private fun minusPixelToDoublePow(firstPixel: Color, secondPixel: Color, color: RGB): Double {
        return (abs(
            firstPixel.getPixelColorEasy(color).toDouble() - secondPixel.getPixelColorEasy(color).toDouble()
        )).pow(2)
    }

    private fun Color.getPixelColorEasy(color: RGB): Int {
        val colorFunction = color.colorFunction
        return this.colorFunction()
    }

    private fun getEnergyPositions(posX: Int, posY: Int): Map<String, Pair<Int, Int>> {
        getEnergyPosPair(posX, posY).apply {
            return mapOf(
                "x1" to Pair(first + 1, posY),//  left pair around x pixel
                "x2" to Pair(first - 1, posY),// right pair around x pixel
                "y1" to Pair(posX, second + 1),// top pair around y pixel
                "y2" to Pair(posX, second - 1)// bottom pair around y pixel
            )
        }

    }

    //get our position of pixel depends on border or not
    private fun getEnergyPosPair(posX: Int, posY: Int): Pair<Int, Int> {
        val xNew = if (posX == 0) 1 else if (posX == width - 1) posX - 1 else posX
        val yNew = if (posY == 0) 1 else if (posY == height - 1) posY - 1 else posY
        return Pair(xNew, yNew)
    }

    fun doNegative() {
        imageIn.doForEveryPixel { posX, posY, color ->
            val newColor = Color(255 - color.red, 255 - color.green, 255 - color.blue)
            imageIn.setRGB(posX, posY, newColor.rgb)
        }
    }

    fun drawTwoLinesAndRect(width: Int, height: Int) {
        imageIn.createGraphics().drawOneRectangleTwoDioganalsLines(width, height)
    }
}


fun BufferedImage.doForEveryPixel(action: (posX: Int, posY: Int, color: Color) -> Unit) {
    for (x in this.minX until this.width) {
        for (y in this.minY until this.height) {
            action(x, y, Color(this.getRGB(x, y)))
        }
    }
}

fun Graphics2D.drawOneRectangleTwoDioganalsLines(width: Int, height: Int) {
    this.apply {
        color = Color.RED
        drawLine(0, 0, width - 1, height - 1)
        drawLine(width - 1, 0, 0, height - 1)
    }
}

class Util {
    fun readlnTexted(text: String): String {
        println(text)
        return readln()
    }
}

class LoadSaveImage {
    fun loadImage(path: String): BufferedImage {
        return ImageIO.read(File(path))
    }

    fun saveImagePng(image: BufferedImage, imageFile: File) {
        ImageIO.write(image, "png", imageFile)
    }
}