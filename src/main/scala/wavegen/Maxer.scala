package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._

class Maxer(inputCount: Int, width: Int) extends Module {
	val io = IO(new Bundle {
		val in = Input(Vec(inputCount, UInt(width.W)))
		val out = UInt(width.W)
	})

	io.out := io.in.reduceTree((a, b) => Mux(a > b, a, b))
}
