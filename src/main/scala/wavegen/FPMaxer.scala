package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chisel3.internal.firrtl.BinaryPoint

class FPMaxer(inputCount: Int, fp: FixedPoint) extends Module {
	val io = IO(new Bundle {
		val in = Input(Vec(inputCount, fp))
		val out = Output(fp)
	})

	io.out := io.in.reduceTree((a, b) => Mux(a > b, a, b))
}
