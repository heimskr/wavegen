package wavegen

import chisel3._
import chisel3.util._

class TriangleGen(period: Int, resolution: Int, startHigh: Boolean = false) extends Module {
	require(1 < period)
	require(1 < resolution)
	val outWidth = log2Ceil(resolution + 1).W

	val io = IO(new Bundle {
		val pause = Input(Bool())
		val out = Output(UInt(outWidth))
	})


	val rising = RegInit((!startHigh).B)


}
