package wavegen.presentation

import chisel3._
import chisel3.util._

class Slideshow(slideCount: Int = 16) extends Module {
	val io = IO(new Bundle {
		val slide = Input(UInt(log2Ceil(slideCount).W))
		val x     = Input(UInt(11.W))
		val y     = Input(UInt(10.W))
		val red   = Output(UInt(8.W))
		val green = Output(UInt(8.W))
		val blue  = Output(UInt(8.W))
	})

	val rom  = Module(new TextROM)
	val font = Module(new Font)

	val width  = 40
	val height = 22

	val scaleUp = 2

	val yOffset = (11 << scaleUp).U
	val x = io.x
	val y = io.y - yOffset

	// 3 bits because of characters being 8x8 pixels, 2 bits to scale up the text 4x in each dimension
	val cx = io.x >> (3 + scaleUp).U
	val cy = y >> (3 + scaleUp).U
	val char = rom.io.douta

	rom.io.clka := clock
	rom.io.addra := io.slide * (width * height).U + cy * width.U + cx
	font.io.char := char
	font.io.x := (io.x >> scaleUp.U)(2, 0) - 1.U // Why is the - 1 necessary?
	font.io.y := (y >> scaleUp.U)(2, 0)

	val color = Mux(yOffset <= io.y && font.io.out, 0.U(8.W), 255.U(8.W))
	io.red   := color
	io.green := color
	io.blue  := color
}
