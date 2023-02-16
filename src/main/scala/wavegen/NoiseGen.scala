package wavegen

import chisel3._
import chisel3.util._
import chisel3.util.random._
import java.util.ArrayList
import scala.jdk.CollectionConverters._

/** Resolution should ideally be a power of two. */
class NoiseGen(resolution: Int) extends Module {
	require(1 < resolution)

	val power = log2Ceil(resolution)

	def fibonacciSet: Set[Int] = {
		val numbers = new ArrayList[Int]()
		numbers.add(0)
		var one = 0
		var two = 1
		while (two < power) {
			numbers.add(two)
			val temp = two
			two = one + two
			one = temp
		}
		numbers.asScala.toSet
	}

	val io = IO(new Bundle {
		val out = Output(UInt(power.W))
	})

	// val lfsr = Module(new GaloisLFSR(power, fibonacciSet, Some(42)))
	// // lfsr.io.seed.bits := 42.U(power.W).asBools
	// lfsr.io.seed.bits := DontCare
	// lfsr.io.seed.valid := false.B
	// lfsr.io.increment := true.B

	// if ((resolution & (resolution - 1)) == 0) {
	// 	// Power of two: no modulo necessary
	// 	io.out := lfsr.io.out.asUInt(power - 1, 0)
	// } else {
	// 	io.out := lfsr.io.out.asUInt % resolution.U
	// }

	// val lfsr = Module(new FibonacciLFSR(15, Set(0, 6)))
	// lfsr.io.seed.bits := DontCare
	// lfsr.io.seed.valid := false.B
	// lfsr.io.increment := true.B

	val (counter, wrap) = Counter(0 until 50)

	val lfsr = FibonacciLFSR(15, Set(1, 7), wrap)

	io.out := Mux(lfsr(0), (resolution/2-1).U(power.W), (-resolution/2).S(power.W).asUInt)
}
