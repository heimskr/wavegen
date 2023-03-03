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

	val width  = 80
	val height = 45

	// 3 bits because of characters being 8x8 pixels, 1 bit to scale up the text 2x in each dimension
	val cx = io.x >> 4.U
	val cy = io.y >> 4.U
	val char = rom.io.douta

	rom.io.clka := clock
	rom.io.addra := io.slide * (width * height).U + cy * width.U + cx
	font.io.char := char
	font.io.x := (io.x >> 1.U)(2, 0)
	font.io.y := (io.y >> 1.U)(2, 0)

	val color = Mux(font.io.out, 0.U(8.W), 255.U(8.W))
	io.red   := color
	io.green := color
	io.blue  := color
}
