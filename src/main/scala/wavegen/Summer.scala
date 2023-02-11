package wavegen

import chisel3._
import chisel3.util._
import scala.collection.immutable.ListMap

class Summer(inputCount: Int, width: Int) extends Module {
	val bigger = width + log2Ceil(inputCount)

	val io = IO(new Bundle {
		val in = Input(Vec(inputCount, UInt(width.W)))
		val out = UInt(bigger.W)
	})

	io.out := VecInit(io.in.map(x => Cat(0.U((bigger - width).W), x))).reduceTree((a, b) => a + b)
}
