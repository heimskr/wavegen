package wavegen

import chisel3._
import chisel3.util._

class TableGen[T <: Generator](val gen: T, val period: Int, val resolution: Int) extends Module {
	require(1 < period)
	require(1 < resolution)
	val inWidth = log2Ceil(period + 1).W
	val outWidth = log2Ceil(resolution + 1).W

	val io = IO(new Bundle {
		val out = Output(UInt(outWidth))
	})

	val (counter, wrap) = Counter(true.B, period)

	val list = List.tabulate(period)(i => (gen(i.doubleValue / period) * resolution).toInt.U(outWidth))
	val data = RegInit(VecInit(list))

	io.out := data(counter)
}
