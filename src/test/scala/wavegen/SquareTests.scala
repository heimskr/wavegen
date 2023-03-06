package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec

class SquareTests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq = 100_000_000

	behavior of "SquareGen"
	it should "generate squares" in {
		test(new SquareGen(16, 3)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.clock.setTimeout(0)
			dut.io.freq.poke(10000000)
			dut.io.max.poke(10000)
			dut.io.wave.poke("b110".U)
			dut.io.pause.poke(false)
			for (i <- 0 until 1000) {
				dut.clock.step()
			}
		}
	}
}
