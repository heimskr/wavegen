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

	val lfsr = Module(new GaloisLFSR(power, fibonacciSet, Some(42)))
	// lfsr.io.seed.bits := 42.U(power.W).asBools
	lfsr.io.seed.bits := DontCare
	lfsr.io.seed.valid := false.B
	lfsr.io.increment := true.B

	if ((resolution & (resolution - 1)) == 0) {
		// Power of two: no modulo necessary
		io.out := lfsr.io.out.asUInt(power - 1, 0)
	} else {
		io.out := lfsr.io.out.asUInt % resolution.U
	}
}
