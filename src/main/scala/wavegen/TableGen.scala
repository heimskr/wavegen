package wavegen

import chisel3._
import chisel3.util._

class TableGen[T <: Generator](gen: T, period: Int, resolution: Int) extends Module {
	require(1 < period)
	require(1 < resolution)
	val outWidth = log2Ceil(resolution + 1).W

	val io = IO(new Bundle {
		val pause = Input(Bool())
		val out = Output(UInt(outWidth))
	})

	val (counter, wrap) = Counter(!io.pause, period)

	val list = List.tabulate(period)(i => (gen(i.doubleValue / period) * resolution).toInt.U(outWidth))
	val data = RegInit(VecInit(list))

	io.out := data(counter)
}
