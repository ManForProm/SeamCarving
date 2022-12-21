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

enum class RGB(val color: Color.() -> Int) { RED({ this.red }), GREEN({ this.green }), BLUE({ this.blue }) }

class Picture(val imageIn: BufferedImage) {
    private val height = imageIn.height
    private val width = imageIn.width

//    private val loadeSave = LoadSaveImage()
//    fun createNewPicture(pathSave:String){
//        imageIn.findEnergy()
//        loadeSave.saveImagePng(imageIn,File(pathSave))
//    }

    fun replacePictureWithEnergy() {
        val maxPixelEnergy = imageIn.findMaxEnergy()
        imageIn.doForEveryPixel { posX, posY, color ->
            val pixelEnergy = calculateEnergy(posX, posY)
            val intensity = (255.0 * pixelEnergy / maxPixelEnergy).toInt()
            imageIn.setRGB(posX, posY, Color(intensity, intensity, intensity).rgb)
        }
    }

    private fun BufferedImage.findMaxEnergy(): Double {
        var maxPixelEnergy = 0.0
        this.doForEveryPixel { posX, posY, color ->
            val pixelEnergy = calculateEnergy(posX, posY)
            if (pixelEnergy > maxPixelEnergy) maxPixelEnergy = pixelEnergy
        }
        return maxPixelEnergy
    }

    private fun calculateEnergy(posX: Int, posY: Int): Double {
        getEnergyPositions(getEnergyPosPair(posX, posY, height - 1, width - 1)).apply {
            return calculateEnergyFormula(
                leftPixelX = mapToColor("x1"),
                rightPixelX = mapToColor("x2"),
                abovePixelY = mapToColor("y1"),
                belowPixelY = mapToColor("y2")
            )
        }
    }

    private fun Map<String, Pair<Int, Int>>.mapToColor(coordKey: String): Color {
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

    private fun minusPixelToDoublePow(firstPixel: Color, secondPixel: Color, color: RGB) =
//            (firstPixel.getPixelColorEasy(color) - secondPixel.getPixelColorEasy(color)).toDouble().pow(2)
        when (color) {
            RGB.RED -> (secondPixel.red - firstPixel.red).toDouble().pow(2)
            RGB.BLUE -> (secondPixel.blue - firstPixel.blue).toDouble().pow(2)
            RGB.GREEN -> (secondPixel.green - firstPixel.green).toDouble().pow(2)
        }

    private fun Color.getPixelColorEasy(color: RGB): Int {
        val colorFunction = color.color
        return this.colorFunction()
    }

    private fun getEnergyPositions(map: Map<String, Pair<Int, Int>>): Map<String, Pair<Int, Int>> {
        return mapOf(
            "x1" to Pair(map.getXPairX() - 1, map.getYPairX()),//  left pair around x pixel
            "x2" to Pair(map.getXPairX() + 1, map.getYPairX()),// right pair around x pixel
            "y1" to Pair(map.getXPairY(), map.getYPairY() - 1),// top pair around y pixel
            "y2" to Pair(map.getXPairY(), map.getYPairY() + 1)// bottom pair around y pixel
        )
    }

    fun Map<String, Pair<Int, Int>>.getXPairX() = this.get("x1")!!.first
    fun Map<String, Pair<Int, Int>>.getYPairX() = this.get("x1")!!.second
    fun Map<String, Pair<Int, Int>>.getXPairY() = this.get("y1")!!.first
    fun Map<String, Pair<Int, Int>>.getYPairY() = this.get("y1")!!.second

    //get our position of pixel depends on border or not
    private fun getEnergyPosPair(posX: Int, posY: Int,height: Int,width: Int): Map<String, Pair<Int, Int>> {
        if (posX == 0 && posY > 0 && posY != height) { // x left border y not top and not bottom
            return positionToMap(posX + 1, posY, posX, posY)
        } else if (posY == 0 && posX > 0 && posX != width) { // y top x not left not right
            return positionToMap(posX, posY, posX, posY + 1)
        } else if (posX == 0 && posY == 0) { // x left y top
            return positionToMap(posX + 1, posY + 1, posX + 1, posY + 1)
        } else if (posX == width && posY != height && posY != 0) {// x right and y not top and not bottom
            return positionToMap(posX - 1, posY, posX, posY)
        } else if (posY == height && posX != width && posX != 0) { // y bottom and x not left right
            return positionToMap(posX, posY, posX, posY - 1)
        } else if (posX == width && posY == height) { //x right y bottom
            return positionToMap(posX - 1, posY - 1, posX - 1, posY - 1)
        } else if (posX == 0 && posY == height) {// x left y bottom
            return positionToMap(posX + 1, posY - 1, posX + 1, posY - 1)
        } else if (posX == width && posY == 0) { //x right y top
            return positionToMap(posX - 1, posY + 1, posX - 1, posY + 1)
        } else return positionToMap(posX, posY, posX, posY) // x not left right y not top bottom
    }

    private fun positionToMap(xPosX: Int, xPosY: Int, yPosX: Int, yPosY: Int) =
//        mapOf("x1" to pairX,"y1" to pairY)
        mapOf("x1" to Pair(xPosX, xPosY), "y1" to Pair(yPosX, yPosY))

    fun BufferedImage.doNegative() {
        this.doForEveryPixel { posX, posY, color ->
            val newColor = Color(255 - color.red, 255 - color.green, 255 - color.blue)
            this.setRGB(posX, posY, newColor.rgb)
        }
    }

    fun drawTwoLinesAndRect(width: Int, height: Int) {
        imageIn.createGraphics().drawOneRectangleTwoDioganalsLines(width, height)
    }
}


fun BufferedImage.doForEveryPixel(action: (posX: Int, posY: Int, color: Color) -> Unit) {
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {
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