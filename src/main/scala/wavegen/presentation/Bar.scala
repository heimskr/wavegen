package wavegen.presentation

import chisel3._
import chisel3.util._
import wavegen.misc.ColorBundle

class Bar(xPos: Int, yPos: Int, width: Int, height: Int, stroke: Int, valueWidth: Int, strokeColor: (Int, Int, Int), fillColor: (Int, Int, Int), xWidth: Int = 11, yWidth: Int = 10) extends Module {
	val io = IO(new Bundle {
		val x     = Input(UInt(xWidth.W))
		val y     = Input(UInt(yWidth.W))
		val value = Input(UInt(valueWidth.W))
		val out   = Valid(ColorBundle())
	})

	io.out.valid := false.B
	io.out.bits  := DontCare

	val adjustedX = io.x - xPos.U
	val adjustedY = io.y - yPos.U

	// TODO: power-of-two optimizations

	when (adjustedX < width.U && adjustedY < height.U) {
		when (adjustedX < stroke.U || (width - stroke).U <= adjustedX || adjustedY < stroke.U || (height - stroke).U <= adjustedY) {
			io.out.valid := true.B
			io.out.bits.red   := strokeColor._1.U
			io.out.bits.green := strokeColor._2.U
			io.out.bits.blue  := strokeColor._3.U
		} .elsewhen (((adjustedX - stroke.U) << (valueWidth - 1).U) <= (io.value * (width - 2 * stroke).U)) { // TODO: disastrous for WNS?
			io.out.valid := true.B
			io.out.bits.red   := fillColor._1.U
			io.out.bits.green := fillColor._2.U
			io.out.bits.blue  := fillColor._3.U
		}
	}
}
